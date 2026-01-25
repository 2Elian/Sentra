package com.sentra.knowledge.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentra.common.enums.DocumentStatus;
import com.sentra.common.exception.BusinessException;
import com.sentra.common.ocr.OcrClient;
import com.sentra.common.ocr.OcrResponse;
import com.sentra.knowledge.client.PythonKnowledgeClient;
import com.sentra.knowledge.config.StorageProperties;
import com.sentra.knowledge.config.RabbitMQConfig;
import com.sentra.knowledge.document.DocumentContent;
import com.sentra.knowledge.entity.Document;
import com.sentra.knowledge.messaging.KbBuildTaskMessage;
import com.sentra.knowledge.messaging.OcrTaskMessage;
import com.sentra.knowledge.service.IDocumentContentService;
import com.sentra.knowledge.service.IDocumentService;
import com.sentra.knowledge.util.SftpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * OCR任务消费者
 * 负责处理文档OCR解析任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OcrConsumer {

    private final OcrClient ocrClient;
    private final IDocumentService documentService;
    private final RabbitTemplate rabbitTemplate;
    private final SftpUtil sftpUtil;
    private final StorageProperties storageProperties;
    private final PythonKnowledgeClient pythonKnowledgeClient;
    private final IDocumentContentService documentContentService;
    private final ObjectMapper objectMapper;

    /**
     * 监听OCR队列，处理文档解析任务
     */
    @RabbitListener(queues = RabbitMQConfig.OCR_QUEUE)
    public void handleOcrTask(OcrTaskMessage message) {
        log.info("收到OCR任务，documentId: {}, kbId: {}, file: {}",
                message.getDocumentId(), message.getKbId(), message.getFilename());

        try {
            // 查询文档记录
            Document document = documentService.getById(message.getDocumentId());
            if (document == null) {
                log.error("文档不存在，documentId: {}", message.getDocumentId());
                return;
            }

            // 更新文档状态为OCR处理中
            document.setStatus(DocumentStatus.PARSING);
            document.setProgress(10);
            documentService.updateById(document);

            // 从SFTP下载文件到本地临时目录
            File tempPdfFile = null;
            try {
                tempPdfFile = File.createTempFile("ocr_", ".pdf");
                log.info("从SFTP下载文件: {} -> {}", message.getRemoteFilePath(), tempPdfFile.getAbsolutePath());

                boolean downloaded = sftpUtil.downloadFile(message.getRemoteFilePath(), tempPdfFile.getAbsolutePath());
                if (!downloaded) {
                    throw new BusinessException("SFTP文件下载失败");
                }

                document.setProgress(30);
                documentService.updateById(document);

                // 调用OCR API解析PDF
                log.info("开始OCR解析，文件: {}", tempPdfFile.getName());
                String ocrOutputDir = message.getOcrOutputDir() != null
                    ? message.getOcrOutputDir()
                    : storageProperties.getSftp().getOcrOutputPath();

                OcrResponse ocrResponse = ocrClient.parsePdf(tempPdfFile, ocrOutputDir);

                document.setProgress(70);
                documentService.updateById(document);

                if (!ocrResponse.isSuccess()) {
                    throw new BusinessException("OCR解析失败: " + ocrResponse.getErrorMessage());
                }

                // 保存OCR结果到MongoDB
                String mdContent = ocrResponse.getMdContent();

                // 创建MongoDB文档内容对象
                DocumentContent content = documentContentService.createFromDocument(document);
                content.setMdContent(mdContent);
                content.setParsedAt(LocalDateTime.now());
                content.setProgress(70);

                // 保存到MongoDB（集合名=kbId）
                boolean saved = documentContentService.save(content, document.getKbId());
                if (!saved) {
                    log.warn("MongoDB保存失败，但继续处理，documentId: {}", document.getId());
                }

                document.setProgress(80);
                documentService.updateById(document);

                // 调用Python服务进行章节重构
                log.info("调用Python mdParse接口进行章节重构，documentId: {}", document.getId());
                String newMdContent = pythonKnowledgeClient.parseMarkdown(
                        document.getId(),
                        document.getKbId(),
                        mdContent
                );

                // 更新MongoDB中的内容（添加newMdContent）
                content.setNewMdContent(newMdContent);
                content.setProgress(90);
                documentContentService.update(content, document.getKbId());

                document.setProgress(90);
                documentService.updateById(document);

                // 更新文档状态为OCR完成
                document.setStatus(DocumentStatus.READY);
                document.setProgress(100);
                document.setOcrResultPath(ocrOutputDir + "/" + document.getId() + ".json");
                document.setUpdatedAt(LocalDateTime.now());
                documentService.updateById(document);

                log.info("文档处理完成，documentId: {}, 原始md长度: {}, 重构后md长度: {}",
                        document.getId(), mdContent.length(), newMdContent.length());

                // 发送知识库构建任务到下一个队列
                sendKbBuildTask(document, newMdContent, message);

            } finally {
                // 清理临时文件
                if (tempPdfFile != null && tempPdfFile.exists()) {
                    boolean deleted = tempPdfFile.delete();
                    log.debug("临时文件删除: {}", deleted);
                }
            }

        } catch (Exception e) {
            log.error("OCR任务处理失败，documentId: {}", message.getDocumentId(), e);

            // 更新文档状态为失败
            try {
                Document document = documentService.getById(message.getDocumentId());
                if (document != null) {
                    document.setStatus(DocumentStatus.FAILED);
                    document.setErrorMessage("OCR解析失败: " + e.getMessage());
                    documentService.updateById(document);
                }
            } catch (Exception ex) {
                log.error("更新文档失败状态时出错", ex);
            }

            // 重试逻辑
            handleRetry(message, e);
        }
    }

    /**
     * 发送知识库构建任务
     */
    private void sendKbBuildTask(Document document, String newMdContent, OcrTaskMessage originalMessage) {
        KbBuildTaskMessage kbBuildMessage = new KbBuildTaskMessage();
        kbBuildMessage.setDocumentId(document.getId());
        kbBuildMessage.setKbId(document.getKbId());
        kbBuildMessage.setTenantId(document.getTenantId());
        kbBuildMessage.setNewMdContent(newMdContent);
        kbBuildMessage.setRetryCount(0);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.KB_BUILD_EXCHANGE,
                RabbitMQConfig.KB_BUILD_ROUTING_KEY,
                kbBuildMessage
        );

        log.info("发送知识库构建任务，documentId: {}", document.getId());
    }

    /**
     * 处理重试逻辑
     */
    private void handleRetry(OcrTaskMessage message, Exception e) {
        int currentRetry = message.getRetryCount() != null ? message.getRetryCount() : 0;
        int maxRetries = 3;

        if (currentRetry < maxRetries) {
            message.setRetryCount(currentRetry + 1);
            log.info("重新入队，重试次数: {}/{}", message.getRetryCount(), maxRetries);

            // 延迟重试（可通过延迟队列实现，这里简化为立即重试）
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.OCR_EXCHANGE,
                    RabbitMQConfig.OCR_ROUTING_KEY,
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
