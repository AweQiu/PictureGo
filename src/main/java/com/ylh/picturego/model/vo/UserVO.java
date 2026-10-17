package com.ylh.picturego.model.vo;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.ylh.picturego.model.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    /**
     * 封装类转对象
     *
     * @param userVO UserVO对象
     * @return User对象
     */
    public static User voToObj(UserVO userVO) {
        if (userVO == null) {
            return null;
        }
        User user = new User();
        BeanUtil.copyProperties(userVO, user);
        return user;
    }

    /**
     * 对象转封装类
     *
     * @param user User对象
     * @return UserVO对象
     */
    public static UserVO objToVo(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 对象列表转封装类列表
     *
     * @param userList User对象列表
     * @return UserVO对象列表
     */
    public static List<UserVO> listToVoList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return null;
        }
        // 转换用户信息
        return userList.stream()
                .map(UserVO::objToVo)
                .collect(Collectors.toList());
    }
}
