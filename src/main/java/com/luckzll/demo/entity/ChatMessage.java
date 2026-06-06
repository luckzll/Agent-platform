package com.luckzll.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 聊天记录实体
 */
@Data
@TableName("chat_message")
public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 会话ID（用于区分不同对话）
     */
    private String sessionId;

    /**
     * 消息角色：user-用户，assistant-AI助手
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 图片URL（多模态消息）
     */
    private String imageUrl;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
