package com.ylh.picturego.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ylh.picturego.exception.ErrorCode;
import com.ylh.picturego.exception.ThrowUtils;
import com.ylh.picturego.manager.FileManager;
import com.ylh.picturego.model.dto.file.UploadResult;
import com.ylh.picturego.model.dto.picture.PictureUploadRequest;
import com.ylh.picturego.model.entity.Picture;
import com.ylh.picturego.model.entity.User;
import com.ylh.picturego.model.vo.PictureVO;
import com.ylh.picturego.service.PictureService;
import com.ylh.picturego.mapper.PictureMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;

/**
* @author V0411
* @description 针对表【picture(图片表)】的数据库操作Service实现
* @createDate 2026-10-16 16:24:36
*/
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {

    @Resource
    private FileManager fileManager;

    @Override
    public PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 校验参数
        ThrowUtils.throwException(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "未登录");
        // 判断新增还是修改
        Long picId = null;
        if (pictureUploadRequest != null) {
            picId = pictureUploadRequest.getId();
        }
        // 如果是修改，则校验图片是否存在
        if (picId != null) {
            boolean exists = this.lambdaQuery()
                    .eq(Picture::getId, picId)
                    .exists();
            ThrowUtils.throwException(!exists, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        // 上传图片，按照用户id划分目录
        String uploadPathPrefix = String.format("/public/%s", loginUser.getId());
        UploadResult uploadResult = fileManager.uploadPicture(multipartFile, uploadPathPrefix);
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
        // 操作数据库
        if (picId != null) {
            // 修改则补充id和更新时间
            newPicture.setId(picId);
            newPicture.setEditTime(new Date());
        }
        ThrowUtils.throwException(!this.saveOrUpdate(newPicture), ErrorCode.SYSTEM_ERROR, "上传数据库失败");
        return PictureVO.objToVo(newPicture);
    }
}




