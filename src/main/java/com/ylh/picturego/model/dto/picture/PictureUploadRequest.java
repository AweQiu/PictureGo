package com.ylh.picturego.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadRequest implements Serializable {

    /**
     * 图片id（用于编辑）
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}
