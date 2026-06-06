package com.luckzll.demo.service;

import java.util.List;

/**
 * 向量嵌入服务接口
 */
public interface EmbeddingService {

    /**
     * 单文本生成向量嵌入
     *
     * @param text 输入文本
     * @return 向量数组（float数组）
     */
    float[] embed(String text);

    /**
     * 批量生成向量嵌入
     *
     * @param texts 文本列表
     * @return 向量列表
     */
    List<float[]> embedBatch(List<String> texts);

    /**
     * 获取向量维度
     *
     * @return 向量维度
     */
    int getDimension();

    /**
     * 计算两个向量的余弦相似度
     *
     * @param vector1 向量1
     * @param vector2 向量2
     * @return 相似度分数（0-1）
     */
    double cosineSimilarity(float[] vector1, float[] vector2);

    /**
     * 将向量转换为JSON字符串存储
     *
     * @param vector 向量数组
     * @return JSON字符串
     */
    String vectorToJson(float[] vector);

    /**
     * 将JSON字符串解析为向量
     *
     * @param json JSON字符串
     * @return 向量数组
     */
    float[] jsonToVector(String json);
}
