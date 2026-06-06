-- 聊天记录表
CREATE TABLE IF NOT EXISTS `chat_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
    `role` VARCHAR(20) NOT NULL COMMENT '消息角色：user-用户，assistant-AI助手',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `image_url` VARCHAR(500) DEFAULT NULL COMMENT '图片URL（多模态消息）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_session` (`user_id`, `session_id`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天记录表';

-- 已有表加字段
ALTER TABLE `chat_message` ADD COLUMN `image_url` VARCHAR(500) DEFAULT NULL COMMENT '图片URL（多模态消息）' AFTER `content`;
