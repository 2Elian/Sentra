import { ApiError, userApiClient } from '@/lib/api/axios';
import {
  LoginRequest,
  LoginResponse,
  LoginResponseData,
  Result,
  User,
  UserRole,
  CreateUserRequest,
  UpdateUserRequest,
  UserListResponse,
  UserListVO,
} from '@/types/user';

/**
 * 用户认证 API
 */
export const authApi = {
  /**
   * 用户登录
   */
  login: async (data: LoginRequest): Promise<LoginResponse> => {
    try {
      // 后端返回格式: { code: 200, message: "登录成功", data: LoginResponseData }
      const response = await userApiClient.post<any, Result<LoginResponseData>>('/v1/auth/login', data);

      console.log('Raw login response:', response);

      // 检查响应状态
      if (response.code !== 200) {
        throw new ApiError(response.message || '登录失败', response.code, response);
      }

      // 转换为前端格式
      const loginResponse: LoginResponse = {
        token: response.data.token,
        user: {
          username: response.data.username,
          tenantId: response.data.tenantId,
          role: response.data.role as UserRole,
        },
      };

      return loginResponse;
    } catch (error) {
      if (error instanceof ApiError) {
        throw error;
      }
      throw new ApiError('登录失败', 0, error);
    }
  },

  /**
   * 用户登出
   */
  logout: async (): Promise<void> => {
    try {
      await userApiClient.post('/v1/auth/logout');
    } catch (error) {
      // 即使接口调用失败，也要清除本地存储
      console.error('登出接口调用失败:', error);
    }
  },

  /**
   * 获取当前用户信息
   */
  getCurrentUser: async (): Promise<User> => {
    try {
      const response = await userApiClient.get<any, Result<User>>('/v1/auth/me');
      return response.data;
    } catch (error) {
      if (error instanceof ApiError) {
        throw error;
      }
      throw new ApiError('获取用户信息失败', 0, error);
    }
  },
};

/**
 * 用户管理 API（仅管理员）
 */
export const userApi = {
  /**
   * 获取用户列表（新版：获取当前租户下的所有用户）
   */
  list: async (): Promise<UserListVO[]> => {
    try {
      console.log('Calling GET /v1/auth/users');
      const response = await userApiClient.get<any, Result<UserListVO[]>>('/v1/auth/users');
      console.log('API response:', response);
      console.log('Response data:', response.data);
      return response.data;
    } catch (error) {
      console.error('API call failed:', error);
      if (error instanceof ApiError) {
        throw error;
      }
      throw new ApiError('获取用户列表失败', 0, error);
    }
  },

  /**
   * 创建用户（仅管理员）
   */
  create: async (data: CreateUserRequest): Promise<User> => {
    try {
      const response = await userApiClient.post<any, Result<User>>('/v1/auth/register', data);
      return response.data;
    } catch (error) {
      if (error instanceof ApiError) {
        throw error;
      }
      throw new ApiError('创建用户失败', 0, error);
    }
  },

  /**
   * 更新用户（仅管理员）
   */
  update: async (userId: string, data: UpdateUserRequest): Promise<User> => {
    try {
      const response = await userApiClient.put<any, Result<User>>(`/v1/auth/user/${userId}`, data);
      return response.data;
    } catch (error) {
      if (error instanceof ApiError) {
        throw error;
      }
      throw new ApiError('更新用户失败', 0, error);
    }
  },

  /**
   * 删除用户（仅管理员）
   */
  delete: async (userId: string): Promise<void> => {
    try {
      await userApiClient.delete(`/v1/auth/user/${userId}`);
    } catch (error) {
      if (error instanceof ApiError) {
        throw error;
      }
      throw new ApiError('删除用户失败', 0, error);
    }
  },

  /**
   * 获取用户详情
   */
  get: async (userId: string): Promise<User> => {
    try {
      const response = await userApiClient.get<any, Result<User>>(`/v1/user/${userId}`);
      return response.data;
    } catch (error) {
      if (error instanceof ApiError) {
        throw error;
      }
      throw new ApiError('获取用户详情失败', 0, error);
    }
  },
};
