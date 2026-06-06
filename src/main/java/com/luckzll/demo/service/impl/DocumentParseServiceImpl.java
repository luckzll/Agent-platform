package com.luckzll.demo.service.impl;

import com.luckzll.demo.service.DocumentParseService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.tika.Tika;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文档解析服务实现
 */
@Slf4j
@Service
public class DocumentParseServiceImpl implements DocumentParseService {

    private final Tika tika = new Tika();

    // 支持的文件类型
    private static final Set<String> SUPPORTED_TYPES = new HashSet<>(Arrays.asList(
            "pdf", "doc", "docx", "txt", "md", "markdown", "html", "htm"
    ));

    @Override
    public String parseDocument(File file) {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("文件不存在");
        }

        String fileName = file.getName().toLowerCase();
        String extension = getFileExtension(fileName);

        try {
            switch (extension) {
                case "pdf":
                    return parsePdf(file);
                case "docx":
                    return parseDocx(file);
                case "doc":
                    // doc格式较复杂，使用Tika解析
                    return tika.parseToString(file);
                case "txt":
                case "md":
                case "markdown":
                    return parseTextFile(file);
                case "html":
                case "htm":
                    return parseHtml(file);
                default:
                    // 使用Tika自动检测和解析
                    return tika.parseToString(file);
            }
        } catch (Exception e) {
            log.error("解析文档失败: {}", file.getAbsolutePath(), e);
            throw new RuntimeException("解析文档失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String parseDocument(InputStream inputStream, String fileType) {
        try {
            String extension = fileType.toLowerCase().replace(".", "");
            switch (extension) {
                case "pdf":
                    return parsePdf(inputStream);
                case "docx":
                    return parseDocx(inputStream);
                case "txt":
                case "md":
                case "markdown":
                    return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                case "html":
                case "htm":
                    return parseHtml(inputStream);
                default:
                    return tika.parseToString(inputStream);
            }
        } catch (Exception e) {
            log.error("解析文档流失败", e);
            throw new RuntimeException("解析文档失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析PDF文件
     */
    private String parsePdf(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * 解析PDF输入流
     */
    private String parsePdf(InputStream inputStream) throws IOException {
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * 解析Word文档
     */
    private String parseDocx(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {
            return document.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.joining("\n"));
        }
    }

    /**
     * 解析Word文档流
     */
    private String parseDocx(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            return document.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .collect(Collectors.joining("\n"));
        }
    }

    /**
     * 解析文本文件
     */
    private String parseTextFile(File file) throws IOException {
        return Files.readString(file.toPath(), StandardCharsets.UTF_8);
    }

    /**
     * 解析HTML文件
     */
    private String parseHtml(File file) throws IOException {
        Document doc = Jsoup.parse(file, "UTF-8");
        return extractTextFromHtml(doc);
    }

    /**
     * 解析HTML流
     */
    private String parseHtml(InputStream inputStream) throws IOException {
        Document doc = Jsoup.parse(inputStream, "UTF-8", "");
        return extractTextFromHtml(doc);
    }

    /**
     * 从HTML中提取文本
     */
    private String extractTextFromHtml(Document doc) {
        // 移除脚本和样式
        doc.select("script, style, nav, footer, header").remove();

        // 提取正文
        StringBuilder text = new StringBuilder();
        Elements paragraphs = doc.select("p, h1, h2, h3, h4, h5, h6, li, article, section");
        for (Element elem : paragraphs) {
            String elemText = elem.text().trim();
            if (!elemText.isEmpty()) {
                text.append(elemText).append("\n");
            }
        }

        // 如果没有找到结构化内容，提取body文本
        if (text.length() == 0) {
            text.append(doc.body().text());
        }

        return text.toString().trim();
    }

    @Override
    public List<String> splitText(String content, int chunkSize, int overlap) {
        if (content == null || content.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> chunks = new ArrayList<>();
        int contentLength = content.length();
        int start = 0;

        while (start < contentLength) {
            int end = Math.min(start + chunkSize, contentLength);

            // 如果不是最后一块，尝试在句子边界处截断
            if (end < contentLength) {
                // 查找最近的句子结束符
                int lastPeriod = content.lastIndexOf("。", end);
                int lastQuestion = content.lastIndexOf("？", end);
                int lastExclaim = content.lastIndexOf("！", end);
                int lastNewline = content.lastIndexOf("\n", end);

                int bestBreak = Math.max(Math.max(lastPeriod, lastQuestion),
                        Math.max(lastExclaim, lastNewline));

                // 如果找到合适的断点且在合理范围内
                if (bestBreak > start && bestBreak > end - chunkSize / 4) {
                    end = bestBreak + 1;
                }
            }

            chunks.add(content.substring(start, end).trim());
            start = end - overlap;

            // 防止无限循环
            if (start >= end) {
                start = end;
            }
        }

        return chunks;
    }

    @Override
    public List<String> splitTextSmart(String content, int maxChunkSize) {
        if (content == null || content.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> chunks = new ArrayList<>();

        // 首先按段落分割
        String[] paragraphs = content.split("\n{2,}|\r\n{2,}");

        StringBuilder currentChunk = new StringBuilder();
        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) continue;

            // 如果当前段落本身就超过最大长度，需要进一步分割
            if (paragraph.length() > maxChunkSize) {
                // 先保存当前块
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                }

                // 分割长段落
                List<String> subChunks = splitText(paragraph, maxChunkSize, maxChunkSize / 10);
                chunks.addAll(subChunks);
            } else {
                // 检查添加当前段落后是否超过限制
                if (currentChunk.length() + paragraph.length() + 2 > maxChunkSize) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                }
                if (currentChunk.length() > 0) {
                    currentChunk.append("\n\n");
                }
                currentChunk.append(paragraph);
            }
        }

        // 添加最后一块
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    @Override
    public String generateSummary(String content, int maxLength) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        // 简单的摘要生成：取前N个字符，并在句子边界截断
        if (content.length() <= maxLength) {
            return content;
        }

        // 查找最后一个完整的句子
        String truncated = content.substring(0, maxLength);
        int lastSentenceEnd = Math.max(
                truncated.lastIndexOf("。"),
                Math.max(truncated.lastIndexOf("！"), truncated.lastIndexOf("？"))
        );

        if (lastSentenceEnd > maxLength * 0.7) {
            return truncated.substring(0, lastSentenceEnd + 1);
        }

        return truncated + "...";
    }

    @Override
    public boolean isSupported(String fileType) {
        if (fileType == null) return false;
        String extension = fileType.toLowerCase().replace(".", "");
        return SUPPORTED_TYPES.contains(extension);
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "";
    }
}
