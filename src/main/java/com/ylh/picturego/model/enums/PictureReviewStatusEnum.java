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

    private final int code;
    private final String message;

    PictureReviewStatusEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public static PictureReviewStatusEnum getEnumByCode(int code) {
        if (ObjUtil.isEmpty(code)) {
            return null;
        }
        for (PictureReviewStatusEnum pictureReviewStatusEnum : PictureReviewStatusEnum.values()) {
            if (pictureReviewStatusEnum.getCode() == code) {
                return pictureReviewStatusEnum;
            }
        }
        return null;
    }
}
