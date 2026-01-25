package com.sentra.knowledge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sentra.knowledge.entity.Document;
import org.springframework.web.multipart.MultipartFile;

public interface IDocumentService extends IService<Document> {
    /**
     * 上传文档并构造知识库
     *
     * @param kbId 知识库ID
     * @param file 文件
     * @return 文档信息
     */
    Document upload(String kbId, MultipartFile file);
}
