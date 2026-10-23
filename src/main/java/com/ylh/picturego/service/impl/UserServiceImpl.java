package com.ylh.picturego.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ylh.picturego.common.EncryptUtil;
import com.ylh.picturego.constant.UserConstant;
import com.ylh.picturego.exception.ErrorCode;
import com.ylh.picturego.exception.ThrowUtils;
import com.ylh.picturego.model.dto.user.UserLoginRequest;
import com.ylh.picturego.model.dto.user.UserQueryRequest;
import com.ylh.picturego.model.dto.user.UserRegisterRequest;
import com.ylh.picturego.model.entity.User;
import com.ylh.picturego.model.enums.UserRoleEnum;
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
        String encryptedPassword = EncryptUtil.getEncryptPassword(userRegisterRequest.getUserPassword(), "PasswordSalt");
        // 保存用户
        User newUser = User.builder()
                .userAccount(userRegisterRequest.getUserAccount())
                .userPassword(encryptedPassword)
                .build();
        this.save(newUser);
        return newUser.getId();
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
        return LoginUserVO.objToLoginVO(user);
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
    public boolean userLogout(HttpServletRequest request) {
        // 判断是否登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        ThrowUtils.throwException(userObj == null, ErrorCode.NOT_LOGIN_ERROR, "未登录");
        // 移除登录态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        ThrowUtils.throwException(userQueryRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("asc"), sortField);
        return queryWrapper;
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getLevel() <= UserRoleEnum.getEnumByValue(user.getUserRole()).getLevel();
    }

    @Override
    public boolean canManageUser(User loginUser, User targetUser) {
        if (loginUser == null || targetUser == null) {
            return false;
        }
        // 本人操作放行
        if (loginUser.getId().equals(targetUser.getId())) {
            return true;
        }
        UserRoleEnum loginRole = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        UserRoleEnum targetRole = UserRoleEnum.getEnumByValue(targetUser.getUserRole());
        // 角色无法识别时拒绝（fail-closed）
        if (loginRole == null || targetRole == null) {
            return false;
        }
        // 必须严格高于目标用户等级，平级不可操作
        return loginRole.getLevel() > targetRole.getLevel();
    }
}




