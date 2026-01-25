package com.sentra.knowledge.dto;

import com.sentra.common.enums.KnowledgeScope;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 更新知识库请求DTO
 */
@Data
public class KnowledgeBaseUpdateRequest {

    /**
     * 知识库名称
     */
    @NotBlank(message = "知识库名称不能为空")
    private String name;

    /**
     * 知识库范围
     */
    private KnowledgeScope scope;

    /**
     * 知识库描述
     */
    private String description;
}
