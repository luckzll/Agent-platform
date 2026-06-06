package com.luckzll.demo.mapper.postgresql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.luckzll.demo.entity.KnowledgeChunk;
import com.luckzll.demo.entity.dto.RetrievalResultDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识库文档分块Mapper - PostgreSQL
 */
@Mapper
public interface PgKnowledgeChunkMapper extends BaseMapper<KnowledgeChunk> {

    /**
     * 批量插入分块（使用JSON格式存储向量）
     */
    int batchInsert(@Param("chunks") List<KnowledgeChunk> chunks);

    /**
     * 根据文档ID删除所有分块
     */
    int deleteByDocId(@Param("docId") Long docId);

    /**
     * 根据文档ID查询所有分块
     */
    List<KnowledgeChunk> selectByDocId(@Param("docId") Long docId);

    /**
     * 查询分块总数
     */
    int countByDocId(@Param("docId") Long docId);

    /**
     * 向量相似度搜索
     */
    List<RetrievalResultDTO> selectForRetrieval(@Param("limit") int limit);

    /**
     * 获取所有分块用于本地相似度计算
     */
    List<RetrievalResultDTO> selectAllChunksWithDoc();

    /**
     * 根据用户ID获取分块用于本地相似度计算
     */
    List<RetrievalResultDTO> selectChunksByUserId(@Param("userId") Long userId);
}
