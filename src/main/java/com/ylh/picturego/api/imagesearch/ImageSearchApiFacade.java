package com.ylh.picturego.api.imagesearch;

import cn.hutool.core.util.StrUtil;
import com.ylh.picturego.api.imagesearch.model.ImageSearchResult;
import com.ylh.picturego.api.imagesearch.sub.GetImageFirstUrlApi;
import com.ylh.picturego.api.imagesearch.sub.GetImageListApi;
import com.ylh.picturego.api.imagesearch.sub.GetImagePageUrlApi;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

@Slf4j
public class ImageSearchApiFacade {

    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        // 百度未返回相似图片时页面里没有 firstUrl，按无搜索结果处理
        if (StrUtil.isBlank(imageFirstUrl)) {
            log.warn("百度搜图未返回相似图片, imagePageUrl={}", imagePageUrl);
            return Collections.emptyList();
        }
        List<ImageSearchResult> imageList = GetImageListApi.getImageList(imageFirstUrl);
        return imageList;
    }

    public static void main(String[] args) {
        List<ImageSearchResult> imageList = searchImage("https://cn.bing.com/th?id=OHR.StileCrossing_ZH-CN9811465935_UHD.jpg&rf=LaDigue_UHD.jpg&pid=hp&w=3840&h=2160&rs=1&c=4");
        System.out.println("结果列表：" + imageList);
    }
}
