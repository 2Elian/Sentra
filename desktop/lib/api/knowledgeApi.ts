import { knowledgeApiClient } from './axios';
import {
  KnowledgeBaseResponse,
  CreateKnowledgeBaseRequest,
  UpdateKnowledgeBaseRequest,
} from '@/types/knowledge';

/**
 * 获取租户ID helper函数
 */
const getTenantId = (): string => {
  if (typeof window !== 'undefined') {
    const userStr = localStorage.getItem('user');
    if (userStr) {
      try {
        const user = JSON.parse(userStr);
        return user?.tenantId || '';
      } catch (error) {
        console.error('Failed to parse user from localStorage:', error);
      }
    }
  }
  return '';
};

/**
 * 获取用户名 helper 函数
 */
const getUserName = (): string => {
  if (typeof window !== 'undefined') {
    const userStr = localStorage.getItem('user');
    if (userStr) {
      try {
        const user = JSON.parse(userStr);
        return user?.username || user?.name || '';
      } catch (error) {
        console.error('Failed to parse user from localStorage:', error);
      }
    }
  }
  return '';
};

/**
 * 知识库管理 API
 */
export const knowledgeApi = {
  /**
   * 获取租户下的所有知识库（管理员）
   */
  listByTenant: async (): Promise<KnowledgeBaseResponse[]> => {
    const tenantId = getTenantId();
    return knowledgeApiClient.get('/v1/kb/list', {
      headers: { 'X-Tenant-Id': tenantId }
    });
  },

  /**
   * 获取用户的知识库列表（普通用户）
   */
  listByOwner: async (ownerUserId: string): Promise<KnowledgeBaseResponse[]> => {
    const tenantId = getTenantId();
    return knowledgeApiClient.get(`/v1/kb/owner/${ownerUserId}`, {
      headers: { 'X-Tenant-Id': tenantId }
    });
  },

  /**
   * 获取知识库详情
   */
  get: async (kbId: string): Promise<KnowledgeBaseResponse> => {
    const tenantId = getTenantId();
    return knowledgeApiClient.get(`/v1/kb/${kbId}`, {
      headers: { 'X-Tenant-Id': tenantId }
    });
  },

  /**
   * 创建知识库
   */
  create: async (data: CreateKnowledgeBaseRequest): Promise<KnowledgeBaseResponse> => {
    const tenantId = getTenantId();
    const userId = getUserName();

    // 添加 ownerUserId 到请求数据
    const requestData = {
      ...data,
      ownerUserId: userId,
    };

    console.log('Creating knowledge base with data:', requestData);

    return knowledgeApiClient.post('/v1/kb', requestData, {
      headers: { 'X-Tenant-Id': tenantId }
    });
  },

  /**
   * 更新知识库
   */
  update: async (kbId: string, data: UpdateKnowledgeBaseRequest): Promise<boolean> => {
    const tenantId = getTenantId();
    return knowledgeApiClient.put(`/v1/kb/${kbId}`, data, {
      headers: { 'X-Tenant-Id': tenantId }
    });
  },

  /**
   * 删除知识库
   */
  delete: async (kbId: string): Promise<boolean> => {
    const tenantId = getTenantId();
    return knowledgeApiClient.delete(`/v1/kb/${kbId}`, {
      headers: { 'X-Tenant-Id': tenantId }
    });
  },
};
