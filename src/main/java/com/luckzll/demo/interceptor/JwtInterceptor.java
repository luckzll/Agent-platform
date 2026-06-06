package com.luckzll.demo.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luckzll.demo.utils.JwtUtils;
import com.luckzll.demo.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求头中的Authorization
        String authHeader = request.getHeader("Authorization");

        // 放行OPTIONS请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 验证Token是否存在
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(Result.unauthorized("缺少Token或格式错误")));
            return false;
        }

        // 提取Token
        String token = authHeader.substring(7);

        // 验证Token有效性
        if (!jwtUtils.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(Result.unauthorized("Token无效或已过期")));
            return false;
        }

        // 将用户ID存入request属性，供后续使用
        Long userId = jwtUtils.getUserIdFromToken(token);
        request.setAttribute("userId", userId);
        
        // 将userId放入MDC，方便日志追踪（覆盖ClientInfoFilter设置的初始值）
        MDC.put("userId", String.valueOf(userId));

        return true;
    }
}
