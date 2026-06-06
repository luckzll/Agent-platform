package com.luckzll.demo.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 客户端信息过滤器
 * 负责收集客户端类型、版本、操作系统等信息存入MDC，供日志使用
 */
@Component
@WebFilter(urlPatterns = "/**")
@Order(1) // 确保最先执行
public class ClientInfoFilter implements Filter {

    // 请求头常量定义
    public static final String HEADER_CLIENT_TYPE = "X-Client-Type";
    public static final String HEADER_CLIENT_VERSION = "X-Client-Version";
    public static final String HEADER_OS_TYPE = "X-os-Type";
    public static final String HEADER_OS_VERSION = "X-os-Version";
    public static final String HEADER_DEVICE_TYPE = "X-Device-Type";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        try {
            // 从请求头获取客户端信息并存入MDC
            String clientType = httpRequest.getHeader(HEADER_CLIENT_TYPE);
            String clientVersion = httpRequest.getHeader(HEADER_CLIENT_VERSION);
            String osType = httpRequest.getHeader(HEADER_OS_TYPE);
            String osVersion = httpRequest.getHeader(HEADER_OS_VERSION);
            String deviceType = httpRequest.getHeader(HEADER_DEVICE_TYPE);
            
            // 设置默认值（如果没有传）
            MDC.put("clientType", clientType != null ? clientType : "-");
            MDC.put("clientVersion", clientVersion != null ? clientVersion : "-");
            MDC.put("osType", osType != null ? osType : "-");
            MDC.put("osVersion", osVersion != null ? osVersion : "-");
            MDC.put("deviceType", deviceType != null ? deviceType : "-");
            MDC.put("userId", "-"); // 初始值，JwtInterceptor会覆盖
            
            // 继续过滤器链
            chain.doFilter(request, response);
            
        } finally {
            // 请求结束后清理所有MDC数据
            MDC.clear();
        }
    }
}
