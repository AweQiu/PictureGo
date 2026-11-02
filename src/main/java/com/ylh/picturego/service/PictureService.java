package com.ylh.picturego.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ylh.picturego.model.dto.picture.*;
import com.ylh.picturego.model.entity.Picture;
import com.ylh.picturego.model.entity.User;
import com.ylh.picturego.model.vo.PictureTagCategory;
import com.ylh.picturego.model.vo.PictureVO;

/**
* @author V0411
* @description 针对表【picture(图片表)】的数据库操作Service
* @createDate 2026-10-16 16:24:36
*/
public interface PictureService extends IService<Picture> {

    /**
     * 校验图片
     * @param picture 图片
     */
    void validPicture(Picture picture);

    /**
     * 上传图片
      * @param inputSource 输入源
     * @param pictureUploadRequest 图片上传请求
     * @param loginUser 登录用户
     * @return 图片信息
     */
    PictureVO uploadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

    /**
     * 删除图片
     * @param pictureId 图片id
     * @param loginUser 登录用户
     */
    void deletePicture(Long pictureId, User loginUser);

    /**
     * 编辑图片
     * @param pictureEditRequest 图片编辑请求
     * @param loginUser 登录用户
     */
    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    /**
     * 更新图片（管理员）
     * @param pictureUpdateRequest 图片更新请求
     * @param loginUser 登录用户
     */
    void updatePicture(PictureUpdateRequest pictureUpdateRequest, User loginUser);

    /**
     * 获取图片信息
     * @param picture 图片
     * @return 图片信息
     */
    PictureVO getPictureVO(Picture picture);

    /**
     * 获取图片信息分页
     * @param picturePage 图片分页
     * @return 图片信息分页
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage);

    /**
     * 根据 id 获取图片（管理员）
     * @param id 图片 id
     * @return 图片
     */
    Picture getPictureById(Long id);

    /**
     * 根据 id 获取图片封装类
     * @param id 图片 id
     * @param loginUser 登录用户（允许为 null，公共图库支持匿名访问）
     * @return 图片封装类
     */
    PictureVO getPictureVOById(Long id, User loginUser);

    /**
     * 分页查询图片（管理员，仅公共图库）
     * @param pictureQueryRequest 图片查询请求
     * @return 图片分页
     */
    Page<Picture> listPictureByPage(PictureQueryRequest pictureQueryRequest);

    /**
     * 分页查询图片封装类（公共图库 / 私有空间）
     * @param pictureQueryRequest 图片查询请求
     * @param loginUser 登录用户（允许为 null，公共图库支持匿名访问）
     * @return 图片封装类分页
     */
    Page<PictureVO> listPictureVOPage(PictureQueryRequest pictureQueryRequest, User loginUser);

    /**
     * 分页查询图片封装类（带多级缓存，公共图库 / 私有空间）
     * @param pictureQueryRequest 图片查询请求
     * @param loginUser 登录用户（允许为 null，公共图库支持匿名访问）
     * @return 图片封装类分页
     */
    Page<PictureVO> listPictureVOPageWithCache(PictureQueryRequest pictureQueryRequest, User loginUser);

    /**
     * 获取图片标签和分类（静态）
     * @return 标签和分类
     */
    PictureTagCategory getPictureTagCategory();

    /**
     * 获取查询条件
     * @param pictureQueryRequest 图片查询请求
     * @return 查询条件
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 审核图片
     * @param pictureReviewRequest 图片审核请求
     * @param loginUser 登录用户
     */
    void reviewPicture(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 填充审核参数
     * @param picture 图片
     * @param loginUser 登录用户
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 批量上传图片
     * @param pictureUploadByBatchRequest 图片批量上传请求
     * @param loginUser 登录用户
     * @return 图片数量
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);

    /**
     * 清理图片文件（异步）
     * @param oldPicture 图片
     */
    void clearPictureFile(Picture oldPicture);

    /**
     * 检查图片权限
     * @param picture 图片
     * @param loginUser 登录用户
     */
    void checkPictureAuth(Picture picture, User loginUser);
}
