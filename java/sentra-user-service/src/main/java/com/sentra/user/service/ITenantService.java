package com.sentra.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sentra.user.entity.Tenant;

public interface ITenantService extends IService<Tenant> {
    /**
     * 创建租户并初始化资源
     *
     * @param tenant 租户信息
     * @return 创建后的租户
     */
    Tenant createTenant(Tenant tenant);
}
