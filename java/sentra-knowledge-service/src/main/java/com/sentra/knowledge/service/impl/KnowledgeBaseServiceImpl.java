package com.sentra.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sentra.common.exception.BusinessException;
import com.sentra.knowledge.dto.KnowledgeBaseCreateRequest;
import com.sentra.knowledge.dto.KnowledgeBaseResponse;
import com.sentra.knowledge.dto.KnowledgeBaseUpdateRequest;
import com.sentra.knowledge.entity.EntityTypeTemplate;
import com.sentra.knowledge.entity.KnowledgeBase;
import com.sentra.knowledge.mapper.KnowledgeBaseMapper;
import com.sentra.knowledge.service.IEntityTypeTemplateService;
import com.sentra.knowledge.service.IKnowledgeBaseService;
import com.sentra.knowledge.service.StorageInitializationService;
import com.sentra.knowledge.util.KbIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> implements IKnowledgeBaseService {

    private final StorageInitializationService storageInitializationService;
    private final IEntityTypeTemplateService entityTypeTemplateService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBaseResponse createKnowledgeBase(KnowledgeBaseCreateRequest request, String tenantId) {
        // 同一租户下 校验知识库名称是否已存在
        LambdaQueryWrapper<KnowledgeBase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeBase::getTenantId, tenantId)
                .eq(KnowledgeBase::getName, request.getName());
        KnowledgeBase existingKb = this.getOne(queryWrapper);
        if (existingKb != null) {
            throw new BusinessException("知识库名称已存在");
        }

        // TODO 校验ownerUserId是否真实存在（TODO: 调用user-service验证）
        // TODO 此处暂时跳过，后续通过Feign调用user-service验证

        // 3. 校验实体类型模板
        String entityTemplateId = request.getEntityTemplateId();
        if (entityTemplateId != null && !entityTemplateId.isEmpty()) {
            EntityTypeTemplate template = entityTypeTemplateService.getById(entityTemplateId);
            if (template == null) {
                throw new BusinessException("实体类型模板不存在");
            }
            // 检查模板是否属于当前租户或是系统模板
            if (!template.getIsSystem() && !template.getTenantId().equals(tenantId)) {
                throw new BusinessException("无权使用该实体类型模板");
            }
            if (!template.getIsActive()) {
                throw new BusinessException("实体类型模板已停用");
            }
        } else {
            // 如果未指定，使用系统默认的合同领域模板
            LambdaQueryWrapper<EntityTypeTemplate> templateWrapper = new LambdaQueryWrapper<>();
            templateWrapper.eq(EntityTypeTemplate::getName, "合同领域")
                    .eq(EntityTypeTemplate::getIsSystem, true);
            EntityTypeTemplate defaultTemplate = entityTypeTemplateService.getOne(templateWrapper);
            if (defaultTemplate != null) {
                entityTemplateId = defaultTemplate.getId();
            }
        }

        // 4. 生成kbId作为唯一的知识库id
        String kbId = KbIdGenerator.generate(tenantId, request.getOwnerUserId(), request.getName());

        // 5. 创建知识库实体
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setName(request.getName());
        knowledgeBase.setOwnerUserId(request.getOwnerUserId());
        knowledgeBase.setScope(request.getScope());
        knowledgeBase.setDescription(request.getDescription());
        knowledgeBase.setKbId(kbId);
        knowledgeBase.setTenantId(tenantId);
        knowledgeBase.setEntityTemplateId(entityTemplateId);
        knowledgeBase.setCreatedAt(LocalDateTime.now());
        knowledgeBase.setUpdatedAt(LocalDateTime.now());


        // 保存到sql数据库
        boolean saved = this.save(knowledgeBase);
        if (!saved) {
            throw new BusinessException("知识库创建失败");
        }

        // 初始化存储 --> MongoDB集合、本地graph目录、Graph命名空间
        try {
            storageInitializationService.initializeKnowledgeBaseStorage(kbId);
        } catch (Exception e) {
            log.error("初始化知识库存储失败，kbId: {}", kbId, e);
            // 回滚数据库
            this.removeById(knowledgeBase.getId());
            throw new BusinessException("知识库存储初始化失败: " + e.getMessage());
        }
        log.info("知识库创建成功，kbId: {}, name: {}", kbId, request.getName());
        return toResponse(knowledgeBase);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateKnowledgeBase(String kbId, KnowledgeBaseUpdateRequest request) {
        // 1. 查询知识库
        LambdaQueryWrapper<KnowledgeBase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeBase::getKbId, kbId);
        KnowledgeBase knowledgeBase = this.getOne(queryWrapper);

        if (knowledgeBase == null) {
            throw new BusinessException("知识库不存在");
        }

        // 2. 检查名称是否重复（排除自己）
        if (!knowledgeBase.getName().equals(request.getName())) {
            LambdaQueryWrapper<KnowledgeBase> nameCheckWrapper = new LambdaQueryWrapper<>();
            nameCheckWrapper.eq(KnowledgeBase::getTenantId, knowledgeBase.getTenantId())
                    .eq(KnowledgeBase::getName, request.getName())
                    .ne(KnowledgeBase::getId, knowledgeBase.getId());
            long count = this.count(nameCheckWrapper);
            if (count > 0) {
                throw new BusinessException("知识库名称已存在");
            }
        }

        // 3. 更新字段
        knowledgeBase.setName(request.getName());
        if (request.getScope() != null) {
            knowledgeBase.setScope(request.getScope());
        }
        knowledgeBase.setDescription(request.getDescription());

        // 4. 保存更新
        boolean updated = this.updateById(knowledgeBase);

        log.info("知识库更新成功，kbId: {}", kbId);

        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteKnowledgeBase(String kbId) {
        // 1. 查询知识库
        LambdaQueryWrapper<KnowledgeBase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeBase::getKbId, kbId);
        KnowledgeBase knowledgeBase = this.getOne(queryWrapper);

        if (knowledgeBase == null) {
            throw new BusinessException("知识库不存在");
        }

        // 2. 删除数据库记录
        boolean deleted = this.removeById(knowledgeBase.getId());

        if (deleted) {
            // 3. 删除相关存储
            try {
                storageInitializationService.deleteKnowledgeBaseStorage(kbId);
                log.info("知识库删除成功，kbId: {}", kbId);
            } catch (Exception e) {
                log.error("删除知识库存储失败，kbId: {}", kbId, e);
                throw new BusinessException("知识库存储删除失败: " + e.getMessage());
            }
        }

        return deleted;
    }

    @Override
    public KnowledgeBaseResponse getKnowledgeBaseByKbId(String kbId) {
        LambdaQueryWrapper<KnowledgeBase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeBase::getKbId, kbId);
        KnowledgeBase knowledgeBase = this.getOne(queryWrapper);

        if (knowledgeBase == null) {
            throw new BusinessException("知识库不存在");
        }

        return toResponse(knowledgeBase);
    }

    @Override
    public List<KnowledgeBaseResponse> listByTenantId(String tenantId) {
        LambdaQueryWrapper<KnowledgeBase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeBase::getTenantId, tenantId);
        List<KnowledgeBase> list = this.list(queryWrapper);

        return list.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<KnowledgeBaseResponse> listByOwner(String tenantId, String ownerUserId) {
        LambdaQueryWrapper<KnowledgeBase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeBase::getTenantId, tenantId)
                .eq(KnowledgeBase::getOwnerUserId, ownerUserId);
        List<KnowledgeBase> list = this.list(queryWrapper);

        return list.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 实体转响应DTO
     */
    private KnowledgeBaseResponse toResponse(KnowledgeBase entity) {
        KnowledgeBaseResponse response = new KnowledgeBaseResponse();
        BeanUtils.copyProperties(entity, response);
        return response;
    }
}
