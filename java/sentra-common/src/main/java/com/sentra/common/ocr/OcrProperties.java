package com.sentra.common.ocr;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * OCR服务配置属性
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "sentra.ocr")
public class OcrProperties {

    /**
     * OCR API地址
     */
    private String apiUrl = "http://172.16.107.15:30000/file_parse";

    /**
     * OCR后端引擎
     */
    private String backend = "hybrid-auto-engine";

    /**
     * 连接超时时间（毫秒）
     */
    private int connectTimeout = 300000; // 5 minutes

    /**
     * 读取超时时间（毫秒）
     */
    private int readTimeout = 1200000; // 20 minutes
}
