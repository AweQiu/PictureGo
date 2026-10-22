package com.ylh.picturego.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ylh.picturego.annotation.AuthCheck;
import com.ylh.picturego.common.DeleteRequest;
import com.ylh.picturego.common.Result;
import com.ylh.picturego.common.ResultUtils;
import com.ylh.picturego.constant.UserConstant;
import com.ylh.picturego.exception.ErrorCode;
import com.ylh.picturego.exception.ThrowUtils;
import com.ylh.picturego.model.dto.picture.*;
import com.ylh.picturego.model.entity.Picture;
import com.ylh.picturego.model.entity.User;
import com.ylh.picturego.model.enums.PictureReviewStatusEnum;
import com.ylh.picturego.model.vo.PictureTagCategory;
import com.ylh.picturego.model.vo.PictureVO;
import com.ylh.picturego.service.PictureService;
import com.ylh.picturego.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/picture")
public class PictureController {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    /**
     * 上传图片到数据库和对象存储（可重复上传）
     * @param file 文件分片
     * @param pictureUploadRequest 图片上传请求
     * @param request 请求
     * @return 图片信息
     */
    @PostMapping("/upload")
    public Result<PictureVO> uploadPicture(
            @RequestPart MultipartFile file,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request
    ) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(file, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 删除图片
     * @param deleteRequest 删除请求
     * @param request 请求
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    public Result<Boolean> deletePicture(
            @RequestBody DeleteRequest deleteRequest,
            HttpServletRequest request
    ) {
        ThrowUtils.throwException(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        Long id = deleteRequest.getId();
        User loginUser = userService.getLoginUser(request);
        Picture oldPicture = pictureService.getById(id);
        // 存在性校验
        ThrowUtils.throwException(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 权限校验，仅允许本人或管理员
        ThrowUtils.throwException(
                !oldPicture.getUserId().equals(loginUser.getId()) &&
                !loginUser.getUserRole().equals(UserConstant.ADMIN_ROLE),
                ErrorCode.OPERATION_ERROR, "无权限删除"
        );
        // 删除图片
        boolean result = pictureService.removeById(id);
        ThrowUtils.throwException(!result, ErrorCode.OPERATION_ERROR, "图片删除失败");
        return ResultUtils.success(true);
    }

    /**
     * 更新图片（管理员）
     * @param pictureUpdateRequest 图片更新请求
     * @return 是否更新成功
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Boolean> updatePicture(
            @RequestBody PictureUpdateRequest pictureUpdateRequest,
            HttpServletRequest request
    ) {
        ThrowUtils.throwException(pictureUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        // DTO类转实体类
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureUpdateRequest, picture);
        // List列表转String
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        // 校验图片
        pictureService.validPicture(picture);
        // 存在性校验
        long id = pictureUpdateRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwException(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 补充审核参数
        pictureService.fillReviewParams(picture, userService.getLoginUser(request));
        // 操作数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwException(!result, ErrorCode.OPERATION_ERROR, "图片更新失败");
        return ResultUtils.success(true);
    }

    /**
     * 编辑图片（用户）
     * @param pictureEditRequest 图片编辑请求
     * @param request 请求
     * @return 是否编辑成功
     */
    @PostMapping("/edit")
    public Result<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        ThrowUtils.throwException(pictureEditRequest == null || pictureEditRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        // DTO类转实体类
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureEditRequest, picture);
        // List列表转String
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 校验图片
        pictureService.validPicture(picture);
        User loginUser = userService.getLoginUser(request);
        // 存在性校验
        long id = pictureEditRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwException(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 权限校验，仅允许本人或管理员
        ThrowUtils.throwException(
                !oldPicture.getUserId().equals(loginUser.getId()) &&
                !loginUser.getUserRole().equals(UserConstant.ADMIN_ROLE),
                ErrorCode.OPERATION_ERROR, "无权限编辑"
        );
        // 补充审核参数
        pictureService.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwException(!result, ErrorCode.OPERATION_ERROR, "图片编辑失败");
        return ResultUtils.success(true);
    }

    /**
     * 根据ID获取图片
     * @param id 图片ID
     * @return 图片
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Picture> getPictureById(@RequestParam Long id) {
        ThrowUtils.throwException(id <= 0, ErrorCode.PARAMS_ERROR, "图片ID有误");
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwException(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        return ResultUtils.success(picture);
    }

    /**
     * 根据ID获取图片VO
     * @param id 图片ID
     * @return 图片VO
     */
    @GetMapping("/get/vo")
    public Result<PictureVO> getPictureVOById(@RequestParam Long id, HttpServletRequest request) {
        ThrowUtils.throwException(id <= 0, ErrorCode.PARAMS_ERROR, "图片ID有误");
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwException(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 只返回审核通过的图片
        ThrowUtils.throwException(picture.getReviewStatus() != PictureReviewStatusEnum.PASS.getCode(), ErrorCode.OPERATION_ERROR, "图片审核未通过");
        // 获取图片VO
        PictureVO pictureVO = pictureService.getPictureVO(picture, request);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 分页获取图片列表
     * @param pictureQueryRequest 图片查询请求
     * @return 图片列表
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(
                new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest)
        );
        return ResultUtils.success(picturePage);
    }
    /**
     * 分页获取图片封装类列表
     * @param pictureQueryRequest 图片查询请求
     * @return 图片封装类列表
     */
    @PostMapping("/list/page/vo")
    public Result<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwException(size > 20, ErrorCode.PARAMS_ERROR, "分页大小不能超过20");
        // 只允许查询通过审核的图片
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getCode());
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(
                new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest)
        );
        // 获取封装类
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
    }

    /**
     * 获取图片标签和分类（静态）
     * @return 图片标签和分类
     */
    @GetMapping("/tag_category")
    public Result<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        // 静态标签列表
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "动漫", "商务", "其他");
        // 静态分类列表
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报", "二次元", "其他");
        // 赋值
        pictureTagCategory.setTags(tagList);
        pictureTagCategory.setCategories(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Boolean> reviewPicture(@RequestBody PictureReviewRequest pictureReviewRequest, HttpServletRequest request) {
        ThrowUtils.throwException(pictureReviewRequest == null || pictureReviewRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        // 获取登录用户
        User loginUser = userService.getLoginUser(request);

        pictureService.reviewPicture(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }
}
