package com.ylh.picturego.controller;

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

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册
     * @param userRegisterRequest 用户注册请求
     * @return 用户ID
     */
    @PostMapping("/register")
    public Result<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwException(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userService.userRegister(userRegisterRequest));
    }

    /**
     * 用户登录
     * @param userLoginRequest 用户登录请求
     * @param request 请求
     * @return 登录结果
     */
    @PostMapping("/login")
    public Result<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwException(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userService.userLogin(userLoginRequest, request));
    }
    /**
     * 获取当前登录用户
     * @param request 请求
     * @return 当前登录用户
     */
    @GetMapping("/get/login")
    public Result<LoginUserVO> getLoginUser(HttpServletRequest request) {
        return ResultUtils.success(LoginUserVO.objToLoginVO(userService.getLoginUser(request)));
    }

    /**
     * 用户注销
     * @param request 请求
     * @return 注销结果
     */
    @PostMapping("/logout")
    public Result<Boolean> logout(HttpServletRequest request) {
        return ResultUtils.success(userService.userLogout(request));
    }

    /**
     * 添加用户
     * @param userAddRequest 添加用户请求
     * @param request 请求
     * @return 添加结果
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Long> addUsers(@RequestBody UserAddRequest userAddRequest, HttpServletRequest request) {
        ThrowUtils.throwException(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.addUser(userAddRequest, loginUser));
    }

    /**
     * 根据ID获取用户（管理员）
     * @param id 用户ID
     * @return 用户
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<User> getUserById(@RequestParam Long id) {
        return ResultUtils.success(userService.getUserById(id));
    }

    /**
     * 根据ID获取用户VO（普通用户）
     * @param id 用户ID
     * @return 用户VO
     */
    @GetMapping("/get/vo")
    public Result<UserVO> getUserVOById(@RequestParam Long id) {
        return ResultUtils.success(userService.getUserVOById(id));
    }

    /**
     * 删除用户
     * @param deleteRequest 删除请求
     * @param request 请求
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwException(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        userService.deleteUser(deleteRequest.getId(), loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 更新用户
     * @param userUpdateRequest 更新请求
     * @param request 请求
     * @return 是否更新成功
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest, HttpServletRequest request) {
        ThrowUtils.throwException(userUpdateRequest == null || userUpdateRequest.getId() == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        userService.updateUser(userUpdateRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 获取用户列表
     * @param userQueryRequest 查询请求
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Page<UserVO>> getUserList(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwException(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userService.listUserVOPage(userQueryRequest));
    }
}
