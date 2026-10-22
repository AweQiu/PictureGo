package com.ylh.picturego.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 图片表
 * @TableName picture
 */
@TableName(value ="picture")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Picture {
    /**
     * 唯一标识
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 图片url
     */
    private String url;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 图片介绍
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签（JSON数组）
     */
    private String tags;

    /**
     * 审核状态（0-待审核，1-审核通过，2-审核拒绝）
     */
    private Integer reviewStatus;

    /**
     * 审核消息
     */
    private String reviewMessage;

    /**
     * 审核人id
     */
    private Long reviewerId;

    /**
     * 审核时间
     */
    private Date reviewTime;

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

    /**
     * 创建用户id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}