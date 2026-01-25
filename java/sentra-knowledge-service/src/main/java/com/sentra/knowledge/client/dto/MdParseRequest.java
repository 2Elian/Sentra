package com.sentra.knowledge.client.dto;

import lombok.Data;

/**
 * Markdown解析请求
 */
@Data
public class MdParseRequest {

    /**
     * 文档ID
     */
    private String documentId;

    /**
     * 知识库ID
     */
    private String kbId;

    /**
     * OCR解析后的Markdown内容
     */
    private String mdContent;
}
