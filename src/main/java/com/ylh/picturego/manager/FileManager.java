package com.ylh.picturego.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.ylh.picturego.config.CosClientConfig;
import com.ylh.picturego.exception.BusinessException;
import com.ylh.picturego.exception.ErrorCode;
import com.ylh.picturego.exception.ThrowUtils;
import com.ylh.picturego.model.dto.file.UploadResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@Deprecated
public class FileManager {

    @Resource
    private CosManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;

    private final List<String> ALLOWED_FORMATS = Arrays.asList("png", "jpg", "jpeg", "gif", "webp");
    private final long ONE_M = 1024 * 1024;

    /**
     * 上传本地图片到对象存储
     * @param multipartFile 文件
     * @param uploadPathPrefix 上传路径前缀
     * @return 上传结果
     */
    public UploadResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        // 校验图片
        validPicture(multipartFile);
        // 自定义上传的文件名，避免冲突增加安全性
        String uuid = RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename();
        String uploadFilename = String.format("%s_%s.%s",
                DateUtil.formatDate(new Date()),
                uuid,
                FileUtil.getSuffix(originalFilename)
                );
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        // 解析结果
        File tempFile = null;
        try {
            // 保存临时文件的路径和后缀（null）
            tempFile = File.createTempFile(uploadPath, null);
            // 保存文件到临时路径
            multipartFile.transferTo(tempFile);
            // 获取图片上传后的信息
            PutObjectResult putObjectResult = cosManager.uploadPicture(uploadPath, tempFile);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 封装返回结果
            double picScale = NumberUtil.round(imageInfo.getWidth() * 1.0 / imageInfo.getHeight(), 2).doubleValue();
            // 返回上传结果
            return UploadResult.builder()
                    .url(cosClientConfig.getHost() + uploadPath)
                    .name(FileUtil.mainName(originalFilename))
                    .picSize(FileUtil.size(tempFile))
                    .picWidth(imageInfo.getWidth())
                    .picHeight(imageInfo.getHeight())
                    .picScale(picScale)
                    .picFormat(imageInfo.getFormat())
                    .build();
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        }finally {
            // 清理临时文件
            deleteTempFile(tempFile);
        }
    }

    /**
     * 上传网络图片到对象存储
     * @param url 图片url
     * @param uploadPathPrefix 上传路径前缀
     * @return 上传结果
     */
    public UploadResult uploadPictureByUrl(String url, String uploadPathPrefix) {
        // 校验图片url
        validPicture(url);

        return null;
    }

    /**
     * 校验图片
     * @param multipartFile 文件
     */
    private void validPicture(MultipartFile multipartFile) {
        ThrowUtils.throwException(multipartFile == null, ErrorCode.PARAMS_ERROR, "图片不能为空");
        // 校验文件大小
        long fileSize = multipartFile.getSize();
        ThrowUtils.throwException(fileSize > 5 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过 5M");
        // 校验文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        ThrowUtils.throwException(!ALLOWED_FORMATS.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "文件后缀只能是 png, jpg, jpeg, gif, webp");
    }

    /**
     * 校验图片
     * @param url 图片url
     */
    private void validPicture(String url) {
        ThrowUtils.throwException(StrUtil.isBlank(url), ErrorCode.PARAMS_ERROR, "图片url不能为空");
        // 校验url格式
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片url格式错误");
        }
        // 校验url协议
        String protocol = url.split(":")[0];
        ThrowUtils.throwException(!"http".equals(protocol) && !"https".equals(protocol), ErrorCode.PARAMS_ERROR, "图片url仅支持http或https协议");
        // 发送head请求确认文件存在
        HttpResponse httpResponse = null;
        try {
            httpResponse = HttpUtil.createRequest(Method.HEAD, url).execute();
            // 如果返回状态不是200，则不执行后续逻辑（某些服务器不支持get外的请求）
            if (httpResponse.getStatus() != HttpStatus.HTTP_OK) return;
            // 校验文件大小
            String contentLength = httpResponse.header("Content-Length");
            if (StrUtil.isNotBlank(contentLength)) {
                try {
                    long fileSize = Long.parseLong(contentLength);
                    ThrowUtils.throwException(fileSize > 5 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过 5M");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式错误");
                }
            }
            // 校验文件类型
            String contentType = httpResponse.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)) {
                // 去掉可能携带的参数部分
                String mediaType = StrUtil.subBefore(contentType, ";", false);
                // 取最后的后缀
                String subType = StrUtil.subAfter(mediaType, "/", true);
                ThrowUtils.throwException(!ALLOWED_FORMATS.contains(subType), ErrorCode.PARAMS_ERROR, "文件后缀只能是 png, jpg, jpeg, gif, webp");
            }
        }finally {
            // 释放资源
            if (httpResponse != null) {
                httpResponse.close();
            }
        }



    }

    /**
     * 删除临时文件
     * @param tempFile 临时文件
     */
    public static void deleteTempFile(File tempFile) {
        if (tempFile != null) {
            // 删除临时文件
            if (!tempFile.delete()) {
                log.error("file delete failed, filepath: {}", tempFile.getPath());
            }
        }
    }
}
