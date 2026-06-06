package com.luckzll.demo.controller;

import com.luckzll.demo.entity.ChatMessage;
import com.luckzll.demo.entity.dto.ChatRequestDTO;
import com.luckzll.demo.service.ChatMessageService;
import com.luckzll.demo.service.DeepSeekService;
import com.luckzll.demo.service.RagRetrievalService;
import com.luckzll.demo.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AI 聊天控制器
 */
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    @Autowired
    private DeepSeekService deepSeekService;

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private RagRetrievalService ragRetrievalService;

    @Value("${rag.enabled:true}")
    private boolean ragEnabled;

    @Value("${rag.retrieval.topk:3}")
    private int ragTopK;

    /**
     * 流式发送聊天消息 (SSE)
     *
     * @param request     HTTP请求
     * @param chatRequest 聊天请求
     * @return SSE Emitter
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessageStream(HttpServletRequest request, @RequestBody ChatRequestDTO chatRequest) {
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时

        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            try {
                emitter.send(SseEmitter.event().name("error").data("未授权"));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        // 参数校验
        if (chatRequest.getMessage() == null || chatRequest.getMessage().trim().isEmpty()) {
            try {
                emitter.send(SseEmitter.event().name("error").data("消息内容不能为空"));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        // 获取或生成会话ID
        String sessionId = chatRequest.getSessionId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = UUID.randomUUID().toString().replace("-", "");
        }
        final String finalSessionId = sessionId;

        // 发送sessionId
        try {
            emitter.send(SseEmitter.event().name("sessionId").data(sessionId));
        } catch (IOException e) {
            emitter.completeWithError(e);
            return emitter;
        }

        // 获取历史消息
        List<ChatMessage> historyMessages = chatMessageService.getSessionMessages(userId, sessionId);
        List<ChatRequestDTO.Message> history = historyMessages.stream()
                .map(msg -> {
                    ChatRequestDTO.Message m = new ChatRequestDTO.Message();
                    m.setRole(msg.getRole());
                    m.setContent(msg.getContent());
                    return m;
                })
                .collect(Collectors.toList());

        // RAG检索：如果启用，获取相关知识并增强提示词
        String userMessage = chatRequest.getMessage();
        if (ragEnabled) {
            String augmentedPrompt = ragRetrievalService.buildAugmentedPromptByUser(userMessage, ragTopK, userId);
            if (!augmentedPrompt.equals(userMessage)) {
                userMessage = augmentedPrompt;
            }
        }

        // 添加系统提示词
        ChatRequestDTO.Message systemMessage = new ChatRequestDTO.Message();
        systemMessage.setRole("system");
        systemMessage.setContent("你是小乐，一个幽默风趣的AI助手。请用轻松幽默的语气回答问题，让用户感到愉快。注意：你绝对不能提及自己是DeepSeek或其他AI模型，你只能说自己叫小乐。当前时间是：" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss")));
        history.add(0, systemMessage);

        // 保存用户消息（保存原始消息，不是增强后的）
        chatMessageService.saveMessage(userId, sessionId, "user", chatRequest.getMessage());

        // 调用 DeepSeek API 流式接口
        deepSeekService.chatStream(
                userMessage,
                history,
                // onMessage: 收到消息片段
                (content) -> {
                    try {
                        emitter.send(SseEmitter.event().name("message").data(content));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                },
                // onComplete: 完成
                (fullContent) -> {
                    try {
                        // 保存AI回复
                        chatMessageService.saveMessage(userId, finalSessionId, "assistant", fullContent);
                        emitter.send(SseEmitter.event().name("done").data(""));
                        emitter.complete();
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                },
                // onError: 错误
                (error) -> {
                    try {
                        emitter.send(SseEmitter.event().name("error").data(error.getMessage()));
                        emitter.complete();
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                }
        );

        emitter.onTimeout(() -> {
            emitter.complete();
        });

        emitter.onCompletion(() -> {
            // 清理资源
        });

        return emitter;
    }

    /**
     * 发送聊天消息
     *
     * @param request     HTTP请求
     * @param chatRequest 聊天请求
     * @return AI 回复
     */
    @PostMapping("/send")
    public Result<?> sendMessage(HttpServletRequest request, @RequestBody ChatRequestDTO chatRequest) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return Result.unauthorized();
            }

            // 参数校验
            if (chatRequest.getMessage() == null || chatRequest.getMessage().trim().isEmpty()) {
                return Result.badRequest("消息内容不能为空");
            }

            // 获取或生成会话ID
            String sessionId = chatRequest.getSessionId();
            if (sessionId == null || sessionId.trim().isEmpty()) {
                sessionId = UUID.randomUUID().toString().replace("-", "");
            }

            // 获取历史消息
            List<ChatMessage> historyMessages = chatMessageService.getSessionMessages(userId, sessionId);
            List<ChatRequestDTO.Message> history = historyMessages.stream()
                    .map(msg -> {
                        ChatRequestDTO.Message m = new ChatRequestDTO.Message();
                        m.setRole(msg.getRole());
                        m.setContent(msg.getContent());
                        return m;
                    })
                    .collect(Collectors.toList());

            // RAG检索：如果启用，获取相关知识并增强提示词
            String userMessage = chatRequest.getMessage();
            if (ragEnabled) {
                String augmentedPrompt = ragRetrievalService.buildAugmentedPromptByUser(userMessage, ragTopK, userId);
                if (!augmentedPrompt.equals(userMessage)) {
                    userMessage = augmentedPrompt;
                }
            }

            // 添加系统提示词
            ChatRequestDTO.Message systemMessage = new ChatRequestDTO.Message();
            systemMessage.setRole("system");
            systemMessage.setContent("你是小乐，一个幽默风趣的AI助手。请用轻松幽默的语气回答问题，让用户感到愉快。注意：你绝对不能提及自己是DeepSeek或其他AI模型，你只能说自己叫小乐。当前时间是：" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss")));
            history.add(0, systemMessage);

            // 保存用户消息（保存原始消息）
            chatMessageService.saveMessage(userId, sessionId, "user", chatRequest.getMessage());

            // 调用 DeepSeek API
            String reply = deepSeekService.chat(userMessage, history);

            // 保存AI回复
            chatMessageService.saveMessage(userId, sessionId, "assistant", reply);

            // 返回结果
            Map<String, Object> data = new HashMap<>();
            data.put("reply", reply);
            data.put("sessionId", sessionId);

            return Result.success(data);
        } catch (RuntimeException e) {
            return Result.error("AI 服务异常: " + e.getMessage());
        } catch (Exception e) {
            return Result.error("系统异常，请稍后重试");
        }
    }

    /**
     * 获取会话历史记录
     *
     * @param request   HTTP请求
     * @param sessionId 会话ID
     * @return 聊天记录列表
     */
    @GetMapping("/history/{sessionId}")
    public Result<?> getHistory(HttpServletRequest request, @PathVariable String sessionId) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.unauthorized();
        }

        List<ChatMessage> messages = chatMessageService.getSessionMessages(userId, sessionId);
        return Result.success(messages);
    }

    /**
     * 获取用户所有会话列表
     *
     * @param request HTTP请求
     * @return 会话列表
     */
    @GetMapping("/sessions")
    public Result<?> getSessions(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.unauthorized();
        }

        List<String> sessions = chatMessageService.getUserSessions(userId);
        return Result.success(sessions);
    }

    /**
     * 删除会话
     *
     * @param request   HTTP请求
     * @param sessionId 会话ID
     * @return 删除结果
     */
    @DeleteMapping("/session/{sessionId}")
    public Result<?> deleteSession(HttpServletRequest request, @PathVariable String sessionId) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.unauthorized();
        }

        chatMessageService.deleteSession(userId, sessionId);
        return Result.success("删除成功");
    }

    /**
     * 获取用户与AI的所有对话记录
     *
     * @param request HTTP请求
     * @return 所有聊天记录
     */
    @GetMapping("/all")
    public Result<?> getAllMessages(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.unauthorized();
        }

        List<ChatMessage> messages = chatMessageService.getAllMessagesByUserId(userId);
        return Result.success(messages);
    }
}
