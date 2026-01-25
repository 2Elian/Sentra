package com.sentra.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sentra.common.exception.BusinessException;
import com.sentra.knowledge.dto.EntityTypeDefinitionRequest;
import com.sentra.knowledge.dto.EntityTypeDefinitionResponse;
import com.sentra.knowledge.dto.EntityTypeTemplateCreateRequest;
import com.sentra.knowledge.dto.EntityTypeTemplateResponse;
import com.sentra.knowledge.entity.EntityTypeDefinition;
import com.sentra.knowledge.entity.EntityTypeTemplate;
import com.sentra.knowledge.mapper.EntityTypeDefinitionMapper;
import com.sentra.knowledge.mapper.EntityTypeTemplateMapper;
import com.sentra.knowledge.service.IEntityTypeDefinitionService;
import com.sentra.knowledge.service.IEntityTypeTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 实体类型模板服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EntityTypeTemplateServiceImpl extends ServiceImpl<EntityTypeTemplateMapper, EntityTypeTemplate>
        implements IEntityTypeTemplateService {

    private final EntityTypeTemplateMapper templateMapper;
    private final EntityTypeDefinitionMapper definitionMapper;
    private final IEntityTypeDefinitionService definitionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EntityTypeTemplateResponse createTemplate(EntityTypeTemplateCreateRequest request, String tenantId, String userId) {
        // 1. 检查名称是否已存在
        long count = this.count(
                new LambdaQueryWrapper<EntityTypeTemplate>()
                        .eq(EntityTypeTemplate::getTenantId, tenantId)
                        .eq(EntityTypeTemplate::getName, request.getName())
        );
        if (count > 0) {
            throw new BusinessException("模板名称已存在");
        }

        // 2. 创建模板
        EntityTypeTemplate template = new EntityTypeTemplate();
        template.setTenantId(tenantId);
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setIsSystem(false);
        template.setIsActive(true);
        template.setCreatedBy(userId);
        template.setUpdatedBy(userId);
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());

        this.save(template);

        // 3. 创建实体类型定义
        if (request.getEntityTypes() != null && !request.getEntityTypes().isEmpty()) {
            for (EntityTypeDefinitionRequest entityReq : request.getEntityTypes()) {
                EntityTypeDefinition definition = new EntityTypeDefinition();
                definition.setTemplateId(template.getId());
                definition.setEntityCode(entityReq.getEntityCode());
                definition.setEntityName(entityReq.getEntityName());
                definition.setEntityDescription(entityReq.getEntityDescription());
                definition.setDisplayOrder(entityReq.getDisplayOrder() != null ? entityReq.getDisplayOrder() : 0);
                definition.setCreatedAt(LocalDateTime.now());
                definition.setUpdatedAt(LocalDateTime.now());

                definitionMapper.insert(definition);
            }
        }

        log.info("创建实体类型模板成功，templateId: {}, name: {}", template.getId(), template.getName());
        return getTemplateDetail(template.getId(), tenantId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTemplate(String templateId, EntityTypeTemplateCreateRequest request, String userId) {
        // 1. 查询模板
        EntityTypeTemplate template = this.getById(templateId);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }

        // 2. 检查是否为系统模板（系统模板不允许修改）
        if (template.getIsSystem()) {
            throw new BusinessException("系统预置模板不允许修改");
        }

        // 3. 检查名称是否与其他模板冲突
        long count = this.count(
                new LambdaQueryWrapper<EntityTypeTemplate>()
                        .eq(EntityTypeTemplate::getTenantId, template.getTenantId())
                        .eq(EntityTypeTemplate::getName, request.getName())
                        .ne(EntityTypeTemplate::getId, templateId)
        );
        if (count > 0) {
            throw new BusinessException("模板名称已存在");
        }

        // 4. 更新模板基本信息
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setUpdatedBy(userId);
        template.setUpdatedAt(LocalDateTime.now());
        this.updateById(template);

        // 5. 删除旧的实体类型定义
        definitionMapper.delete(
                new LambdaQueryWrapper<EntityTypeDefinition>()
                        .eq(EntityTypeDefinition::getTemplateId, templateId)
        );

        // 6. 插入新的实体类型定义
        if (request.getEntityTypes() != null && !request.getEntityTypes().isEmpty()) {
            for (EntityTypeDefinitionRequest entityReq : request.getEntityTypes()) {
                EntityTypeDefinition definition = new EntityTypeDefinition();
                definition.setTemplateId(templateId);
                definition.setEntityCode(entityReq.getEntityCode());
                definition.setEntityName(entityReq.getEntityName());
                definition.setEntityDescription(entityReq.getEntityDescription());
                definition.setDisplayOrder(entityReq.getDisplayOrder() != null ? entityReq.getDisplayOrder() : 0);
                definition.setCreatedAt(LocalDateTime.now());
                definition.setUpdatedAt(LocalDateTime.now());

                definitionMapper.insert(definition);
            }
        }

        log.info("更新实体类型模板成功，templateId: {}", templateId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTemplate(String templateId, String tenantId) {
        // 1. 查询模板
        EntityTypeTemplate template = this.getById(templateId);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }

        // 2. 检查租户权限
        if (!template.getTenantId().equals(tenantId)) {
            throw new BusinessException("无权删除该模板");
        }

        // 3. 检查是否为系统模板（系统模板不允许删除）
        if (template.getIsSystem()) {
            throw new BusinessException("系统预置模板不允许删除");
        }

        // 4. 检查是否有知识库正在使用该模板
        // TODO: 添加知识库表的关联检查

        // 5. 删除模板（级联删除实体类型定义）
        this.removeById(templateId);

        log.info("删除实体类型模板成功，templateId: {}", templateId);
        return true;
    }

    @Override
    public EntityTypeTemplateResponse getTemplateDetail(String templateId, String tenantId) {
        // 1. 查询模板
        EntityTypeTemplate template = this.getById(templateId);
        if (template == null) {
            throw new BusinessException("模板不存在");
        }

        // 2. 检查租户权限（系统模板所有租户可见）
        if (!template.getIsSystem() && !template.getTenantId().equals(tenantId)) {
            throw new BusinessException("无权访问该模板");
        }

        // 3. 查询实体类型定义列表
        List<EntityTypeDefinition> definitions = definitionMapper.selectList(
                new LambdaQueryWrapper<EntityTypeDefinition>()
                        .eq(EntityTypeDefinition::getTemplateId, templateId)
                        .orderByAsc(EntityTypeDefinition::getDisplayOrder)
        );

        // 4. 转换为响应DTO
        EntityTypeTemplateResponse response = new EntityTypeTemplateResponse();
        BeanUtils.copyProperties(template, response);

        List<EntityTypeDefinitionResponse> definitionResponses = definitions.stream()
                .map(definition -> {
                    EntityTypeDefinitionResponse defResponse = new EntityTypeDefinitionResponse();
                    BeanUtils.copyProperties(definition, defResponse);
                    return defResponse;
                })
                .collect(Collectors.toList());

        response.setEntityTypes(definitionResponses);

        return response;
    }

    @Override
    public List<EntityTypeTemplateResponse> listTemplates(String tenantId) {
        // 查询租户的自定义模板 + 系统预置模板
        List<EntityTypeTemplate> templates = templateMapper.selectList(
                new LambdaQueryWrapper<EntityTypeTemplate>()
                        .and(wrapper -> wrapper
                                .eq(EntityTypeTemplate::getTenantId, tenantId)
                                .or()
                                .eq(EntityTypeTemplate::getIsSystem, true)
                        )
                        .eq(EntityTypeTemplate::getIsActive, true)
                        .orderByDesc(EntityTypeTemplate::getIsSystem)
                        .orderByAsc(EntityTypeTemplate::getCreatedAt)
        );

        return templates.stream()
                .map(template -> {
                    EntityTypeTemplateResponse response = new EntityTypeTemplateResponse();
                    BeanUtils.copyProperties(template, response);
                    // 不返回实体类型列表，仅返回基本信息
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<EntityTypeTemplateResponse> listSystemTemplates() {
        List<EntityTypeTemplate> templates = templateMapper.selectList(
                new LambdaQueryWrapper<EntityTypeTemplate>()
                        .eq(EntityTypeTemplate::getIsSystem, true)
                        .eq(EntityTypeTemplate::getIsActive, true)
                        .orderByAsc(EntityTypeTemplate::getCreatedAt)
        );

        return templates.stream()
                .map(template -> {
                    EntityTypeTemplateResponse response = new EntityTypeTemplateResponse();
                    BeanUtils.copyProperties(template, response);
                    return response;
                })
                .collect(Collectors.toList());
    }
}
