package com.ylh.picturego.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.ylh.picturego.exception.ErrorCode;
import com.ylh.picturego.exception.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * 本地图片上传
 */
@Service
public class FilePictureUpload extends PictureUploadTemplate {

    @Override
    protected void validPicture(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        ThrowUtils.throwException(multipartFile == null, ErrorCode.PARAMS_ERROR, "图片不能为空");
        // 校验文件大小
        long fileSize = multipartFile.getSize();
        ThrowUtils.throwException(fileSize > 5 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过 5M");
        // 校验文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        ThrowUtils.throwException(!ALLOWED_FORMATS.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "文件后缀只能是 png, jpg, jpeg, gif, webp");
    }

    @Override
    protected void processFile(Object inputSource, File tempFile) throws Exception {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        multipartFile.transferTo(tempFile);
    }

    @Override
    protected String getOriginalFilename(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }
}
