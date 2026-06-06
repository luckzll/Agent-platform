package com.luckzll.demo.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luckzll.demo.service.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 向量嵌入服务实现（基于DeepSeek/OpenAI API）
 */
@Slf4j
@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    @Value("${deepseek.api.key}")
    private String apiKey;

    @Value("${embedding.api.url:https://api.deepseek.com/embeddings}")
    private String embeddingApiUrl;

    @Value("${embedding.api.model:text-embedding-3-small}")
    private String embeddingModel;

    @Value("${embedding.dimension:1536}")
    private int dimension;

    @Value("${embedding.batch.size:100}")
    private int batchSize;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    @Override
    public float[] embed(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new float[dimension];
        }

        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", embeddingModel);
            requestBody.put("input", text);
            requestBody.put("encoding_format", "float");

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            // 发送请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(embeddingApiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode responseJson = objectMapper.readTree(response.body());
                JsonNode data = responseJson.get("data");
                if (data != null && data.isArray() && data.size() > 0) {
                    JsonNode embedding = data.get(0).get("embedding");
                    if (embedding != null && embedding.isArray()) {
                        return parseEmbeddingArray(embedding);
                    }
                }
                throw new RuntimeException("无法解析嵌入响应");
            } else {
                throw new RuntimeException("嵌入API请求失败: " + response.statusCode() + " - " + response.body());
            }
        } catch (Exception e) {
            log.error("生成向量嵌入失败", e);
            throw new RuntimeException("生成向量嵌入失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return new ArrayList<>();
        }

        List<float[]> results = new ArrayList<>();

        // 分批处理
        for (int i = 0; i < texts.size(); i += batchSize) {
            List<String> batch = texts.subList(i, Math.min(i + batchSize, texts.size()));
            try {
                List<float[]> batchResults = embedBatchInternal(batch);
                results.addAll(batchResults);
            } catch (Exception e) {
                log.error("批量嵌入失败，回退到单条处理", e);
                // 回退到单条处理
                for (String text : batch) {
                    try {
                        results.add(embed(text));
                    } catch (Exception ex) {
                        log.error("单条嵌入失败: {}", text.substring(0, Math.min(50, text.length())));
                        results.add(new float[dimension]);
                    }
                }
            }
        }

        return results;
    }

    /**
     * 批量嵌入内部实现
     */
    private List<float[]> embedBatchInternal(List<String> texts) throws Exception {
        // 过滤空文本
        List<String> validTexts = texts.stream()
                .filter(t -> t != null && !t.trim().isEmpty())
                .toList();

        if (validTexts.isEmpty()) {
            return texts.stream()
                    .map(t -> new float[dimension])
                    .toList();
        }

        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", embeddingModel);
        requestBody.put("input", validTexts);
        requestBody.put("encoding_format", "float");

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(embeddingApiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(120))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode data = responseJson.get("data");

            if (data != null && data.isArray()) {
                List<float[]> embeddings = new ArrayList<>();
                for (JsonNode item : data) {
                    JsonNode embedding = item.get("embedding");
                    if (embedding != null && embedding.isArray()) {
                        embeddings.add(parseEmbeddingArray(embedding));
                    } else {
                        embeddings.add(new float[dimension]);
                    }
                }
                return embeddings;
            }
        }

        throw new RuntimeException("批量嵌入请求失败: " + response.statusCode());
    }

    /**
     * 解析嵌入数组
     */
    private float[] parseEmbeddingArray(JsonNode embeddingNode) {
        float[] result = new float[embeddingNode.size()];
        for (int i = 0; i < embeddingNode.size(); i++) {
            result[i] = (float) embeddingNode.get(i).asDouble();
        }
        return result;
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public double cosineSimilarity(float[] vector1, float[] vector2) {
        if (vector1 == null || vector2 == null || vector1.length != vector2.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += vector1[i] * vector1[i];
            norm2 += vector2[i] * vector2[i];
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    @Override
    public String vectorToJson(float[] vector) {
        if (vector == null) return "[]";

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public float[] jsonToVector(String json) {
        if (json == null || json.isEmpty() || json.equals("[]")) {
            log.warn("向量为空，返回零向量");
            return new float[dimension];
        }

        try {
            // 移除方括号
            json = json.trim();
            if (json.startsWith("[")) json = json.substring(1);
            if (json.endsWith("]")) json = json.substring(0, json.length() - 1);

            String[] parts = json.split(",");
            log.debug("解析向量，维度: {}, 配置维度: {}", parts.length, dimension);
            
            float[] vector = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                vector[i] = Float.parseFloat(parts[i].trim());
            }
            return vector;
        } catch (Exception e) {
            log.error("解析向量JSON失败: {}, 错误: {}", json.substring(0, Math.min(100, json.length())), e.getMessage());
            return new float[dimension];
        }
    }
}
