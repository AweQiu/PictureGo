package com.ylh.picturego.manager;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.ylh.picturego.config.CosClientConfig;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传对象
     *
     * @param key  对象键
     * @param file 文件
     * @return 上传结果
     */
    public PutObjectResult uploadObject(String key, File file) {
        // 指定上传桶、对象键和文件
        return cosClient.putObject(new PutObjectRequest(cosClientConfig.getBucket(), key, file));
    }

    /**
     * 下载对象
     *
     * @param key 对象键
     * @return 下载结果
     */
    public COSObject downloadObject(String key) {
        return cosClient.getObject(new GetObjectRequest(cosClientConfig.getBucket(), key));
    }
}
