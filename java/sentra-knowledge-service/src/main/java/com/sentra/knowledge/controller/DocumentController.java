package com.sentra.knowledge.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sentra.common.result.Result;
import com.sentra.knowledge.entity.Document;
import com.sentra.knowledge.service.IDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档管理Controller
 * 负责文档上传和文档管理
 */
@RestController
    @RequestMapping("/v1/document")
@RequiredArgsConstructor
public class DocumentController {

    private final IDocumentService documentService;

    /**
     * 上传文档到知识库
     *
     * @param kbId 知识库ID
     * @param entityTemplateId 实体类型模板ID（可选，不传则使用知识库的默认模板）
     * @param file 文档文件
     * @return 上传的文档信息
     */
    @PostMapping("/upload")
    public Result<Document> upload(@RequestParam("kbId") String kbId,
                                   @RequestParam(value = "entityTemplateId", required = false) String entityTemplateId,
                                   @RequestParam("file") MultipartFile file) {
        Document document = documentService.upload(kbId, entityTemplateId, file);
        return Result.success(document);
    }

    /**
     * 获取知识库下的文档列表
     */
    @GetMapping("/list")
    public Result<List<Document>> listByKbId(@RequestParam("kbId") String kbId) {
        LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Document::getKbId, kbId)
                .orderByDesc(Document::getCreatedAt);
        List<Document> list = documentService.list(queryWrapper);
        return Result.success(list);
    }

    /**
     * 获取文档详情
     */
    @GetMapping("/{documentId}")
    public Result<Document> get(@PathVariable("documentId") String documentId) {
        Document document = documentService.getById(documentId);
        if (document == null) {
            return Result.error("文档不存在");
        }
        return Result.success(document);
    }

    /**
     * 删除文档
     * 会同时删除：SFTP文件、OCR结果、Python知识库数据、本地图谱、Neo4j节点、MongoDB内容
     */
    @DeleteMapping("/{documentId}")
    public Result<Boolean> delete(@PathVariable("documentId") String documentId) {
        boolean deleted = documentService.deleteDocument(documentId);
        return Result.success(deleted);
    }

    /**
     * 获取某个文档知识库的产物路径
     */
    @GetMapping("/product-path/{documentId}")
    public Result<List<String>> getProductPath(@PathVariable("documentId") String documentId,
                                               @RequestParam("kbId") String kbId) {
        List<String> productPath = documentService.getProductPath(documentId, kbId);
        return Result.success(productPath);
    }
}
