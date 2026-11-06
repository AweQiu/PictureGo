package com.ylh.picturego.api.imagesearch.sub;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.ylh.picturego.exception.BusinessException;
import com.ylh.picturego.exception.ErrorCode;
import com.ylh.picturego.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 1.获取图片搜索页面url
 */
@Slf4j
public class GetImagePageUrlApi {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    /**
     * 获取图片搜索页面url
     * <p>
     * 采用“先下载到本地、再以 multipart 文件上传”的方式，而非 URL 拉取模式（image_source=PC_UPLOAD_URL）。
     * 原因：URL 模式下百度服务器会拒绝或拉取失败部分站点（如防盗链、按 TLS 指纹拦截非浏览器客户端的 CDN），
     * 返回 {"status":1002,"msg":"Params illegal"}；而由本项目服务端下载后直接传文件，成功率和可控性更高。
     */
    public static String getImagePageUrl(String imageUrl) {
        ThrowUtils.throwException(StrUtil.isBlank(imageUrl), ErrorCode.PARAMS_ERROR, "图片url不能为空");
        // 先将图片下载到本地临时文件
        File tempFile = null;
        try {
            tempFile = File.createTempFile("picture-search-", ".tmp");
            HttpUtil.downloadFile(imageUrl, tempFile);
            ThrowUtils.throwException(tempFile.length() == 0, ErrorCode.OPERATION_ERROR, "图片下载失败");
            Map<String, Object> formData = new HashMap<>();
            formData.put("image", tempFile);
            formData.put("tn", "pc");
            formData.put("from", "pc");
            formData.put("image_source", "PC_UPLOAD_SEARCH_FILE");
            // 获取当前时间
            long uptime = System.currentTimeMillis();
            // 构建请求
            String url = "https://graph.baidu.com/upload?uptime=" + uptime;
            // 发送请求
            try (HttpResponse httpResponse = HttpRequest.post(url)
                    .form(formData)
                    .header("User-Agent", USER_AGENT)
                    // 百度新版风控要求携带 acs-token 请求头，缺失或格式不对会返回 {"status":1,"msg":"Reject"}
                    .header("acs-token", RandomUtil.randomString(1))
                    .timeout(5000)
                    .execute()
            ) {
                ThrowUtils.throwException(httpResponse.getStatus() != HttpStatus.HTTP_OK, ErrorCode.OPERATION_ERROR, "接口调用失败");
                // 解析响应
                String body = httpResponse.body();
                log.info("百度搜图接口原始响应: {}", body);
                Map<String, Object> result = JSONUtil.toBean(body, Map.class);
                // 处理响应结果
                ThrowUtils.throwException(
                        result == null ||
                        !(result.get("status") instanceof Number number) || number.longValue() != 0L,
                        ErrorCode.OPERATION_ERROR,
                        "接口调用失败"
                );
                Map<String, Object> data = (Map<String, Object>) result.get("data");
                // 解码URL
                String rawUrl = (String) data.get("url");
                String searchResultUrl = URLUtil.decode(rawUrl, StandardCharsets.UTF_8);
                ThrowUtils.throwException(StrUtil.isBlank(searchResultUrl), ErrorCode.OPERATION_ERROR, "未返回有效搜索结果URL");
                return searchResultUrl;
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("百度搜图接口调用失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
        } finally {
            // 删除临时文件
            if (tempFile != null) {
                FileUtil.del(tempFile);
            }
        }
    }
}
