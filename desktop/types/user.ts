/**
 * 用户角色
 */
export enum UserRole {
  ADMIN = 'ADMIN',
  USER = 'USER',
}

/**
 * 用户信息
 */
export interface User {
  id?: string;
  username: string;
  tenantId: string;
  role: UserRole;
  createdAt?: string;
  updatedAt?: string;
}

/**
 * 登录请求
 */
export interface LoginRequest {
  username: string;
  password: string;
}

/**
 * 后端统一响应格式
 */
export interface Result<T> {
  code: number;
  message: string;
  data: T;
}

/**
 * 登录响应（后端实际返回）
 */
export interface LoginResponseData {
  token: string;
  username: string;
  tenantId: string;
  role: string;
}

/**
 * 前端处理的登录响应
 */
export interface LoginResponse {
  token: string;
  user: User;
}

/**
 * 创建用户请求（仅管理员）
 */
export interface CreateUserRequest {
  username: string;
  password: string;
  tenantId: string;
  role: UserRole;
}

/**
 * 更新用户请求
 */
export interface UpdateUserRequest {
  username?: string;
  password?: string;
  role?: UserRole;
}

/**
 * 用户列表响应（旧版，已废弃）
 */
export interface UserListResponse {
  users: User[];
  total: number;
}

/**
 * 用户列表视图对象（后端返回格式）
 */
export interface UserListVO {
  id: string;
  username: string;
  role: string;
  tenantId: string;
  tenantName: string;
  createdAt: string;
  updatedAt: string;
}
