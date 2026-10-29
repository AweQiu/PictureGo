package com.ylh.picturego.model.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间创建请求
 */
@Data
public class SpaceAddRequest implements Serializable {

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间级别
     */
    private Integer spaceLevel;

    private static final long serialVersionUID = 1L;
}
