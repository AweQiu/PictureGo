package com.ylh.picturego.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 批量上传图片请求
 */
@Data
public class PictureUploadByBatchRequest implements Serializable {

    /**
     * 搜索内容
     */
    private String searchText;

    /**
     * 搜索数量
     */
    private Integer count = 10;

    /**
     * 名称前缀
     */
    private String namePrefix;

    private static final long serialVersionUID = 1L;
}
