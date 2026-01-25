package com.sentra.knowledge.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OCR任务消息 | RabbitMQ消息DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcrTaskMessage {

    /**
     * 文档ID
     */
    private String documentId;

    /**
     * 知识库ID
     */
    private String kbId;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 远程文件路径（SFTP）
     */
    private String remoteFilePath;

    /**
     * 原始文件名
     */
    private String filename;

    /**
     * OCR输出目录
     */
    private String ocrOutputDir;

    /**
     * 重试次数
     */
    private Integer retryCount = 0;
}
