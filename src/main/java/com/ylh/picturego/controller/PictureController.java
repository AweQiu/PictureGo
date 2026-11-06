package com.ylh.picturego.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ylh.picturego.annotation.AuthCheck;
import com.ylh.picturego.api.imagesearch.model.ImageSearchResult;
import com.ylh.picturego.common.DeleteRequest;
import com.ylh.picturego.common.Result;
import com.ylh.picturego.common.ResultUtils;
import com.ylh.picturego.constant.UserConstant;
import com.ylh.picturego.exception.ErrorCode;
import com.ylh.picturego.exception.ThrowUtils;
import com.ylh.picturego.model.dto.picture.*;
import com.ylh.picturego.model.entity.Picture;
import com.ylh.picturego.model.entity.User;
import com.ylh.picturego.model.vo.PictureTagCategory;
import com.ylh.picturego.model.vo.PictureVO;
import com.ylh.picturego.service.PictureService;
import com.ylh.picturego.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/picture")
public class PictureController {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    /**
     * 上传本地图片
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
        return ResultUtils.success(pictureService.uploadPicture(file, pictureUploadRequest, loginUser));
    }

    /**
     * 上传网络图片
     * @param pictureUploadRequest 图片上传请求
     * @param request 请求
     * @return 图片信息
     */
    @PostMapping("/upload/url")
    public Result<PictureVO> uploadPictureByUrl(
            @RequestBody PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request
    ) {
        User loginUser = userService.getLoginUser(request);
        String url = pictureUploadRequest.getUrl();
        return ResultUtils.success(pictureService.uploadPicture(url, pictureUploadRequest, loginUser));
    }

    /**
     * 批量上传图片
     * @param pictureUploadByBatchRequest 图片上传请求
     * @param request 请求
     * @return 图片信息
     */
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Integer> uploadPictureByBatch(
            @RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
            HttpServletRequest request
    ) {
        ThrowUtils.throwException(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser));
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
        Long pictureId = deleteRequest.getId();
        User loginUser = userService.getLoginUser(request);
        pictureService.deletePicture(pictureId, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 更新图片（管理员）
     * @param pictureUpdateRequest 图片更新请求
     * @param request 请求
     * @return 是否更新成功
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Boolean> updatePicture(
            @RequestBody PictureUpdateRequest pictureUpdateRequest,
            HttpServletRequest request
    ) {
        User loginUser = userService.getLoginUser(request);
        pictureService.updatePicture(pictureUpdateRequest, loginUser);
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
        User loginUser = userService.getLoginUser(request);
        pictureService.editPicture(pictureEditRequest, loginUser);
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
        return ResultUtils.success(pictureService.getPictureById(id));
    }

    /**
     * 根据ID获取图片VO
     * @param id 图片ID
     * @param request 请求
     * @return 图片VO
     */
    @GetMapping("/get/vo")
    public Result<PictureVO> getPictureVOById(@RequestParam Long id, HttpServletRequest request) {
        User loginUser = userService.getLoginUserOrNull(request);
        return ResultUtils.success(pictureService.getPictureVOById(id, loginUser));
    }

    /**
     * 分页获取图片列表
     * @param pictureQueryRequest 图片查询请求
     * @return 图片列表
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        return ResultUtils.success(pictureService.listPictureByPage(pictureQueryRequest));
    }

    /**
     * 分页获取图片封装类列表（普通版，用于私有空间）
     * @param pictureQueryRequest 图片查询请求
     * @param request 请求
     * @return 图片封装类列表
     */
    @PostMapping("/list/page/vo")
    public Result<Page<PictureVO>> listPictureVOByPage(
            @RequestBody PictureQueryRequest pictureQueryRequest,
            HttpServletRequest request
    ) {
        User loginUser = userService.getLoginUserOrNull(request);
        return ResultUtils.success(pictureService.listPictureVOPage(pictureQueryRequest, loginUser));
    }

    /**
     * 分页获取图片封装类列表（缓存版，用于公共图库）
     * @param pictureQueryRequest 图片查询请求
     * @param request 请求
     * @return 图片封装类列表
     */
    @PostMapping("/list/page/vo/cache")
    public Result<Page<PictureVO>> listPictureVOByPageWithCache(
            @RequestBody PictureQueryRequest pictureQueryRequest,
            HttpServletRequest request
    ) {
        User loginUser = userService.getLoginUserOrNull(request);
        return ResultUtils.success(pictureService.listPictureVOPageWithCache(pictureQueryRequest, loginUser));
    }

    /**
     * 获取图片标签和分类（静态）
     * @return 标签和分类
     */
    @GetMapping("/tag_category")
    public Result<PictureTagCategory> listPictureTagCategory() {
        return ResultUtils.success(pictureService.getPictureTagCategory());
    }

    /**
     * 审核图片（管理员）
     * @param pictureReviewRequest 图片审核请求
     * @param request 请求
     * @return 是否审核成功
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<Boolean> reviewPicture(@RequestBody PictureReviewRequest pictureReviewRequest, HttpServletRequest request) {
        ThrowUtils.throwException(pictureReviewRequest == null || pictureReviewRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        // 获取登录用户
        User loginUser = userService.getLoginUser(request);
        pictureService.reviewPicture(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 根据图片搜索图片
     * @param searchPictureByPictureRequest 图片搜索请求
     * @return 图片搜索结果
     */
    @PostMapping("/search/picture")
    public Result<List<ImageSearchResult>> searchPictureByPicture(@RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest) {
        ThrowUtils.throwException(searchPictureByPictureRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(pictureService.searchPictureByPicture(searchPictureByPictureRequest));
    }
}
