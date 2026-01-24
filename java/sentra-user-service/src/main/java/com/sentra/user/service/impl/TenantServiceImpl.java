package com.sentra.user.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sentra.user.entity.Tenant;
import com.sentra.user.enums.TenantStatus;
import com.sentra.user.mapper.TenantMapper;
import com.sentra.user.service.ITenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class TenantServiceImpl extends ServiceImpl<TenantMapper, Tenant> implements ITenantService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Tenant createTenant(Tenant tenant) {
        if (tenant.getId() == null) {
            tenant.setId(IdUtil.getSnowflakeNextIdStr());
        }
        // 对于租户自身记录，将其归属租户ID设置为自身ID，实现自包含
        if (tenant.getTenantId() == null) {
            tenant.setTenantId(tenant.getId());
        }

        if (tenant.getStatus() == null) {
            tenant.setStatus(TenantStatus.ENABLED);
        }
        
        // BaseEntity's @PrePersist might not be triggered by MyBatis-Plus directly if we don't use JPA repository
        // So we set them manually to be safe, although BaseEntity logic exists.
        if (tenant.getCreatedAt() == null) {
            tenant.setCreatedAt(LocalDateTime.now());
        }
        tenant.setUpdatedAt(LocalDateTime.now());
        
        this.save(tenant);
        
        // TODO: 调用 Knowledge Service 初始化默认知识库 (可通过 Feign)
        log.info("Tenant created: {}", tenant.getId());
        
        return tenant;
    }
}
