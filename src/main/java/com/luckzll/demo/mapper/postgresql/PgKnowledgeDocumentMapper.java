package com.luckzll.demo.mapper.postgresql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.luckzll.demo.entity.KnowledgeDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识库文档Mapper - PostgreSQL
 */
@Mapper
public interface PgKnowledgeDocumentMapper extends BaseMapper<KnowledgeDocument> {

    /**
     * 根据状态查询文档列表
     */
    List<KnowledgeDocument> selectByStatus(@Param("status") String status);

    /**
     * 更新文档状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 更新文档状态和分块数量
     */
    int updateStatusAndChunkCount(@Param("id") Long id, @Param("status") String status, @Param("chunkCount") Integer chunkCount);

    /**
     * 更新错误信息
     */
    int updateError(@Param("id") Long id, @Param("errorMsg") String errorMsg);

    /**
     * 查询所有已完成状态的文档
     */
    List<KnowledgeDocument> selectCompletedDocs();

    /**
     * 根据用户ID查询文档列表
     */
    List<KnowledgeDocument> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询已完成状态的文档
     */
    List<KnowledgeDocument> selectCompletedDocsByUserId(@Param("userId") Long userId);
}
