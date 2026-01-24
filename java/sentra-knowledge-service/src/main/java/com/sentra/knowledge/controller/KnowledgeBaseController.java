package com.sentra.knowledge.controller;

import com.sentra.common.result.Result;
import com.sentra.knowledge.entity.KnowledgeBase;
import com.sentra.knowledge.service.IKnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/kb")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final IKnowledgeBaseService kbService;

    @PostMapping
    public Result<Boolean> create(@RequestBody KnowledgeBase kb) {
        return Result.success(kbService.save(kb));
    }

    @GetMapping
    public Result<List<KnowledgeBase>> list() {
        return Result.success(kbService.list());
    }
    
    @GetMapping("/{id}")
    public Result<KnowledgeBase> get(@PathVariable String id) {
        return Result.success(kbService.getById(id));
    }
}
