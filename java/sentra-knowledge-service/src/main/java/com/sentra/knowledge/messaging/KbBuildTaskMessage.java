package com.sentra.knowledge.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库构建任务消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KbBuildTaskMessage {

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
     * Python章节重构后的Markdown内容
     */
    private String newMdContent;

    /**
     * 重试次数
     */
    private Integer retryCount = 0;
}
