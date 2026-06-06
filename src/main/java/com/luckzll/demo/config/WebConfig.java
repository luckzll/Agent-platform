package com.luckzll.demo.config;

import com.luckzll.demo.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ServletComponentScan("com.luckzll.demo.filter") // 启用@WebFilter扫描
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                // 拦截所有请求
                .addPathPatterns("/**")
                // 放行登录注册接口和验证码接口
                .excludePathPatterns(
                        "/api/v1/auth/login",
                        "/api/v1/auth/register",
                        "/api/v1/captcha/**",
                        "/api/v1/video/**",
                        // 放行支付回调接口（支付宝/微信服务器调用）
                        "/api/pay/alipay/notify",
                        "/api/pay/wechat/notify",
                        // 放行静态资源
                        "/",
                        "/index.html",
                        "/guide/**",
                        "/*.html",
                        "/*.js",
                        "/*.css",
                        "/static/**",
                        "/favicon.ico",
                        // 放行Swagger相关
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**",
                        "/webjars/**",
                        // 放行错误页面
                        "/error"
                );
    }

    /**
     * 跨域配置 - 暴露自定义响应头
     */
    @Bean
    public CorsFilter corsFilter() {
        // 1. 配置跨域规则
        CorsConfiguration config = new CorsConfiguration();
        // 允许所有来源域名访问，生产环境建议限制为具体域名
        config.addAllowedOriginPattern("*");
        config.setAllowCredentials(true);
        // 允许所有 HTTP 方法
        config.addAllowedMethod("*");
        // 允许所有请求头
        config.addAllowedHeader("*");
        
        // 暴露Token响应头（前端可以读取）
        config.addExposedHeader("Authorization");
        config.addExposedHeader("Token");
        // 暴露客户端类型响应头
        config.addExposedHeader("X-Client-Type");
        config.addExposedHeader("X-Client-Version");
        config.addExposedHeader("X-os-Type");
        config.addExposedHeader("X-os-Version");
        config.addExposedHeader("X-Device-Type");
        
        // 设置预检请求缓存时间为3600秒
        config.setMaxAge(3600L);

        // 2. 配置拦截路径（所有路径）
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        // 3. 返回过滤器
        return new CorsFilter(source);
    }
}
