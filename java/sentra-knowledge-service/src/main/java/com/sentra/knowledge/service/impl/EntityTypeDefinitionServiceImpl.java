package com.sentra.knowledge.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sentra.knowledge.entity.EntityTypeDefinition;
import com.sentra.knowledge.mapper.EntityTypeDefinitionMapper;
import com.sentra.knowledge.service.IEntityTypeDefinitionService;
import org.springframework.stereotype.Service;

/**
 * 实体类型定义服务实现类
 */
@Service
public class EntityTypeDefinitionServiceImpl extends ServiceImpl<EntityTypeDefinitionMapper, EntityTypeDefinition>
        implements IEntityTypeDefinitionService {
}
