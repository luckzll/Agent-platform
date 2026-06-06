package com.luckzll.demo.controller;

import com.luckzll.demo.entity.dto.VideoParseResultDTO;
import com.luckzll.demo.service.VideoParseService;
import com.luckzll.demo.utils.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 视频解析控制器
 */
@RestController
@RequestMapping("/api/v1/video")
public class VideoController {

    @Autowired
    private VideoParseService videoParseService;

    // 抖音链接正则表达式
    private static final Pattern DOUYIN_URL_PATTERN = Pattern.compile("https?://v\\.douyin\\.com/[a-zA-Z0-9_-]+");

    /**
     * 从文本中提取抖音链接
     *
     * @param text 包含链接的文本
     * @return 提取的纯净链接
     */
    private String extractDouyinUrl(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        Matcher matcher = DOUYIN_URL_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    /**
     * 解析抖音视频链接（表单方式）
     *
     * @param url 抖音分享链接
     * @return 视频解析结果
     */
    @PostMapping("/parse")
    public Result<VideoParseResultDTO> parseVideo(@RequestParam String url) {
        try {
            String cleanUrl = extractDouyinUrl(url);
            if (cleanUrl == null) {
                return Result.error("未能从文本中提取到有效的抖音链接");
            }
            VideoParseResultDTO result = videoParseService.parseVideo(cleanUrl);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 解析抖音视频链接（JSON方式）
     *
     * @param request 请求体
     * @return 视频解析结果
     */
    @PostMapping("/parse/json")
    public Result<VideoParseResultDTO> parseVideoJson(@RequestBody ParseRequest request) {
        try {
            String cleanUrl = extractDouyinUrl(request.getUrl());
            if (cleanUrl == null) {
                return Result.error("未能从文本中提取到有效的抖音链接");
            }
            VideoParseResultDTO result = videoParseService.parseVideo(cleanUrl);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 代理播放/下载视频
     *
     * @param videoUrl 视频URL
     * @param filename 文件名（可选，不传则直接播放）
     * @param response HTTP响应
     */
    @GetMapping("/proxy")
    public void proxyVideo(@RequestParam String videoUrl, @RequestParam(required = false) String filename, HttpServletResponse response) {
        try {
            URL url = new URL(videoUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);
            // 设置请求头模拟浏览器访问
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            conn.setRequestProperty("Referer", "https://www.douyin.com/");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                response.setContentType(conn.getContentType());
                // 如果传了filename则下载，否则直接播放
                if (filename != null && !filename.isEmpty()) {
                    response.setHeader("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(filename, "UTF-8") + "\"");
                }

                try (InputStream in = conn.getInputStream();
                     java.io.OutputStream out = response.getOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
            } else {
                response.setStatus(responseCode);
            }
        } catch (Exception e) {
            response.setStatus(500);
        }
    }

    /**
     * 解析请求体
     */
    public static class ParseRequest {
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
