package com.ylh.picturego.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 登录用户视图
 */
@Data
public class LoginUserVO implements Serializable {

    /**
     * 唯一标识
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
     * 编辑时间（用户编辑个人信息）
     */
    private Date editTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
