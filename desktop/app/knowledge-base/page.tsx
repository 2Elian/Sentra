'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { MainLayout } from '@/components/layout/main-layout';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { knowledgeApi } from '@/lib/api/knowledgeApi';
import { KnowledgeBaseResponse, KnowledgeScope, CreateKnowledgeBaseRequest } from '@/types/knowledge';
import { useAuthStore } from '@/lib/stores/authStore';
import { ApiError } from '@/lib/api/axios';

export default function KnowledgeBasePage() {
  const router = useRouter();
  const { user, isAdmin } = useAuthStore();

  const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBaseResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const [selectedKb, setSelectedKb] = useState<KnowledgeBaseResponse | null>(null);

  // 创建知识库表单
  const [createFormData, setCreateFormData] = useState<CreateKnowledgeBaseRequest>({
    name: '',
    description: '',
    scope: KnowledgeScope.PUBLIC,
  });

  // 编辑知识库表单
  const [editFormData, setEditFormData] = useState<CreateKnowledgeBaseRequest>({
    name: '',
    description: '',
    scope: KnowledgeScope.PUBLIC,
  });

  const fetchKnowledgeBases = async () => {
    setIsLoading(true);
    setError(null);

    try {
      let response: any;
      if (isAdmin()) {
        // 管理员：获取租户下的所有知识库
        response = await knowledgeApi.listByTenant();
      } else {
        // 普通用户：获取自己的知识库
        response = await knowledgeApi.listByOwner(user!.username);
      }

      console.log('Knowledge bases response:', response);
      console.log('Response type:', typeof response);
      console.log('Is array?', Array.isArray(response));

      // Handle different response formats
      // 后端返回的是 Result{code, message, data} 格式
      let kbList: KnowledgeBaseResponse[];
      if (response?.data && Array.isArray(response.data)) {
        // Result格式: {code: 200, message: "success", data: [...]}
        kbList = response.data;
      } else if (Array.isArray(response)) {
        // 直接是数组
        kbList = response;
      } else {
        console.error('Unexpected response format:', response);
        kbList = [];
      }

      setKnowledgeBases(kbList);
    } catch (error: any) {
      console.error('Failed to fetch knowledge bases:', error);
      setError(error.message || '获取知识库列表失败');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchKnowledgeBases();
  }, []);

  const handleCreateKnowledgeBase = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      await knowledgeApi.create(createFormData);
      await fetchKnowledgeBases();
      setIsCreateDialogOpen(false);
      setCreateFormData({ name: '', description: '', scope: KnowledgeScope.PUBLIC });
    } catch (error: any) {
      console.error('Failed to create knowledge base:', error);
      alert(error.message || '创建知识库失败');
    }
  };

  const handleDeleteKnowledgeBase = async () => {
    if (!selectedKb) return;

    try {
      await knowledgeApi.delete(selectedKb.kbId);
      await fetchKnowledgeBases();
      setIsDeleteDialogOpen(false);
      setSelectedKb(null);
    } catch (error: any) {
      console.error('Failed to delete knowledge base:', error);
      alert(error.message || '删除知识库失败');
    }
  };

  const handleEditKnowledgeBase = (kb: KnowledgeBaseResponse) => {
    setSelectedKb(kb);
    setEditFormData({
      name: kb.name,
      description: kb.description,
      scope: kb.scope,
    });
    setIsEditDialogOpen(true);
  };

  const handleUpdateKnowledgeBase = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedKb?.kbId) return;

    try {
      await knowledgeApi.update(selectedKb.kbId, editFormData);
      await fetchKnowledgeBases();
      setIsEditDialogOpen(false);
      setSelectedKb(null);
    } catch (error: any) {
      console.error('Failed to update knowledge base:', error);
      alert(error.message || '更新知识库失败');
    }
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
                  知识库管理
                </h1>
                <p className="text-sm text-muted-foreground">
                  {isAdmin() ? '管理租户下的所有知识库' : '管理您的知识库'}
                </p>
              </div>
            </div>
          </div>

          <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
            <DialogTrigger asChild>
              <Button className="shadow-lg shadow-primary/20 hover:shadow-xl hover:shadow-primary/30 transition-all">
                <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                </svg>
                创建知识库
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>创建知识库</DialogTitle>
                <DialogDescription>
                  创建一个新的知识库来存储和管理您的文档
                </DialogDescription>
              </DialogHeader>
              <form onSubmit={handleCreateKnowledgeBase}>
                <div className="space-y-4 py-4">
                  <div className="space-y-2">
                    <Label htmlFor="kb-name">知识库名称</Label>
                    <Input
                      id="kb-name"
                      value={createFormData.name}
                      onChange={(e) =>
                        setCreateFormData({ ...createFormData, name: e.target.value })
                      }
                      placeholder="输入知识库名称"
                      required
                    />
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="kb-description">描述</Label>
                    <Input
                      id="kb-description"
                      value={createFormData.description}
                      onChange={(e) =>
                        setCreateFormData({ ...createFormData, description: e.target.value })
                      }
                      placeholder="输入知识库描述"
                      required
                    />
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="kb-scope">作用域</Label>
                    <select
                      id="kb-scope"
                      className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                      value={createFormData.scope}
                      onChange={(e) =>
                        setCreateFormData({
                          ...createFormData,
                          scope: e.target.value as KnowledgeScope,
                        })
                      }
                    >
                      <option value={KnowledgeScope.PUBLIC}>公开</option>
                      <option value={KnowledgeScope.PRIVATE}>私有</option>
                    </select>
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
        </div>

        {/* 知识库列表 */}
        <Card className="shadow-lg border-border/50">
          <CardHeader className="border-b border-border/50">
            <div className="flex items-center justify-between">
              <div>
                <CardTitle className="text-xl">知识库列表</CardTitle>
                <CardDescription className="mt-1">
                  共 {knowledgeBases.length} 个知识库
                </CardDescription>
              </div>
              {knowledgeBases.length > 0 && (
                <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-primary/10 border border-primary/20">
                  <svg className="w-4 h-4 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                  </svg>
                  <span className="text-sm font-medium text-primary">{knowledgeBases.length}</span>
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
            ) : error ? (
              <div className="flex h-32 items-center justify-center">
                <div className="text-center space-y-2">
                  <svg className="mx-auto h-12 w-12 text-destructive/50" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                  </svg>
                  <p className="text-destructive">{error}</p>
                  <Button variant="outline" size="sm" onClick={fetchKnowledgeBases}>
                    重试
                  </Button>
                </div>
              </div>
            ) : knowledgeBases.length === 0 ? (
              <div className="flex h-32 items-center justify-center">
                <div className="text-center space-y-3">
                  <div className="mx-flex h-16 w-16 items-center justify-center rounded-full bg-muted/50">
                    <svg className="mx-auto h-8 w-8 text-muted-foreground/50" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                    </svg>
                  </div>
                  <p className="text-muted-foreground">暂无知识库</p>
                  <p className="text-sm text-muted-foreground/70">点击上方按钮创建第一个知识库</p>
                </div>
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>知识库名称</TableHead>
                    <TableHead>描述</TableHead>
                    <TableHead>作用域</TableHead>
                    <TableHead>所有者</TableHead>
                    <TableHead>创建时间</TableHead>
                    <TableHead>操作</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {knowledgeBases.map((kb) => (
                    <TableRow key={kb.kbId}>
                      <TableCell className="font-medium">{kb.name}</TableCell>
                      <TableCell className="text-muted-foreground max-w-md truncate">{kb.description}</TableCell>
                      <TableCell>
                        {kb.scope === KnowledgeScope.PUBLIC ? (
                          <span className="rounded bg-green-500/10 px-2 py-0.5 text-xs text-green-600 dark:text-green-400">
                            公开
                          </span>
                        ) : (
                          <span className="rounded bg-secondary px-2 py-0.5 text-xs text-secondary-foreground">
                            私有
                          </span>
                        )}
                      </TableCell>
                      <TableCell>{kb.ownerUserId}</TableCell>
                      <TableCell>
                        {new Date(kb.createdAt).toLocaleString('zh-CN')}
                      </TableCell>
                      <TableCell>
                        <div className="flex gap-2">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => router.push(`/knowledge-base/${kb.kbId}`)}
                          >
                            查看
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleEditKnowledgeBase(kb)}
                          >
                            编辑
                          </Button>
                          <Button
                            variant="destructive"
                            size="sm"
                            onClick={() => {
                              setSelectedKb(kb);
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
                确定要删除知识库 &quot;{selectedKb?.name}&quot; 吗？此操作不可撤销，知识库下的所有文档也将被删除。
              </DialogDescription>
            </DialogHeader>
            <DialogFooter>
              <Button variant="outline" onClick={() => setIsDeleteDialogOpen(false)}>
                取消
              </Button>
              <Button variant="destructive" onClick={handleDeleteKnowledgeBase}>
                删除
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>

        {/* 编辑知识库对话框 */}
        <Dialog open={isEditDialogOpen} onOpenChange={setIsEditDialogOpen}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>编辑知识库</DialogTitle>
              <DialogDescription>
                修改知识库 &quot;{selectedKb?.name}&quot; 的信息
              </DialogDescription>
            </DialogHeader>
            <form onSubmit={handleUpdateKnowledgeBase}>
              <div className="space-y-4 py-4">
                <div className="space-y-2">
                  <Label htmlFor="edit-kb-name">知识库名称</Label>
                  <Input
                    id="edit-kb-name"
                    value={editFormData.name}
                    onChange={(e) =>
                      setEditFormData({ ...editFormData, name: e.target.value })
                    }
                    required
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="edit-kb-description">描述</Label>
                  <Input
                    id="edit-kb-description"
                    value={editFormData.description}
                    onChange={(e) =>
                      setEditFormData({ ...editFormData, description: e.target.value })
                    }
                    required
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="edit-kb-scope">作用域</Label>
                  <select
                    id="edit-kb-scope"
                    className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                    value={editFormData.scope}
                    onChange={(e) =>
                      setEditFormData({
                        ...editFormData,
                        scope: e.target.value as KnowledgeScope,
                      })
                    }
                  >
                    <option value={KnowledgeScope.PUBLIC}>公开</option>
                    <option value={KnowledgeScope.PRIVATE}>私有</option>
                  </select>
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
    </MainLayout>
  );
}
