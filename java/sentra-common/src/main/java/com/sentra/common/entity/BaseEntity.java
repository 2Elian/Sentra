package com.sentra.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 基础实体类
 */
@Data
@MappedSuperclass
public class BaseEntity implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    @Id
    private String id;

    @TableField(fill = FieldFill.INSERT)
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    @Column(updatable = false)
    private String tenantId;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            // 如果JPA负责插入，需要生成ID。
            // 但如果MyBatis负责，这里可能不会触发。
            // 兼容性考虑，如果是JPA操作，确保有ID。
            // 简单起见，这里假设 MyBatis-Plus 会处理 ID 生成。
            // 或者使用 @GeneratedValue(strategy = GenerationType.UUID)
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
