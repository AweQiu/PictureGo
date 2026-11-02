package com.ylh.picturego.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ylh.picturego.exception.BusinessException;
import com.ylh.picturego.exception.ErrorCode;
import com.ylh.picturego.exception.ThrowUtils;
import com.ylh.picturego.manager.CosManager;
import com.ylh.picturego.manager.cache.MultiLevelCache;
import com.ylh.picturego.manager.upload.FilePictureUpload;
import com.ylh.picturego.manager.upload.PictureUploadTemplate;
import com.ylh.picturego.manager.upload.UrlPictureUpload;
import com.ylh.picturego.model.dto.file.UploadPictureResult;
import com.ylh.picturego.model.dto.picture.*;
import com.ylh.picturego.model.entity.Picture;
import com.ylh.picturego.model.entity.Space;
import com.ylh.picturego.model.entity.User;
import com.ylh.picturego.model.enums.PictureReviewStatusEnum;
import com.ylh.picturego.model.vo.PictureTagCategory;
import com.ylh.picturego.model.vo.PictureVO;
import com.ylh.picturego.model.vo.UserVO;
import com.ylh.picturego.service.PictureService;
import com.ylh.picturego.mapper.PictureMapper;
import com.ylh.picturego.service.SpaceService;
import com.ylh.picturego.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.Arrays;
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
@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {

    /**
     * 图片列表缓存 key 前缀（沿用原 Controller 中的取值，保证历史缓存 key 不变）
     */
    private static final String LIST_PICTURE_VO_CACHE_PREFIX = "picturego:listPictureVOByPage";

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private CosManager cosManager;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private MultiLevelCache multiLevelCache;

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
        ThrowUtils.throwException(pictureUploadRequest == null, ErrorCode.PARAMS_ERROR, "上传图片参数为空");
        // 校验空间
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null && spaceId > 0) {
            // 校验空间是否存在
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwException(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 是否有空间权限
            ThrowUtils.throwException(!space.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "无空间权限");
            // 校验空间额度
            ThrowUtils.throwException(space.getTotalCount() >= space.getMaxCount(), ErrorCode.NO_AUTH_ERROR, "空间图片数量超出限制");
            ThrowUtils.throwException(space.getTotalSize() >= space.getMaxSize(), ErrorCode.NO_AUTH_ERROR, "空间图片大小超出限制");
        }
        Long picId = pictureUploadRequest.getId();
        boolean isUpdate = picId != null && picId > 0;
        // 如果是修改，则校验图片是否存在
        Picture oldPicture;
        if (isUpdate) {
            oldPicture = this.getById(picId);
            ThrowUtils.throwException(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            // 仅允许用户自己和管理员修改图片
            ThrowUtils.throwException(
                    !oldPicture.getUserId().equals(loginUser.getId()) &&
                    !userService.isAdmin(loginUser),
                    ErrorCode.NO_AUTH_ERROR, "无权限修改图片"
            );
            // 校验图片空间是否一致
            // 没有传递spaceId，则复用原有spaceId（兼容公共图库，公共图库图片无spaceId）
            if (spaceId == null) {
                spaceId = oldPicture.getSpaceId();
            } else {
                ThrowUtils.throwException(!oldPicture.getSpaceId().equals(spaceId), ErrorCode.NO_AUTH_ERROR, "图片空间不一致");
            }
        } else {
            oldPicture = null;
        }
        // 上传图片，按照用户id划分目录
        String uploadPathPrefix = null;
        if (spaceId == null) {
            // 公共图库
            uploadPathPrefix = String.format("/public/%s", loginUser.getId());
        }else {
            // 空间图库
            uploadPathPrefix = String.format("/space/%s", spaceId);
        }
        // 根据inputSource的类型，区分上传方式，默认使用文件上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        // 如果inputSource是String类型，则使用url上传方式
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        // 构造图片信息
        Picture newPicture = Picture.builder()
                .url(uploadPictureResult.getUrl())
                .thumbnailUrl(uploadPictureResult.getThumbnailUrl())
                .name(uploadPictureResult.getName())
                .picSize(uploadPictureResult.getPicSize())
                .picWidth(uploadPictureResult.getPicWidth())
                .picHeight(uploadPictureResult.getPicHeight())
                .picScale(uploadPictureResult.getPicScale())
                .picFormat(uploadPictureResult.getPicFormat())
                .userId(loginUser.getId())
                .spaceId(spaceId) // 指定空间id
                .build();
        // 逐字段填充扩展参数,外层传了才覆盖，未传则保留上传解析值
        if (StrUtil.isNotBlank(pictureUploadRequest.getName())) {
            newPicture.setName(pictureUploadRequest.getName());
        }
        if (pictureUploadRequest.getIntroduction() != null) {
            newPicture.setIntroduction(pictureUploadRequest.getIntroduction());
        }
        if (pictureUploadRequest.getCategory() != null) {
            newPicture.setCategory(pictureUploadRequest.getCategory());
        }
        if (CollUtil.isNotEmpty(pictureUploadRequest.getTags())) {
            newPicture.setTags(JSONUtil.toJsonStr(pictureUploadRequest.getTags()));
        }
        // 补充审核参数
        this.fillReviewParams(newPicture, loginUser);
        // 操作数据库
        if (picId != null) {
            // 修改则补充id和更新时间
            newPicture.setId(picId);
            newPicture.setEditTime(new Date());
        }
        // 开启事务
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            // 插入数据
            boolean result = this.saveOrUpdate(newPicture);
            ThrowUtils.throwException(!result, ErrorCode.SYSTEM_ERROR, "上传数据库失败");
            // 私有空间操作
            if (finalSpaceId != null) {
                // 校验空间
                Space space = spaceService.getById(finalSpaceId);
                ThrowUtils.throwException(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
                // 计算空间额度变化
                long deltaSize = isUpdate
                        ? newPicture.getPicSize() - oldPicture.getPicSize()
                        : newPicture.getPicSize();
                // 更新空间使用额度
                boolean updateSpaceUsage = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize + " + deltaSize)
                        .setSql("totalCount = totalCount + " + (isUpdate ? 0 : 1))
                        .update();
                ThrowUtils.throwException(!updateSpaceUsage, ErrorCode.SYSTEM_ERROR, "空间额度更新失败");
            }
            return newPicture;
        });
        // 删除旧图片
        if (oldPicture != null) {
            this.clearPictureFile(oldPicture);
        }
        return PictureVO.objToVo(newPicture);
    }

    @Override
    public void deletePicture(Long pictureId, User loginUser) {
        ThrowUtils.throwException(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR, "图片id异常");
        ThrowUtils.throwException(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "未登录");
        // 存在性校验
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwException(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 空间权限校验
        this.checkPictureAuth(oldPicture, loginUser);
        // 开启事务
        transactionTemplate.execute(status -> {
            // 操作数据库
            boolean result = this.removeById(pictureId);
            ThrowUtils.throwException(!result, ErrorCode.OPERATION_ERROR, "图片删除失败");
            // 私有空间操作
            Long spaceId = oldPicture.getSpaceId();
            if (spaceId != null) {
                // 校验空间
                Space space = spaceService.getById(spaceId);
                ThrowUtils.throwException(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
                // 释放额度
                boolean updateSpaceUsage = spaceService.lambdaUpdate()
                        .eq(Space::getId, spaceId)
                        .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
                        .setSql("totalCount = totalCount - 1")
                        .update();
                ThrowUtils.throwException(!updateSpaceUsage, ErrorCode.SYSTEM_ERROR, "空间额度释放失败");
            }
            return true;
        });
        // 清理图片文件
        this.clearPictureFile(oldPicture);
    }

    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        // DTO类转实体类
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureEditRequest, picture);
        // List列表转String
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 校验图片
        this.validPicture(picture);
        // 存在性校验
        long id = pictureEditRequest.getId();
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwException(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 空间权限校验
        this.checkPictureAuth(oldPicture, loginUser);
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = this.updateById(picture);
        ThrowUtils.throwException(!result, ErrorCode.OPERATION_ERROR, "图片编辑失败");
    }

    @Override
    public void updatePicture(PictureUpdateRequest pictureUpdateRequest, User loginUser) {
        ThrowUtils.throwException(pictureUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        // DTO类转实体类
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureUpdateRequest, picture);
        // List列表转String
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        // 校验图片
        this.validPicture(picture);
        // 存在性校验
        long id = pictureUpdateRequest.getId();
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwException(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 操作数据库
        ThrowUtils.throwException(!this.updateById(picture), ErrorCode.OPERATION_ERROR, "图片更新失败");
    }

    @Override
    public PictureVO getPictureVO(Picture picture) {
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
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表转封装对象列表，此时PictureVO对象中不包含UserVO对象
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
    public Picture getPictureById(Long id) {
        ThrowUtils.throwException(id <= 0, ErrorCode.PARAMS_ERROR, "图片ID有误");
        // 查询数据库
        Picture picture = this.getById(id);
        ThrowUtils.throwException(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        return picture;
    }

    @Override
    public PictureVO getPictureVOById(Long id, User loginUser) {
        ThrowUtils.throwException(id <= 0, ErrorCode.PARAMS_ERROR, "图片ID有误");
        // 查询数据库
        Picture picture = this.getById(id);
        ThrowUtils.throwException(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 空间权限校验
        Long spaceId = picture.getSpaceId();
        if (spaceId != null) {
            // 私有空间图片，必须登录且是空间主人
            ThrowUtils.throwException(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
            this.checkPictureAuth(picture, loginUser);
        }
        // 只返回审核通过的图片
        ThrowUtils.throwException(
                picture.getReviewStatus() != PictureReviewStatusEnum.PASS.getValue(),
                ErrorCode.OPERATION_ERROR, "图片审核未通过");
        // 获取图片VO
        return this.getPictureVO(picture);
    }

    @Override
    public Page<Picture> listPictureByPage(PictureQueryRequest pictureQueryRequest) {
        // 管理员只能管理公共图库
        pictureQueryRequest.setIsPublic(true);
        // 查询数据库
        return this.page(
                new Page<>(pictureQueryRequest.getCurrent(), pictureQueryRequest.getPageSize()),
                this.getQueryWrapper(pictureQueryRequest));
    }

    @Override
    public Page<PictureVO> listPictureVOPage(PictureQueryRequest pictureQueryRequest, User loginUser) {
        this.prepareListQuery(pictureQueryRequest, loginUser);
        return this.queryPictureVOPage(pictureQueryRequest);
    }

    @Override
    public Page<PictureVO> listPictureVOPageWithCache(PictureQueryRequest pictureQueryRequest, User loginUser) {
        // 先做分页校验、空间权限校验和查询条件规范化，保证缓存 key 正确且私有空间不越权
        this.prepareListQuery(pictureQueryRequest, loginUser);
        // 读多级缓存，未命中则回源查询并回填缓存
        return multiLevelCache.getOrLoad(
                LIST_PICTURE_VO_CACHE_PREFIX,
                pictureQueryRequest,
                new TypeReference<Page<PictureVO>>() {},
                () -> this.queryPictureVOPage(pictureQueryRequest));
    }

    @Override
    public PictureTagCategory getPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        // 静态标签列表
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "动漫", "商务", "其他");
        // 静态分类列表
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报", "二次元", "其他");
        // 赋值
        pictureTagCategory.setTags(tagList);
        pictureTagCategory.setCategories(categoryList);
        return pictureTagCategory;
    }

    /**
     * 图片列表查询的公共前置处理：分页校验 + 空间权限校验 + 查询条件规范化
     *
     * @param pictureQueryRequest 图片查询请求
     * @param loginUser 登录用户（允许为 null，公共图库支持匿名访问）
     */
    private void prepareListQuery(PictureQueryRequest pictureQueryRequest, User loginUser) {
        // 限制爬虫
        ThrowUtils.throwException(pictureQueryRequest.getPageSize() > 20, ErrorCode.PARAMS_ERROR, "分页大小不能超过20");
        // 空间权限校验
        Long spaceId = pictureQueryRequest.getSpaceId();
        if (spaceId == null) {
            // 公共图库
            // 只允许查询通过审核的图片
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setIsPublic(true);
        } else {
            // 私有空间
            ThrowUtils.throwException(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwException(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            ThrowUtils.throwException(!loginUser.getId().equals(space.getUserId()), ErrorCode.NO_AUTH_ERROR, "无空间权限");
            pictureQueryRequest.setIsPublic(false);
        }
    }

    /**
     * 查询图片分页并转换为 PictureVO 分页
     *
     * @param pictureQueryRequest 图片查询请求
     * @return 图片封装类分页
     */
    private Page<PictureVO> queryPictureVOPage(PictureQueryRequest pictureQueryRequest) {
        // 查询数据库
        Page<Picture> picturePage = this.page(
                new Page<>(pictureQueryRequest.getCurrent(), pictureQueryRequest.getPageSize()),
                this.getQueryWrapper(pictureQueryRequest));
        // 获取封装类
        return this.getPictureVOPage(picturePage);
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
        Long spaceId = pictureQueryRequest.getSpaceId();
        boolean isPublic = BooleanUtil.isTrue(pictureQueryRequest.getIsPublic());
        // 查询时间范围
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
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
                .eq(ObjUtil.isNotNull(spaceId), "spaceId", spaceId)
                .isNull(isPublic, "spaceId")
                .like(StrUtil.isNotBlank(name), "name", name)
                .like(StrUtil.isNotBlank(introduction), "introduction", introduction)
                .eq(StrUtil.isNotBlank(category), "category", category)
                .eq(ObjUtil.isNotNull(reviewStatus), "reviewStatus", reviewStatus)
                .like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage)
                .eq(ObjUtil.isNotNull(reviewerId), "reviewerId", reviewerId)
//                .eq(ObjUtil.isNotNull(reviewTime), "reviewTime", reviewTime)
                .ge(ObjUtil.isNotNull(startEditTime), "editTime", startEditTime) // 开始编辑时间大于等于
                .le(ObjUtil.isNotNull(endEditTime), "editTime", endEditTime) // 结束编辑时间小于等于
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
        PictureReviewStatusEnum reviewStatus = PictureReviewStatusEnum.getEnumByValue(pictureReviewRequest.getReviewStatus());
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
                oldPicture.getReviewStatus().equals(reviewStatus.getValue()),
                ErrorCode.PARAMS_ERROR, "审核状态未改变");
        // 审核通过且未填写意见时，默认填充“审核通过”
        if (reviewStatus == PictureReviewStatusEnum.PASS && StrUtil.isBlank(reviewMessage)) {
            reviewMessage = "审核通过";
        }
        // 数据库操作
        Picture newPicture = Picture.builder()
                .id(id)
                .reviewStatus(reviewStatus.getValue())
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
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
        }else {
            // 普通用户
            if (picture.getSpaceId() != null) {
                // 私有空间，自动过审
                picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
                picture.setReviewMessage("私有空间自动过审");
            }else {
                // 公共图库，普通用户待审核
                picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
            }
        }
    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        // 校验参数
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwException(count > 30, ErrorCode.PARAMS_ERROR, "数量不能超过30");
        // 获取名称前缀，默认使用搜索关键词
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }
        // 抓取内容
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document = null;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取网页失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取网页失败");
        }
        // 解析内容
        Element div = document.getElementsByClass("dgControl").first();
        if (div == null) {
            log.error("获取元素失败");
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        Elements imgElementList = div.select("img.mimg");
        // 遍历元素，依次处理上传图片
        int uploadedCount = 0;
        for (Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("链接为空，已跳过：{}", fileUrl);
                continue;
            }
            // 处理图片url，防止转义或和对象存储冲突
            fileUrl = StrUtil.subBefore(fileUrl, "?", false);
            // 上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setName(namePrefix + "_" + (uploadedCount + 1));
            pictureUploadRequest.setUrl(fileUrl);
            try {
                PictureVO pictureVO = uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("上传图片成功，新图片id为：{}", pictureVO.getId());
                uploadedCount++;
            } catch (Exception e) {
                log.error("上传图片失败，链接为：{}", fileUrl, e);
                continue;
            }
            if (uploadedCount >= count) break;
        }
        return uploadedCount;
    }

    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 判断图片是否被多条记录使用
        String pictureUrl = oldPicture.getUrl();
        long count = lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        if (count > 1) {
            return; // 图片被多条记录使用，不删除
        }
        // 删除对象存储中的图片
        cosManager.deleteObject(pictureUrl);
        // 删除对象存储中的缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl);
        }
    }

    @Override
    public void checkPictureAuth(Picture picture, User loginUser) {
        Long spaceId = picture.getSpaceId();
        Long loginUserId = loginUser.getId();
        if (spaceId == null) {
            // 公共图库，仅本人和管理员可操作
            ThrowUtils.throwException(
                    !loginUserId.equals(picture.getUserId()) &&
                    !userService.isAdmin(loginUser),
                    ErrorCode.NO_AUTH_ERROR, "无权限操作"
            );
        }else {
            // 私有图库，仅本人可操作
            ThrowUtils.throwException(
                    !loginUserId.equals(picture.getUserId()),
                    ErrorCode.NO_AUTH_ERROR, "无权限操作"
            );
        }

    }
}




