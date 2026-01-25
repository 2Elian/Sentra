package com.sentra.knowledge.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sentra.common.enums.DocumentStatus;
import com.sentra.common.exception.BusinessException;
import com.sentra.knowledge.config.RabbitMQConfig;
import com.sentra.knowledge.config.StorageProperties;
import com.sentra.knowledge.entity.Document;
import com.sentra.knowledge.entity.KnowledgeBase;
import com.sentra.knowledge.mapper.DocumentMapper;
import com.sentra.knowledge.messaging.OcrTaskMessage;
import com.sentra.knowledge.service.IDocumentService;
import com.sentra.knowledge.service.IKnowledgeBaseService;
import com.sentra.knowledge.util.SftpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文档服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl extends ServiceImpl<DocumentMapper, Document> implements IDocumentService {

    private final SftpUtil sftpUtil;
    private final IKnowledgeBaseService kbService;
    private final StorageProperties storageProperties;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Document upload(String kbId, MultipartFile file) {
        // 校验知识库是否存在
        KnowledgeBase kb = kbService.lambdaQuery()
                .eq(KnowledgeBase::getKbId, kbId)
                .one();
        if (kb == null) {
            throw new BusinessException("知识库不存在");
        }

        // 校验文件格式（v0.0.1只支持PDF）
        // TODO 这里需要有一个文件类型的判断 pdf走ocr一套的pipeline txt、excel之类的可能要单独处理
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new BusinessException("仅支持PDF格式的文件");
        }

        // 生成远程文件路径
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String remoteFileName = kbId + "_" + timestamp + "_" + UUID.randomUUID().toString().substring(0, 8) + fileExtension;
        String remoteFilePath = storageProperties.getSftp().getUploadPath() + "/" + remoteFileName;

        // 上传文件到SFTP服务器
        try (InputStream inputStream = file.getInputStream()) {
            boolean uploaded = sftpUtil.uploadFile(inputStream, remoteFilePath);
            if (!uploaded) {
                throw new BusinessException("文件上传失败");
            }
        } catch (Exception e) {
            log.error("文件上传失败: {}", remoteFilePath, e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }

        // 创建文档记录
        Document document = new Document();
        document.setKbId(kbId);
        document.setFilename(originalFilename);
        document.setFileSize(file.getSize());
        document.setFileType(fileExtension);
        document.setRemoteFilePath(remoteFilePath);
        document.setStatus(DocumentStatus.UPLOADED);
        document.setProgress(0);
        document.setTenantId(kb.getTenantId());
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());

        // 保存到数据库
        boolean saved = this.save(document);
        if (!saved) {
            throw new BusinessException("文档记录保存失败");
        }

        log.info("文档上传成功，documentId: {}, kbId: {}, filename: {}",
                document.getId(), kbId, originalFilename);

        // 发送OCR解析任务到RabbitMQ
        sendOcrTask(document, kb);

        return document;
    }

    /**
     * 发送OCR任务到消息队列
     */
    private void sendOcrTask(Document document, KnowledgeBase kb) {
        OcrTaskMessage message = new OcrTaskMessage();
        message.setDocumentId(document.getId());
        message.setKbId(document.getKbId());
        message.setTenantId(document.getTenantId());
        message.setRemoteFilePath(document.getRemoteFilePath());
        message.setFilename(document.getFilename());
        message.setOcrOutputDir(storageProperties.getSftp().getOcrOutputPath());
        message.setRetryCount(0);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.OCR_EXCHANGE,
                RabbitMQConfig.OCR_ROUTING_KEY,
                message
        );

        log.info("OCR任务已发送到队列，documentId: {}", document.getId());
    }
}
