package com.ylh.picturego.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.ylh.picturego.exception.BusinessException;
import com.ylh.picturego.exception.ErrorCode;
import com.ylh.picturego.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * Url图片上传
 */
@Service
public class UrlPictureUpload extends PictureUploadTemplate {

    @Override
    protected void validPicture(Object inputSource) {
        String url = (String) inputSource;
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

    @Override
    protected void processFile(Object inputSource, File tempFile) throws Exception {
        String url = (String) inputSource;
        // 下载文件到临时目录
        HttpUtil.downloadFile(url, tempFile);
    }

    @Override
    protected String getOriginalFilename(Object inputSource) {
        String url = (String) inputSource;
//        return url.substring(url.lastIndexOf("/") + 1);
        return FileUtil.mainName(url);
    }
}
