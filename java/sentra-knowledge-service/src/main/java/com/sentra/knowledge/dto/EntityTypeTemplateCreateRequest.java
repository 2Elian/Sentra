package com.sentra.knowledge.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 创建实体类型模板请求DTO
 */
@Data
public class EntityTypeTemplateCreateRequest {

    /**
     * 模板名称
     */
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100, message = "模板名称长度不能超过100")
    private String name;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 实体类型列表
     */
    private java.util.List<EntityTypeDefinitionRequest> entityTypes;
}
