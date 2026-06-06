package com.luckzll.demo.service;

import java.io.File;
import java.io.InputStream;
import java.util.List;

/**
 * 文档解析服务接口
 */
public interface DocumentParseService {

    /**
     * 解析文档文件，提取文本内容
     *
     * @param file 文档文件
     * @return 文档文本内容
     */
    String parseDocument(File file);

    /**
     * 解析文档输入流，提取文本内容
     *
     * @param inputStream 文档输入流
     * @param fileType    文件类型
     * @return 文档文本内容
     */
    String parseDocument(InputStream inputStream, String fileType);

    /**
     * 将文本内容分块
     *
     * @param content    原始文本内容
     * @param chunkSize  每块最大字符数
     * @param overlap    块之间重叠字符数
     * @return 分块后的文本列表
     */
    List<String> splitText(String content, int chunkSize, int overlap);

    /**
     * 智能分块：根据段落和语义边界分块
     *
     * @param content   原始文本内容
     * @param maxChunkSize 最大块大小
     * @return 分块后的文本列表
     */
    List<String> splitTextSmart(String content, int maxChunkSize);

    /**
     * 生成文档摘要
     *
     * @param content 文档内容
     * @param maxLength 摘要最大长度
     * @return 文档摘要
     */
    String generateSummary(String content, int maxLength);

    /**
     * 支持的文档类型
     *
     * @param fileType 文件类型
     * @return 是否支持
     */
    boolean isSupported(String fileType);
}
