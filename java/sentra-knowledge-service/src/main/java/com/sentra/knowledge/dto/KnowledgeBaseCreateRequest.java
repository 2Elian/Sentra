package com.sentra.knowledge.dto;

import com.sentra.common.enums.KnowledgeScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建知识库请求DTO
 */
@Data
public class KnowledgeBaseCreateRequest {

    /**
     * 知识库名称
     */
    @NotBlank(message = "知识库名称不能为空")
    private String name;

    /**
     * 所有者用户ID
     */
    @NotBlank(message = "所有者用户ID不能为空")
    private String ownerUserId;

    /**
     * 知识库范围
     */
    @NotNull(message = "知识库范围不能为空")
    private KnowledgeScope scope;

    /**
     * 知识库描述
     */
    private String description;

    /**
     * 实体类型模板ID（可选，不填则使用系统默认合同领域模板）
     */
    private String entityTemplateId;
}
