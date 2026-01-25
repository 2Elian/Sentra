package com.sentra.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 实体类型模板实体
 * 代表一个领域的实体类型配置（如"合同领域"、"论文领域"）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_entity_type_template")
public class EntityTypeTemplate {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 模板名称（如：合同领域、论文领域）
     */
    private String name;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 是否为系统预置模板
     */
    private Boolean isSystem;

    /**
     * 是否启用
     */
    private Boolean isActive;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 创建人ID
     */
    private String createdBy;

    /**
     * 更新人ID
     */
    private String updatedBy;
}
