package com.ylh.picturego.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 图片审核状态枚举
 */
@Getter
public enum PictureReviewStatusEnum {
    REVIEWING(0, "待审核"),
    PASS(1, "审核通过"),
    REJECT(2, "审核拒绝");

    private final int value;
    private final String text;

    PictureReviewStatusEnum(int value, String text) {
        this.value = value;
        this.text = text;
    }

    public static PictureReviewStatusEnum getEnumByValue(int value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (PictureReviewStatusEnum pictureReviewStatusEnum : PictureReviewStatusEnum.values()) {
            if (pictureReviewStatusEnum.getValue() == value) {
                return pictureReviewStatusEnum;
            }
        }
        return null;
    }
}
