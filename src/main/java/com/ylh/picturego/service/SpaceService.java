package com.ylh.picturego.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ylh.picturego.model.dto.space.SpaceAddRequest;
import com.ylh.picturego.model.dto.space.SpaceQueryRequest;
import com.ylh.picturego.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ylh.picturego.model.entity.User;
import com.ylh.picturego.model.vo.SpaceVO;
import jakarta.servlet.http.HttpServletRequest;

/**
* @author V0411
* @description 针对表【space(空间表)】的数据库操作Service
* @createDate 2026-10-30 03:55:37
*/
public interface SpaceService extends IService<Space> {

    /**
     * 校验空间
     *
     * @param space 空间
     */
    void validSpace(Space space, boolean isAdd);

    /**
     * 添加空间
     *
     * @param spaceAddRequest 空间添加请求
     * @return 空间id
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 获取空间VO
     *
     * @param space 空间
     * @param request 请求
     * @return 空间VO
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 获取空间VO分页
     *
     * @param spacePage 空间分页
     * @param request 请求
     * @return 空间VO分页
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 获取查询条件
     *
     * @param spaceQueryRequest 空间查询请求
     * @return 查询条件
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 根据级别填充空间对象
     *
     * @param space 空间
     */
    void fillSpaceByLevel(Space space);
}
