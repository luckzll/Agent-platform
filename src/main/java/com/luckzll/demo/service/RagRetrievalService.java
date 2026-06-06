package com.luckzll.demo.service;

import com.luckzll.demo.entity.dto.RetrievalResultDTO;

import java.util.List;

/**
 * RAG检索服务接口
 */
public interface RagRetrievalService {

    /**
     * 根据查询检索相关知识
     *
     * @param query        用户查询
     * @param topK         返回最相关的K个结果
     * @return 检索结果列表
     */
    List<RetrievalResultDTO> retrieve(String query, int topK);

    /**
     * 根据查询检索相关知识（带分数阈值）
     *
     * @param query         用户查询
     * @param topK          返回最相关的K个结果
     * @param minScore      最小相似度分数
     * @return 检索结果列表
     */
    List<RetrievalResultDTO> retrieve(String query, int topK, double minScore);

    /**
     * 根据查询检索相关知识（按用户隔离）
     *
     * @param query         用户查询
     * @param topK          返回最相关的K个结果
     * @param minScore      最小相似度分数
     * @param userId        用户ID
     * @return 检索结果列表
     */
    List<RetrievalResultDTO> retrieveByUser(String query, int topK, double minScore, Long userId);

    /**
     * 检索并构建增强提示词
     *
     * @param query    用户查询
     * @param topK     检索结果数量
     * @return 增强后的提示词（包含检索到的上下文）
     */
    String buildAugmentedPrompt(String query, int topK);

    /**
     * 检索并构建增强提示词（按用户隔离）
     *
     * @param query    用户查询
     * @param topK     检索结果数量
     * @param userId   用户ID
     * @return 增强后的提示词（包含检索到的上下文）
     */
    String buildAugmentedPromptByUser(String query, int topK, Long userId);

    /**
     * 添加文档到知识库
     *
     * @param docId 文档ID
     * @param chunks 文档分块内容列表
     * @return 是否成功
     */
    boolean addDocumentToIndex(Long docId, List<String> chunks);

    /**
     * 从知识库删除文档
     *
     * @param docId 文档ID
     * @return 是否成功
     */
    boolean removeDocumentFromIndex(Long docId);

    /**
     * 重新索引文档
     *
     * @param docId  文档ID
     * @param chunks 新的分块内容
     * @return 是否成功
     */
    boolean reindexDocument(Long docId, List<String> chunks);
}
