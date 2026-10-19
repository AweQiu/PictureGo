package com.ylh.picturego.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ylh.picturego.model.dto.picture.PictureQueryRequest;
import com.ylh.picturego.model.dto.picture.PictureUploadRequest;
import com.ylh.picturego.model.entity.Picture;
import com.ylh.picturego.model.entity.User;
import com.ylh.picturego.model.vo.PictureVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

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
     * @param multipartFile 多部分文件
     * @param pictureUploadRequest 图片上传请求
     * @param loginUser 登录用户
     * @return 图片信息
     */
    PictureVO uploadPicture(MultipartFile multipartFile,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

    /**
     * 获取图片信息
     * @param picture 图片
     * @param request 请求
     * @return 图片信息
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 获取图片信息分页
     * @param picturePage 图片分页
     * @param request 请求
     * @return 图片信息分页
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 获取查询条件
     * @param pictureQueryRequest 图片查询请求
     * @return 查询条件
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);
}
