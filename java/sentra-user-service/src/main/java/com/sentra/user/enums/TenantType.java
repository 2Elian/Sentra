package com.sentra.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TenantType {
    FREE, // 免费版
    PRO, // 专业版
    ENTERPRISE // 企业版
}
