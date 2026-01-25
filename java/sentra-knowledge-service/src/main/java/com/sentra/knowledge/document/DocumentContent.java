package com.sentra.knowledge.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * MongoDB文档内容实体
 * 集合名称：{kbId}
 * 存储文档的OCR解析结果和章节重构内容
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "#{@kbId}")  // 动态集合名，运行时根据kbId确定
public class DocumentContent {

    /**
     * 文档ID（与PostgreSQL中的document.id一致）
     */
    @Id
    private String documentId;

    /**
     * 知识库ID
     */
    private String kbId;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 原始文件名
     */
    private String filename;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * OCR解析后的原始Markdown内容
     */
    private String mdContent;

    /**
     * Python mdParse重构后的Markdown内容
     */
    private String newMdContent;

    /**
     * OCR结果文件路径
     */
    private String ocrResultPath;

    /**
     * Python返回的文档唯一标识（用于定位GraphML文件）
     */
    private String documentUniqueId;

    /**
     * 图谱文件本地路径
     */
    private String graphPath;

    /**
     * 文档状态
     */
    private String status;

    /**
     * 处理进度（0-100）
     */
    private Integer progress;

    /**
     * OCR解析时间
     */
    private LocalDateTime parsedAt;

    /**
     * 章节重构时间
     */
    private String restructuredAt;

    /**
     * 知识库构建时间
     */
    private LocalDateTime kbBuiltAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 错误信息（如果处理失败）
     */
    private String errorMessage;

    /**
     * 额外元数据（JSON格式，存储OCR的原始响应等）
     */
    private String metadata;
}
