package com.sentra.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.sentra.common.entity.BaseEntity;
import com.sentra.common.enums.DocumentStatus;
import com.sentra.knowledge.enums.DocumentParseStrategy;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_document")
@Entity
@Table(name = "t_document")
public class Document extends BaseEntity {

    private String kbId;

    private String filename;

    @Enumerated(EnumType.STRING)
    private DocumentStatus status;

    @Enumerated(EnumType.STRING)
    private DocumentParseStrategy parseStrategy;
}
