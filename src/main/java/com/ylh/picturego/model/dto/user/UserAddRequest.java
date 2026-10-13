package com.ylh.picturego.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户添加请求
 */
@Data
public class UserAddRequest implements Serializable {

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色
     */
    private String userRole;

    private static final long serialVersionUID = 1L;
}