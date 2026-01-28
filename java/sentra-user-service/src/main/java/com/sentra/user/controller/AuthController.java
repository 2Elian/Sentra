package com.sentra.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.sentra.common.result.Result;
import com.sentra.user.dto.LoginRequest;
import com.sentra.user.vo.UserListVO;
import com.sentra.user.dto.RegisterRequest;
import com.sentra.user.vo.LoginResponse;
import com.sentra.user.entity.User;
import com.sentra.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.sentra.user.dto.UpdateUserRequest;
import org.springframework.web.bind.annotation.*;
import java.util.List;

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
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        User user = userService.getByUsername(request.getUsername());
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return Result.error("密码错误");
        }
        // Sa-Token 登录
        StpUtil.login(user.getId());
        LoginResponse response = new LoginResponse(
                StpUtil.getTokenValue(),
                user.getUsername(),
                user.getTenantId(),
                user.getRole().toString()
        );
        return Result.success(response, "登录成功");
    }
    
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return Result.success();
    }
    
    @DeleteMapping("/user/{id}")
    public Result<Boolean> deleteUser(@PathVariable("id") String id) {
        // TODO: 建议加上权限校验，例如只有管理员可以删除用户
        return Result.success(userService.deleteUser(id));
    }

    @PutMapping("/user/{id}")
    public Result<User> updateUser(@PathVariable("id") String id, @RequestBody UpdateUserRequest request) {
        // TODO: 建议加上权限校验
        try {
            return Result.success(userService.updateUser(id, request));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询当前用户所属租户下的所有用户
      * @return 用户列表
     */
    @GetMapping("/users")
    public Result<List<UserListVO>> listUsersInTenant() {
        return Result.success(userService.listUsersInCurrentTenant());
    }
}
