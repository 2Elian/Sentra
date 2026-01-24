package com.sentra.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.sentra.common.entity.BaseEntity;
import com.sentra.user.enums.TenantStatus;
import com.sentra.user.enums.TenantType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_tenant")
@Entity
@Table(name = "t_tenant")
public class Tenant extends BaseEntity {

    private String name;

    @Enumerated(EnumType.STRING)
    private TenantType type;

    @Enumerated(EnumType.STRING)
    private TenantStatus status;
}
