package com.luckzll.demo.entity.dto;

import lombok.Data;

/**
 * RAG检索结果DTO
 */
@Data
public class RetrievalResultDTO {

    /**
     * 分块ID
     */
    private Long chunkId;

    /**
     * 所属文档ID
     */
    private Long docId;

    /**
     * 文档名称
     */
    private String docName;

    /**
     * 分块内容
     */
    private String content;

    /**
     * 相似度分数（0-1）
     */
    private Double similarity;

    /**
     * 分块序号
     */
    private Integer chunkIndex;

    /**
     * 向量嵌入（JSON字符串）
     */
    private String embedding;
}
