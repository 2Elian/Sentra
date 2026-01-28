package com.sentra.user.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * @author: lizimo@nuist.edu.cn
 * @ClassName: LoginResponse
 * @Date: 2026年01月28日 18:28
 * @Description: 登录响应VO
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {

    /** Sa-Token token */
    private String token;

    /** 用户名 */
    private String username;

    /** 租户 ID */
    private String tenantId;

    /** 角色 */
    private String role;
}