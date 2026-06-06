package com.luckzll.demo.entity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 视频解析结果DTO
 */
@Data
public class VideoParseResultDTO {

    /**
     * 视频URL
     */
    @JsonProperty("video_url")
    private String videoUrl;

    /**
     * 封面URL
     */
    @JsonProperty("cover_url")
    private String coverUrl;

    /**
     * 标题
     */
    private String title;

    /**
     * 音乐URL
     */
    @JsonProperty("music_url")
    private String musicUrl;

    /**
     * 图集图片列表
     */
    private List<ImageInfo> images;

    /**
     * 作者信息
     */
    private AuthorInfo author;

    /**
     * 内容描述
     */
    private String content;

    /**
     * 图片信息
     */
    @Data
    public static class ImageInfo {
        /**
         * 图片URL
         */
        private String url;

        /**
         * 实况视频URL
         */
        @JsonProperty("live_photo_url")
        private String livePhotoUrl;
    }

    /**
     * 作者信息
     */
    @Data
    public static class AuthorInfo {
        /**
         * 用户ID
         */
        private String uid;

        /**
         * 昵称
         */
        private String name;

        /**
         * 头像URL
         */
        private String avatar;
    }
}
