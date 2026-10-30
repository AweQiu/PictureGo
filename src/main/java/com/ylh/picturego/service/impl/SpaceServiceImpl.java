package com.ylh.picturego.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ylh.picturego.exception.ErrorCode;
import com.ylh.picturego.exception.ThrowUtils;
import com.ylh.picturego.model.dto.space.SpaceAddRequest;
import com.ylh.picturego.model.dto.space.SpaceQueryRequest;
import com.ylh.picturego.model.entity.Space;
import com.ylh.picturego.model.entity.User;
import com.ylh.picturego.model.enums.SpaceLevelEnum;
import com.ylh.picturego.model.vo.SpaceVO;
import com.ylh.picturego.model.vo.UserVO;
import com.ylh.picturego.service.SpaceService;
import com.ylh.picturego.mapper.SpaceMapper;
import com.ylh.picturego.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
* @author V0411
* @description 针对表【space(空间表)】的数据库操作Service实现
* @createDate 2026-10-30 03:55:37
*/
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceService {

    @Resource
    private UserService userService;

    @Resource
    private TransactionTemplate transactionTemplate;

    Map<Long, Object> lockMap = new ConcurrentHashMap<>();

    @Override
    public void validSpace(Space space, boolean isAdd) {
        // 参数校验
        ThrowUtils.throwException(space == null, ErrorCode.PARAMS_ERROR);
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        // 创建时校验
        if (isAdd) {
            ThrowUtils.throwException(StrUtil.isBlank(spaceName), ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            ThrowUtils.throwException(spaceName.length() > 30, ErrorCode.PARAMS_ERROR, "空间名称不能超过30个字符");
            ThrowUtils.throwException(spaceLevel == null, ErrorCode.PARAMS_ERROR, "空间等级不能为空");
            ThrowUtils.throwException(spaceLevelEnum == null, ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }
        // 更新时校验
        else {
            ThrowUtils.throwException(spaceName.length() > 30, ErrorCode.PARAMS_ERROR, "空间名称不能超过30个字符");
            ThrowUtils.throwException(spaceLevel != null && spaceLevelEnum == null, ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }
    }

    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        // DTO转实体类
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);
        // 填充默认值
        if (StrUtil.isBlank(space.getSpaceName())) {
            space.setSpaceName("空间" + RandomUtil.randomString(10));
        }
        if (ObjUtil.isNull(space.getSpaceLevel())) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        fillSpaceByLevel(space);
        // 校验
        validSpace(space, true);
        // 非管理员只能创建普通空间
        long userId = loginUser.getId();
        space.setUserId(userId);
        ThrowUtils.throwException(
                space.getSpaceLevel() != SpaceLevelEnum.COMMON.getValue() &&
                !userService.isAdmin(loginUser),
                ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别空间"
        );
        // 一个用户只能创建一个私有空间
        Object lock = lockMap.computeIfAbsent(userId, key -> new Object());
        synchronized (lock) {
            Long newSpaceId = transactionTemplate.execute(status -> {
                try {
                    // 判断是否已存在私有空间
                    boolean exists = this.lambdaQuery()
                            .eq(Space::getUserId, userId)
                            .exists();
                    // 如果已有私有空间，则不允许创建
                    ThrowUtils.throwException(exists, ErrorCode.OPERATION_ERROR, "仅能创建一个私有空间");
                    // 创建私有空间
                    ThrowUtils.throwException(!this.save(space), ErrorCode.OPERATION_ERROR, "创建空间失败");
                }finally {
                    lockMap.remove(userId);
                }
                // 返回空间id
                return space.getId();
            });
            return Optional.ofNullable(newSpaceId).orElse(-1L);
        }
    }

    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        // 对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 查询用户信息
        User user = userService.getById(space.getUserId());
        spaceVO.setUserVO(UserVO.objToVo(user));
        return spaceVO;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        // 对象列表转封装对象列表，此时SpaceVO对象中不包含UserVO对象
        List<SpaceVO> spaceVOList = spaceList.stream().map(SpaceVO::objToVo).collect(Collectors.toList());
        // 关联查询用户信息
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 填充用户信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).getFirst();
            }
            UserVO userVO = UserVO.objToVo(user);
            if (userVO == null) {
                userVO = UserVO.builder().userName("用户已注销").build();
            }
            spaceVO.setUserVO(userVO);
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        // 拼接查询条件
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id)
                .eq(ObjUtil.isNotNull(userId), "userId", userId)
                .like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName)
                .eq(ObjUtil.isNotNull(spaceLevel), "spaceLevel", spaceLevel);

        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("asc"), sortField);
        return queryWrapper;
    }

    @Override
    public void fillSpaceByLevel(Space space) {
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        ThrowUtils.throwException(spaceLevelEnum == null, ErrorCode.PARAMS_ERROR, "空间级别不存在");
        // 未指定初始值，则使用枚举的初始值
        if (space.getMaxSize() == null) {
            space.setMaxSize(spaceLevelEnum.getMaxSize());
        }
        if (space.getMaxCount() == null) {
            space.setMaxCount(spaceLevelEnum.getMaxCount());
        }
    }
}




