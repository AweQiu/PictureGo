package com.ylh.picturego.controller;

import com.ylh.picturego.annotation.AuthCheck;
import com.ylh.picturego.common.Result;
import com.ylh.picturego.common.ResultUtils;
import com.ylh.picturego.constant.UserConstant;
import com.ylh.picturego.model.dto.picture.PictureUploadRequest;
import com.ylh.picturego.model.entity.User;
import com.ylh.picturego.model.vo.PictureVO;
import com.ylh.picturego.service.PictureService;
import com.ylh.picturego.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/picture")
public class PictureController {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    /**
     * 上传图片（可重新上传）
     *
     * @param file 文件分片
     * @param pictureUploadRequest 图片上传请求
     * @param request  请求
     * @return 图片信息
     */
    @PostMapping("/upload")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Result<PictureVO> uploadPicture(
            @RequestPart MultipartFile file,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(file, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }
}
