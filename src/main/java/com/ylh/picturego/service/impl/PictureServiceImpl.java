package com.ylh.picturego.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ylh.picturego.exception.ErrorCode;
import com.ylh.picturego.exception.ThrowUtils;
import com.ylh.picturego.manager.FileManager;
import com.ylh.picturego.manager.upload.FilePictureUpload;
import com.ylh.picturego.manager.upload.PictureUploadTemplate;
import com.ylh.picturego.manager.upload.UrlPictureUpload;
import com.ylh.picturego.model.dto.file.UploadResult;
import com.ylh.picturego.model.dto.picture.PictureQueryRequest;
import com.ylh.picturego.model.dto.picture.PictureReviewRequest;
import com.ylh.picturego.model.dto.picture.PictureUploadRequest;
import com.ylh.picturego.model.entity.Picture;
import com.ylh.picturego.model.entity.User;
import com.ylh.picturego.model.enums.PictureReviewStatusEnum;
import com.ylh.picturego.model.vo.PictureVO;
import com.ylh.picturego.model.vo.UserVO;
import com.ylh.picturego.service.PictureService;
import com.ylh.picturego.mapper.PictureMapper;
import com.ylh.picturego.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author V0411
* @description 针对表【picture(图片表)】的数据库操作Service实现
* @createDate 2026-10-16 16:24:36
*/
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {

    @Resource
    private UserService userService;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwException(picture == null, ErrorCode.PARAMS_ERROR, "图片信息为空");
        // 从对象取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改信息时，id不能为空
        ThrowUtils.throwException(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR);
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwException(url.length() > 1024, ErrorCode.PARAMS_ERROR, "图片地址长度超过1024");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwException(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "图片介绍长度超过800");
        }
    }

    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 校验参数
        ThrowUtils.throwException(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "未登录");
        // 判断新增还是修改
        Long picId = null;
        if (pictureUploadRequest != null) {
            picId = pictureUploadRequest.getId();
        }
        // 如果是修改，则校验图片是否存在
        if (picId != null) {
            Picture oldPicture = this.getById(picId);
            ThrowUtils.throwException(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            // 仅允许用户自己和管理员修改图片
            ThrowUtils.throwException(
                    !oldPicture.getUserId().equals(loginUser.getId()) &&
                    !userService.isAdmin(loginUser),
                    ErrorCode.NO_AUTH_ERROR, "无权限修改图片"
            );
        }
        // 上传图片，按照用户id划分目录
        String uploadPathPrefix = String.format("/public/%s", loginUser.getId());
        // 根据inputSource的类型，区分上传方式
        // 默认使用文件上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadResult uploadResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        // 构造图片信息
        Picture newPicture = Picture.builder()
                .url(uploadResult.getUrl())
                .name(uploadResult.getName())
                .picSize(uploadResult.getPicSize())
                .picWidth(uploadResult.getPicWidth())
                .picHeight(uploadResult.getPicHeight())
                .picScale(uploadResult.getPicScale())
                .picFormat(uploadResult.getPicFormat())
                .userId(loginUser.getId())
                .build();
        // 补充审核参数
        this.fillReviewParams(newPicture, loginUser);
        // 操作数据库
        if (picId != null) {
            // 修改则补充id和更新时间
            newPicture.setId(picId);
            newPicture.setEditTime(new Date());
        }
        ThrowUtils.throwException(!this.saveOrUpdate(newPicture), ErrorCode.SYSTEM_ERROR, "上传数据库失败");
        return PictureVO.objToVo(newPicture);
    }

    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            pictureVO.setUserVO(UserVO.objToVo(user));
        }
        return pictureVO;
    }

    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表转封装对象列表，此时PictureVO对象中只包含用户id，不包含UserVO对象
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 填充用户信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).getFirst();
            }
            UserVO userVO = UserVO.objToVo(user);
            if (userVO == null) {
                userVO = UserVO.builder().userName("用户已注销").build();
            }
            pictureVO.setUserVO(userVO);
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
//        Date reviewTime = pictureQueryRequest.getReviewTime();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        // 排序字段和排序方式
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        // 从多字段搜索
        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.and(
                    qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText));
        }
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id)
                .eq(ObjUtil.isNotNull(userId), "userId", userId)
                .like(StrUtil.isNotBlank(name), "name", name)
                .like(StrUtil.isNotBlank(introduction), "introduction", introduction)
                .eq(StrUtil.isNotBlank(category), "category", category)
                .eq(ObjUtil.isNotNull(reviewStatus), "reviewStatus", reviewStatus)
                .like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage)
                .eq(ObjUtil.isNotNull(reviewerId), "reviewerId", reviewerId)
//                .eq(ObjUtil.isNotNull(reviewTime), "reviewTime", reviewTime)
                .eq(ObjUtil.isNotNull(picSize), "picSize", picSize)
                .eq(ObjUtil.isNotNull(picWidth), "picWidth", picWidth)
                .eq(ObjUtil.isNotNull(picHeight), "picHeight", picHeight)
                .eq(ObjUtil.isNotNull(picScale), "picScale", picScale)
                .like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        // JSON数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("asc"), sortField);
        return queryWrapper;
    }

    @Override
    public void reviewPicture(PictureReviewRequest pictureReviewRequest, User loginUser) {
        // 校验参数
        ThrowUtils.throwException(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR, "审核图片参数为空");
        Long id = pictureReviewRequest.getId();
        PictureReviewStatusEnum reviewStatus = PictureReviewStatusEnum.getEnumByCode(pictureReviewRequest.getReviewStatus());
        String reviewMessage = pictureReviewRequest.getReviewMessage();
        ThrowUtils.throwException(
                id == null ||
                reviewStatus == null ||
                reviewStatus.equals(PictureReviewStatusEnum.REVIEWING), // 不允许审核状态设置为审核中
                ErrorCode.PARAMS_ERROR);
        // 判断图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwException(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 校验审核状态是否重复
        ThrowUtils.throwException(
                oldPicture.getReviewStatus().equals(reviewStatus.getCode()),
                ErrorCode.PARAMS_ERROR, "审核状态未改变");
        // 审核通过且未填写意见时，默认填充“审核通过”
        if (reviewStatus == PictureReviewStatusEnum.PASS && StrUtil.isBlank(reviewMessage)) {
            reviewMessage = "审核通过";
        }
        // 数据库操作
        Picture newPicture = Picture.builder()
                .id(id)
                .reviewStatus(reviewStatus.getCode())
                .reviewMessage(reviewMessage)
                .reviewTime(new Date())
                .reviewerId(loginUser.getId())
                .build();
        ThrowUtils.throwException(!this.updateById(newPicture), ErrorCode.SYSTEM_ERROR, "审核图片失败");
    }

    /**
     * 填充审核参数
     *
     * @param picture 图片
     * @param loginUser 登录用户
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            // 管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getCode());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
        }else {
            // 普通用户待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getCode());
        }
    }
}




