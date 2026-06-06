package com.luckzll.demo.service.impl;

import com.luckzll.demo.entity.KnowledgeChunk;
import com.luckzll.demo.entity.KnowledgeDocument;
import com.luckzll.demo.mapper.postgresql.PgKnowledgeChunkMapper;
import com.luckzll.demo.mapper.postgresql.PgKnowledgeDocumentMapper;
import com.luckzll.demo.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * 知识库管理服务实现
 */
@Slf4j
@Service
public class KnowledgeServiceImpl implements KnowledgeService {

    @Autowired
    private PgKnowledgeDocumentMapper documentMapper;

    @Autowired
    private PgKnowledgeChunkMapper chunkMapper;

    @Autowired
    private DocumentParseService documentParseService;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private RagRetrievalService ragRetrievalService;

    @Value("${knowledge.upload.path:./uploads/knowledge}")
    private String uploadPath;

    @Value("${knowledge.chunk.max-size:1000}")
    private int maxChunkSize;

    @Value("${knowledge.chunk.overlap:100}")
    private int chunkOverlap;

    @Override
    @Transactional
    public Long uploadDocument(MultipartFile file, String docName, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 检查文件类型
        String extension = getFileExtension(originalName);
        if (!documentParseService.isSupported(extension)) {
            throw new IllegalArgumentException("不支持的文件类型: " + extension);
        }

        // 生成文档名称
        String finalDocName = (docName != null && !docName.trim().isEmpty())
                ? docName.trim()
                : originalName.substring(0, originalName.lastIndexOf('.'));

        // 生成存储文件名
        String storageName = UUID.randomUUID().toString() + "_" + originalName;

        try {
            // 创建上传目录
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // 保存文件
            Path filePath = uploadDir.resolve(storageName);
            file.transferTo(filePath.toFile());

            // 创建文档记录
            KnowledgeDocument document = new KnowledgeDocument();
            document.setDocName(finalDocName);
            document.setOriginalName(originalName);
            document.setDocType(extension);
            document.setDocSize(file.getSize());
            document.setStoragePath(filePath.toString());
            document.setStatus("processing");
            document.setChunkCount(0);
            document.setUserId(userId);  // 设置上传用户ID

            documentMapper.insert(document);

            Long docId = document.getId();

            // 异步处理文档
            processDocumentAsync(docId, filePath.toFile());

            log.info("文档上传成功: docId={}, name={}, userId={}", docId, finalDocName, userId);
            return docId;

        } catch (IOException e) {
            log.error("保存上传文件失败: {}", originalName, e);
            throw new RuntimeException("保存文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 异步处理文档
     */
    @Async
    protected void processDocumentAsync(Long docId, File file) {
        try {
            log.info("开始处理文档: docId={}", docId);

            // 1. 解析文档
            String content = documentParseService.parseDocument(file);

            if (content == null || content.trim().isEmpty()) {
                throw new RuntimeException("文档内容为空");
            }

            // 2. 生成摘要
            String summary = documentParseService.generateSummary(content, 500);

            // 3. 智能分块
            List<String> chunks = documentParseService.splitTextSmart(content, maxChunkSize);

            log.info("文档分块完成: docId={}, chunks={}", docId, chunks.size());

            // 4. 生成向量嵌入并保存
            if (!chunks.isEmpty()) {
                // 批量生成嵌入
                List<float[]> embeddings = embeddingService.embedBatch(chunks);

                // 构建分块实体
                List<KnowledgeChunk> chunkEntities = new java.util.ArrayList<>();
                for (int i = 0; i < chunks.size(); i++) {
                    KnowledgeChunk chunk = new KnowledgeChunk();
                    chunk.setDocId(docId);
                    chunk.setChunkIndex(i);
                    chunk.setContent(chunks.get(i));
                    chunk.setEmbedding(embeddingService.vectorToJson(embeddings.get(i)));
                    chunk.setCharCount(chunks.get(i).length());
                    chunkEntities.add(chunk);
                }

                // 批量插入
                chunkMapper.batchInsert(chunkEntities);
            }

            // 5. 更新文档状态
            documentMapper.updateStatusAndChunkCount(docId, "completed", chunks.size());

            // 6. 更新摘要
            KnowledgeDocument doc = new KnowledgeDocument();
            doc.setId(docId);
            doc.setSummary(summary);
            documentMapper.updateById(doc);

            // 7. 通知检索服务更新索引
            ragRetrievalService.addDocumentToIndex(docId, chunks);

            log.info("文档处理完成: docId={}, chunks={}", docId, chunks.size());

        } catch (Exception e) {
            log.error("处理文档失败: docId={}", docId, e);
            documentMapper.updateError(docId, e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean deleteDocument(Long docId, Long userId) {
        if (docId == null) {
            return false;
        }

        // 查询文档并校验权限
        KnowledgeDocument doc = documentMapper.selectById(docId);
        if (doc == null) {
            return false;
        }
        
        // 只能删除自己的文档
        if (!doc.getUserId().equals(userId)) {
            log.warn("用户{}尝试删除不属于自己的文档{}", userId, docId);
            return false;
        }

        try {
            // 1. 从检索服务删除
            ragRetrievalService.removeDocumentFromIndex(docId);

            // 2. 删除数据库记录（级联删除分块）
            documentMapper.deleteById(docId);

            // 3. 删除物理文件
            try {
                Path filePath = Paths.get(doc.getStoragePath());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                log.warn("删除物理文件失败: {}", doc.getStoragePath());
            }

            log.info("文档删除成功: docId={}, userId={}", docId, userId);
            return true;

        } catch (Exception e) {
            log.error("删除文档失败: docId={}", docId, e);
            return false;
        }
    }

    @Override
    public List<KnowledgeDocument> listAllDocuments(Long userId) {
        return documentMapper.selectByUserId(userId);
    }

    @Override
    public List<KnowledgeDocument> listCompletedDocuments(Long userId) {
        return documentMapper.selectCompletedDocsByUserId(userId);
    }

    @Override
    public KnowledgeDocument getDocument(Long docId, Long userId) {
        KnowledgeDocument doc = documentMapper.selectById(docId);
        if (doc == null) {
            return null;
        }
        // 只能查看自己的文档
        if (!doc.getUserId().equals(userId)) {
            return null;
        }
        return doc;
    }

    @Override
    @Transactional
    public boolean reprocessDocument(Long docId, Long userId) {
        KnowledgeDocument doc = documentMapper.selectById(docId);
        if (doc == null) {
            return false;
        }
        // 只能重新处理自己的文档
        if (!doc.getUserId().equals(userId)) {
            return false;
        }

        File file = new File(doc.getStoragePath());
        if (!file.exists()) {
            documentMapper.updateError(docId, "原始文件不存在");
            return false;
        }

        // 删除旧的分块
        chunkMapper.deleteByDocId(docId);

        // 更新状态为处理中
        documentMapper.updateStatus(docId, "processing");

        // 重新异步处理
        processDocumentAsync(docId, file);

        return true;
    }

    @Override
    public KnowledgeStats getStats(Long userId) {
        List<KnowledgeDocument> allDocs = documentMapper.selectByUserId(userId);

        int completed = (int) allDocs.stream()
                .filter(d -> "completed".equals(d.getStatus()))
                .count();
        int processing = (int) allDocs.stream()
                .filter(d -> "processing".equals(d.getStatus()))
                .count();
        int failed = (int) allDocs.stream()
                .filter(d -> "failed".equals(d.getStatus()))
                .count();

        int totalChunks = allDocs.stream()
                .mapToInt(d -> d.getChunkCount() != null ? d.getChunkCount() : 0)
                .sum();

        KnowledgeStats stats = new KnowledgeStats();
        stats.setTotalDocuments(allDocs.size());
        stats.setCompletedDocuments(completed);
        stats.setProcessingDocuments(processing);
        stats.setFailedDocuments(failed);
        stats.setTotalChunks(totalChunks);

        return stats;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "";
    }
}
