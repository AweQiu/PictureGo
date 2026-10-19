package com.ylh.picturego.controller;


import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import com.ylh.picturego.annotation.AuthCheck;
import com.ylh.picturego.common.Result;
import com.ylh.picturego.common.ResultUtils;
import com.ylh.picturego.constant.UserConstant;
import com.ylh.picturego.exception.BusinessException;
import com.ylh.picturego.exception.ErrorCode;
import com.ylh.picturego.manager.CosManager;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {

    @Resource
    private CosManager cosManager;

    /**
     * 上传文件到对象存储（测试）
     * @param multipartFile 文件对象
     * @return 可访问路径
     */
    @PostMapping("/upload")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<String> uploadFile(@RequestPart("file") MultipartFile multipartFile) {
        String fileName = multipartFile.getOriginalFilename();
        String filePath = String.format("/test/%s", fileName);
        // 创建临时文件空对象，用于保存上传的文件
        File tempFile = null;
        try {
            // 保存临时文件的路径和后缀（null）
            tempFile = File.createTempFile(filePath, null);
            // 保存文件到临时路径
            multipartFile.transferTo(tempFile);
            cosManager.uploadObject(filePath, tempFile);
            // 返回可访问地址
            return ResultUtils.success(filePath);
        } catch (Exception e) {
            log.error("file upload failed, filepath: {}", filePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        }finally {
            if (tempFile != null) {
                // 删除临时文件
                if (!tempFile.delete()) {
                    log.error("file delete failed, filepath: {}", filePath);
                }
            }
        }
    }

    /**
     * 下载文件
     * @param filePath 文件路径
     * @param response 响应对象
     * @throws IOException
     */
    @GetMapping("/download")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public void downloadFile(String filePath, HttpServletResponse response) throws IOException {
        COSObjectInputStream cosObjectInputStream = null;
        try {
            // 下载文件
            COSObject cosObject = cosManager.downloadObject(filePath);
            // 获取文件输入流
            cosObjectInputStream = cosObject.getObjectContent();
            byte[] fileBytes = IOUtils.toByteArray(cosObjectInputStream);
            // 设置响应头
            response.setContentType("application/octet-stream;charset=utf-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + filePath);
            // 写入文件内容到响应流
            response.getOutputStream().write(fileBytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("file download failed, filepath: {}", filePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件下载失败");
        } finally {
            // 关闭流
            if (cosObjectInputStream != null)
                cosObjectInputStream.close();
        }
    }
}
