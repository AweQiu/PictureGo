package com.ylh.picturego.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ylh.picturego.model.dto.picture.PictureUploadRequest;
import com.ylh.picturego.model.entity.Picture;
import com.ylh.picturego.model.entity.User;
import com.ylh.picturego.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

/**
* @author V0411
* @description 针对表【picture(图片表)】的数据库操作Service
* @createDate 2026-10-16 16:24:36
*/
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     *
     * @param multipartFile 多部分文件
     * @param pictureUploadRequest 图片上传请求
     * @param loginUser 登录用户
     * @return 图片信息
     */
    PictureVO uploadPicture(MultipartFile multipartFile,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);
}
