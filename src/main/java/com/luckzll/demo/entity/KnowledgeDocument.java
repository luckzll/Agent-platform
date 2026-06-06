package com.luckzll.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识库文档实体
 */
@Data
@TableName("knowledge_document")
public class KnowledgeDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文档ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 文档名称
     */
    private String docName;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 文档类型：pdf, doc, docx, txt, md等
     */
    private String docType;

    /**
     * 文档大小（字节）
     */
    private Long docSize;

    /**
     * 存储路径
     */
    private String storagePath;

    /**
     * 文档内容摘要
     */
    private String summary;

    /**
     * 文档状态：processing-处理中, completed-已完成, failed-失败
     */
    private String status;

    /**
     * 分块数量
     */
    private Integer chunkCount;

    /**
     * 上传用户ID
     */
    private Long userId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 错误信息
     */
    private String errorMsg;
}
