package com.sentra.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.sentra.common.entity.BaseEntity;
import com.sentra.common.enums.DocumentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文档实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_document")
@Entity
@Table(name = "t_document")
public class Document extends BaseEntity {

    /**
     * 知识库ID
     */
    @Column(nullable = false)
    private String kbId;

    /**
     * 原始文件名
     */
    @Column(nullable = false)
    private String filename;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件类型（扩展名）
     */
    private String fileType;

    /**
     * 远程文件路径（SFTP）
     */
    private String remoteFilePath;

    /**
     * OCR结果文件路径
     */
    private String ocrResultPath;

    /**
     * 文档状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;

    /**
     * 处理进度（0-100）
     */
    private Integer progress;

    /**
     * 错误信息（如果处理失败）
     */
    private String errorMessage;

    /**
     * Python返回的文档唯一标识
     * 用于定位图谱文件：{graphPath}/{kbId}/{documentUniqueId}.graphml
     */
    private String documentUniqueId;
}
