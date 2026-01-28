package com.sentra.knowledge.controller;

import com.sentra.common.result.Result;
import com.sentra.knowledge.dto.KnowledgeBaseCreateRequest;
import com.sentra.knowledge.dto.KnowledgeBaseResponse;
import com.sentra.knowledge.dto.KnowledgeBaseUpdateRequest;
import com.sentra.knowledge.service.IKnowledgeBaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库管理Controller
 */
@RestController
@RequestMapping("/v1/kb")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final IKnowledgeBaseService kbService;

    /**
     * 创建知识库
     */
    @PostMapping
    public Result<KnowledgeBaseResponse> create(@RequestBody @Valid KnowledgeBaseCreateRequest request,
                                                 @RequestHeader("X-Tenant-Id") String tenantId) {
        KnowledgeBaseResponse response = kbService.createKnowledgeBase(request, tenantId);
        return Result.success(response);
    }

    /**
     * 更新知识库
     */
    @PutMapping("/{kbId}")
    public Result<Boolean> update(@PathVariable("kbId") String kbId,
                                  @RequestBody @Valid KnowledgeBaseUpdateRequest request) {
        boolean updated = kbService.updateKnowledgeBase(kbId, request);
        return Result.success(updated);
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/{kbId}")
    public Result<Boolean> delete(@PathVariable("kbId") String kbId) {
        boolean deleted = kbService.deleteKnowledgeBase(kbId);
        return Result.success(deleted);
    }

    /**
     * 获取知识库详情
     */
    @GetMapping("/{kbId}")
    public Result<KnowledgeBaseResponse> get(@PathVariable("kbId") String kbId) {
        KnowledgeBaseResponse response = kbService.getKnowledgeBaseByKbId(kbId);
        return Result.success(response);
    }

    /**
     * 获取租户下的所有知识库
     */
    @GetMapping("/list")
    public Result<List<KnowledgeBaseResponse>> listByTenant(@RequestHeader("X-Tenant-Id") String tenantId) {
        List<KnowledgeBaseResponse> list = kbService.listByTenantId(tenantId);
        return Result.success(list);
    }

    /**
     * 获取用户的知识库列表
     */
    @GetMapping("/owner/{ownerUserId}")
    public Result<List<KnowledgeBaseResponse>> listByOwner(@PathVariable("ownerUserId") String ownerUserId,
                                                            @RequestHeader("X-Tenant-Id") String tenantId) {
        List<KnowledgeBaseResponse> list = kbService.listByOwner(tenantId, ownerUserId);
        return Result.success(list);
    }
}
