package com.ylh.picturego.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 图片标签分类视图
 */
@Data
public class PictureTagCategory implements Serializable {

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 分类列表
     */
    private List<String> categories;

    private static final Long serialVersionUID = 1L;
}
