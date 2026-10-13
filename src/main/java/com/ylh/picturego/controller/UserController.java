package com.ylh.picturego.controller;

import com.ylh.picturego.common.Result;
import com.ylh.picturego.common.ResultUtils;
import com.ylh.picturego.exception.ErrorCode;
import com.ylh.picturego.exception.ThrowUtils;
import com.ylh.picturego.model.dto.UserLoginRequest;
import com.ylh.picturego.model.dto.UserRegisterRequest;
import com.ylh.picturego.model.vo.LoginUserVO;
import com.ylh.picturego.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/logout")
    public Result<Boolean> logout(HttpServletRequest request) {
        return ResultUtils.success(userService.userLogout(request));
    }
}
