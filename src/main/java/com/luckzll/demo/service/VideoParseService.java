package com.luckzll.demo.service;

import com.luckzll.demo.entity.dto.VideoParseResultDTO;

/**
 * 视频解析服务接口
 */
public interface VideoParseService {

    /**
     * 解析抖音视频链接
     *
     * @param url 抖音分享链接
     * @return 视频解析结果
     */
    VideoParseResultDTO parseVideo(String url);
}
