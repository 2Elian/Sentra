package com.sentra.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 实体类型定义实体
 * 存储每个模板下的具体实体类型
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_entity_type_definition")
public class EntityTypeDefinition {

    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 所属模板ID
     */
    private String templateId;

    /**
     * 实体编码（如：ContractParty）
     */
    private String entityCode;

    /**
     * 实体名称（如：合同主体）
     */
    private String entityName;

    /**
     * 实体描述（如：合同主体（甲乙方））
     */
    private String entityDescription;

    /**
     * 显示顺序
     */
    private Integer displayOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
