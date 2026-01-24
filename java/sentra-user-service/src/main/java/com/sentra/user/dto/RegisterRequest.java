package com.sentra.user.dto;

import com.sentra.user.enums.UserRole;
import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String tenantId; // 必填
    private UserRole role; // 必填，用户角色
}
