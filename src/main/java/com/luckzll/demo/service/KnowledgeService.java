package com.luckzll.demo.service;

import com.luckzll.demo.entity.KnowledgeDocument;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识库管理服务接口
 */
public interface KnowledgeService {

    /**
     * 上传文档到知识库
     *
     * @param file     上传的文件
     * @param docName  文档名称（可选，默认为文件名）
     * @param userId   上传用户ID
     * @return 文档ID
     */
    Long uploadDocument(MultipartFile file, String docName, Long userId);

    /**
     * 删除知识库文档
     *
     * @param docId  文档ID
     * @param userId 当前用户ID（用于权限校验）
     * @return 是否成功
     */
    boolean deleteDocument(Long docId, Long userId);

    /**
     * 获取用户的所有文档列表
     *
     * @param userId 用户ID
     * @return 文档列表
     */
    List<KnowledgeDocument> listAllDocuments(Long userId);

    /**
     * 获取用户已完成处理的文档列表
     *
     * @param userId 用户ID
     * @return 文档列表
     */
    List<KnowledgeDocument> listCompletedDocuments(Long userId);

    /**
     * 根据ID获取文档
     *
     * @param docId  文档ID
     * @param userId 当前用户ID（用于权限校验）
     * @return 文档信息
     */
    KnowledgeDocument getDocument(Long docId, Long userId);

    /**
     * 重新处理文档
     *
     * @param docId  文档ID
     * @param userId 当前用户ID（用于权限校验）
     * @return 是否成功
     */
    boolean reprocessDocument(Long docId, Long userId);

    /**
     * 获取知识库统计信息
     *
     * @param userId 用户ID
     * @return 统计信息
     */
    KnowledgeStats getStats(Long userId);

    /**
     * 知识库统计信息
     */
    class KnowledgeStats {
        private int totalDocuments;
        private int completedDocuments;
        private int processingDocuments;
        private int failedDocuments;
        private int totalChunks;

        // Getters and Setters
        public int getTotalDocuments() { return totalDocuments; }
        public void setTotalDocuments(int totalDocuments) { this.totalDocuments = totalDocuments; }

        public int getCompletedDocuments() { return completedDocuments; }
        public void setCompletedDocuments(int completedDocuments) { this.completedDocuments = completedDocuments; }

        public int getProcessingDocuments() { return processingDocuments; }
        public void setProcessingDocuments(int processingDocuments) { this.processingDocuments = processingDocuments; }

        public int getFailedDocuments() { return failedDocuments; }
        public void setFailedDocuments(int failedDocuments) { this.failedDocuments = failedDocuments; }

        public int getTotalChunks() { return totalChunks; }
        public void setTotalChunks(int totalChunks) { this.totalChunks = totalChunks; }
    }
}
