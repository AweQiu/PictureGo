package com.ylh.picturego.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ylh.picturego.annotation.AuthCheck;
import com.ylh.picturego.common.DeleteRequest;
import com.ylh.picturego.common.EncryptUtil;
import com.ylh.picturego.common.Result;
import com.ylh.picturego.common.ResultUtils;
import com.ylh.picturego.constant.UserConstant;
import com.ylh.picturego.exception.ErrorCode;
import com.ylh.picturego.exception.ThrowUtils;
import com.ylh.picturego.model.dto.user.*;
import com.ylh.picturego.model.entity.User;
import com.ylh.picturego.model.enums.UserRoleEnum;
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
        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);
        // 角色缺省为默认角色
        if (StrUtil.isBlank(user.getUserRole())) {
            user.setUserRole(UserConstant.DEFAULT_ROLE);
        }
        // 仅允许创建等级严格低于自己的用户，防止创建平级/高等级账号绕过限制
        User loginUser = userService.getLoginUser(request);
        UserRoleEnum loginRole = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        UserRoleEnum newRole = UserRoleEnum.getEnumByValue(user.getUserRole());
        ThrowUtils.throwException(
                loginRole == null || newRole == null || newRole.getLevel() >= loginRole.getLevel(),
                ErrorCode.NO_AUTH_ERROR, "无权限创建该角色");
        // 设置默认密码
        String originPassword = EncryptUtil.getEncryptPassword(UserConstant.DEFAULT_USER_PASSWORD, "PasswordSalt");
        user.setUserPassword(originPassword);
        // 保存用户
        boolean result = userService.save(user);
        ThrowUtils.throwException(!result, ErrorCode.OPERATION_ERROR, "用户添加失败");
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据ID获取用户（管理员）
     * @param id 用户ID
     * @return 用户
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<User> getUserById(@RequestParam Long id) {
        ThrowUtils.throwException(id == null || id <= 0, ErrorCode.PARAMS_ERROR, "用户ID有误");
        User user = userService.getById(id);
        ThrowUtils.throwException(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        return ResultUtils.success(user);
    }

    /**
     * 根据ID获取用户VO（普通用户）
     * @param id 用户ID
     * @return 用户VO
     */
    @GetMapping("/get/vo")
    public Result<UserVO> getUserVOById(@RequestParam Long id) {
        Result<User> result = getUserById(id);
        User user = result.getData();
        // 返回脱敏用户信息
        return ResultUtils.success(UserVO.objToVo(user));
    }

    /**
     * 删除用户
     * @param deleteRequest 删除请求
     * @param request 请求
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Boolean> deleteUser(@RequestParam DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwException(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        // 校验目标用户存在及等级权限（禁止平级操作）
        User loginUser = userService.getLoginUser(request);
        User targetUser = userService.getById(deleteRequest.getId());
        ThrowUtils.throwException(targetUser == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        ThrowUtils.throwException(
                !userService.canManageUser(loginUser, targetUser),
                ErrorCode.NO_AUTH_ERROR, "无权限操作同级或更高级别的用户");
        boolean result = userService.removeById(deleteRequest.getId());
        ThrowUtils.throwException(!result, ErrorCode.OPERATION_ERROR, "用户删除失败");
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
        // 校验目标用户存在及等级权限（禁止平级操作）
        User loginUser = userService.getLoginUser(request);
        User targetUser = userService.getById(userUpdateRequest.getId());
        ThrowUtils.throwException(targetUser == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        ThrowUtils.throwException(
                !userService.canManageUser(loginUser, targetUser),
                ErrorCode.NO_AUTH_ERROR, "无权限操作同级或更高级别的用户");
        // 限制角色变更：仅等级严格高于目标用户者可修改其角色，任何人不能修改自己的角色
        String newRole = userUpdateRequest.getUserRole();
        if (StrUtil.isNotBlank(newRole) && !newRole.equals(targetUser.getUserRole())) {
            UserRoleEnum loginRole = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
            UserRoleEnum targetRole = UserRoleEnum.getEnumByValue(targetUser.getUserRole());
            ThrowUtils.throwException(
                    loginRole == null || targetRole == null || loginRole.getLevel() <= targetRole.getLevel(),
                    ErrorCode.NO_AUTH_ERROR, "无权限修改该用户的角色");
        }
        User user = new User();
        BeanUtil.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwException(!result, ErrorCode.OPERATION_ERROR, "用户编辑失败");
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
        // 获取分页参数
        int current = userQueryRequest.getCurrent();
        int pageSize = userQueryRequest.getPageSize();
        // 获取分页数据
        Page<User> userPage = userService.page(new Page<>(current, pageSize), userService.getQueryWrapper(userQueryRequest));
        // 传递分页元信息（当前页、每页条数、总条数）
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        // 转换用户信息为用户VO
        List<UserVO> userVOList = UserVO.listToVoList(userPage.getRecords());
        // 设置分页数据（用户VO列表）
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }
}
