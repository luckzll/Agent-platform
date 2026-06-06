package com.luckzll.demo.controller;

import com.luckzll.demo.config.AliyunOssConfig;
import com.luckzll.demo.utils.OssUtil;
import com.luckzll.demo.utils.Result;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * 阿里云 OSS 文件上传接口
 * 前端通过 type 参数指定上传类型（IMAGE/VIDEO/FILE），自动路由到不同OSS目录
 */
@RestController
@RequestMapping("/api/v1/oss")
@RequiredArgsConstructor
public class OssUploadController {

    private final OssUtil ossUtil;
    private final AliyunOssConfig ossConfig;

    /**
     * 文件类型枚举
     */
    @Getter
    public enum FileType {
        IMAGE(10 * 1024 * 1024L,
                Arrays.asList("image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp", "image/svg+xml"),
                "图片", "jpg/png/gif/webp/bmp/svg"),
        VIDEO(500L * 1024 * 1024,
                Arrays.asList("video/mp4", "video/avi", "video/quicktime", "video/x-msvideo", "video/x-flv", "video/webm", "video/x-matroska"),
                "视频", "mp4/avi/mov/flv/webm/mkv"),
        FILE(100L * 1024 * 1024,
                null,
                "文件", null);

        private final long maxSize;
        private final List<String> allowedTypes;
        private final String label;
        private final String formatDesc;

        FileType(long maxSize, List<String> allowedTypes, String label, String formatDesc) {
            this.maxSize = maxSize;
            this.allowedTypes = allowedTypes;
            this.label = label;
            this.formatDesc = formatDesc;
        }
    }

    /**
     * 统一上传接口
     *
     * @param file 上传的文件
     * @param type 文件类型：IMAGE / VIDEO / FILE
     */
    @PostMapping("/upload")
    public Result<?> upload(@RequestParam("file") MultipartFile file,
                            @RequestParam("type") FileType type) {
        if (file.isEmpty()) {
            return Result.error("请选择要上传的" + type.getLabel());
        }
        // 校验文件格式（FILE类型不限格式）
        if (type.getAllowedTypes() != null && !type.getAllowedTypes().contains(file.getContentType())) {
            return Result.error("不支持的" + type.getLabel() + "格式，仅支持: " + type.getFormatDesc());
        }
        // 校验文件大小
        if (file.getSize() > type.getMaxSize()) {
            return Result.error(type.getLabel() + "大小不能超过" + (type.getMaxSize() / 1024 / 1024) + "MB");
        }
        // 根据类型获取对应OSS目录前缀
        String dirPrefix = getDirPrefix(type);
        String url = ossUtil.uploadFile(file, dirPrefix);
        return Result.success("上传成功", url);
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/delete")
    public Result<?> delete(@RequestParam("url") String fileUrl) {
        ossUtil.deleteFile(fileUrl);
        return Result.success("删除成功");
    }

    /**
     * 根据枚举类型获取OSS目录前缀
     */
    private String getDirPrefix(FileType type) {
        return switch (type) {
            case IMAGE -> ossConfig.getDir().getImage();
            case VIDEO -> ossConfig.getDir().getVideo();
            case FILE -> ossConfig.getDir().getFile();
        };
    }
}
