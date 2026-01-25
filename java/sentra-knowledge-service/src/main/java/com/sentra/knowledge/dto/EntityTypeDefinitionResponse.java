package com.sentra.knowledge.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 实体类型定义响应DTO
 */
@Data
public class EntityTypeDefinitionResponse {

    private String id;
    private String templateId;
    private String entityCode;
    private String entityName;
    private String entityDescription;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
