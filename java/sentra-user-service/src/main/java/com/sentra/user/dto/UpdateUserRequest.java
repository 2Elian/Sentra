package com.sentra.user.dto;

import com.sentra.user.enums.UserRole;
import lombok.Data;

@Data
public class UpdateUserRequest {
    private String password;
    private UserRole role;
    private String tenantId; // 允许修改租户归属（仅限高级管理员场景，一般不改）
}
