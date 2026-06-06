package com.luckzll.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.luckzll.demo.entity.ChatMessage;
import com.luckzll.demo.mapper.mysql.MysqlChatMessageMapper;
import com.luckzll.demo.service.ChatMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天记录服务实现
 */
@Service
public class ChatMessageServiceImpl extends ServiceImpl<MysqlChatMessageMapper, ChatMessage> implements ChatMessageService {

    @Autowired
    private MysqlChatMessageMapper chatMessageMapper;

    @Override
    public ChatMessage saveMessage(Long userId, String sessionId, String role, String content, String imageUrl) {
        ChatMessage message = new ChatMessage();
        message.setUserId(userId);
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setImageUrl(imageUrl);
        message.setCreateTime(LocalDateTime.now());
        chatMessageMapper.insert(message);
        return message;
    }

    @Override
    public List<ChatMessage> getSessionMessages(Long userId, String sessionId) {
        return chatMessageMapper.selectByUserAndSession(userId, sessionId);
    }

    @Override
    public List<String> getUserSessions(Long userId) {
        return chatMessageMapper.selectSessionsByUserId(userId);
    }

    @Override
    public void deleteSession(Long userId, String sessionId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getUserId, userId)
                .eq(ChatMessage::getSessionId, sessionId);
        chatMessageMapper.delete(wrapper);
    }

    @Override
    public List<ChatMessage> getAllMessagesByUserId(Long userId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getUserId, userId)
                .orderByAsc(ChatMessage::getCreateTime);
        return chatMessageMapper.selectList(wrapper);
    }
}
