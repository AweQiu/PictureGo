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
     * 用户注销
     * @param request 请求
     * @return 注销结果
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取查询条件
     * @param userQueryRequest 查询请求
     * @return 查询条件
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 是否是管理员
     * @param user 用户
     * @return 是否是管理员
     */
    boolean isAdmin(User user);

    /**
     * 是否有权限管理该用户
     * @param loginUser 登录用户
     * @param targetUser 目标用户
     * @return 是否有权限
     */
    boolean canManageUser(User loginUser, User targetUser);
}
