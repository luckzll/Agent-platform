package com.luckzll.demo.service;

import com.luckzll.demo.entity.dto.ChatRequestDTO;

import java.util.List;
import java.util.function.Consumer;

/**
 * DeepSeek AI 服务接口（支持多模态）
 */
public interface DeepSeekService {

    /**
     * 发送聊天请求
     *
     * @param message  用户消息
     * @param imageUrl 图片URL（可为null）
     * @param history  历史消息
     * @return AI 回复内容
     */
    String chat(String message, String imageUrl, List<ChatRequestDTO.Message> history);

    /**
     * 流式聊天请求
     *
     * @param message       用户消息
     * @param imageUrl      图片URL（可为null）
     * @param history       历史消息
     * @param onMessage     收到消息片段的回调
     * @param onComplete    完成回调，返回完整内容
     * @param onError       错误回调
     */
    void chatStream(String message, String imageUrl, List<ChatRequestDTO.Message> history,
                    Consumer<String> onMessage, Consumer<String> onComplete, Consumer<Exception> onError);
}
