package com.luckzll.demo.controller;

import com.luckzll.demo.entity.KnowledgeDocument;
import com.luckzll.demo.entity.dto.RetrievalResultDTO;
import com.luckzll.demo.service.KnowledgeService;
import com.luckzll.demo.service.RagRetrievalService;
import com.luckzll.demo.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {

    @Autowired
    private KnowledgeService knowledgeService;

    @Autowired
    private RagRetrievalService ragRetrievalService;

    /**
     * 上传文档到知识库
     *
     * @param request HTTP请求
     * @param file    上传的文件
     * @param docName 文档名称（可选）
     * @return 上传结果
     */
    @PostMapping("/upload")
    public Result<?> uploadDocument(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "docName", required = false) String docName) {

        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.unauthorized();
        }

        try {
            if (file == null || file.isEmpty()) {
                return Result.badRequest("请选择要上传的文件");
            }

            // 检查文件大小（限制100MB）
            if (file.getSize() > 100 * 1024 * 1024) {
                return Result.badRequest("文件大小不能超过100MB");
            }

            Long docId = knowledgeService.uploadDocument(file, docName, userId);

            Map<String, Object> data = new HashMap<>();
            data.put("docId", docId);
            data.put("message", "文档上传成功，正在处理中");

            return Result.success(data);

        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("上传文档失败", e);
            return Result.error("上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有文档列表
     *
     * @param request HTTP请求
     * @return 文档列表
     */
    @GetMapping("/documents")
    public Result<?> listDocuments(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.unauthorized();
        }

        List<KnowledgeDocument> documents = knowledgeService.listAllDocuments(userId);
        return Result.success(documents);
    }

    /**
     * 获取已完成的文档列表
     *
     * @param request HTTP请求
     * @return 文档列表
     */
    @GetMapping("/documents/completed")
    public Result<?> listCompletedDocuments(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.unauthorized();
        }

        List<KnowledgeDocument> documents = knowledgeService.listCompletedDocuments(userId);
        return Result.success(documents);
    }

    /**
     * 获取文档详情
     *
     * @param request HTTP请求
     * @param docId   文档ID
     * @return 文档详情
     */
    @GetMapping("/documents/{docId}")
    public Result<?> getDocument(HttpServletRequest request, @PathVariable Long docId) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.unauthorized();
        }

        KnowledgeDocument document = knowledgeService.getDocument(docId, userId);
        if (document == null) {
            return Result.error("文档不存在或无权访问");
        }

        return Result.success(document);
    }

    /**
     * 删除文档
     *
     * @param request HTTP请求
     * @param docId   文档ID
     * @return 删除结果
     */
    @DeleteMapping("/documents/{docId}")
    public Result<?> deleteDocument(HttpServletRequest request, @PathVariable Long docId) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.unauthorized();
        }

        boolean success = knowledgeService.deleteDocument(docId, userId);
        if (success) {
            return Result.success("删除成功");
        } else {
            return Result.error("删除失败或无权删除");
        }
    }

    /**
     * 重新处理文档
     *
     * @param request HTTP请求
     * @param docId   文档ID
     * @return 处理结果
     */
    @PostMapping("/documents/{docId}/reprocess")
    public Result<?> reprocessDocument(HttpServletRequest request, @PathVariable Long docId) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.unauthorized();
        }

        boolean success = knowledgeService.reprocessDocument(docId, userId);
        if (success) {
            return Result.success("重新处理已启动");
        } else {
            return Result.error("重新处理失败或无权操作");
        }
    }

    /**
     * 获取知识库统计信息
     *
     * @param request HTTP请求
     * @return 统计信息
     */
    @GetMapping("/stats")
    public Result<?> getStats(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.unauthorized();
        }

        KnowledgeService.KnowledgeStats stats = knowledgeService.getStats(userId);
        return Result.success(stats);
    }

    /**
     * 测试RAG检索
     *
     * @param request HTTP请求
     * @param query   查询内容
     * @param topK    返回结果数量（可选，默认5）
     * @return 检索结果
     */
    @GetMapping("/search")
    public Result<?> search(
            HttpServletRequest request,
            @RequestParam("query") String query,
            @RequestParam(value = "topK", defaultValue = "5") int topK) {

        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.unauthorized();
        }

        if (query == null || query.trim().isEmpty()) {
            return Result.badRequest("查询内容不能为空");
        }

        try {
            List<RetrievalResultDTO> results = ragRetrievalService.retrieveByUser(query, topK, 0.0, userId);

            Map<String, Object> data = new HashMap<>();
            data.put("query", query);
            data.put("results", results);
            data.put("count", results.size());

            return Result.success(data);

        } catch (Exception e) {
            log.error("RAG检索失败", e);
            return Result.error("检索失败: " + e.getMessage());
        }
    }

    /**
     * 测试RAG增强提示词
     *
     * @param request HTTP请求
     * @param query   查询内容
     * @param topK    返回结果数量（可选，默认3）
     * @return 增强后的提示词
     */
    @GetMapping("/augment")
    public Result<?> augmentPrompt(
            HttpServletRequest request,
            @RequestParam("query") String query,
            @RequestParam(value = "topK", defaultValue = "3") int topK) {

        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.unauthorized();
        }

        if (query == null || query.trim().isEmpty()) {
            return Result.badRequest("查询内容不能为空");
        }

        try {
            String augmentedPrompt = ragRetrievalService.buildAugmentedPromptByUser(query, topK, userId);

            Map<String, Object> data = new HashMap<>();
            data.put("originalQuery", query);
            data.put("augmentedPrompt", augmentedPrompt);

            return Result.success(data);

        } catch (Exception e) {
            log.error("构建增强提示词失败", e);
            return Result.error("处理失败: " + e.getMessage());
        }
    }
}
