package com.ylh.picturego.manager.upload;

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
import com.ylh.picturego.manager.CosManager;
import com.ylh.picturego.model.dto.file.UploadResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 图片上传模板类
 */
@Slf4j
public abstract class PictureUploadTemplate {
    @Resource
    private CosManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;

    public final List<String> ALLOWED_FORMATS = Arrays.asList("png", "jpg", "jpeg", "gif", "webp");

    public final long ONE_M = 1024 * 1024;

    /**
     * 上传本地图片到对象存储
     * @param inputSource 文件
     * @param uploadPathPrefix 上传路径前缀
     * @return 上传结果
     */
    public UploadResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        // 校验图片
        validPicture(inputSource);
        // 自定义上传的文件名，避免冲突增加安全性
        String uuid = RandomUtil.randomString(16);
        String originalFilename = getOriginalFilename(inputSource);
        String uploadFilename = String.format("%s_%s.%s",
                DateUtil.formatDate(new Date()),
                uuid,
                FileUtil.getSuffix(originalFilename)
        );
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        // 解析结果
        File tempFile = null;
        try {
            // 创建临时文件，获取文件到服务器
            tempFile = File.createTempFile(uploadPath, null);
            // 处理文件来源
            processFile(inputSource, tempFile);
            // 上传图片到对象存储
            PutObjectResult putObjectResult = cosManager.uploadPicture(uploadPath, tempFile);
            // 获取图片信息，封装上传结果
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            return buildResult(imageInfo, uploadPath, originalFilename, tempFile);
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        }finally {
            // 清理临时文件
            deleteTempFile(tempFile);
        }
    }

    /**
     * 构建上传结果
     * @param imageInfo 图片信息
     * @param uploadPath 上传路径
     * @param originalFilename 原始文件名
     * @param tempFile 临时文件
     * @return 上传结果
     */
    private UploadResult buildResult(ImageInfo imageInfo, String uploadPath, String originalFilename, File tempFile) {
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
    }

    /**
     * 校验图片
     * @param inputSource 文件
     */
    protected abstract void validPicture(Object inputSource);

    /**
     * 处理文件来源
     * @param inputSource 文件
     */
    protected abstract void processFile(Object inputSource, File tempFile) throws Exception;

    /**
     * 获取文件名
     * @param inputSource 文件
     * @return 文件名
     */
    protected abstract String getOriginalFilename(Object inputSource);

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
