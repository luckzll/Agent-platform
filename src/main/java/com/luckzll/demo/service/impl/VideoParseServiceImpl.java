package com.luckzll.demo.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luckzll.demo.entity.dto.VideoParseResultDTO;
import com.luckzll.demo.service.VideoParseService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 视频解析服务实现
 */
@Service
public class VideoParseServiceImpl implements VideoParseService {

    @Value("${video.parser.api.url}")
    private String apiUrl;

    @Value("${video.parser.api.appId}")
    private String appId;

    @Value("${video.parser.api.appKey}")
    private String appKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    @Override
    public VideoParseResultDTO parseVideo(String url) {
        try {
            // 构建请求体
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("appId", appId);
            requestBody.put("appKey", appKey);
            requestBody.put("url", url);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            // 发送POST请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode responseJson = objectMapper.readTree(response.body());
                
                // 检查响应状态
                int code = responseJson.get("code").asInt();
                if (code != 200) {
                    String msg = responseJson.get("msg").asText();
                    throw new RuntimeException("视频解析失败: " + msg);
                }

                // 解析data字段
                JsonNode dataNode = responseJson.get("data");
                if (dataNode == null) {
                    throw new RuntimeException("视频解析返回数据为空");
                }

                return objectMapper.treeToValue(dataNode, VideoParseResultDTO.class);
            } else {
                throw new RuntimeException("视频解析请求失败: " + response.statusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("调用视频解析API失败: " + e.getMessage(), e);
        }
    }
}
