package com.ylh.picturego.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户视图对象
 */
@Data
public class UserVO implements Serializable {

    /**
     * 用户id
     */
    private Long id;

    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String userName;

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

    /**
     * 用户创建时间
     */
    private String createTime;

    private static final long serialVersionUID = 1L;
}
