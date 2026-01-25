package com.sentra.knowledge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sentra.knowledge.dto.EntityTypeTemplateCreateRequest;
import com.sentra.knowledge.dto.EntityTypeTemplateResponse;
import com.sentra.knowledge.entity.EntityTypeTemplate;

import java.util.List;

/**
 * 实体类型模板服务接口
 */
public interface IEntityTypeTemplateService extends IService<EntityTypeTemplate> {

    /**
     * 创建实体类型模板
     *
     * @param request  创建请求
     * @param tenantId 租户ID
     * @param userId   用户ID
     * @return 模板响应
     */
    EntityTypeTemplateResponse createTemplate(EntityTypeTemplateCreateRequest request, String tenantId, String userId);

    /**
     * 更新实体类型模板
     *
     * @param templateId 模板ID
     * @param request    更新请求
     * @param userId     用户ID
     * @return 是否成功
     */
    boolean updateTemplate(String templateId, EntityTypeTemplateCreateRequest request, String userId);

    /**
     * 删除实体类型模板
     *
     * @param templateId 模板ID
     * @param tenantId   租户ID
     * @return 是否成功
     */
    boolean deleteTemplate(String templateId, String tenantId);

    /**
     * 获取模板详情（包含实体类型列表）
     *
     * @param templateId 模板ID
     * @param tenantId   租户ID
     * @return 模板响应
     */
    EntityTypeTemplateResponse getTemplateDetail(String templateId, String tenantId);

    /**
     * 获取租户的所有模板列表
     *
     * @param tenantId 租户ID
     * @return 模板列表
     */
    List<EntityTypeTemplateResponse> listTemplates(String tenantId);

    /**
     * 获取系统预置模板列表
     *
     * @return 系统模板列表
     */
    List<EntityTypeTemplateResponse> listSystemTemplates();
}
