package com.sentra.common.filter;

import com.sentra.common.context.TenantContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 租户上下文过滤器
 * 从请求头中获取租户ID并设置到上下文中
 */
@Slf4j
public class TenantContextFilter implements Filter {

    private static final String HEADER_TENANT_ID = "X-Tenant-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String tenantId = httpRequest.getHeader(HEADER_TENANT_ID);

        if (tenantId != null) {
            TenantContext.setTenantId(tenantId);
            log.debug("Set TenantContext ID: {}", tenantId);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
