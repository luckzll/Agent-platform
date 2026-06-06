package com.luckzll.demo.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.luckzll.demo.config.AliyunOssConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * 阿里云 OSS 工具类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OssUtil {

    private final OSS ossClient;
    private final AliyunOssConfig ossConfig;

    /**
     * 上传文件到 OSS
     *
     * @param file      上传的文件
     * @param dirPrefix 目录前缀（如 image/、video/、file/）
     * @return 文件访问 URL
     */
    public String uploadFile(MultipartFile file, String dirPrefix) {
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        // 直接放在类型目录下，不再按日期分子目录
        String objectName = dirPrefix + UUID.randomUUID().toString().replace("-", "") + extension;

        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            // 设置为 inline，浏览器直接预览而非下载
            metadata.setContentDisposition("inline");

            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    ossConfig.getBucketName(), objectName, inputStream, metadata);
            ossClient.putObject(putObjectRequest);

            // Bucket已设为公共读，直接返回普通URL
            String fileUrl = ossConfig.getBaseUrl() + objectName;
            log.info("文件上传成功: {}", fileUrl);
            return fileUrl;
        } catch (IOException e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除 OSS 文件
     */
    public void deleteFile(String fileUrl) {
        String objectName = fileUrl.replace(ossConfig.getBaseUrl(), "");
        ossClient.deleteObject(ossConfig.getBucketName(), objectName);
        log.info("文件删除成功: {}", objectName);
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
