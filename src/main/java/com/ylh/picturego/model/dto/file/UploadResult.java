package com.ylh.picturego.model.dto.file;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UploadResult {

    /**
     * 图片url
     */
    private String url;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 宽高比例
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;
}
