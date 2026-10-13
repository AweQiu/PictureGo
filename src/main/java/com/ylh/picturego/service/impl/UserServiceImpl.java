package com.ylh.picturego.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ylh.picturego.constant.UserConstant;
import com.ylh.picturego.exception.ErrorCode;
import com.ylh.picturego.exception.ThrowUtils;
import com.ylh.picturego.model.dto.UserLoginRequest;
import com.ylh.picturego.model.dto.UserRegisterRequest;
import com.ylh.picturego.model.entity.User;
import com.ylh.picturego.model.vo.LoginUserVO;
import com.ylh.picturego.service.UserService;
import com.ylh.picturego.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
* @author V0411
* @description 针对表【user(用户表)】的数据库操作Service实现
* @createDate 2026-10-12 16:27:05
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Override
    public long userRegister(UserRegisterRequest userRegisterRequest) {
        // 参数校验
        ThrowUtils.throwException(StrUtil.hasBlank(userRegisterRequest.getUserAccount(), userRegisterRequest.getUserPassword(), userRegisterRequest.getCheckPassword()), ErrorCode.PARAMS_ERROR, "参数为空");
        ThrowUtils.throwException(userRegisterRequest.getUserAccount().length() < 6, ErrorCode.PARAMS_ERROR, "账号长度不能小于6");
        ThrowUtils.throwException(userRegisterRequest.getUserPassword().length() < 8, ErrorCode.PARAMS_ERROR, "密码长度不能小于8");
        ThrowUtils.throwException(!userRegisterRequest.getUserPassword().equals(userRegisterRequest.getCheckPassword()), ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        // 重复性校验
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userRegisterRequest.getUserAccount());
        ThrowUtils.throwException(this.baseMapper.selectCount(queryWrapper) > 0, ErrorCode.PARAMS_ERROR, "用户账号已存在");
        // 密码加密
        String encryptedPassword = userRegisterRequest.getUserPassword();
        encryptedPassword = DigestUtil.md5Hex(encryptedPassword + "PasswordSalt");
        // 保存用户
        User user = new User();
        user.setUserAccount(userRegisterRequest.getUserAccount());
        user.setUserPassword(encryptedPassword);
        this.save(user);
        return user.getId();
    }

    @Override
    public LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwException(StrUtil.hasBlank(userLoginRequest.getUserAccount(), userLoginRequest.getUserPassword()), ErrorCode.PARAMS_ERROR, "参数为空");
        ThrowUtils.throwException(userLoginRequest.getUserAccount().length() < 6, ErrorCode.PARAMS_ERROR, "账号长度不能小于6");
        ThrowUtils.throwException(userLoginRequest.getUserPassword().length() < 8, ErrorCode.PARAMS_ERROR, "密码长度不能小于8");
        // 密码加密
        String encryptedPassword = userLoginRequest.getUserPassword();
        encryptedPassword = DigestUtil.md5Hex(encryptedPassword + "PasswordSalt");
        // 查询用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userLoginRequest.getUserAccount());
        queryWrapper.eq("userPassword", encryptedPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        if (user == null) {
            log.info("User Login Failed: userAccount cannot match userPassword");
        }
        ThrowUtils.throwException(user == null, ErrorCode.PARAMS_ERROR, "用户不存在或账号或密码错误");
        // 保存登录态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 判断是否登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        ThrowUtils.throwException(currentUser == null || currentUser.getId() == null, ErrorCode.NOT_LOGIN_ERROR);
        // 从数据库查询，更新内存中的用户信息
        currentUser = this.getById(currentUser.getId());
        ThrowUtils.throwException(currentUser == null, ErrorCode.NOT_LOGIN_ERROR);
        return currentUser;
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 判断是否登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        ThrowUtils.throwException(userObj == null, ErrorCode.NOT_LOGIN_ERROR, "未登录");
        // 移除登录态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }
}




