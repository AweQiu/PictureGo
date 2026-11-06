package com.ylh.picturego.api.imagesearch.sub;

import cn.hutool.core.util.StrUtil;
import com.ylh.picturego.exception.BusinessException;
import com.ylh.picturego.exception.ErrorCode;
import com.ylh.picturego.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 2.获取图片列表接口API
 */
@Slf4j
public class GetImageFirstUrlApi {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    /**
     * 匹配页面脚本中的 firstUrl 字段，兼容 key 带引号与不带引号两种写法
     */
    private static final Pattern FIRST_URL_PATTERN = Pattern.compile("\"?firstUrl\"?\\s*:\\s*\"(.*?)\"");

    /**
     * 获取图片列表页面地址
     *
     * @param url 百度搜图结果页URL
     * @return 图片列表页面地址，页面中没有相似图片时为 null
     */
    public static String getImageFirstUrl(String url) {
        ThrowUtils.throwException(StrUtil.isBlank(url), ErrorCode.PARAMS_ERROR, "搜索页url不能为空");
        Document document;
        try {
            // 获取HTML内容，必须携带浏览器 UA，否则百度可能返回风控页
            document = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .referrer("https://graph.baidu.com/")
                    .timeout(10000)
                    .get();
        } catch (Exception e) {
            log.error("请求百度搜图结果页失败, url={}", url, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "请求百度搜图结果页失败");
        }
        // firstUrl 位于 window.cardData 的“相似图片”卡片中，直接匹配整页脚本
        Matcher matcher = FIRST_URL_PATTERN.matcher(document.html());
        if (matcher.find()) {
            return matcher.group(1).replace("\\/", "/");
        }
        // 页面正常但没有相似图片卡片，属于无搜索结果，交由上层按空列表处理
        log.warn("百度搜图结果页未找到firstUrl, title={}, htmlLength={}", document.title(), document.html().length());
        return null;
    }
}
