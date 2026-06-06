package com.luckzll.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识库文档分块实体
 */
@Data
@TableName("knowledge_chunk")
public class KnowledgeChunk implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分块ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属文档ID
     */
    private Long docId;

    /**
     * 分块序号
     */
    private Integer chunkIndex;

    /**
     * 分块内容
     */
    private String content;

    /**
     * 向量嵌入（存储为JSON字符串，实际查询用原生SQL）
     */
    private String embedding;

    /**
     * 分块字符数
     */
    private Integer charCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
