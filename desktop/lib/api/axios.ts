import axios, { AxiosError, AxiosResponse } from 'axios';

/**
 * API 错误类
 */
export class ApiError extends Error {
  constructor(
    public message: string,
    public statusCode: number,
    public details?: any
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

/**
 * 检测是否为开发环境
 */
const isDevelopment = process.env.NODE_ENV === 'development';

/**
 * 创建用户服务 Axios 实例
 */
export const userApiClient = axios.create({
  // 开发环境使用代理路径，生产环境使用完整 URL
  baseURL: isDevelopment
    ? '/api/user'  // 开发环境：通过 Next.js 代理
    : (process.env.NEXT_PUBLIC_USER_API_URL || 'http://localhost:8081'),  // 生产环境：直接访问
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * 创建知识库服务 Axios 实例
 */
export const knowledgeApiClient = axios.create({
  // 开发环境使用代理路径，生产环境使用完整 URL
  baseURL: isDevelopment
    ? '/api/knowledge'  // 开发环境：通过 Next.js 代理
    : (process.env.NEXT_PUBLIC_KNOWLEDGE_API_URL || 'http://localhost:8082'),  // 生产环境：直接访问
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * 创建通用 Axios 实例（默认使用用户服务）
 */
export const apiClient = userApiClient;

/**
 * 为指定实例配置拦截器
 */
const setupInterceptors = (instance: any) => {
  // 请求拦截器
  instance.interceptors.request.use(
    (config: any) => {
      // 从 localStorage 获取 token
      if (typeof window !== 'undefined') {
        const token = localStorage.getItem('token');
        console.log('Request interceptor - token:', token);
        console.log('Request interceptor - URL:', config.url);
        if (token) {
          // 直接发送 token，不添加 Bearer 前缀（Sa-Token 配置的 token-name 是 Authorization）
          config.headers.Authorization = token;
          console.log('Request interceptor - Authorization header set:', config.headers.Authorization);
        }
      }
      return config;
    },
    (error: any) => {
      return Promise.reject(error);
    }
  );

  // 响应拦截器
  instance.interceptors.response.use(
    (response: AxiosResponse) => {
      return response.data;
    },
    (error: AxiosError) => {
      console.error('API Error Details:', {
        status: error.response?.status,
        statusText: error.response?.statusText,
        data: error.response?.data,
        headers: error.response?.headers,
        config: {
          url: error.config?.url,
          method: error.config?.method,
          baseURL: error.config?.baseURL,
        }
      });

      if (error.response) {
        const status = error.response.status;
        const data = error.response.data as any;

        // 处理 401 未授权错误
        if (status === 401) {
          if (typeof window !== 'undefined') {
            // 清除 token 并跳转到登录页
            localStorage.removeItem('token');
            localStorage.removeItem('user');
            window.location.href = '/login';
          }
          return Promise.reject(
            new ApiError(data?.message || '未授权，请重新登录', status, data)
          );
        }

        // 处理其他错误
        const message = data?.message || data?.msg || '请求失败';
        return Promise.reject(new ApiError(message, status, data));
      }

      if (error.request) {
        // 网络错误
        return Promise.reject(
          new ApiError('网络连接失败，请检查网络设置', 0, error.request)
        );
      }

      // 其他错误
      return Promise.reject(new ApiError(error.message || '发生未知错误', 0, error));
    }
  );
};

// 为所有 Axios 实例配置拦截器
setupInterceptors(userApiClient);
setupInterceptors(knowledgeApiClient);

export default apiClient;
