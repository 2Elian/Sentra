import { knowledgeApiClient } from './axios';
import { Document, UploadDocumentRequest } from '@/types/knowledge';

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
 * 文档管理 API
 */
export const documentApi = {
  /**
   * 获取知识库下的文档列表
   */
  listByKbId: async (kbId: string): Promise<Document[]> => {
    return knowledgeApiClient.get('/v1/document/list', {
      params: { kbId },
    });
  },

  /**
   * 获取文档详情
   */
  get: async (documentId: string): Promise<Document> => {
    return knowledgeApiClient.get(`/v1/document/${documentId}`);
  },

  /**
   * 上传文档到知识库
   */
  upload: async (data: UploadDocumentRequest): Promise<Document> => {
    const formData = new FormData();
    formData.append('kbId', data.kbId);
    formData.append('file', data.file);

    if (data.entityTemplateId) {
      formData.append('entityTemplateId', data.entityTemplateId);
    }

    return knowledgeApiClient.post('/v1/document/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
  },

  /**
   * 删除文档
   */
  delete: async (documentId: string): Promise<boolean> => {
    return knowledgeApiClient.delete(`/v1/document/${documentId}`);
  },

  /**
   * 获取文档产物路径列表
   */
  getProductPaths: async (documentId: string, kbId: string): Promise<string[]> => {
    const response: any = await knowledgeApiClient.get(`/v1/document/product-path/${documentId}`, {
      params: { kbId }
    });

    // 后端返回 Result{code, message, data} 格式
    return response?.data || response;
  },
};
