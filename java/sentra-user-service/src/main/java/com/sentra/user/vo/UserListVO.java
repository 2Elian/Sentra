package com.sentra.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author: lizimo@nuist.edu.cn
 * @ClassName: UserListVO
 * @Date: 2026年01月28日 18:42
 * @Description:
 */
@Data
public class UserListVO {

    private String id;

    private String username;

    private String role;

    private String tenantId;

    private String tenantName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}