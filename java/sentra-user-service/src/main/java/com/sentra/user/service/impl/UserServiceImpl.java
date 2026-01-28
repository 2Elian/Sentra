package com.sentra.user.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sentra.common.exception.BusinessException;
import com.sentra.user.dto.UpdateUserRequest;
import com.sentra.user.vo.UserListVO;
import com.sentra.user.entity.User;
import com.sentra.user.enums.UserRole;
import com.sentra.user.mapper.UserMapper;
import com.sentra.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import cn.dev33.satoken.stp.StpUtil;

import com.sentra.user.entity.Tenant;
import com.sentra.user.service.ITenantService;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private final PasswordEncoder passwordEncoder;
    private final ITenantService tenantService;

    @Override
    public User getByUsername(String username) {
        return this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
    }

    @Override
    public User register(String username, String password, String tenantId, UserRole role) {
        if (StrUtil.isBlank(tenantId)) {
            throw new RuntimeException("注册用户必须指定租户ID (tenantId)");
        }
        
        // 校验租户是否存在
        Tenant tenant = tenantService.getById(tenantId);
        if (tenant == null) {
            throw new RuntimeException("指定的租户ID不存在: " + tenantId);
        }

        if (role == null) {
            throw new RuntimeException("注册用户必须指定用户角色 (role)");
        }

        User existUser = getByUsername(username);
        if (existUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        // 加密密码
        user.setPassword(passwordEncoder.encode(password));
        user.setTenantId(tenantId);
        user.setRole(role);
        
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        this.save(user);
        return user;
    }

    @Override
    public boolean deleteUser(String id) {
        // 逻辑删除或物理删除，这里演示物理删除
        return this.removeById(id);
    }

    @Override
    public User updateUser(String id, UpdateUserRequest request) {
        User user = this.getById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (StrUtil.isNotBlank(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (StrUtil.isNotBlank(request.getTenantId())) {
            user.setTenantId(request.getTenantId());
        }
        
        user.setUpdatedAt(LocalDateTime.now());
        this.updateById(user);
        
        return user;
    }

    @Override
    public List<UserListVO> listUsersInCurrentTenant() {

        // 使用 String 类型获取用户 ID，因为数据库中 id 字段是 VARCHAR 类型
        String userId = StpUtil.getLoginIdAsString();

        User currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException("当前用户不存在");
        }

        if (!UserRole.ADMIN.equals(currentUser.getRole())) {
            throw new BusinessException("无权限访问该接口");
        }

        String tenantId = currentUser.getTenantId();

        Tenant tenant = tenantService.getById(tenantId);
        if (tenant == null) {
            throw new BusinessException("租户不存在");
        }

        List<User> users = this.list(
                new LambdaQueryWrapper<User>()
                        .eq(User::getTenantId, tenantId)
        );

        return users.stream().map(user -> {
            UserListVO vo = new UserListVO();
            vo.setId(user.getId());  // 添加用户 ID
            vo.setUsername(user.getUsername());
            vo.setRole(user.getRole().name());
            vo.setTenantId(user.getTenantId());
            vo.setTenantName(tenant.getName());
            vo.setCreatedAt(user.getCreatedAt());
            vo.setUpdatedAt(user.getUpdatedAt());
            return vo;
        }).toList();
    }
}
