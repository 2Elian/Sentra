package com.sentra.knowledge.dto;

import com.sentra.common.enums.KnowledgeScope;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库响应DTO
 */
@Data
public class KnowledgeBaseResponse {

    private String id;

    private String kbId;

    private String name;

    private String ownerUserId;

    private KnowledgeScope scope;

    private String description;

    private String tenantId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
