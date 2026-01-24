package com.sentra.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.sentra.common.result.Result;
import com.sentra.user.dto.LoginRequest;
import com.sentra.user.dto.RegisterRequest;
import com.sentra.user.entity.User;
import com.sentra.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.sentra.user.dto.UpdateUserRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IUserService userService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public Result<User> register(@RequestBody RegisterRequest request) {
        try {
            User user = userService.register(request.getUsername(), request.getPassword(), request.getTenantId(), request.getRole());
            return Result.success(user);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginRequest request) {
        User user = userService.getByUsername(request.getUsername());
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return Result.error("密码错误");
        }
        StpUtil.login(user.getId());
        return Result.success(StpUtil.getTokenValue(), "登录成功");
    }
    
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.success();
    }
    
    @DeleteMapping("/user/{id}")
    public Result<Boolean> deleteUser(@PathVariable String id) {
        // TODO: 建议加上权限校验，例如只有管理员可以删除用户
        return Result.success(userService.deleteUser(id));
    }
    
    @PutMapping("/user/{id}")
    public Result<User> updateUser(@PathVariable String id, @RequestBody UpdateUserRequest request) {
        // TODO: 建议加上权限校验
        try {
            return Result.success(userService.updateUser(id, request));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
