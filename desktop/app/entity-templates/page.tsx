'use client';

import { useEffect, useState } from 'react';
import { MainLayout } from '@/components/layout/main-layout';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { entityTemplateApi } from '@/lib/api/entityTemplateApi';
import {
  EntityTypeTemplateResponse,
  CreateEntityTypeTemplateRequest,
  EntityType,
} from '@/types/knowledge';

export default function EntityTemplatesPage() {
  const [templates, setTemplates] = useState<EntityTypeTemplateResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [isViewDialogOpen, setIsViewDialogOpen] = useState(false);
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const [selectedTemplate, setSelectedTemplate] = useState<EntityTypeTemplateResponse | null>(null);

  // 创建表单
  const [createFormData, setCreateFormData] = useState<CreateEntityTypeTemplateRequest>({
    name: '',
    description: '',
    entityTypes: [{ name: '', entityCode: '', description: '', color: '#3B82F6' }],
  });

  // 编辑表单
  const [editFormData, setEditFormData] = useState<CreateEntityTypeTemplateRequest>({
    name: '',
    description: '',
    entityTypes: [{ name: '', entityCode: '', description: '', color: '#3B82F6' }],
  });

  const fetchTemplates = async () => {
    setIsLoading(true);
    setError(null);

    try {
      // 只获取用户模板（已经包含了系统模板）
      const response = await entityTemplateApi.list();

      console.log('Templates response:', response);

      // Handle different response formats
      // 后端返回的是 Result{code, message, data} 格式
      let templateList: EntityTypeTemplateResponse[] = [];
      if (response?.data && Array.isArray(response.data)) {
        // Result格式: {code: 200, message: "success", data: [...]}
        templateList = response.data;
      } else if (Array.isArray(response)) {
        // 直接是数组
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

      console.log('Unique templates:', uniqueTemplates);
      console.log('Normalized templates:', normalizedTemplates);

      // 打印每个模板的 isSystem 值，用于调试
      normalizedTemplates.forEach((t: any) => {
        console.log(`Template "${t.name}": isSystem=${t.isSystem}, id=${t.id}`);
      });

      setTemplates(normalizedTemplates);
    } catch (error: any) {
      console.error('Failed to fetch templates:', error);
      setError(error.message || '获取模板列表失败');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchTemplates();
  }, []);

  const handleCreateTemplate = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      // 转换字段名以匹配后端期望的格式
      const requestData = {
        name: createFormData.name,
        description: createFormData.description,
        entityTypes: createFormData.entityTypes.map(et => ({
          entityCode: et.entityCode,
          entityName: et.name,  // 前端的 name -> 后端的 entityName
          entityDescription: et.description,  // 前端的 description -> 后端的 entityDescription
          color: et.color,
        }))
      };

      console.log('Creating template with data:', requestData);

      await entityTemplateApi.create(requestData);
      await fetchTemplates();
      setIsCreateDialogOpen(false);
      setCreateFormData({
        name: '',
        description: '',
        entityTypes: [{ name: '', entityCode: '', description: '', color: '#3B82F6' }],
      });
    } catch (error: any) {
      console.error('Failed to create template:', error);
      alert(error.message || '创建模板失败');
    }
  };

  const handleDeleteTemplate = async () => {
    if (!selectedTemplate) return;

    try {
      // 后端字段名是 id，不是 templateId
      const templateId = (selectedTemplate as any).id || selectedTemplate.templateId;
      if (!templateId) {
        throw new Error('模板ID不存在');
      }

      await entityTemplateApi.delete(templateId);
      await fetchTemplates();
      setIsDeleteDialogOpen(false);
      setSelectedTemplate(null);
    } catch (error: any) {
      console.error('Failed to delete template:', error);
      alert(error.message || '删除模板失败');
    }
  };

  const handleEditTemplate = async (template: EntityTypeTemplateResponse) => {
    try {
      // 需要获取完整的模板详情，因为列表中的数据可能不完整
      const templateId = (template as any).id || template.templateId;
      if (!templateId) {
        throw new Error('模板ID不存在');
      }

      console.log('Editing template, fetching full detail for ID:', templateId);

      const response: any = await entityTemplateApi.get(templateId);
      console.log('Raw API response for edit:', response);

      // 后端返回的是 Result{code, message, data} 格式，需要提取 data 字段
      const detail = response?.data || response;
      console.log('Extracted detail for edit:', detail);

      // 处理后端返回的字段名映射
      const normalizedDetail = {
        ...detail,
        entityTypes: detail.entityTypes?.map((et: any) => ({
          ...et,
          name: et.entityName || et.name,
          entityCode: et.entityCode,
          description: et.entityDescription || et.description,
          color: et.color || '#3B82F6',
        })) || []
      };

      console.log('Normalized detail for edit:', normalizedDetail);

      setSelectedTemplate(normalizedDetail);
      setEditFormData({
        name: normalizedDetail.name,
        description: normalizedDetail.description || '',
        entityTypes: normalizedDetail.entityTypes.map((et: any) => ({
          name: et.name,
          entityCode: et.entityCode,
          description: et.description || '',
          color: et.color || '#3B82F6',
        })),
      });
      setIsEditDialogOpen(true);
    } catch (error: any) {
      console.error('Failed to fetch template for editing:', error);
      alert(error.message || '获取模板详情失败');
    }
  };

  const handleUpdateTemplate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedTemplate) return;

    try {
      const templateId = (selectedTemplate as any).id || selectedTemplate.templateId;
      if (!templateId) {
        throw new Error('模板ID不存在');
      }

      // 转换字段名以匹配后端期望的格式
      const requestData = {
        name: editFormData.name,
        description: editFormData.description,
        entityTypes: editFormData.entityTypes.map(et => ({
          entityCode: et.entityCode,
          entityName: et.name,  // 前端的 name -> 后端的 entityName
          entityDescription: et.description,  // 前端的 description -> 后端的 entityDescription
          color: et.color,
        }))
      };

      console.log('Updating template with data:', requestData);

      await entityTemplateApi.update(templateId, requestData);
      await fetchTemplates();
      setIsEditDialogOpen(false);
      setSelectedTemplate(null);
    } catch (error: any) {
      console.error('Failed to update template:', error);
      alert(error.message || '更新模板失败');
    }
  };

  const handleViewTemplate = async (template: EntityTypeTemplateResponse) => {
    try {
      console.log('Template object:', template);

      // 后端字段名是 id，不是 templateId
      const templateId = (template as any).id || template.templateId;
      if (!templateId) {
        throw new Error('模板ID不存在');
      }

      console.log('Fetching template detail for ID:', templateId);

      const response: any = await entityTemplateApi.get(templateId);
      console.log('Raw API response:', response);

      // 后端返回的是 Result{code, message, data} 格式，需要提取 data 字段
      const detail = response?.data || response;

      console.log('Extracted detail:', detail);

      // 处理后端返回的字段名映射
      const normalizedDetail = {
        ...detail,
        entityTypes: detail.entityTypes?.map((et: any) => ({
          ...et,
          name: et.entityName || et.name,
          description: et.entityDescription || et.description,
        })) || []
      };

      console.log('Normalized detail:', normalizedDetail);

      setSelectedTemplate(normalizedDetail);
      setIsViewDialogOpen(true);
    } catch (error: any) {
      console.error('Failed to fetch template detail:', error);
      alert(error.message || '获取模板详情失败');
    }
  };

  const addEntityType = () => {
    setCreateFormData({
      ...createFormData,
      entityTypes: [...createFormData.entityTypes, { name: '', entityCode: '', description: '', color: '#3B82F6' }],
    });
  };

  const removeEntityType = (index: number) => {
    setCreateFormData({
      ...createFormData,
      entityTypes: createFormData.entityTypes.filter((_, i) => i !== index),
    });
  };

  const updateEntityType = (index: number, field: string, value: string) => {
    const updatedEntityTypes = [...createFormData.entityTypes];
    updatedEntityTypes[index] = { ...updatedEntityTypes[index], [field]: value };
    setCreateFormData({ ...createFormData, entityTypes: updatedEntityTypes });
  };

  const addEditEntityType = () => {
    setEditFormData({
      ...editFormData,
      entityTypes: [...editFormData.entityTypes, { name: '', entityCode: '', description: '', color: '#3B82F6' }],
    });
  };

  const removeEditEntityType = (index: number) => {
    setEditFormData({
      ...editFormData,
      entityTypes: editFormData.entityTypes.filter((_, i) => i !== index),
    });
  };

  const updateEditEntityType = (index: number, field: string, value: string) => {
    const updatedEntityTypes = [...editFormData.entityTypes];
    updatedEntityTypes[index] = { ...updatedEntityTypes[index], [field]: value };
    setEditFormData({ ...editFormData, entityTypes: updatedEntityTypes });
  };

  return (
    <MainLayout>
      <div className="space-y-6">
        {/* 页面头部 */}
        <div className="flex items-center justify-between">
          <div className="space-y-1">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-lg bg-primary/10 flex items-center justify-center">
                <svg className="w-5 h-5 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                </svg>
              </div>
              <div>
                <h1 className="text-3xl font-bold bg-gradient-to-r from-foreground to-primary/70 bg-clip-text text-transparent">
                  实体类型模板
                </h1>
                <p className="text-sm text-muted-foreground">管理知识抽取的实体类型模板</p>
              </div>
            </div>
          </div>

          <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
            <DialogTrigger asChild>
              <Button className="shadow-lg shadow-primary/20 hover:shadow-xl hover:shadow-primary/30 transition-all">
                <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                </svg>
                创建模板
              </Button>
            </DialogTrigger>
            <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
              <DialogHeader>
                <DialogTitle>创建实体类型模板</DialogTitle>
                <DialogDescription>
                  定义用于知识抽取的实体类型，例如：人物、组织、时间等
                </DialogDescription>
              </DialogHeader>
              <form onSubmit={handleCreateTemplate}>
                <div className="space-y-4 py-4">
                  <div className="space-y-2">
                    <Label htmlFor="template-name">模板名称</Label>
                    <Input
                      id="template-name"
                      value={createFormData.name}
                      onChange={(e) =>
                        setCreateFormData({ ...createFormData, name: e.target.value })
                      }
                      placeholder="输入模板名称"
                      required
                    />
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="template-description">描述</Label>
                    <Input
                      id="template-description"
                      value={createFormData.description}
                      onChange={(e) =>
                        setCreateFormData({ ...createFormData, description: e.target.value })
                      }
                      placeholder="输入模板描述"
                    />
                  </div>

                  <div className="space-y-3">
                    <div className="flex items-center justify-between">
                      <Label>实体类型</Label>
                      <Button type="button" variant="outline" size="sm" onClick={addEntityType}>
                        <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                        </svg>
                        添加实体
                      </Button>
                    </div>

                    {createFormData.entityTypes.map((entity, index) => (
                      <div key={index} className="space-y-2 p-4 border rounded-lg">
                        <div className="flex items-center justify-between mb-2">
                          <span className="text-sm font-medium">实体 {index + 1}</span>
                          {createFormData.entityTypes.length > 1 && (
                            <Button
                              type="button"
                              variant="ghost"
                              size="sm"
                              onClick={() => removeEntityType(index)}
                            >
                              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                              </svg>
                            </Button>
                          )}
                        </div>

                        <div className="grid grid-cols-2 gap-3">
                          <div className="space-y-1">
                            <Label htmlFor={`entity-name-${index}`} className="text-xs">名称</Label>
                            <Input
                              id={`entity-name-${index}`}
                              value={entity.name}
                              onChange={(e) => updateEntityType(index, 'name', e.target.value)}
                              placeholder="例如：人物"
                              required
                            />
                          </div>

                          <div className="space-y-1">
                            <Label htmlFor={`entity-code-${index}`} className="text-xs">编码</Label>
                            <Input
                              id={`entity-code-${index}`}
                              value={(entity as any).entityCode || ''}
                              onChange={(e) => updateEntityType(index, 'entityCode', e.target.value)}
                              placeholder="例如：PERSON"
                              required
                            />
                          </div>
                        </div>

                        <div className="grid grid-cols-2 gap-3">
                          <div className="space-y-1">
                            <Label htmlFor={`entity-color-${index}`} className="text-xs">颜色</Label>
                            <div className="flex gap-2">
                              <Input
                                id={`entity-color-${index}`}
                                type="color"
                                value={entity.color}
                                onChange={(e) => updateEntityType(index, 'color', e.target.value)}
                                className="w-16 h-10 p-1"
                              />
                              <Input
                                value={entity.color}
                                onChange={(e) => updateEntityType(index, 'color', e.target.value)}
                                placeholder="#3B82F6"
                                className="flex-1"
                              />
                            </div>
                          </div>

                          <div className="space-y-1">
                            <Label htmlFor={`entity-desc-${index}`} className="text-xs">描述</Label>
                            <Input
                              id={`entity-desc-${index}`}
                              value={entity.description}
                              onChange={(e) => updateEntityType(index, 'description', e.target.value)}
                              placeholder="例如：文档中的人物实体"
                            />
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                <DialogFooter>
                  <Button type="button" variant="outline" onClick={() => setIsCreateDialogOpen(false)}>
                    取消
                  </Button>
                  <Button type="submit">创建</Button>
                </DialogFooter>
              </form>
            </DialogContent>
          </Dialog>

          <Dialog open={isEditDialogOpen} onOpenChange={setIsEditDialogOpen}>
            <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
              <DialogHeader>
                <DialogTitle>编辑实体类型模板</DialogTitle>
                <DialogDescription>
                  修改模板名称、描述和实体类型定义
                </DialogDescription>
              </DialogHeader>
              <form onSubmit={handleUpdateTemplate}>
                <div className="space-y-4 py-4">
                  <div className="space-y-2">
                    <Label htmlFor="edit-template-name">模板名称</Label>
                    <Input
                      id="edit-template-name"
                      value={editFormData.name}
                      onChange={(e) =>
                        setEditFormData({ ...editFormData, name: e.target.value })
                      }
                      placeholder="输入模板名称"
                      required
                    />
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="edit-template-description">描述</Label>
                    <Input
                      id="edit-template-description"
                      value={editFormData.description}
                      onChange={(e) =>
                        setEditFormData({ ...editFormData, description: e.target.value })
                      }
                      placeholder="输入模板描述"
                    />
                  </div>

                  <div className="space-y-3">
                    <div className="flex items-center justify-between">
                      <Label>实体类型</Label>
                      <Button type="button" variant="outline" size="sm" onClick={addEditEntityType}>
                        <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                        </svg>
                        添加实体
                      </Button>
                    </div>

                    {editFormData.entityTypes.map((entity, index) => (
                      <div key={index} className="space-y-2 p-4 border rounded-lg">
                        <div className="flex items-center justify-between mb-2">
                          <span className="text-sm font-medium">实体 {index + 1}</span>
                          {editFormData.entityTypes.length > 1 && (
                            <Button
                              type="button"
                              variant="ghost"
                              size="sm"
                              onClick={() => removeEditEntityType(index)}
                            >
                              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                              </svg>
                            </Button>
                          )}
                        </div>

                        <div className="grid grid-cols-2 gap-3">
                          <div className="space-y-1">
                            <Label htmlFor={`edit-entity-name-${index}`} className="text-xs">名称</Label>
                            <Input
                              id={`edit-entity-name-${index}`}
                              value={entity.name}
                              onChange={(e) => updateEditEntityType(index, 'name', e.target.value)}
                              placeholder="例如：人物"
                              required
                            />
                          </div>

                          <div className="space-y-1">
                            <Label htmlFor={`edit-entity-code-${index}`} className="text-xs">编码</Label>
                            <Input
                              id={`edit-entity-code-${index}`}
                              value={entity.entityCode}
                              onChange={(e) => updateEditEntityType(index, 'entityCode', e.target.value)}
                              placeholder="例如：PERSON"
                              required
                            />
                          </div>
                        </div>

                        <div className="grid grid-cols-2 gap-3">
                          <div className="space-y-1">
                            <Label htmlFor={`edit-entity-color-${index}`} className="text-xs">颜色</Label>
                            <div className="flex gap-2">
                              <Input
                                id={`edit-entity-color-${index}`}
                                type="color"
                                value={entity.color}
                                onChange={(e) => updateEditEntityType(index, 'color', e.target.value)}
                                className="w-16 h-10 p-1"
                              />
                              <Input
                                value={entity.color}
                                onChange={(e) => updateEditEntityType(index, 'color', e.target.value)}
                                placeholder="#3B82F6"
                                className="flex-1"
                              />
                            </div>
                          </div>

                          <div className="space-y-1">
                            <Label htmlFor={`edit-entity-desc-${index}`} className="text-xs">描述</Label>
                            <Input
                              id={`edit-entity-desc-${index}`}
                              value={entity.description}
                              onChange={(e) => updateEditEntityType(index, 'description', e.target.value)}
                              placeholder="例如：文档中的人物实体"
                            />
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                <DialogFooter>
                  <Button type="button" variant="outline" onClick={() => setIsEditDialogOpen(false)}>
                    取消
                  </Button>
                  <Button type="submit">保存</Button>
                </DialogFooter>
              </form>
            </DialogContent>
          </Dialog>
        </div>

        {/* 模板列表 */}
        <Card className="shadow-lg border-border/50">
          <CardHeader className="border-b border-border/50">
            <div className="flex items-center justify-between">
              <div>
                <CardTitle className="text-xl">模板列表</CardTitle>
                <CardDescription className="mt-1">
                  共 {templates.length} 个模板
                </CardDescription>
              </div>
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
            ) : templates.length === 0 ? (
              <div className="flex h-32 items-center justify-center">
                <div className="text-center space-y-3">
                  <p className="text-muted-foreground">暂无模板</p>
                </div>
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>模板名称</TableHead>
                    <TableHead>描述</TableHead>
                    <TableHead>实体数量</TableHead>
                    <TableHead>类型</TableHead>
                    <TableHead>操作</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {templates.map((template) => {
                    // 后端字段名是 id，不是 templateId
                    const templateId = (template as any).id || template.templateId || Math.random().toString();
                    return (
                      <TableRow key={templateId}>
                        <TableCell className="font-medium">{template.name}</TableCell>
                      <TableCell className="text-muted-foreground max-w-md truncate">
                        {template.description || '-'}
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <span className="text-sm">{template.entityTypes?.length || 0}</span>
                          <div className="flex -space-x-1">
                            {(template.entityTypes || []).slice(0, 3).map((entity, idx) => (
                              <div
                                key={idx}
                                className="w-5 h-5 rounded-full border-2 border-background"
                                style={{ backgroundColor: entity.color }}
                                title={entity.name}
                              />
                            ))}
                          </div>
                        </div>
                      </TableCell>
                      <TableCell>
                        {template.isSystem ? (
                          <span className="rounded bg-purple-500/10 px-2 py-0.5 text-xs text-purple-600 dark:text-purple-400">
                            系统
                          </span>
                        ) : (
                          <span className="rounded bg-secondary px-2 py-0.5 text-xs text-secondary-foreground">
                            自定义
                          </span>
                        )}
                      </TableCell>
                      <TableCell>
                        <div className="flex gap-2">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleViewTemplate(template)}
                          >
                            查看
                          </Button>
                          {!template.isSystem && (
                            <>
                              <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => handleEditTemplate(template)}
                              >
                                编辑
                              </Button>
                              <Button
                                variant="destructive"
                                size="sm"
                                onClick={() => {
                                  setSelectedTemplate(template);
                                  setIsDeleteDialogOpen(true);
                                }}
                              >
                                删除
                              </Button>
                            </>
                          )}
                        </div>
                      </TableCell>
                    </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>

        {/* 查看模板详情对话框 */}
        <Dialog open={isViewDialogOpen} onOpenChange={setIsViewDialogOpen}>
          <DialogContent className="max-w-4xl max-h-[80vh] overflow-y-auto">
            <DialogHeader>
              <div className="flex items-start gap-3">
                <div className="h-12 w-12 rounded-lg bg-primary/10 flex items-center justify-center flex-shrink-0">
                  <svg className="w-6 h-6 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                  </svg>
                </div>
                <div className="flex-1">
                  <DialogTitle className="text-2xl">{selectedTemplate?.name}</DialogTitle>
                  <DialogDescription className="mt-2 text-base">
                    {selectedTemplate?.description || '暂无描述'}
                  </DialogDescription>
                </div>
              </div>
            </DialogHeader>
            <div className="space-y-4 py-6">
              <div className="flex items-center justify-between">
                <h4 className="text-lg font-semibold">实体类型列表</h4>
                <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-primary/10 border border-primary/20">
                  <svg className="w-4 h-4 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                  </svg>
                  <span className="text-sm font-semibold text-primary">{selectedTemplate?.entityTypes?.length || 0}</span>
                </div>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                {(selectedTemplate?.entityTypes || []).map((entity, idx) => (
                  <div
                    key={idx}
                    className="group relative p-4 rounded-xl border-2 border-border/50 bg-card hover:border-primary/30 hover:shadow-md transition-all duration-200"
                  >
                    <div className="flex items-start gap-3">
                      <div
                        className="w-10 h-10 rounded-lg flex items-center justify-center flex-shrink-0 ring-2 ring-background"
                        style={{ backgroundColor: entity.color || '#3B82F6' }}
                      >
                        <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
                        </svg>
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="font-semibold text-base mb-1">{entity.name}</div>
                        <div className="text-sm text-muted-foreground mb-2 font-mono bg-muted/50 px-2 py-0.5 rounded inline-block">
                          {entity.entityCode}
                        </div>
                        <div className="text-sm text-muted-foreground line-clamp-2">
                          {entity.description || '无描述'}
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
            <DialogFooter>
              <Button
                onClick={() => setIsViewDialogOpen(false)}
                className="min-w-[100px]"
              >
                关闭
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>

        {/* 删除确认对话框 */}
        <Dialog open={isDeleteDialogOpen} onOpenChange={setIsDeleteDialogOpen}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>确认删除</DialogTitle>
              <DialogDescription>
                确定要删除模板 &quot;{selectedTemplate?.name}&quot; 吗？此操作不可撤销。
              </DialogDescription>
            </DialogHeader>
            <DialogFooter>
              <Button variant="outline" onClick={() => setIsDeleteDialogOpen(false)}>
                取消
              </Button>
              <Button variant="destructive" onClick={handleDeleteTemplate}>
                删除
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>
    </MainLayout>
  );
}
