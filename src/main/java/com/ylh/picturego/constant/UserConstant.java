package com.ylh.picturego.constant;

public interface UserConstant {

    /**
     * 用户登录状态键
     */
    String USER_LOGIN_STATE = "user_login";

    // region 权限
    /**
     * 默认角色
     */
    String DEFAULT_ROLE = "user";

    /**
     * 管理员角色
     */
    String ADMIN_ROLE = "admin";

    /**
     * 超级管理员角色
     */
    String SUPER_ADMIN_ROLE = "superadmin";

    // endregion

    /**
     * 默认用户名
     */
    String DEFAULT_USER_NAME = "user";

    /**
     * 默认用户密码
     */
    String DEFAULT_USER_PASSWORD = "12345678";
}
