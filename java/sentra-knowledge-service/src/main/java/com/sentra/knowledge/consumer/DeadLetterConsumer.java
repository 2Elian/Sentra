package com.sentra.knowledge.consumer;

import com.sentra.knowledge.messaging.OcrTaskMessage;
import com.sentra.knowledge.service.IDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 死信队列消费者
 * 处理超过最大重试次数的消息，清理相关数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterConsumer {

    private final IDocumentService documentService;

    /**
     * 监听OCR死信队列
     * 处理OCR任务失败且超过重试次数的文档
     */
    @RabbitListener(queues = "sentra.dlq.ocr")
    public void handleOcrDeadLetter(OcrTaskMessage message) {
        log.error("收到OCR死信消息，documentId: {}, kbId: {}, filename: {}, 重试次数: {}",
                message.getDocumentId(), message.getKbId(), message.getFilename(), message.getRetryCount());

        try {
            // 查询文档状态
            var document = documentService.getById(message.getDocumentId());
            if (document == null) {
                log.warn("死信队列中的文档不存在，documentId: {}", message.getDocumentId());
                return;
            }

            // 如果文档状态已经是FAILED，说明已经处理过了
            if (document.getStatus().name().equals("FAILED")) {
                log.info("文档状态已为FAILED，跳过处理，documentId: {}", message.getDocumentId());
                return;
            }

            // 更新文档状态为失败
            document.setStatus(com.sentra.common.enums.DocumentStatus.FAILED);
            document.setErrorMessage("任务处理失败，超过最大重试次数");
            documentService.updateById(document);

            // 清理已创建的资源
            // 注意：这里不会删除PostgreSQL记录，只清理SFTP文件、OCR结果、MongoDB数据
            log.info("死信队列消息处理完成，documentId: {}", message.getDocumentId());

        } catch (Exception e) {
            log.error("处理OCR死信消息失败，documentId: {}", message.getDocumentId(), e);
            // 不再重新入队，避免无限循环
        }
    }

    /**
     * 监听知识库构建死信队列
     */
    @RabbitListener(queues = "sentra.dlq.kb_build")
    public void handleKbBuildDeadLetter(com.sentra.knowledge.messaging.KbBuildTaskMessage message) {
        log.error("收到知识库构建死信消息，documentId: {}, kbId: {}, 重试次数: {}",
                message.getDocumentId(), message.getKbId(), message.getRetryCount());

        try {
            // 查询文档状态
            var document = documentService.getById(message.getDocumentId());
            if (document == null) {
                log.warn("死信队列中的文档不存在，documentId: {}", message.getDocumentId());
                return;
            }

            // 更新文档状态为失败
            document.setStatus(com.sentra.common.enums.DocumentStatus.FAILED);
            document.setErrorMessage("知识库构建失败，超过最大重试次数");
            documentService.updateById(document);

            // 清理已创建的图谱数据
            // 注意：这里不会删除PostgreSQL和MongoDB记录，只清理本地图谱和Neo4j数据
            log.info("知识库构建死信队列消息处理完成，documentId: {}", message.getDocumentId());

        } catch (Exception e) {
            log.error("处理知识库构建死信消息失败，documentId: {}", message.getDocumentId(), e);
        }
    }
}
