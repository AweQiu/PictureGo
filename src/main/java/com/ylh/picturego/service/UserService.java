package com.ylh.picturego.service;

import com.ylh.picturego.model.dto.UserLoginRequest;
import com.ylh.picturego.model.dto.UserRegisterRequest;
import com.ylh.picturego.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ylh.picturego.model.vo.LoginUserVO;
import jakarta.servlet.http.HttpServletRequest;

/**
* @author V0411
* @description 针对表【user(用户表)】的数据库操作Service
* @createDate 2026-10-12 16:27:05
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     * @param userRegisterRequest
     * @return 用户ID
     */
    long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 用户登录
     * @param userLoginRequest
     * @return 登录结果
     */
    LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request);

    /**
     * 获取当前登录用户（Service之间调用）
     * @param request
     * @return 当前登录用户
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取脱敏类用户信息
     * @return 脱敏后用户信息
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 用户注销
     * @param request
     * @return 注销结果
     */
    boolean userLogout(HttpServletRequest request);
}
