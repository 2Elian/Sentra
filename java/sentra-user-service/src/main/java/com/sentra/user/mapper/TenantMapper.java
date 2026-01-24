package com.sentra.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sentra.user.entity.Tenant;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {
}
