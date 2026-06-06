package com.luckzll.demo.service.impl;

import com.luckzll.demo.entity.dto.RetrievalResultDTO;
import com.luckzll.demo.mapper.postgresql.PgKnowledgeChunkMapper;
import com.luckzll.demo.service.EmbeddingService;
import com.luckzll.demo.service.RagRetrievalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * RAG检索服务实现
 */
@Slf4j
@Service
public class RagRetrievalServiceImpl implements RagRetrievalService {

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private PgKnowledgeChunkMapper knowledgeChunkMapper;

    @Value("${rag.retrieval.default-topk:5}")
    private int defaultTopK;

    @Value("${rag.retrieval.min-score:0.7}")
    private double defaultMinScore;

    @Value("${rag.retrieval.max-chars:3000}")
    private int maxContextChars;

    @Value("${rag.retrieval.use-cache:true}")
    private boolean useCache;

    // 本地向量缓存，避免重复查询数据库
    private final Map<Long, ChunkVector> vectorCache = new ConcurrentHashMap<>();

    /**
     * 分块向量缓存对象
     */
    private static class ChunkVector {
        final Long chunkId;
        final Long docId;
        final String docName;
        final String content;
        final float[] embedding;
        final Integer chunkIndex;

        ChunkVector(Long chunkId, Long docId, String docName, String content, float[] embedding, Integer chunkIndex) {
            this.chunkId = chunkId;
            this.docId = docId;
            this.docName = docName;
            this.content = content;
            this.embedding = embedding;
            this.chunkIndex = chunkIndex;
        }
    }

    @Override
    public List<RetrievalResultDTO> retrieve(String query, int topK) {
        return retrieve(query, topK, 0.0);
    }

    @Override
    public List<RetrievalResultDTO> retrieve(String query, int topK, double minScore) {
        // 兼容旧版本，默认检索所有（管理员或公共知识库用）
        return retrieveByUser(query, topK, minScore, null);
    }

    @Override
    public List<RetrievalResultDTO> retrieveByUser(String query, int topK, double minScore, Long userId) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // 1. 生成查询向量
            float[] queryVector = embeddingService.embed(query);

            // 2. 获取候选分块（按用户过滤）
            List<ChunkVector> candidates = getCandidateChunksByUser(userId);

            if (candidates.isEmpty()) {
                log.warn("知识库为空，无法检索, userId={}", userId);
                return new ArrayList<>();
            }

            // 3. 计算相似度并排序
            log.info("查询向量维度: {}", queryVector.length);
            List<RetrievalResultDTO> results = candidates.stream()
                    .map(chunk -> {
                        log.info("处理分块: chunkId={}, 文档向量维度={}", chunk.chunkId, chunk.embedding.length);
                        double similarity = embeddingService.cosineSimilarity(queryVector, chunk.embedding);
                        log.info("计算相似度: chunkId={}, 相似度={}", chunk.chunkId, similarity);
                        RetrievalResultDTO dto = new RetrievalResultDTO();
                        dto.setChunkId(chunk.chunkId);
                        dto.setDocId(chunk.docId);
                        dto.setDocName(chunk.docName);
                        dto.setContent(chunk.content);
                        dto.setSimilarity(similarity);
                        dto.setChunkIndex(chunk.chunkIndex);
                        return dto;
                    })
                    .filter(dto -> dto.getSimilarity() >= minScore && !Double.isNaN(dto.getSimilarity()))
                    .sorted((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()))
                    .limit(topK)
                    .collect(Collectors.toList());

            log.info("RAG检索完成: query='{}', userId={}, 候选数={}, 结果数={}",
                    query.substring(0, Math.min(50, query.length())),
                    userId, candidates.size(), results.size());

            return results;

        } catch (Exception e) {
            log.error("RAG检索失败: {}, userId={}", query, userId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public String buildAugmentedPrompt(String query, int topK) {
        return buildAugmentedPromptByUser(query, topK, null);
    }

    @Override
    public String buildAugmentedPromptByUser(String query, int topK, Long userId) {
        List<RetrievalResultDTO> results = retrieveByUser(query, topK, defaultMinScore, userId);

        if (results.isEmpty()) {
            return query;
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("基于以下参考资料回答问题：\n\n");
        prompt.append("=== 参考资料 ===\n");

        int totalChars = 0;
        int refNumber = 1;

        for (RetrievalResultDTO result : results) {
            String content = result.getContent();

            // 检查是否超过最大上下文长度
            if (totalChars + content.length() > maxContextChars) {
                // 截取剩余可用长度
                int remaining = maxContextChars - totalChars;
                if (remaining > 100) {
                    content = content.substring(0, remaining) + "...";
                } else {
                    break;
                }
            }

            prompt.append("[").append(refNumber).append("] ");
            prompt.append("(来自: ").append(result.getDocName()).append(")\n");
            prompt.append(content).append("\n\n");

            totalChars += content.length();
            refNumber++;
        }

        prompt.append("=== 用户问题 ===\n");
        prompt.append(query);
        prompt.append("\n\n请基于上述参考资料回答问题。如果参考资料中没有相关信息，请明确说明。");

        return prompt.toString();
    }

    @Override
    public boolean addDocumentToIndex(Long docId, List<String> chunks) {
        // 这里由KnowledgeService调用，实际插入在KnowledgeService中完成
        // 清除缓存，下次检索时重新加载
        if (useCache) {
            refreshCache();
        }
        return true;
    }

    @Override
    public boolean removeDocumentFromIndex(Long docId) {
        try {
            // 删除数据库中的分块
            knowledgeChunkMapper.deleteByDocId(docId);

            // 清除缓存
            if (useCache) {
                vectorCache.values().removeIf(chunk -> chunk.docId.equals(docId));
            }

            log.info("从知识库删除文档: docId={}", docId);
            return true;
        } catch (Exception e) {
            log.error("从知识库删除文档失败: docId={}", docId, e);
            return false;
        }
    }

    @Override
    public boolean reindexDocument(Long docId, List<String> chunks) {
        // 先删除旧索引
        removeDocumentFromIndex(docId);
        // 添加新索引
        return addDocumentToIndex(docId, chunks);
    }

    /**
     * 获取候选分块（按用户过滤）
     */
    private List<ChunkVector> getCandidateChunksByUser(Long userId) {
        // 从数据库加载（带用户过滤）
        List<RetrievalResultDTO> chunks;
        if (userId != null) {
            chunks = knowledgeChunkMapper.selectChunksByUserId(userId);
        } else {
            // userId 为 null 时查询所有（兼容旧逻辑）
            chunks = knowledgeChunkMapper.selectAllChunksWithDoc();
        }

        List<ChunkVector> result = chunks.stream()
                .filter(dto -> dto.getChunkId() != null && dto.getEmbedding() != null)
                .map(dto -> {
                    float[] vector = embeddingService.jsonToVector(dto.getEmbedding());
                    log.info("加载分块: chunkId={}, 向量维度={}", dto.getChunkId(), vector.length);
                    return new ChunkVector(
                            dto.getChunkId(),
                            dto.getDocId(),
                            dto.getDocName(),
                            dto.getContent(),
                            vector,
                            dto.getChunkIndex()
                    );
                })
                .collect(Collectors.toList());
        
        log.info("加载候选分块完成: userId={}, 总数={}, 有效数={}", userId, chunks.size(), result.size());

        return result;
    }

    /**
     * 刷新缓存
     */
    public void refreshCache() {
        vectorCache.clear();
        log.info("RAG向量缓存已刷新");
    }

    /**
     * 获取缓存统计
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheSize", vectorCache.size());
        stats.put("cacheEnabled", useCache);
        return stats;
    }
}
