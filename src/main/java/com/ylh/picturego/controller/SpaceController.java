package com.ylh.picturego.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ylh.picturego.annotation.AuthCheck;
import com.ylh.picturego.common.DeleteRequest;
import com.ylh.picturego.common.Result;
import com.ylh.picturego.common.ResultUtils;
import com.ylh.picturego.constant.UserConstant;
import com.ylh.picturego.exception.ErrorCode;
import com.ylh.picturego.exception.ThrowUtils;
import com.ylh.picturego.model.dto.space.SpaceAddRequest;
import com.ylh.picturego.model.dto.space.SpaceEditRequest;
import com.ylh.picturego.model.dto.space.SpaceQueryRequest;
import com.ylh.picturego.model.dto.space.SpaceUpdateRequest;
import com.ylh.picturego.model.entity.Space;
import com.ylh.picturego.model.entity.User;
import com.ylh.picturego.model.enums.SpaceLevelEnum;
import com.ylh.picturego.model.vo.SpaceLevel;
import com.ylh.picturego.model.vo.SpaceVO;
import com.ylh.picturego.service.SpaceService;
import com.ylh.picturego.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/space")
public class SpaceController {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    /**
     * 添加空间（普通用户）
     *
     * @param spaceAddRequest 添加请求
     * @param request 请求
     * @return 结果
     */
    @PostMapping("/add")
    public Result<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        ThrowUtils.throwException(spaceAddRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        long newId = spaceService.addSpace(spaceAddRequest, loginUser);
        return ResultUtils.success(newId);
    }

    /**
     * 删除空间
     *
     * @param deleteRequest 删除请求
     * @param request 请求
     * @return 结果
     */
    @PostMapping("/delete")
    public Result<Boolean> deleteSpace(
            @RequestBody DeleteRequest deleteRequest,
            HttpServletRequest request
    ) {
        ThrowUtils.throwException(deleteRequest.getId() == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        Long id = deleteRequest.getId();
        User loginUser = userService.getLoginUser(request);
        // 判断是否存在
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwException(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人和管理员可以删除
        ThrowUtils.throwException(
                !loginUser.getId().equals(oldSpace.getUserId()) &&
                !userService.isAdmin(loginUser),
                ErrorCode.NO_AUTH_ERROR, "无权限删除"
        );
        // 删除空间
        ThrowUtils.throwException(!spaceService.removeById(id), ErrorCode.OPERATION_ERROR, "删除失败");
        return ResultUtils.success(true);
    }

    /**
     * 更新空间
     *
     * @param spaceUpdateRequest 更新请求
     * @param request 请求
     * @return 结果
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Boolean> updateSpace(
            @RequestBody SpaceUpdateRequest spaceUpdateRequest,
            HttpServletRequest request
    ) {
        ThrowUtils.throwException(spaceUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        // DTO类转实体类
        Space space = new Space();
        BeanUtils.copyProperties(spaceUpdateRequest, space);
        // 填充数据
        spaceService.fillSpaceByLevel(space);
        // 校验
        spaceService.validSpace(space, false);
        // 判断是否存在
        Long id = spaceUpdateRequest.getId();
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwException(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        ThrowUtils.throwException(!spaceService.updateById(space), ErrorCode.OPERATION_ERROR, "更新失败");
        return ResultUtils.success(true);
    }

    /**
     * 编辑空间
     *
     * @param spaceEditRequest 编辑请求
     * @param request 请求
     * @return 结果
     */
    @PostMapping("/edit")
    public Result<Boolean> editSpace(
            @RequestBody SpaceEditRequest spaceEditRequest,
            HttpServletRequest request
    ) {
        ThrowUtils.throwException(spaceEditRequest == null || spaceEditRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        // DTO类转实体类
        Space space = new Space();
        BeanUtil.copyProperties(spaceEditRequest, space);
        // 填充数据
        spaceService.fillSpaceByLevel(space);
        // 校验空间
        spaceService.validSpace(space, false);
        User loginUser = userService.getLoginUser(request);
        // 存在性校验
        long id = spaceEditRequest.getId();
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwException(oldSpace == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        // 权限校验，非本人则权限不得低于管理员
        ThrowUtils.throwException(
                !oldSpace.getUserId().equals(loginUser.getId()) &&
                !userService.isAdmin(loginUser),
                ErrorCode.NO_AUTH_ERROR, "无权限编辑"
        );
        // 操作数据库
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwException(!result, ErrorCode.OPERATION_ERROR, "空间编辑失败");
        return ResultUtils.success(true);
    }

    /**
     * 获取空间
     *
     * @param id id
     * @return 结果
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Space> getSpaceById(@RequestParam Long id) {
        ThrowUtils.throwException(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Space space = spaceService.getById(id);
        ThrowUtils.throwException(space == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(space);
    }

    /**
     * 获取空间封装类
     *
     * @param id id
     * @param request 请求
     * @return 结果
     */
    @GetMapping("/get/vo")
    public Result<SpaceVO> getSpaceVOById(@RequestParam Long id, HttpServletRequest request) {
        ThrowUtils.throwException(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Space space = spaceService.getById(id);
        ThrowUtils.throwException(space == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        SpaceVO spaceVO = spaceService.getSpaceVO(space, request);
        return ResultUtils.success(spaceVO);
    }

    /**
     * 获取空间分页列表
     *
     * @param spaceQueryRequest 查询请求
     * @return 结果
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();
        // 查询数据库
        Page<Space> spacePage = spaceService.page(
                new Page<>(current, size),
                spaceService.getQueryWrapper(spaceQueryRequest)
        );
        return ResultUtils.success(spacePage);
    }

    /**
     * 获取空间分页列表封装类
     *
     * @param spaceQueryRequest 查询请求
     * @param request 请求
     * @return 结果
     */
    @PostMapping("/list/page/vo")
    public Result<Page<SpaceVO>> listSpaceVOByPage(@RequestBody SpaceQueryRequest spaceQueryRequest, HttpServletRequest request) {
        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwException(size > 20, ErrorCode.PARAMS_ERROR, "分页大小不能超过20");
        // 查询数据库
        Page<Space> spacePage = spaceService.page(
                new Page<>(current, size),
                spaceService.getQueryWrapper(spaceQueryRequest)
        );
        // 获取封装类
        return ResultUtils.success(spaceService.getSpaceVOPage(spacePage, request));
    }

    /**
     * 获取空间级别列表，供前端展示
     * @return 结果
     */
    @GetMapping("/list/level")
    public Result<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values())
                .map(spaceLevelEnum -> new SpaceLevel(
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()
                ))
                .collect(Collectors.toList());
        return ResultUtils.success(spaceLevelList);
    }
}
