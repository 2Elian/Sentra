package com.sentra.knowledge.service.impl;

import com.sentra.knowledge.document.DocumentContent;
import com.sentra.knowledge.entity.Document;
import com.sentra.knowledge.service.IDocumentContentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * MongoDB文档内容服务实现类
 */
@Slf4j
@Service
public class DocumentContentServiceImpl implements IDocumentContentService {

    private final MongoTemplate mongoTemplate;

    public DocumentContentServiceImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public boolean save(DocumentContent documentContent, String kbId) {
        try {
            // 使用kbId作为集合名称
            String collectionName = kbId;
            documentContent.setUpdatedAt(LocalDateTime.now());

            if (documentContent.getCreatedAt() == null) {
                documentContent.setCreatedAt(LocalDateTime.now());
            }

            mongoTemplate.save(documentContent, collectionName);
            log.info("文档内容保存到MongoDB成功，documentId: {}, collection: {}",
                    documentContent.getDocumentId(), collectionName);
            return true;

        } catch (Exception e) {
            log.error("保存文档内容到MongoDB失败，documentId: {}", documentContent.getDocumentId(), e);
            return false;
        }
    }

    @Override
    public DocumentContent getByDocumentId(String documentId, String kbId) {
        try {
            String collectionName = kbId;
            Query query = new Query(Criteria.where("documentId").is(documentId));

            DocumentContent content = mongoTemplate.findOne(query, DocumentContent.class, collectionName);
            log.debug("从MongoDB查询文档内容，documentId: {}, found: {}", documentId, content != null);

            return content;

        } catch (Exception e) {
            log.error("从MongoDB查询文档内容失败，documentId: {}", documentId, e);
            return null;
        }
    }

    @Override
    public boolean update(DocumentContent documentContent, String kbId) {
        try {
            String collectionName = kbId;
            documentContent.setUpdatedAt(LocalDateTime.now());

            // 先删除旧记录
            mongoTemplate.remove(
                    new Query(Criteria.where("documentId").is(documentContent.getDocumentId())),
                    DocumentContent.class,
                    collectionName
            );

            // 保存新记录
            mongoTemplate.save(documentContent, collectionName);

            log.info("MongoDB文档内容更新成功，documentId: {}, collection: {}",
                    documentContent.getDocumentId(), collectionName);
            return true;

        } catch (Exception e) {
            log.error("更新MongoDB文档内容失败，documentId: {}", documentContent.getDocumentId(), e);
            return false;
        }
    }

    @Override
    public boolean deleteByDocumentId(String documentId, String kbId) {
        try {
            String collectionName = kbId;
            Query query = new Query(Criteria.where("documentId").is(documentId));

            mongoTemplate.remove(query, DocumentContent.class, collectionName);

            log.info("MongoDB文档内容删除成功，documentId: {}, collection: {}", documentId, collectionName);
            return true;

        } catch (Exception e) {
            log.error("删除MongoDB文档内容失败，documentId: {}", documentId, e);
            return false;
        }
    }

    @Override
    public DocumentContent createFromDocument(Document document) {
        DocumentContent content = new DocumentContent();
        BeanUtils.copyProperties(document, content);

        // 设置特殊字段
        content.setDocumentId(document.getId());
        content.setFileSize(document.getFileSize());
        content.setFileType(document.getFileType());
        content.setOcrResultPath(document.getOcrResultPath());
        content.setDocumentUniqueId(document.getDocumentUniqueId());
        content.setProgress(document.getProgress());
        content.setStatus(document.getStatus().name());
        content.setCreatedAt(document.getCreatedAt());
        content.setUpdatedAt(document.getUpdatedAt());

        return content;
    }
}
