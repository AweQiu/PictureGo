package com.ylh.picturego.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ylh.picturego.model.dto.user.UserLoginRequest;
import com.ylh.picturego.model.dto.user.UserQueryRequest;
import com.ylh.picturego.model.dto.user.UserRegisterRequest;
import com.ylh.picturego.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ylh.picturego.model.vo.LoginUserVO;
import com.ylh.picturego.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

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
     * 加密密码
     * @param password
     * @return 加密后的密码
     */
    String getEncryptPassword(String password);

    /**
     * 用户注销
     * @param request
     * @return 注销结果
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取查询条件
     *
     * @param userQueryRequest
     * @return 查询条件
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);
}
