package com.sentra.common.ocr;

import lombok.Data;

import java.util.Map;

/**
 * OCR响应结果
 */
@Data
public class OcrResponse {

    /**
     * Markdown格式的内容
     */
    private String mdContent;

    /**
     * 原始JSON响应
     */
    private Map<String, Object> rawResponse;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;
}
