package com.sentra.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.sentra.common.entity.BaseEntity;
import com.sentra.common.enums.KnowledgeScope;
import com.sentra.knowledge.enums.KnowledgeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_knowledge_base")
@Entity
@Table(name = "t_knowledge_base")
public class KnowledgeBase extends BaseEntity {

    private String ownerUserId;

    private String name;

    @Enumerated(EnumType.STRING)
    private KnowledgeScope scope;

    @Enumerated(EnumType.STRING)
    private KnowledgeType type;
}
