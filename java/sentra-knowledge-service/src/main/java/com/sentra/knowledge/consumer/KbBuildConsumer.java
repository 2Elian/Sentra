package com.sentra.knowledge.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sentra.common.enums.DocumentStatus;
import com.sentra.common.exception.BusinessException;
import com.sentra.knowledge.client.PythonKnowledgeClient;
import com.sentra.knowledge.config.RabbitMQConfig;
import com.sentra.knowledge.document.DocumentContent;
import com.sentra.knowledge.entity.Document;
import com.sentra.knowledge.entity.EntityTypeDefinition;
import com.sentra.knowledge.entity.KnowledgeBase;
import com.sentra.knowledge.messaging.KbBuildTaskMessage;
import com.sentra.knowledge.service.IDocumentContentService;
import com.sentra.knowledge.service.IDocumentService;
import com.sentra.knowledge.service.IEntityTypeDefinitionService;
import com.sentra.knowledge.service.IEntityTypeTemplateService;
import com.sentra.knowledge.service.IKnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库构建消费者
 * 负责处理文档知识库构建任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KbBuildConsumer {

    private final PythonKnowledgeClient pythonKnowledgeClient;
    private final IDocumentService documentService;
    private final IDocumentContentService documentContentService;
    private final RabbitTemplate rabbitTemplate;
    private final IKnowledgeBaseService knowledgeBaseService;
    private final IEntityTypeDefinitionService entityTypeDefinitionService;
    private final IEntityTypeTemplateService entityTypeTemplateService;
    private final Neo4jClient neo4jClient;
    private final String graphPath = "G:\\项目成果打包\\基于图结构的文档问答助手\\logs\\sentra\\graph"; // TODO 从配置读取

    /**
     * 监听知识库构建队列，处理知识库构建任务
     */
    @RabbitListener(queues = RabbitMQConfig.KB_BUILD_QUEUE)
    public void handleKbBuildTask(KbBuildTaskMessage message) {
        log.info("收到知识库构建任务，documentId: {}, kbId: {}",
                message.getDocumentId(), message.getKbId());

        try {
            // 查询文档记录
            Document document = documentService.getById(message.getDocumentId());
            if (document == null) {
                log.error("文档不存在，documentId: {}", message.getDocumentId());
                return;
            }

            // 更新文档状态为知识库构建中
            document.setStatus(DocumentStatus.KB_BUILDING);
            document.setProgress(95);
            documentService.updateById(document);

            // 获取实体类型配置
            List<String> entityTypesList = new ArrayList<>();
            Map<String, String> entityTypesDes = new HashMap<>();

            // 优先使用文档指定的实体类型模板
            String entityTemplateId = document.getEntityTemplateId();

            if (entityTemplateId != null && !entityTemplateId.isEmpty()) {
                // 使用文档指定的实体类型模板
                List<EntityTypeDefinition> definitions = entityTypeDefinitionService.list(
                        new LambdaQueryWrapper<EntityTypeDefinition>()
                                .eq(EntityTypeDefinition::getTemplateId, entityTemplateId)
                );

                if (definitions != null && !definitions.isEmpty()) {
                    for (EntityTypeDefinition def : definitions) {
                        entityTypesList.add(def.getEntityCode());
                        entityTypesDes.put(def.getEntityCode(), def.getEntityDescription());
                    }
                    log.info("加载文档指定的实体类型配置，templateId: {}, 数量: {}",
                            entityTemplateId, entityTypesList.size());
                }
            } else {
                // 文档未指定实体类型模板，使用系统默认的"合同领域"模板
                log.info("文档未指定实体类型模板，使用系统默认的合同领域模板");

                LambdaQueryWrapper<com.sentra.knowledge.entity.EntityTypeTemplate> templateWrapper =
                    new LambdaQueryWrapper<>();
                templateWrapper.eq(com.sentra.knowledge.entity.EntityTypeTemplate::getName, "合同领域")
                        .eq(com.sentra.knowledge.entity.EntityTypeTemplate::getIsSystem, true);

                com.sentra.knowledge.entity.EntityTypeTemplate defaultTemplate =
                    entityTypeTemplateService.getOne(templateWrapper);

                if (defaultTemplate != null) {
                    List<EntityTypeDefinition> definitions = entityTypeDefinitionService.list(
                            new LambdaQueryWrapper<EntityTypeDefinition>()
                                    .eq(EntityTypeDefinition::getTemplateId, defaultTemplate.getId())
                    );

                    if (definitions != null && !definitions.isEmpty()) {
                        for (EntityTypeDefinition def : definitions) {
                            entityTypesList.add(def.getEntityCode());
                            entityTypesDes.put(def.getEntityCode(), def.getEntityDescription());
                        }
                        log.info("加载系统默认的合同领域实体类型配置，templateId: {}, 数量: {}",
                                defaultTemplate.getId(), entityTypesList.size());
                    }
                } else {
                    log.warn("未找到系统默认的合同领域实体类型模板，使用空配置");
                }
            }

            // 从MongoDB查询文档内容
            DocumentContent content = documentContentService.getByDocumentId(
                    message.getDocumentId(),
                    message.getKbId()
            );

            if (content == null) {
                throw new BusinessException("MongoDB中未找到文档内容");
            }

            // 调用Python服务构建知识库 (KbPipeline)
            log.info("调用Python KbPipeline接口构建知识库，documentId: {}, entityTypes数量: {}",
                    document.getId(), entityTypesList.size());
            var kbPipelineResponse = pythonKnowledgeClient.buildKnowledgeBase(
                    message.getDocumentId(),          // docID
                    document.getKbId(),                // kbID
                    message.getNewMdContent(),         // content
                    document.getFilename(),            // title
                    entityTypesList,                   // entityTypes (List[str])
                    entityTypesDes                     // entityTypesDes (Dict[str, str])
            );

            // 使用docID作为documentUniqueId（后续可能需要从Python响应中获取）
            String documentUniqueId = message.getDocumentId();

            // 更新MongoDB中的内容（添加documentUniqueId和kbBuiltAt）
            content.setDocumentUniqueId(documentUniqueId);
            content.setKbBuiltAt(LocalDateTime.now());
            content.setProgress(100);
            content.setStatus(DocumentStatus.COMPLETED.name());
            documentContentService.update(content, document.getKbId());

            // 更新PostgreSQL中的文档状态
            document.setDocumentUniqueId(documentUniqueId);
            document.setProgress(100);
            document.setStatus(DocumentStatus.COMPLETED);
            document.setUpdatedAt(LocalDateTime.now());
            documentService.updateById(document);

            log.info("知识库构建成功，documentId: {}, documentUniqueId: {}, totalChunks: {}, totalEntities: {}, totalEdges: {}",
                    document.getId(), documentUniqueId,
                    kbPipelineResponse.getTotalChunks(),
                    kbPipelineResponse.getTotalEntities(),
                    kbPipelineResponse.getTotalEdges());

        } catch (Exception e) {
            log.error("知识库构建失败，documentId: {}", message.getDocumentId(), e);

            // 更新文档状态为失败并清理已创建的图谱数据
            try {
                Document document = documentService.getById(message.getDocumentId());
                if (document != null) {
                    document.setStatus(DocumentStatus.FAILED);
                    document.setErrorMessage("知识库构建失败: " + e.getMessage());
                    documentService.updateById(document);

                    // 同时更新MongoDB
                    DocumentContent content = documentContentService.getByDocumentId(
                            message.getDocumentId(),
                            message.getKbId()
                    );
                    if (content != null) {
                        content.setStatus(DocumentStatus.FAILED.name());
                        content.setErrorMessage("知识库构建失败: " + e.getMessage());
                        documentContentService.update(content, message.getKbId());
                    }

                    // 清理已创建的图谱数据（本地图谱文件、Neo4j节点）
                    cleanupFailedKbBuild(document);
                }
            } catch (Exception ex) {
                log.error("更新文档失败状态或清理图谱数据时出错", ex);
            }

            // 重试逻辑
            handleRetry(message, e);
        }
    }

    /**
     * 处理重试逻辑
     */
    private void handleRetry(KbBuildTaskMessage message, Exception e) {
        int currentRetry = message.getRetryCount() != null ? message.getRetryCount() : 0;
        int maxRetries = 3;

        if (currentRetry < maxRetries) {
            message.setRetryCount(currentRetry + 1);
            log.info("重新入队，重试次数: {}/{}", message.getRetryCount(), maxRetries);

            // 延迟重试（可通过延迟队列实现，这里简化为立即重试）
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.KB_BUILD_EXCHANGE,
                    RabbitMQConfig.KB_BUILD_ROUTING_KEY,
                    message
            );
        } else {
            log.error("超过最大重试次数，发送到死信队列，documentId: {}", message.getDocumentId());
            // 发送到死信队列
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.KB_BUILD_DEAD_LETTER_EXCHANGE,
                    RabbitMQConfig.KB_BUILD_ROUTING_KEY,
                    message
            );
        }
    }

    /**
     * 清理知识库构建失败时已创建的图谱数据
     * 包括：本地图谱GraphML文件、Neo4j节点
     */
    private void cleanupFailedKbBuild(Document document) {
        log.info("开始清理知识库构建失败的图谱数据，documentId: {}", document.getId());

        try {
            // 1. 删除本地图谱GraphML文件
            if (document.getDocumentUniqueId() != null && !document.getDocumentUniqueId().isEmpty()) {
                deleteLocalGraphFile(document.getKbId(), document.getDocumentUniqueId());
            }

            // 2. 删除Neo4j中的文档节点
            if (document.getDocumentUniqueId() != null && !document.getDocumentUniqueId().isEmpty()) {
                deleteNeo4jDocumentNodes(document.getKbId(), document.getDocumentUniqueId());
            }

            log.info("知识库构建失败的图谱数据清理完成，documentId: {}", document.getId());

        } catch (Exception e) {
            log.error("清理知识库构建失败的图谱数据时出错，documentId: {}", document.getId(), e);
        }
    }

    /**
     * 删除本地图谱GraphML文件
     */
    private void deleteLocalGraphFile(String kbId, String documentUniqueId) {
        try {
            File kbDir = new File(graphPath, kbId);
            File graphFile = new File(kbDir, documentUniqueId + ".graphml");

            if (graphFile.exists()) {
                boolean deleted = graphFile.delete();
                if (deleted) {
                    log.info("本地图谱文件删除成功: {}", graphFile.getAbsolutePath());
                } else {
                    log.warn("本地图谱文件删除失败: {}", graphFile.getAbsolutePath());
                }
            } else {
                log.debug("本地图谱文件不存在: {}", graphFile.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("删除本地图谱文件失败，kbId: {}, documentUniqueId: {}", kbId, documentUniqueId, e);
        }
    }

    /**
     * 删除Neo4j中的文档节点
     */
    private void deleteNeo4jDocumentNodes(String kbId, String documentUniqueId) {
        try {
            // 删除该文档相关的所有节点和关系
            String cypher = """
                MATCH (n)
                WHERE n.kbId = $kbId AND n.documentUniqueId = $documentUniqueId
                DETACH DELETE n
                """;

            neo4jClient.query(cypher)
                    .bind(kbId).to("kbId")
                    .bind(documentUniqueId).to("documentUniqueId")
                    .run();

            log.info("Neo4j文档节点删除成功，kbId: {}, documentUniqueId: {}", kbId, documentUniqueId);
        } catch (Exception e) {
            log.error("删除Neo4j文档节点失败，kbId: {}, documentUniqueId: {}", kbId, documentUniqueId, e);
        }
    }
}
