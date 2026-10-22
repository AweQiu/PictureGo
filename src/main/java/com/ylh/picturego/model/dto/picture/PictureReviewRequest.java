package com.ylh.picturego.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
@Data
public class PictureReviewRequest implements Serializable {
    /**
     * 图片id
     */
    private Long id;

    /**
     * 审核状态（0-待审核，1-审核通过，2-审核拒绝）
     */
    private Integer reviewStatus;

    /**
     * 审核消息
     */
    private String reviewMessage;

    private static final long serialVersionUID = 1L;
}
