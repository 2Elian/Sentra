'use client';

import { useEffect, useState } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { MainLayout } from '@/components/layout/main-layout';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { knowledgeApi } from '@/lib/api/knowledgeApi';
import { documentApi } from '@/lib/api/documentApi';
import { entityTemplateApi } from '@/lib/api/entityTemplateApi';
import {
  KnowledgeBaseResponse,
  Document,
  DocumentStatus,
  EntityTypeTemplateResponse,
} from '@/types/knowledge';
import { DocumentProductsDialog } from '@/components/documents/products-dialog';

export default function KnowledgeBaseDetailPage() {
  const router = useRouter();
  const params = useParams();
  const kbId = params.kbId as string;

  const [knowledgeBase, setKnowledgeBase] = useState<KnowledgeBaseResponse | null>(null);
  const [documents, setDocuments] = useState<Document[]>([]);
  const [templates, setTemplates] = useState<EntityTypeTemplateResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const [selectedDocument, setSelectedDocument] = useState<Document | null>(null);
  const [isUploadDialogOpen, setIsUploadDialogOpen] = useState(false);
  const [isProductsDialogOpen, setIsProductsDialogOpen] = useState(false);
  const [selectedDocumentForProducts, setSelectedDocumentForProducts] = useState<Document | null>(null);

  // 上传表单
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [selectedTemplateId, setSelectedTemplateId] = useState<string>('');

  useEffect(() => {
    fetchKnowledgeBase();
    fetchDocuments();
    fetchTemplates();
  }, [kbId]);

  // 单独的轮询 useEffect，依赖于文档状态
  useEffect(() => {
    // 检查是否有正在处理的文档
    const hasProcessingDocuments = documents.some(
      doc => doc.status === DocumentStatus.UPLOADED ||
              doc.status === DocumentStatus.OCR_PROCESSING ||
              doc.status === DocumentStatus.PROCESSING
    );

    if (!hasProcessingDocuments) {
      return; // 如果没有处理中的文档，不启动轮询
    }

    // 设置轮询，每2秒更新一次文档状态
    const interval = setInterval(() => {
      fetchDocuments();
    }, 2000);

    // 清理函数：组件卸载或没有处理中的文档时清除定时器
    return () => clearInterval(interval);
  }, [documents]);

  const fetchKnowledgeBase = async () => {
    try {
      const response: any = await knowledgeApi.get(kbId);

      // 后端返回的是 Result{code, message, data} 格式，需要提取 data 字段
      const kb = response?.data || response;
      setKnowledgeBase(kb);
    } catch (error: any) {
      console.error('Failed to fetch knowledge base:', error);
      setError(error.message || '获取知识库信息失败');
    }
  };

  const fetchDocuments = async () => {
    setIsLoading(true);
    try {
      const response: any = await documentApi.listByKbId(kbId);

      // 后端返回的是 Result{code, message, data} 格式，需要提取 data 字段
      let docList: Document[] = [];
      if (response?.data && Array.isArray(response.data)) {
        docList = response.data;
      } else if (Array.isArray(response)) {
        docList = response;
      }

      console.log('Documents fetched:', docList);
      setDocuments(docList);
    } catch (error: any) {
      console.error('Failed to fetch documents:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const fetchTemplates = async () => {
    try {
      // 只需要获取用户模板，因为后端的 list() 接口已经包含了系统模板
      const response: any = await entityTemplateApi.list();

      // 后端返回的是 Result{code, message, data} 格式，需要提取 data 字段
      let templateList: EntityTypeTemplateResponse[] = [];
      if (response?.data && Array.isArray(response.data)) {
        templateList = response.data;
      } else if (Array.isArray(response)) {
        templateList = response;
      }

      // 使用 Map 去重（基于 id 字段，不是 templateId）
      const uniqueTemplates = Array.from(
        new Map(templateList.map((t: any) => [t.id || t.templateId, t])).values()
      );

      // 标准化字段名（后端返回 entityName/entityDescription，前端期望 name/description）
      const normalizedTemplates = uniqueTemplates.map((t: any) => ({
        ...t,
        entityTypes: t.entityTypes?.map((et: any) => ({
          ...et,
          name: et.entityName || et.name,
          description: et.entityDescription || et.description,
        })) || []
      }));

      console.log('Templates for upload dialog:', normalizedTemplates);
      setTemplates(normalizedTemplates);
    } catch (error: any) {
      console.error('Failed to fetch templates:', error);
    }
  };

  const handleUploadDocument = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!uploadFile) {
      alert('请选择文件');
      return;
    }

    setIsUploading(true);
    try {
      await documentApi.upload({
        kbId,
        entityTemplateId: selectedTemplateId || undefined,
        file: uploadFile,
      });

      // 重新获取文档列表
      await fetchDocuments();

      // 关闭对话框并重置表单
      setIsUploadDialogOpen(false);
      setUploadFile(null);
      setSelectedTemplateId('');
      (e.target as HTMLFormElement).reset();
    } catch (error: any) {
      console.error('Failed to upload document:', error);
      alert(error.message || '上传文档失败');
    } finally {
      setIsUploading(false);
    }
  };

  const handleDeleteDocument = async () => {
    if (!selectedDocument) return;

    try {
      await documentApi.delete(selectedDocument.id);
      await fetchDocuments();
      setIsDeleteDialogOpen(false);
      setSelectedDocument(null);
    } catch (error: any) {
      console.error('Failed to delete document:', error);
      alert(error.message || '删除文档失败');
    }
  };

  const getStatusBadge = (status: DocumentStatus) => {
    const statusMap = {
      [DocumentStatus.UPLOADED]: {
        label: '已上传',
        className: 'bg-blue-500/10 text-blue-600 dark:text-blue-400',
      },
      [DocumentStatus.OCR_PROCESSING]: {
        label: 'OCR处理中',
        className: 'bg-yellow-500/10 text-yellow-600 dark:text-yellow-400',
      },
      [DocumentStatus.OCR_COMPLETED]: {
        label: 'OCR完成',
        className: 'bg-purple-500/10 text-purple-600 dark:text-purple-400',
      },
      [DocumentStatus.PROCESSING]: {
        label: '处理中',
        className: 'bg-orange-500/10 text-orange-600 dark:text-orange-400',
      },
      [DocumentStatus.COMPLETED]: {
        label: '已完成',
        className: 'bg-green-500/10 text-green-600 dark:text-green-400',
      },
      [DocumentStatus.FAILED]: {
        label: '失败',
        className: 'bg-destructive/10 text-destructive',
      },
    };

    const { label, className } = statusMap[status] || statusMap[DocumentStatus.UPLOADED];

    return (
      <span className={`rounded px-2 py-0.5 text-xs ${className}`}>
        {label}
      </span>
    );
  };

  const getProgressWidth = (progress: number) => {
    return `${Math.min(100, Math.max(0, progress))}%`;
  };

  return (
    <MainLayout>
      <div className="space-y-6">
        {/* 返回按钮和标题 */}
        <div className="flex items-center gap-4">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => router.back()}
            className="h-9 w-9"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
          </Button>

          <div className="flex-1">
            <h1 className="text-3xl font-bold bg-gradient-to-r from-foreground to-primary/70 bg-clip-text text-transparent">
              {knowledgeBase?.name}
            </h1>
            <p className="text-sm text-muted-foreground mt-1">{knowledgeBase?.description}</p>
          </div>

          <Dialog open={isUploadDialogOpen} onOpenChange={setIsUploadDialogOpen}>
            <DialogTrigger asChild>
              <Button className="shadow-lg shadow-primary/20 hover:shadow-xl hover:shadow-primary/30 transition-all">
                <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                </svg>
                上传文档
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>上传文档</DialogTitle>
                <DialogDescription>
                  上传 PDF 文档到知识库，系统将自动进行解析和知识抽取
                </DialogDescription>
              </DialogHeader>
              <form onSubmit={handleUploadDocument}>
                <div className="space-y-4 py-4">
                  <div className="space-y-2">
                    <Label htmlFor="file">选择文件</Label>
                    <Input
                      id="file"
                      type="file"
                      accept=".pdf"
                      onChange={(e) => setUploadFile(e.target.files?.[0] || null)}
                      required
                      disabled={isUploading}
                    />
                    <p className="text-xs text-muted-foreground">仅支持 PDF 格式文件</p>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="template">选择实体类型模板</Label>
                    <select
                      id="template"
                      className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                      value={selectedTemplateId}
                      onChange={(e) => setSelectedTemplateId(e.target.value)}
                      disabled={isUploading}
                    >
                      <option value="">使用系统默认模板</option>
                      {templates.map((template) => {
                        // 使用 id 或 templateId 作为 key
                        const templateId = (template as any).id || template.templateId;
                        return (
                          <option key={templateId} value={templateId}>
                            {template.name} {template.isSystem ? '(系统)' : '(自定义)'}
                          </option>
                        );
                      })}
                    </select>
                    <p className="text-xs text-muted-foreground">
                      选择用于知识抽取的实体类型模板
                    </p>
                  </div>
                </div>

                <DialogFooter>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => {
                      setIsUploadDialogOpen(false);
                      setUploadFile(null);
                      setSelectedTemplateId('');
                    }}
                    disabled={isUploading}
                  >
                    取消
                  </Button>
                  <Button type="submit" disabled={isUploading}>
                    {isUploading ? '上传中...' : '上传'}
                  </Button>
                </DialogFooter>
              </form>
            </DialogContent>
          </Dialog>
        </div>

        {/* 文档列表 */}
        <Card className="shadow-lg border-border/50">
          <CardHeader className="border-b border-border/50">
            <div className="flex items-center justify-between">
              <div>
                <CardTitle className="text-xl">文档列表</CardTitle>
                <CardDescription className="mt-1">
                  共 {documents.length} 个文档
                </CardDescription>
              </div>
              {documents.length > 0 && (
                <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-primary/10 border border-primary/20">
                  <svg className="w-4 h-4 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                  </svg>
                  <span className="text-sm font-medium text-primary">{documents.length}</span>
                </div>
              )}
            </div>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="flex h-32 items-center justify-center">
                <div className="flex flex-col items-center gap-4">
                  <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
                  <p className="text-muted-foreground text-sm">加载中...</p>
                </div>
              </div>
            ) : documents.length === 0 ? (
              <div className="flex h-32 items-center justify-center">
                <div className="text-center space-y-3">
                  <div className="flex h-16 w-16 items-center justify-center rounded-full bg-muted/50 mx-auto">
                    <svg className="h-8 w-8 text-muted-foreground/50" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                  </div>
                  <p className="text-muted-foreground">暂无文档</p>
                  <p className="text-sm text-muted-foreground/70">点击上方按钮上传第一个文档</p>
                </div>
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>文件名</TableHead>
                    <TableHead>文件大小</TableHead>
                    <TableHead>状态</TableHead>
                    <TableHead>进度</TableHead>
                    <TableHead>上传时间</TableHead>
                    <TableHead>操作</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {documents.map((doc) => (
                    <TableRow key={doc.id}>
                      <TableCell className="font-medium">{doc.filename}</TableCell>
                      <TableCell>
                        {(doc.fileSize / 1024 / 1024).toFixed(2)} MB
                      </TableCell>
                      <TableCell>{getStatusBadge(doc.status)}</TableCell>
                      <TableCell>
                        <div className="flex items-center gap-3">
                          <div className="w-full bg-secondary rounded-full h-2 max-w-[120px]">
                            <div
                              className="bg-primary h-2 rounded-full transition-all duration-300"
                              style={{ width: getProgressWidth(doc.progress) }}
                            />
                          </div>
                          <span className="text-xs text-muted-foreground min-w-[40px]">
                            {doc.progress}%
                          </span>
                          {(doc.status === DocumentStatus.OCR_PROCESSING ||
                            doc.status === DocumentStatus.PROCESSING) && (
                            <div className="h-4 w-4 animate-spin rounded-full border-2 border-primary border-t-transparent" />
                          )}
                        </div>
                      </TableCell>
                      <TableCell>
                        {new Date(doc.createdAt).toLocaleString('zh-CN')}
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          {doc.status === DocumentStatus.COMPLETED && (
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => {
                                setSelectedDocumentForProducts(doc);
                                setIsProductsDialogOpen(true);
                              }}
                            >
                              <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                              </svg>
                              查看产物
                            </Button>
                          )}
                          <Button
                            variant="destructive"
                            size="sm"
                            onClick={() => {
                              setSelectedDocument(doc);
                              setIsDeleteDialogOpen(true);
                            }}
                          >
                            删除
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>

        {/* 删除确认对话框 */}
        <Dialog open={isDeleteDialogOpen} onOpenChange={setIsDeleteDialogOpen}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>确认删除</DialogTitle>
              <DialogDescription>
                确定要删除文档 &quot;{selectedDocument?.filename}&quot; 吗？
                此操作不可撤销，将同时删除相关的知识图谱数据。
              </DialogDescription>
            </DialogHeader>
            <DialogFooter>
              <Button variant="outline" onClick={() => setIsDeleteDialogOpen(false)}>
                取消
              </Button>
              <Button variant="destructive" onClick={handleDeleteDocument}>
                删除
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>

        {/* 文档产物查看对话框 */}
        {selectedDocumentForProducts && (
          <DocumentProductsDialog
            document={selectedDocumentForProducts}
            open={isProductsDialogOpen}
            onOpenChange={setIsProductsDialogOpen}
          />
        )}
      </div>
    </MainLayout>
  );
}
