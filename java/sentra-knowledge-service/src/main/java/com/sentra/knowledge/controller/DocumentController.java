package com.sentra.knowledge.controller;

import com.sentra.common.result.Result;
import com.sentra.knowledge.entity.Document;
import com.sentra.knowledge.service.IDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/v1/document")
@RequiredArgsConstructor
public class DocumentController {

    private final IDocumentService documentService;

    @PostMapping("/upload")
    public Result<Document> upload(@RequestParam("kbId") String kbId,
                                   @RequestParam("file") MultipartFile file) {
        return Result.success(documentService.upload(kbId, file));
    }

    @GetMapping("/list")
    public Result<List<Document>> list(@RequestParam("kbId") String kbId) {
        // TODO: 实际应加 Filter
        return Result.success(documentService.list());
    }
}
