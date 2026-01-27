package com.sentra.knowledge.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Markdown解析响应
 */
@Data
public class MdParseResponse {

    /**
     * 状态
     * Python端字段名: status
     */
    @JsonProperty("status")
    private String status;

    /**
     * 重构后的Markdown内容
     * Python端字段名: new_md_content
     */
    @JsonProperty("new_md_content")
    private String newMdContent;
}
