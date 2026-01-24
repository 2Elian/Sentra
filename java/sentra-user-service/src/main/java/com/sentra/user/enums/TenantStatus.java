package com.sentra.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TenantStatus {
    ENABLED, // 启用
    DISABLED // 禁用
}
