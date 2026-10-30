package com.ylh.picturego.model.enums;

import cn.hutool.core.util.ObjUtil;
import com.ylh.picturego.constant.SpaceConstant;
import lombok.Getter;

/**
 * 空间级别枚举
 */
@Getter
public enum SpaceLevelEnum {

    COMMON("普通空间", 0, SpaceConstant.SPACE_SIZE_COMMON, SpaceConstant.SPACE_COUNT_COMMON),
    PROFESSIONAL("专业空间", 1, SpaceConstant.SPACE_SIZE_PROFESSIONAL, SpaceConstant.SPACE_COUNT_PROFESSIONAL),
    FLAGSHIP("旗舰空间", 2, SpaceConstant.SPACE_SIZE_FLAGSHIP, SpaceConstant.SPACE_COUNT_FLAGSHIP);

    private final String text;

    private final int value;

    private final long maxSize;

    private final long maxCount;

    /**
     * 枚举构造函数
     * @param text 枚举文本
     * @param value 枚举值
     * @param maxSize 最大存储空间
     * @param maxCount 最大图片数量
     */
    SpaceLevelEnum(String text, int value, long maxSize, long maxCount) {
        this.text = text;
        this.value = value;
        this.maxSize = maxSize;
        this.maxCount = maxCount;
    }

    /**
     * 根据值获取枚举
     * @param value 枚举值
     * @return 枚举
     */
    public static SpaceLevelEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (SpaceLevelEnum spaceLevelEnum : SpaceLevelEnum.values()) {
            if (spaceLevelEnum.getValue() == value) {
                return spaceLevelEnum;
            }
        }
        return null;
    }
}
