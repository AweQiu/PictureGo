package com.ylh.picturego.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 空间级别
 */
@Data
@AllArgsConstructor
public class SpaceLevel {

    /**
     * 级别值
     */
    private int value;

    /**
     * 级别名称
     */
    private String text;

    /**
     * 最大图片数
     */
    private long maxCount;

    /**
     * 最大空间大小
     */
    private long maxSize;
}
