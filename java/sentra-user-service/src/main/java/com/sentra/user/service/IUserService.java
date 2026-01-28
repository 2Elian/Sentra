package com.sentra.user.service;

import com.sentra.user.vo.UserListVO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sentra.user.entity.User;
import com.sentra.user.enums.UserRole;

import java.util.List;

public interface IUserService extends IService<User> {
    /**
     * 根据用户名获取用户
     *
     * @param username 用户名
     * @return 用户
     */
    User getByUsername(String username);

    /**
     * 注册用户
     *
     * @param username 用户名
     * @param password 密码
     * @param tenantId 租户ID
     * @param role     用户角色
     * @return 注册后的用户
     */
    User register(String username, String password, String tenantId, UserRole role);

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 是否成功
     */
    boolean deleteUser(String id);

    /**
     * 更新用户信息
     *
     * @param id 用户ID
     * @param request 更新请求
     * @return 更新后的用户
     */
    User updateUser(String id, com.sentra.user.dto.UpdateUserRequest request);

    /**
     * 列出当前租户的所有用户
     *
     * @return 用户列表
     */
    List<UserListVO> listUsersInCurrentTenant();
}
