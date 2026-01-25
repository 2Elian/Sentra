package com.sentra.knowledge.service;

import com.sentra.knowledge.document.DocumentContent;
import com.sentra.knowledge.entity.Document;

/**
 * MongoDB文档内容服务接口
 */
public interface IDocumentContentService {

    /**
     * 保存文档内容到MongoDB
     *
     * @param documentContent 文档内容
     * @param kbId            知识库ID（用于确定集合名称）
     * @return 是否保存成功
     */
    boolean save(DocumentContent documentContent, String kbId);

    /**
     * 根据documentId查询文档内容
     *
     * @param documentId 文档ID
     * @param kbId       知识库ID
     * @return 文档内容
     */
    DocumentContent getByDocumentId(String documentId, String kbId);

    /**
     * 更新文档内容
     *
     * @param documentContent 文档内容
     * @param kbId            知识库ID
     * @return 是否更新成功
     */
    boolean update(DocumentContent documentContent, String kbId);

    /**
     * 删除文档内容
     *
     * @param documentId 文档ID
     * @param kbId       知识库ID
     * @return 是否删除成功
     */
    boolean deleteByDocumentId(String documentId, String kbId);

    /**
     * 根据PostgreSQL的Document实体创建MongoDB文档
     *
     * @param document PostgreSQL中的文档实体
     * @return MongoDB文档内容
     */
    DocumentContent createFromDocument(Document document);
}
