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
import com.ylh.picturego.model.dto.user.*;
import com.ylh.picturego.model.entity.User;
import com.ylh.picturego.model.vo.LoginUserVO;
import com.ylh.picturego.model.vo.UserVO;
import com.ylh.picturego.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    public UserService userService;

    /**
     * 用户注册
     * @param userRegisterRequest
     * @return 用户ID
     */
    @PostMapping("/register")
    public Result<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwException(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userService.userRegister(userRegisterRequest));
    }

    /**
     * 用户登录
     * @param userLoginRequest
     * @param request
     * @return 登录结果
     */
    @PostMapping("/login")
    public Result<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwException(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userService.userLogin(userLoginRequest, request));
    }
    /**
     * 获取当前登录用户
     * @param request
     * @return 当前登录用户
     */
    @GetMapping("/get/login")
    public Result<LoginUserVO> getLoginUser(HttpServletRequest request) {
        return ResultUtils.success(userService.getLoginUserVO(userService.getLoginUser(request)));
    }

    /**
     * 用户注销
     *
     * @param request
     * @return 注销结果
     */
    @PostMapping("/logout")
    public Result<Boolean> logout(HttpServletRequest request) {
        return ResultUtils.success(userService.userLogout(request));
    }

    /**
     * 添加用户
     * @return 新用户ID
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Long> addUsers(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwException(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);
        // 设置默认密码
        String originPassword = userService.getEncryptPassword(UserConstant.DEFAULT_USER_PASSWORD);
        user.setUserPassword(originPassword);
        // 保存用户
        boolean result = userService.save(user);
        ThrowUtils.throwException(!result, ErrorCode.OPERATION_ERROR, "用户添加失败");
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据ID获取用户（管理员）
     * @param id
     * @return 用户
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<User> getUserById(@RequestParam Long id) {
        ThrowUtils.throwException(id == null || id <= 0, ErrorCode.PARAMS_ERROR, "用户ID为空或小于等于0");
        User user = userService.getById(id);
        ThrowUtils.throwException(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        return ResultUtils.success(user);
    }

    /**
     * 根据ID获取用户VO（普通用户）
     * @param id
     * @return 用户VO
     */
    @GetMapping("/get/vo")
    public Result<UserVO> getUserVOById(@RequestParam Long id) {
        Result<User> result = getUserById(id);
        User user = result.getData();
        // 返回脱敏用户信息
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 删除用户
     * @param deleteRequest
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Boolean> deleteUser(@RequestParam DeleteRequest deleteRequest) {
        ThrowUtils.throwException(deleteRequest == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.removeById(deleteRequest.getId());
        ThrowUtils.throwException(!result, ErrorCode.OPERATION_ERROR, "用户删除失败");
        return ResultUtils.success(result);
    }

    /**
     * 更新用户
     * @param userUpdateRequest
     * @return 是否更新成功
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        ThrowUtils.throwException(userUpdateRequest == null || userUpdateRequest.getId() == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwException(!result, ErrorCode.OPERATION_ERROR, "用户编辑失败");
        return ResultUtils.success(result);
    }

    /**
     * 获取用户列表
     * @return 用户列表
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Page<UserVO>> getUserList(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwException(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 获取分页参数
        int current = userQueryRequest.getCurrent();
        int pageSize = userQueryRequest.getPageSize();
        // 获取分页数据
        Page<User> userPage = userService.page(new Page<>(current, pageSize), userService.getQueryWrapper(userQueryRequest));
        // 传递分页元信息（当前页、每页条数、总条数）
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        // 转换用户信息为用户VO
        List<UserVO> userVOList = userService.getUsersVOList(userPage.getRecords());
        // 设置分页数据（用户VO列表）
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }
}
