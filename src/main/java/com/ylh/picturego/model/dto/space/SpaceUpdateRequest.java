package com.ylh.picturego.model.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间更新请求
 */
@Data
public class SpaceUpdateRequest implements Serializable {

    /**
     * 空间id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间级别
     */
    private Integer spaceLevel;

    /**
     * 空间最大占用空间
     */
    private Long maxSize;

    /**
     * 空间最大图片数量
     */
    private Long maxCount;

    private static final long serialVersionUID = 1L;
}
