package com.sentra.knowledge.controller;

import com.sentra.common.result.Result;
import com.sentra.knowledge.dto.EntityTypeTemplateCreateRequest;
import com.sentra.knowledge.dto.EntityTypeTemplateResponse;
import com.sentra.knowledge.service.IEntityTypeTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 实体类型模板管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/v1/knowledge/entity-template")
@RequiredArgsConstructor
public class EntityTypeTemplateController {

    private final IEntityTypeTemplateService templateService;

    /**
     * 创建实体类型模板
     *
     * @param request 创建请求
     * @param tenantId 租户ID（从请求头获取）
     * @param userId   用户ID（从请求头获取）
     * @return 模板响应
     */
    @PostMapping
    public Result<EntityTypeTemplateResponse> createTemplate(
            @Valid @RequestBody EntityTypeTemplateCreateRequest request,
            @RequestHeader("X-TenantId") String tenantId,
            @RequestHeader("X-UserId") String userId
    ) {
        log.info("创建实体类型模板，name: {}, tenantId: {}", request.getName(), tenantId);
        EntityTypeTemplateResponse response = templateService.createTemplate(request, tenantId, userId);
        return Result.success(response);
    }

    /**
     * 更新实体类型模板
     *
     * @param templateId 模板ID
     * @param request    更新请求
     * @param userId     用户ID
     * @return 是否成功
     */
    @PutMapping("/{templateId}")
    public Result<Void> updateTemplate(
            @PathVariable String templateId,
            @Valid @RequestBody EntityTypeTemplateCreateRequest request,
            @RequestHeader("X-UserId") String userId
    ) {
        log.info("更新实体类型模板，templateId: {}", templateId);
        templateService.updateTemplate(templateId, request, userId);
        return Result.success();
    }

    /**
     * 删除实体类型模板
     *
     * @param templateId 模板ID
     * @param tenantId   租户ID
     * @return 是否成功
     */
    @DeleteMapping("/{templateId}")
    public Result<Void> deleteTemplate(
            @PathVariable String templateId,
            @RequestHeader("X-TenantId") String tenantId
    ) {
        log.info("删除实体类型模板，templateId: {}", templateId);
        templateService.deleteTemplate(templateId, tenantId);
        return Result.success();
    }

    /**
     * 获取模板详情（包含实体类型列表）
     *
     * @param templateId 模板ID
     * @param tenantId   租户ID
     * @return 模板详情
     */
    @GetMapping("/{templateId}")
    public Result<EntityTypeTemplateResponse> getTemplateDetail(
            @PathVariable String templateId,
            @RequestHeader("X-TenantId") String tenantId
    ) {
        EntityTypeTemplateResponse response = templateService.getTemplateDetail(templateId, tenantId);
        return Result.success(response);
    }

    /**
     * 获取租户的所有模板列表
     *
     * @param tenantId 租户ID
     * @return 模板列表
     */
    @GetMapping
    public Result<List<EntityTypeTemplateResponse>> listTemplates(
            @RequestHeader("X-TenantId") String tenantId
    ) {
        List<EntityTypeTemplateResponse> templates = templateService.listTemplates(tenantId);
        return Result.success(templates);
    }

    /**
     * 获取系统预置模板列表
     *
     * @return 系统模板列表
     */
    @GetMapping("/system")
    public Result<List<EntityTypeTemplateResponse>> listSystemTemplates() {
        List<EntityTypeTemplateResponse> templates = templateService.listSystemTemplates();
        return Result.success(templates);
    }
}
