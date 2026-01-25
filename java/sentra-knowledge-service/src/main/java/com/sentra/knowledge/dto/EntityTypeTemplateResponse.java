package com.sentra.knowledge.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 实体类型模板响应DTO
 */
@Data
public class EntityTypeTemplateResponse {

    private String id;
    private String tenantId;
    private String name;
    private String description;
    private Boolean isSystem;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    /**
     * 实体类型列表
     */
    private List<EntityTypeDefinitionResponse> entityTypes;
}
