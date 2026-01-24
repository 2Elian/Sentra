package com.sentra.knowledge.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sentra.common.enums.DocumentStatus;
import com.sentra.knowledge.entity.Document;
import com.sentra.knowledge.enums.DocumentParseStrategy;
import com.sentra.knowledge.mapper.DocumentMapper;
import com.sentra.knowledge.service.IDocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class DocumentServiceImpl extends ServiceImpl<DocumentMapper, Document> implements IDocumentService {

    @Override
    public Document upload(String kbId, MultipartFile file) {
        // 1. 保存元数据
        Document doc = new Document();
        doc.setKbId(kbId);
        doc.setFilename(file.getOriginalFilename());
        doc.setStatus(DocumentStatus.UPLOADED);
        doc.setParseStrategy(DocumentParseStrategy.TEXT); // 默认策略
        
        this.save(doc);
        
        // 2. TODO: 保存文件到 MongoDB
        log.info("Saving file to MongoDB: {}", doc.getId());
        
        // 3. TODO: 发送解析任务到 RabbitMQ
        log.info("Sending parse task to RabbitMQ: {}", doc.getId());
        
        return doc;
    }
}
