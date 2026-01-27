package com.sentra.knowledge.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Markdown解析请求
 */
@Data
public class MdParseRequest {

    /**
     * OCR解析后的Markdown内容
     * Python端字段名: md_content
     */
    @JsonProperty("md_content")
    private String mdContent;

    /**
     * 文档ID
     */
    @JsonProperty("documentId")
    private String documentId;

    /**
     * 知识库ID
     */
    @JsonProperty("kbId")
    private String kbId;
}
