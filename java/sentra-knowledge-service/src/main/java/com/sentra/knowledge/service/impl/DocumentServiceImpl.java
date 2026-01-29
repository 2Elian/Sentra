package com.sentra.knowledge.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sentra.common.enums.DocumentStatus;
import com.sentra.common.exception.BusinessException;
import com.sentra.knowledge.client.PythonKnowledgeClient;
import com.sentra.knowledge.config.RabbitMQConfig;
import com.sentra.knowledge.config.StorageProperties;
import com.sentra.knowledge.document.DocumentContent;
import com.sentra.knowledge.entity.Document;
import com.sentra.knowledge.entity.KnowledgeBase;
import com.sentra.knowledge.mapper.DocumentMapper;
import com.sentra.knowledge.messaging.OcrTaskMessage;
import com.sentra.knowledge.service.IDocumentContentService;
import com.sentra.knowledge.service.IDocumentService;
import com.sentra.knowledge.service.IKnowledgeBaseService;
import com.sentra.knowledge.util.SftpUtil;
import com.sentra.knowledge.util.KbIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 文档服务实现类
 */
@Slf4j
@Service
public class DocumentServiceImpl extends ServiceImpl<DocumentMapper, Document> implements IDocumentService {

    private final SftpUtil sftpUtil;
    @Lazy
    private final IKnowledgeBaseService kbService;
    private final StorageProperties storageProperties;
    private final RabbitTemplate rabbitTemplate;
    private final PythonKnowledgeClient pythonKnowledgeClient;
    private final IDocumentContentService documentContentService;
    private final Neo4jClient neo4jClient;

    public DocumentServiceImpl(SftpUtil sftpUtil,
                               @Lazy IKnowledgeBaseService kbService,
                               StorageProperties storageProperties,
                               RabbitTemplate rabbitTemplate,
                               PythonKnowledgeClient pythonKnowledgeClient,
                               IDocumentContentService documentContentService,
                               Neo4jClient neo4jClient) {
        this.sftpUtil = sftpUtil;
        this.kbService = kbService;
        this.storageProperties = storageProperties;
        this.rabbitTemplate = rabbitTemplate;
        this.pythonKnowledgeClient = pythonKnowledgeClient;
        this.documentContentService = documentContentService;
        this.neo4jClient = neo4jClient;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Document upload(String kbId, String entityTemplateId, MultipartFile file) {
        // 校验知识库是否存在
        KnowledgeBase kb = kbService.lambdaQuery()
                .eq(KnowledgeBase::getKbId, kbId)
                .one();
        if (kb == null) {
            throw new BusinessException("知识库不存在");
        }

        // 校验实体类型模板（如果提供了）
        if (entityTemplateId != null && !entityTemplateId.isEmpty()) {
            // TODO: 校验模板是否存在以及是否属于当前租户 --> 首先需要更改实体表和实体描述表 增加租户ID字段 才能进行校验 这里先log一下
            log.info("文档指定使用实体类型模板: {}", entityTemplateId);
        } else {
            log.info("文档未指定实体类型模板，将使用系统默认模板");
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
        // 创建documentid
        String documentId = KbIdGenerator.generate(kb.getTenantId(), kb.getOwnerUserId(), originalFilename);

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
        document.setDocumentUniqueId(documentId);
        document.setEntityTemplateId(entityTemplateId); // 保存实体类型模板ID

        // 保存到数据库
        boolean saved = this.save(document);
        if (!saved) {
            throw new BusinessException("文档记录保存失败");
        }

        log.info("文档上传成功，documentId: {}, kbId: {}, filename: {}, entityTemplateId: {}",
                document.getId(), kbId, originalFilename, entityTemplateId);

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDocument(String documentId) {
        // 查询文档信息
        Document document = this.getById(documentId);
        if (document == null) {
            throw new BusinessException("文档不存在");
        }

        log.info("开始删除文档，documentId: {}, kbId: {}, filename: {}",
                documentId, document.getKbId(), document.getFilename());

        try {
            // 删除SFTP上的原始文件
            if (document.getRemoteFilePath() != null && !document.getRemoteFilePath().isEmpty()) {
                boolean sftpDeleted = sftpUtil.deleteFile(document.getRemoteFilePath());
                if (sftpDeleted) {
                    log.info("SFTP文件删除成功: {}", document.getRemoteFilePath());
                } else {
                    log.warn("SFTP文件删除失败: {}", document.getRemoteFilePath());
                }
            }

//            // TODO 删除OCR结果文件 因为OCR结果文件在服务器上的名称是无序的 所以这个可能无法删除
//            if (document.getOcrResultPath() != null && !document.getOcrResultPath().isEmpty()) {
//                boolean ocrResultDeleted = sftpUtil.deleteFile(document.getOcrResultPath());
//                if (ocrResultDeleted) {
//                    log.info("OCR结果文件删除成功: {}", document.getOcrResultPath());
//                } else {
//                    log.warn("OCR结果文件删除失败: {}", document.getOcrResultPath());
//                }
//            }

//            // 调用Python删除接口，删除知识库中的向量库对应的embedding
//            // TODO python接口还没写好嘿嘿
//            if (document.getDocumentUniqueId() != null && !document.getDocumentUniqueId().isEmpty()) {
//                boolean pythonDeleted = pythonKnowledgeClient.deleteDocumentFromKnowledgeBase(
//                        document.getKbId(),
//                        document.getDocumentUniqueId()
//                );
//                if (pythonDeleted) {
//                    log.info("Python知识库数据删除成功，kbId: {}, documentUniqueId: {}",
//                            document.getKbId(), document.getDocumentUniqueId());
//                } else {
//                    log.warn("Python知识库数据删除失败或部分失败，kbId: {}, documentUniqueId: {}",
//                            document.getKbId(), document.getDocumentUniqueId());
//                }
//            }

            // 删除本地图谱GraphML文件
            if (document.getDocumentUniqueId() != null && !document.getDocumentUniqueId().isEmpty()) {
                deleteLocalGraphDir(document.getKbId(), document.getDocumentUniqueId());
            }

            // 删除Neo4j中的文档节点
            if (document.getDocumentUniqueId() != null) {
                deleteNeo4jDocumentNodes(document.getKbId(), document.getDocumentUniqueId());
            }

            // 删除MongoDB中的文档内容
            try {
                documentContentService.deleteByDocumentId(documentId, document.getKbId());
                log.info("MongoDB文档内容删除成功，documentId: {}", documentId);
            } catch (Exception e) {
                log.error("MongoDB文档内容删除失败，documentId: {}", documentId, e);
            }

            // 删除PostgreSQL中的文档记录
            boolean dbDeleted = this.removeById(documentId);
            if (dbDeleted) {
                log.info("PostgreSQL文档记录删除成功，documentId: {}", documentId);
            }

            log.info("文档删除完成，documentId: {}", documentId);
            return dbDeleted;

        } catch (Exception e) {
            log.error("删除文档失败，documentId: {}", documentId, e);
            throw new BusinessException("删除文档失败: " + e.getMessage());
        }
    }

    /**
     * 获取文档产品路径
     *
     * @param documentId 文档ID
     * @return 产品路径列表
     */
    @Override
    public List<String> getProductPath(String documentId, String kbId) {

        String rootPath = "G:\\项目成果打包\\基于图结构的文档问答助手\\logs\\sentra\\graph";

        String dirName = String.format("graph_%s_%s", kbId, documentId);

        String baseDir = String.join("\\", rootPath, kbId, dirName);

        List<String> paths = new ArrayList<>();

        paths.add(baseDir + "\\" + dirName + ".graphml");
        paths.add(baseDir + "\\chunks.json");
        paths.add(baseDir + "\\aggregated.json");
        paths.add(baseDir + "\\multi_hop.json");
        paths.add(baseDir + "\\cot.json");

        return paths;
    }

    /**
     * 删除本地图谱GraphML文件
     * 已作废
     */
    private void deleteLocalGraphFile(String kbId, String documentUniqueId) {
        try {
            String graphPath = storageProperties.getGraphPath();
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
     * 删除本地图谱目录
     */
    private void deleteLocalGraphDir(String kbId, String documentUniqueId) {
        String graphPath = storageProperties.getGraphPath();

        Path graphDir = Paths.get(
                graphPath,
                kbId,
                "graph_" + kbId + "_" + documentUniqueId
        );

        if (!Files.exists(graphDir)) {
            log.debug("本地图谱目录不存在: {}", graphDir.toAbsolutePath());
            return;
        }

        try {
            Files.walkFileTree(graphDir, new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                        throws IOException {
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });

            log.info("本地图谱目录删除成功: {}", graphDir.toAbsolutePath());

        } catch (IOException e) {
            log.error("删除本地图谱目录失败, kbId: {}, documentUniqueId: {}",
                    kbId, documentUniqueId, e);
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
