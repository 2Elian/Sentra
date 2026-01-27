package com.sentra.knowledge.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 实体类型定义请求DTO
 */
@Data
public class EntityTypeDefinitionRequest {

    /**
     * 实体编码
     */
    @NotBlank(message = "实体编码不能为空")
    @Size(max = 50, message = "实体编码长度不能超过50")
    private String entityCode;

    /**
     * 实体名称
     */
    @NotBlank(message = "实体名称不能为空")
    @Size(max = 100, message = "实体名称长度不能超过100")
    private String entityName;

    /**
     * 实体描述
     */
    @NotBlank(message = "实体描述不能为空")
    private String entityDescription;

    /**
     * 显示顺序
     */
    private Integer displayOrder;
}
