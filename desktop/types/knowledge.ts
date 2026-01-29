/**
 * 知识库相关类型定义
 */

/**
 * 知识库作用域
 */
export enum KnowledgeScope {
  PUBLIC = 'PUBLIC',
  PRIVATE = 'PRIVATE',
}

/**
 * 知识库响应
 */
export interface KnowledgeBaseResponse {
  id: string;
  kbId: string;
  name: string;
  ownerUserId: string;
  scope: KnowledgeScope;
  description: string;
  tenantId: string;
  createdAt: string;
  updatedAt: string;
}

/**
 * 创建知识库请求
 */
export interface CreateKnowledgeBaseRequest {
  name: string;
  description: string;
  scope?: KnowledgeScope;
}

/**
 * 更新知识库请求
 */
export interface UpdateKnowledgeBaseRequest {
  name?: string;
  description?: string;
  scope?: KnowledgeScope;
}

/**
 * 文档状态
 */
export enum DocumentStatus {
  UPLOADED = 'UPLOADED',           // 已上传
  OCR_PROCESSING = 'OCR_PROCESSING', // OCR处理中
  OCR_COMPLETED = 'OCR_COMPLETED',   // OCR完成
  PROCESSING = 'PROCESSING',         // 知识图谱构建中
  COMPLETED = 'COMPLETED',           // 完成
  FAILED = 'FAILED',                 // 失败
}

/**
 * 文档
 */
export interface Document {
  id: string;
  documentUniqueId: string;
  kbId: string;
  filename: string;
  fileSize: number;
  fileType: string;
  remoteFilePath: string;
  status: DocumentStatus;
  progress: number;
  entityTemplateId?: string;
  tenantId: string;
  createdAt: string;
  updatedAt: string;
}

/**
 * 上传文档请求（FormData）
 */
export interface UploadDocumentRequest {
  kbId: string;
  entityTemplateId?: string;
  file: File;
}

/**
 * 实体类型模板响应
 */
export interface EntityTypeTemplateResponse {
  id: string;
  templateId: string;
  name: string;
  description?: string;
  tenantId?: string;
  isSystem: boolean;
  entityTypes: EntityType[];
  createdAt: string;
  updatedAt: string;
}

/**
 * 实体类型
 */
export interface EntityType {
  id: string;
  entityTypeId: string;
  name: string;
  entityName?: string;  // 后端字段名
  entityCode: string;  // 实体编码，必填
  description?: string;
  entityDescription?: string;  // 后端字段名
  color?: string;
  templateId: string;
  createdAt: string;
  updatedAt: string;
}

/**
 * 创建实体类型模板请求
 */
export interface CreateEntityTypeTemplateRequest {
  name: string;
  description?: string;
  entityTypes: Omit<EntityType, 'id' | 'entityTypeId' | 'templateId' | 'createdAt' | 'updatedAt'>[];
}

/**
 * 更新实体类型模板请求
 */
export interface UpdateEntityTypeTemplateRequest {
  name?: string;
  description?: string;
  entityTypes?: Omit<EntityType, 'id' | 'entityTypeId' | 'templateId' | 'createdAt' | 'updatedAt'>[];
}
