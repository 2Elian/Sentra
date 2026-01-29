package com.sentra.knowledge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sentra.knowledge.entity.Document;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IDocumentService extends IService<Document> {
    /**
     * 上传文档并构造知识库
     *
     * @param kbId 知识库ID
     * @param entityTemplateId 实体类型模板ID（可选）
     * @param file 文件
     * @return 文档信息
     */
    Document upload(String kbId, String entityTemplateId, MultipartFile file);

    /**
     * 删除文档及其所有相关数据
     * 包括：SFTP文件、OCR结果、Python知识库数据、本地图谱、Neo4j节点、MongoDB内容、PostgreSQL记录
     *
     * @param documentId 文档ID
     * @return 是否删除成功
     */
    boolean deleteDocument(String documentId);

    /**
     * 获取文档产品路径
     *
     * @param documentId 文档ID
     * @return 产品路径列表
     */
    List<String> getProductPath(String documentId, String kbId);
}
