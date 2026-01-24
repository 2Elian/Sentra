package com.sentra.user.controller;

import com.sentra.common.result.Result;
import com.sentra.user.entity.Tenant;
import com.sentra.user.service.ITenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/tenant")
@RequiredArgsConstructor
public class TenantController {

    private final ITenantService tenantService;

    // 创建租户
    @PostMapping
    public Result<Tenant> create(@RequestBody Tenant tenant) {
        return Result.success(tenantService.createTenant(tenant));
    }

    // 删除租户
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable("id") String id) {
        tenantService.removeById(id);
        return Result.success("删除租户成功");
    }

    // 根据id查询租户
    @GetMapping("/{id}")
    public Result<Tenant> get(@PathVariable("id") String id) {
        return Result.success(tenantService.getById(id));
    }
    
    // 查询所有租户列表
    @GetMapping
    public Result<List<Tenant>> list() {
        return Result.success(tenantService.list());
    }
}
