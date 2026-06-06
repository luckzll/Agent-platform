package com.luckzll.demo.mapper.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.luckzll.demo.entity.ChatMessage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 聊天记录 Mapper (MySQL)
 */
public interface MysqlChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 根据用户ID和会话ID查询聊天记录
     */
    List<ChatMessage> selectByUserAndSession(@Param("userId") Long userId, @Param("sessionId") String sessionId);

    /**
     * 查询用户的所有会话ID
     */
    List<String> selectSessionsByUserId(@Param("userId") Long userId);
}
