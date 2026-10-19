package com.ylh.picturego.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.OriginalInfo;
import com.ylh.picturego.common.ResultUtils;
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
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class FileManager {

    @Resource
    private CosManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;

    /**
     * 上传图片到对象存储
     * @param multipartFile 文件
     * @param uploadPathPrefix 上传路径前缀
     * @return 上传结果
     */
    public UploadResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        // 校验图片
        validPicture(multipartFile);
        // 图片上传
        String uuid = RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename();
        // 自定义上传的文件名，避免冲突增加安全性
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
     * 校验图片
     * @param multipartFile 文件
     */
    private void validPicture(MultipartFile multipartFile) {
        ThrowUtils.throwException(multipartFile == null, ErrorCode.PARAMS_ERROR, "图片不能为空");
        // 校验文件大小
        long fileSize = multipartFile.getSize();
        final long ONE_M = 1024 * 1024;
        ThrowUtils.throwException(fileSize > 5 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过 5M");
        // 校验文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        final List<String> ALLOWED_FORMATS = Arrays.asList("png", "jpg", "jpeg", "gif");
        ThrowUtils.throwException(!ALLOWED_FORMATS.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "文件后缀只能是 png, jpg, jpeg, gif");
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
