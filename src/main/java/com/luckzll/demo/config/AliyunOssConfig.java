package com.luckzll.demo.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云 OSS 配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "aliyun.oss")
public class AliyunOssConfig {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
    private Dir dir;

    @Data
    public static class Dir {
        private String image;
        private String video;
        private String file;
    }

    @Bean
    public OSS ossClient() {
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }

    /**
     * 获取文件访问的基础URL
     */
    public String getBaseUrl() {
        return "https://" + bucketName + "." + endpoint + "/";
    }
}
