package com.ylh.picturego.api.imagesearch.sub;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ylh.picturego.api.imagesearch.model.ImageSearchResult;
import com.ylh.picturego.exception.BusinessException;
import com.ylh.picturego.exception.ErrorCode;
import com.ylh.picturego.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 3.获取图片列表
 */
@Slf4j
public class GetImageListApi {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    /**
     * 获取图片列表
     *
     * @param url 请求的URL
     * @return 图片列表
     */
    public static List<ImageSearchResult> getImageList(String url) {
        ThrowUtils.throwException(StrUtil.isBlank(url), ErrorCode.PARAMS_ERROR, "图片列表url不能为空");
        try (HttpResponse httpResponse = HttpUtil.createGet(url)
                .header("User-Agent", USER_AGENT)
                .header("Referer", "https://graph.baidu.com/")
                .timeout(10000)
                .execute()) {
            // 获取响应内容
            int statusCode = httpResponse.getStatus();
            String body = httpResponse.body();
            // 处理响应结果
            if (statusCode == 200) {
                // 处理成功响应
                return processResponse(body);
            } else {
                // 处理错误响应
                log.error("获取图片列表失败, url={}, statusCode={}, body={}", url, statusCode, body);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取图片列表失败, url={}", url, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取图片列表失败");
        }
    }

    /**
     * 处理响应结果
     *
     * @param responsebody 接口返回的JSON字符串
     * @return 图片列表
     */
    private static List<ImageSearchResult> processResponse(String responsebody) {
        // 解析响应对象
        JSONObject jsonObject = new JSONObject(responsebody);
        ThrowUtils.throwException(!jsonObject.containsKey("data"), ErrorCode.OPERATION_ERROR, "未获取到图片列表");
        JSONObject data = jsonObject.getJSONObject("data");
        ThrowUtils.throwException(!data.containsKey("list"), ErrorCode.OPERATION_ERROR, "未获取到图片列表");
        JSONArray list = data.getJSONArray("list");
        return JSONUtil.toList(list, ImageSearchResult.class);
    }
}
