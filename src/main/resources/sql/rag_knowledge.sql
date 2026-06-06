-- ============================================
-- RAG知识库相关表结构
-- 基于PostgreSQL + pgvector扩展
-- ============================================

-- 1. 启用pgvector扩展（需要提前安装pgvector）
-- CREATE EXTENSION IF NOT EXISTS vector;

-- 2. 知识库文档表
CREATE TABLE IF NOT EXISTS knowledge_document (
    id BIGSERIAL PRIMARY KEY,
    doc_name VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    doc_type VARCHAR(50) NOT NULL,
    doc_size BIGINT NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    summary TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'processing',
    chunk_count INTEGER DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    error_msg TEXT
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_kd_status ON knowledge_document(status);
CREATE INDEX IF NOT EXISTS idx_kd_create_time ON knowledge_document(create_time);

-- 添加表和列注释
COMMENT ON TABLE knowledge_document IS '知识库文档表';
COMMENT ON COLUMN knowledge_document.doc_name IS '文档名称';
COMMENT ON COLUMN knowledge_document.original_name IS '原始文件名';
COMMENT ON COLUMN knowledge_document.doc_type IS '文档类型：pdf, doc, docx, txt, md等';
COMMENT ON COLUMN knowledge_document.doc_size IS '文档大小（字节）';
COMMENT ON COLUMN knowledge_document.storage_path IS '存储路径';
COMMENT ON COLUMN knowledge_document.summary IS '文档内容摘要';
COMMENT ON COLUMN knowledge_document.status IS '文档状态：processing-处理中, completed-已完成, failed-失败';
COMMENT ON COLUMN knowledge_document.chunk_count IS '分块数量';
COMMENT ON COLUMN knowledge_document.create_time IS '创建时间';
COMMENT ON COLUMN knowledge_document.update_time IS '更新时间';
COMMENT ON COLUMN knowledge_document.error_msg IS '错误信息';

-- 3. 知识库文档分块表（使用pgvector存储向量）
CREATE TABLE IF NOT EXISTS knowledge_chunk (
    id BIGSERIAL PRIMARY KEY,
    doc_id BIGINT NOT NULL,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    -- embedding vector(1536),
    embedding TEXT,
    char_count INTEGER NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chunk_doc FOREIGN KEY (doc_id) REFERENCES knowledge_document(id) ON DELETE CASCADE
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_kc_doc_id ON knowledge_chunk(doc_id);
-- 如果使用pgvector，创建向量索引
-- CREATE INDEX IF NOT EXISTS idx_kc_embedding ON knowledge_chunk USING ivfflat (embedding vector_cosine_ops);

-- 添加表和列注释
COMMENT ON TABLE knowledge_chunk IS '知识库文档分块表';
COMMENT ON COLUMN knowledge_chunk.doc_id IS '所属文档ID';
COMMENT ON COLUMN knowledge_chunk.chunk_index IS '分块序号';
COMMENT ON COLUMN knowledge_chunk.content IS '分块内容';
COMMENT ON COLUMN knowledge_chunk.embedding IS '向量嵌入JSON字符串（如果使用pgvector则改为vector类型）';
COMMENT ON COLUMN knowledge_chunk.char_count IS '分块字符数';
COMMENT ON COLUMN knowledge_chunk.create_time IS '创建时间';

-- ============================================
-- 可选：使用pgvector扩展的表结构（推荐生产环境使用）
-- ============================================
/*
-- 需要先安装pgvector: https://github.com/pgvector/pgvector

-- 启用扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 分块表（使用vector类型）
CREATE TABLE IF NOT EXISTS knowledge_chunk_vector (
    id BIGSERIAL PRIMARY KEY,
    doc_id BIGINT NOT NULL REFERENCES knowledge_document(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    embedding vector(1536) NOT NULL,  -- OpenAI embedding维度
    char_count INTEGER NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建向量索引（IVFFlat索引适合高维向量）
CREATE INDEX ON knowledge_chunk_vector USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- 相似度搜索查询示例：
-- SELECT id, content, 1 - (embedding <=> query_embedding) AS similarity
-- FROM knowledge_chunk_vector
-- ORDER BY embedding <=> query_embedding
-- LIMIT 5;
*/
