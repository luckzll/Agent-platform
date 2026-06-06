package com.luckzll.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.luckzll.demo.entity.ChatMessage;

import java.util.List;

/**
 * 聊天记录服务接口
 */
public interface ChatMessageService extends IService<ChatMessage> {

    /**
     * 保存聊天消息
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @param role      消息角色
     * @param content   消息内容
     * @param imageUrl  图片URL（可为null）
     * @return 保存的消息
     */
    ChatMessage saveMessage(Long userId, String sessionId, String role, String content, String imageUrl);

    /**
     * 获取会话的聊天记录
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @return 聊天记录列表
     */
    List<ChatMessage> getSessionMessages(Long userId, String sessionId);

    /**
     * 获取用户的所有会话
     *
     * @param userId 用户ID
     * @return 会话ID列表
     */
    List<String> getUserSessions(Long userId);

    /**
     * 删除会话
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     */
    void deleteSession(Long userId, String sessionId);

    /**
     * 获取用户所有聊天记录
     *
     * @param userId 用户ID
     * @return 所有聊天记录列表
     */
    List<ChatMessage> getAllMessagesByUserId(Long userId);
}
