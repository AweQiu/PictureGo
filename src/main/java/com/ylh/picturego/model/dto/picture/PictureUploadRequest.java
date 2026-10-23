package com.ylh.picturego.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 图片上传请求
 */
@Data
public class PictureUploadRequest implements Serializable {

    /**
     * 图片id（用于编辑）
     */
    private Long id;

    /**
     * 图片url
     */
    private String url;

    private static final long serialVersionUID = 1L;
}
