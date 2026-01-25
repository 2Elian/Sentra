package com.sentra.knowledge.client.dto;

import lombok.Data;

/**
 * Markdown解析响应
 */
@Data
public class MdParseResponse {

    /**
     * 重构后的Markdown内容
     */
    private String newMdContent;
}
