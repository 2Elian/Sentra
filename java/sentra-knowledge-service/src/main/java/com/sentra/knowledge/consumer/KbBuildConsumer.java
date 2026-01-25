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
import com.sentra.knowledge.service.IKnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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

    /**
     * 监听知识库构建队列，处理知识库构建任务
     */
    @RabbitListener(queues = RabbitMQConfig.KB_BUILD_QUEUE)
    public void handleKbBuildTask(KbBuildTaskMessage message) {
        log.info("收到知识库构建任务，documentId: {}, kbId: {}",
                message.getDocumentId(), message.getKbId());

        try {
            // 1. 查询文档记录
            Document document = documentService.getById(message.getDocumentId());
            if (document == null) {
                log.error("文档不存在，documentId: {}", message.getDocumentId());
                return;
            }

            // 2. 更新文档状态为知识库构建中
            document.setStatus(DocumentStatus.KB_BUILDING);
            document.setProgress(95);
            documentService.updateById(document);

            // 3. 查询知识库，获取实体类型模板ID
            LambdaQueryWrapper<KnowledgeBase> kbWrapper = new LambdaQueryWrapper<>();
            kbWrapper.eq(KnowledgeBase::getKbId, document.getKbId());
            KnowledgeBase knowledgeBase = knowledgeBaseService.getOne(kbWrapper);
            if (knowledgeBase == null) {
                throw new BusinessException("知识库不存在");
            }

            // 4. 获取实体类型配置
            Map<String, String> entityTypes = null;
            if (knowledgeBase.getEntityTemplateId() != null) {
                List<EntityTypeDefinition> definitions = entityTypeDefinitionService.list(
                        new LambdaQueryWrapper<EntityTypeDefinition>()
                                .eq(EntityTypeDefinition::getTemplateId, knowledgeBase.getEntityTemplateId())
                );

                if (definitions != null && !definitions.isEmpty()) {
                    entityTypes = new HashMap<>();
                    for (EntityTypeDefinition def : definitions) {
                        entityTypes.put(def.getEntityCode(), def.getEntityDescription());
                    }
                    log.info("加载实体类型配置，templateId: {}, 数量: {}",
                            knowledgeBase.getEntityTemplateId(), entityTypes.size());
                }
            }

            // 5. 从MongoDB查询文档内容（验证内容存在）
            DocumentContent content = documentContentService.getByDocumentId(
                    message.getDocumentId(),
                    message.getKbId()
            );

            if (content == null) {
                throw new BusinessException("MongoDB中未找到文档内容");
            }

            // 6. 调用Python服务构建知识图谱
            log.info("调用Python D2KG接口构建知识图谱，documentId: {}", document.getId());
            String documentUniqueId = pythonKnowledgeClient.buildKnowledgeGraph(
                    document.getKbId(),
                    message.getNewMdContent(),
                    entityTypes
            );

            // 7. 更新MongoDB中的内容（添加documentUniqueId和kbBuiltAt）
            content.setDocumentUniqueId(documentUniqueId);
            content.setKbBuiltAt(LocalDateTime.now());
            content.setProgress(100);
            content.setStatus(DocumentStatus.COMPLETED.name());
            documentContentService.update(content, document.getKbId());

            // 8. 更新PostgreSQL中的文档状态
            document.setDocumentUniqueId(documentUniqueId);
            document.setProgress(100);
            document.setStatus(DocumentStatus.COMPLETED);
            document.setUpdatedAt(LocalDateTime.now());
            documentService.updateById(document);

            log.info("知识库构建成功，documentId: {}, documentUniqueId: {}",
                    document.getId(), documentUniqueId);

        } catch (Exception e) {
            log.error("知识库构建失败，documentId: {}", message.getDocumentId(), e);

            // 更新文档状态为失败
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
                }
            } catch (Exception ex) {
                log.error("更新文档失败状态时出错", ex);
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
                    RabbitMQConfig.DEAD_LETTER_EXCHANGE,
                    RabbitMQConfig.DEAD_LETTER_ROUTING_KEY,
                    message
            );
        }
    }
}
