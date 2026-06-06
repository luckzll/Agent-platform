package com.luckzll.demo.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luckzll.demo.entity.dto.ChatRequestDTO;
import com.luckzll.demo.service.DeepSeekService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * DeepSeek AI 服务实现
 */
@Service
public class DeepSeekServiceImpl implements DeepSeekService {

    @Value("${deepseek.api.key}")
    private String apiKey;

    @Value("${deepseek.api.url:https://api.deepseek.com/chat/completions}")
    private String apiUrl;

    @Value("${deepseek.api.model:deepseek-chat}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Override
    public String chat(String message, String imageUrl, List<ChatRequestDTO.Message> history) {
        try {
            // 构建消息列表
            List<Map<String, Object>> messages = new ArrayList<>();

            // 添加历史消息（第一条应为 system 消息，由 Controller 层传入）
            if (history != null && !history.isEmpty()) {
                for (ChatRequestDTO.Message msg : history) {
                    Map<String, Object> historyMessage = new HashMap<>();
                    historyMessage.put("role", msg.getRole());
                    historyMessage.put("content", msg.getContent());
                    messages.add(historyMessage);
                }
            }

            // 添加用户消息（支持多模态）
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", buildContent(message, imageUrl));
            messages.add(userMessage);

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("stream", false);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            // 发送请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // 解析响应
                JsonNode responseJson = objectMapper.readTree(response.body());
                JsonNode choices = responseJson.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    JsonNode firstChoice = choices.get(0);
                    JsonNode messageNode = firstChoice.get("message");
                    if (messageNode != null) {
                        return messageNode.get("content").asText();
                    }
                }
                throw new RuntimeException("无法解析 AI 响应");
            } else {
                throw new RuntimeException("AI 服务请求失败: " + response.statusCode() + " - " + response.body());
            }
        } catch (Exception e) {
            throw new RuntimeException("调用 DeepSeek API 失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void chatStream(String message, String imageUrl, List<ChatRequestDTO.Message> history,
                           Consumer<String> onMessage, Consumer<String> onComplete, Consumer<Exception> onError) {
        executorService.submit(() -> {
            StringBuilder fullContent = new StringBuilder();
            try {
                // 构建消息列表
                List<Map<String, Object>> messages = new ArrayList<>();

                // 添加历史消息（第一条应为 system 消息，由 Controller 层传入）
                if (history != null && !history.isEmpty()) {
                    for (ChatRequestDTO.Message msg : history) {
                        Map<String, Object> historyMessage = new HashMap<>();
                        historyMessage.put("role", msg.getRole());
                        historyMessage.put("content", msg.getContent());
                        messages.add(historyMessage);
                    }
                }

                // 添加用户消息（支持多模态）
                Map<String, Object> userMessage = new HashMap<>();
                userMessage.put("role", "user");
                userMessage.put("content", buildContent(message, imageUrl));
                messages.add(userMessage);

                // 构建请求体，开启流式
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", model);
                requestBody.put("messages", messages);
                requestBody.put("stream", true);

                String jsonBody = objectMapper.writeValueAsString(requestBody);

                // 发送请求
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .timeout(Duration.ofMinutes(5))
                        .build();

                HttpResponse<java.io.InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() == 200) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("data: ")) {
                                String data = line.substring(6).trim();
                                if ("[DONE]".equals(data)) {
                                    break;
                                }
                                try {
                                    JsonNode jsonNode = objectMapper.readTree(data);
                                    JsonNode choices = jsonNode.get("choices");
                                    if (choices != null && choices.isArray() && choices.size() > 0) {
                                        JsonNode delta = choices.get(0).get("delta");
                                        if (delta != null && delta.has("content")) {
                                            JsonNode contentNode = delta.get("content");
                                            if (contentNode != null && !contentNode.isNull()) {
                                                String content = contentNode.asText();
                                                if (content != null && !content.isEmpty()) {
                                                    fullContent.append(content);
                                                    onMessage.accept(content);
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    // 跳过解析错误的行
                                }
                            }
                        }
                    }
                    onComplete.accept(fullContent.toString());
                } else {
                    throw new RuntimeException("AI 服务请求失败: " + response.statusCode());
                }
            } catch (Exception e) {
                onError.accept(e);
            }
        });
    }

    /**
     * 构建消息内容（支持多模态）
     * 有图片时返回数组格式，无图片时返回纯文本
     */
    private Object buildContent(String text, String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return text;
        }
        // 多模态格式: [{type:"image_url", image_url:{url:"..."}}, {type:"text", text:"..."}]
        List<Map<String, Object>> contentList = new ArrayList<>();

        Map<String, Object> imagePart = new HashMap<>();
        imagePart.put("type", "image_url");
        Map<String, String> imageUrlMap = new HashMap<>();
        imageUrlMap.put("url", imageUrl);
        imagePart.put("image_url", imageUrlMap);
        contentList.add(imagePart);

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("type", "text");
        textPart.put("text", text);
        contentList.add(textPart);

        return contentList;
    }
}
