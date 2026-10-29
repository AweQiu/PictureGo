package com.ylh.picturego.model.vo;

import com.ylh.picturego.model.entity.Space;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * 空间VO
 */
@Data
public class SpaceVO implements Serializable {
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
     * 空间最大存储空间
     */
    private Long maxSize;

    /**
     * 空间最大图片数量
     */
    private Long maxCount;

    /**
     * 空间已用存储空间
     */
    private Long totalSize;

    /**
     * 空间已有图片数量
     */
    private Long totalCount;

    /**
     * 所属用户id
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
     * 所属用户信息
     */
    private UserVO userVO;

    private static final long serialVersionUID = 1L;

    /**
     * 对象转VO
     * @param space 空间对象
     * @return 空间VO
     */
    public static SpaceVO objToVo(Space space) {
        if (space == null) {
            return null;
        }
        SpaceVO spaceVO = new SpaceVO();
        BeanUtils.copyProperties(space, spaceVO);
        return spaceVO;
    }

    /**
     * VO转对象
     * @param spaceVO 空间VO
     * @return 空间对象
     */
    public static Space voToObj(SpaceVO spaceVO) {
        if (spaceVO == null) {
            return null;
        }
        Space space = new Space();
        BeanUtils.copyProperties(spaceVO, space);
        return space;
    }
}
