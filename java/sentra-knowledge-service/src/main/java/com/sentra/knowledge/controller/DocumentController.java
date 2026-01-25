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
     */
    @PostMapping("/upload")
    public Result<Document> upload(@RequestParam("kbId") String kbId,
                                   @RequestParam("file") MultipartFile file) {
        Document document = documentService.upload(kbId, file);
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
    public Result<Document> get(@PathVariable String documentId) {
        Document document = documentService.getById(documentId);
        if (document == null) {
            return Result.error("文档不存在");
        }
        return Result.success(document);
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/{documentId}")
    public Result<Boolean> delete(@PathVariable String documentId) {
        boolean deleted = documentService.removeById(documentId);
        // TODO: 删除SFTP上的文件和OCR结果
        return Result.success(deleted);
    }
}
