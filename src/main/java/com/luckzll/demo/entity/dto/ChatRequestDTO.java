package com.luckzll.demo.entity.dto;

import lombok.Data;

import java.util.List;

/**
 * DeepSeek 聊天请求 DTO
 */
@Data
public class ChatRequestDTO {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 用户消息内容
     */
    private String message;

    /**
     * 图片URL（多模态消息，可选）
     */
    private String imageUrl;

    /**
     * 历史消息列表（可选）
     */
    private List<Message> history;

    @Data
    public static class Message {
        private String role;
        private String content;
    }
}
