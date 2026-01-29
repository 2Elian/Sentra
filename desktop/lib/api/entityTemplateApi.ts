import { knowledgeApiClient } from './axios';
import {
  EntityTypeTemplateResponse,
  CreateEntityTypeTemplateRequest,
  UpdateEntityTypeTemplateRequest,
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
 * 实体类型模板管理 API
 */
export const entityTemplateApi = {
  /**
   * 获取租户的所有模板列表
   */
  list: async (): Promise<EntityTypeTemplateResponse[]> => {
    const tenantId = getTenantId();
    return knowledgeApiClient.get('/v1/knowledge/entity-template', {
      headers: { 'X-Tenant-Id': tenantId }
    });
  },

  /**
   * 获取系统预置模板列表
   */
  listSystem: async (): Promise<EntityTypeTemplateResponse[]> => {
    const tenantId = getTenantId();
    return knowledgeApiClient.get('/v1/knowledge/entity-template/system', {
      headers: { 'X-Tenant-Id': tenantId }
    });
  },

  /**
   * 获取模板详情（包含实体类型列表）
   */
  get: async (templateId: string): Promise<EntityTypeTemplateResponse> => {
    const tenantId = getTenantId();
    return knowledgeApiClient.get(`/v1/knowledge/entity-template/${templateId}`, {
      headers: { 'X-Tenant-Id': tenantId }
    });
  },

  /**
   * 创建实体类型模板
   */
  create: async (data: CreateEntityTypeTemplateRequest): Promise<EntityTypeTemplateResponse> => {
    const tenantId = getTenantId();
    const userId = getUserName();
    return knowledgeApiClient.post('/v1/knowledge/entity-template', data, {
      headers: { 'X-Tenant-Id': tenantId , "X-User-Id": userId}
    });
  },

  /**
   * 更新实体类型模板
   */
  update: async (templateId: string, data: UpdateEntityTypeTemplateRequest): Promise<void> => {
    const tenantId = getTenantId();
    const userId = getUserName();
    return knowledgeApiClient.put(`/v1/knowledge/entity-template/${templateId}`, data, {
      headers: { "X-User-Id": userId }
    });
  },

  /**
   * 删除实体类型模板
   */
  delete: async (templateId: string): Promise<void> => {
    const tenantId = getTenantId();
    return knowledgeApiClient.delete(`/v1/knowledge/entity-template/${templateId}`, {
      headers: { 'X-Tenant-Id': tenantId }
    });
  },
};
