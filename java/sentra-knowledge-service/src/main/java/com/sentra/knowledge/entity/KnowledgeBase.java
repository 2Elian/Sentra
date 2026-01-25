package com.sentra.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.sentra.common.entity.BaseEntity;
import com.sentra.common.enums.KnowledgeScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_knowledge_base")
@Entity
@Table(name = "t_knowledge_base")
public class KnowledgeBase extends BaseEntity {

    /**
     * 所有者用户ID
     */
    @Column(nullable = false)
    private String ownerUserId;

    /**
     * 知识库名称（同一租户下唯一）
     */
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * 知识库范围
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private KnowledgeScope scope;

    /**
     * 知识库唯一标识
     * 由 tenant_id + owner_user_id + name 的哈希值生成
     */
    @Column(nullable = false, unique = true, updatable = false)
    private String kbId;

    /**
     * 知识库描述
     */
    private String description;

    /**
     * 关联的实体类型模板ID
     */
    private String entityTemplateId;
}
