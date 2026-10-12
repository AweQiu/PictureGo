package com.ylh.picturego.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ylh.picturego.entity.User;
import com.ylh.picturego.service.UserService;
import com.ylh.picturego.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
* @author V0411
* @description 针对表【user(用户表)】的数据库操作Service实现
* @createDate 2026-10-12 16:27:05
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

}




