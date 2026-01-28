'use client';

import { useEffect, useState } from 'react';
import { MainLayout } from '@/components/layout/main-layout';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { userApi } from '@/lib/api/userApi';
import { User, UserRole, CreateUserRequest, UserListVO } from '@/types/user';
import { useAuthStore } from '@/lib/stores/authStore';
import { ApiError } from '@/lib/api/axios';

export default function UsersPage() {
  const { user: currentUser } = useAuthStore();
  const [users, setUsers] = useState<UserListVO[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<UserListVO | null>(null);

  // 创建用户表单
  const [createFormData, setCreateFormData] = useState({
    username: '',
    password: '',
    role: UserRole.USER,
  });

  // 编辑用户表单
  const [editFormData, setEditFormData] = useState({
    username: '',
    password: '',
    role: UserRole.USER,
  });

  const fetchUsers = async () => {
    setIsLoading(true);
    setError(null);

    try {
      console.log('Fetching users from /v1/auth/users...');
      const response = await userApi.list();
      console.log('User list response:', response);
      setUsers(response);
    } catch (error: any) {
      console.error('Failed to fetch users:', error);
      if (error.statusCode === 500) {
        setError('后端服务错误，请检查后端日志或联系管理员');
      } else {
        setError(error.message || '获取用户列表失败');
      }
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const handleCreateUser = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      // 创建用户需要 tenantId，使用当前用户的租户ID
      const data = {
        username: createFormData.username,
        password: createFormData.password,
        tenantId: currentUser?.tenantId,
        role: createFormData.role,
      };

      console.log('Creating user with data:', data);
      await userApi.create(data);

      // 重新获取用户列表
      await fetchUsers();

      // 关闭对话框并重置表单
      setIsCreateDialogOpen(false);
      setCreateFormData({ username: '', password: '', role: UserRole.USER });
    } catch (error: any) {
      console.error('Failed to create user:', error);
      alert(error.message || '创建用户失败');
    }
  };

  const handleDeleteUser = async () => {
    if (!selectedUser) return;

    try {
      console.log('Deleting user with id:', selectedUser.id);
      await userApi.delete(selectedUser.id);

      // 重新获取用户列表
      await fetchUsers();

      // 关闭对话框
      setIsDeleteDialogOpen(false);
      setSelectedUser(null);
    } catch (error: any) {
      console.error('Failed to delete user:', error);
      alert(error.message || '删除用户失败');
    }
  };

  const handleEditUser = (user: UserListVO) => {
    setSelectedUser(user);
    setEditFormData({
      username: user.username,
      password: '',
      role: user.role as UserRole,
    });
    setIsEditDialogOpen(true);
  };

  const handleUpdateUser = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedUser?.id) return;

    try {
      const data = {
        username: editFormData.username,
        role: editFormData.role,
        ...(editFormData.password && { password: editFormData.password }),
      };

      console.log('Updating user:', selectedUser.id, 'with data:', data);
      await userApi.update(selectedUser.id, data);

      // 重新获取用户列表
      await fetchUsers();

      // 关闭对话框
      setIsEditDialogOpen(false);
      setSelectedUser(null);
      setEditFormData({ username: '', password: '', role: UserRole.USER });
    } catch (error: any) {
      console.error('Failed to update user:', error);
      alert(error.message || '更新用户失败');
    }
  };

  return (
    <MainLayout requireAdmin>
      <div className="space-y-6">
        {/* 页面头部 */}
        <div className="flex items-center justify-between">
          <div className="space-y-1">
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-lg bg-primary/10 flex items-center justify-center">
                <svg className="w-5 h-5 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
                </svg>
              </div>
              <div>
                <h1 className="text-3xl font-bold bg-gradient-to-r from-foreground to-primary/70 bg-clip-text text-transparent">
                  用户管理
                </h1>
                <p className="text-sm text-muted-foreground">管理当前租户的用户和权限</p>
              </div>
            </div>
          </div>

          <Dialog open={isCreateDialogOpen} onOpenChange={setIsCreateDialogOpen}>
            <DialogTrigger asChild>
              <Button className="shadow-lg shadow-primary/20 hover:shadow-xl hover:shadow-primary/30 transition-all">
                <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                </svg>
                创建用户
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>创建新用户</DialogTitle>
                <DialogDescription>
                  创建一个新用户，新用户将隶属于当前租户
                </DialogDescription>
              </DialogHeader>
              <form onSubmit={handleCreateUser}>
                <div className="space-y-4 py-4">
                  <div className="space-y-2">
                    <Label htmlFor="username">用户名</Label>
                    <Input
                      id="username"
                      value={createFormData.username}
                      onChange={(e) =>
                        setCreateFormData({ ...createFormData, username: e.target.value })
                      }
                      required
                    />
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="password">密码</Label>
                    <Input
                      id="password"
                      type="password"
                      value={createFormData.password}
                      onChange={(e) =>
                        setCreateFormData({ ...createFormData, password: e.target.value })
                      }
                      required
                    />
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="role">角色</Label>
                    <select
                      id="role"
                      className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                      value={createFormData.role}
                      onChange={(e) =>
                        setCreateFormData({
                          ...createFormData,
                          role: e.target.value as UserRole,
                        })
                      }
                    >
                      <option value={UserRole.USER}>普通用户</option>
                      <option value={UserRole.ADMIN}>管理员</option>
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

        <Card className="shadow-lg border-border/50">
          <CardHeader className="border-b border-border/50">
            <div className="flex items-center justify-between">
              <div>
                <CardTitle className="text-xl">用户列表</CardTitle>
                <CardDescription className="mt-1">
                  当前租户的所有用户（共 {users.length} 个）
                </CardDescription>
              </div>
              {users.length > 0 && (
                <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-primary/10 border border-primary/20">
                  <svg className="w-4 h-4 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                  </svg>
                  <span className="text-sm font-medium text-primary">{users.length}</span>
                </div>
              )}
            </div>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="flex h-32 items-center justify-center">
                <p className="text-muted-foreground">加载中...</p>
              </div>
            ) : error ? (
              <div className="flex h-32 items-center justify-center">
                <p className="text-destructive">{error}</p>
              </div>
            ) : users.length === 0 ? (
              <div className="flex h-32 items-center justify-center">
                <p className="text-muted-foreground">暂无用户</p>
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>用户名</TableHead>
                    <TableHead>角色</TableHead>
                    <TableHead>租户名称</TableHead>
                    <TableHead>创建时间</TableHead>
                    <TableHead>操作</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {users.map((user) => (
                    <TableRow key={user.username + user.tenantId}>
                      <TableCell className="font-medium">{user.username}</TableCell>
                      <TableCell>
                        {user.role === UserRole.ADMIN ? (
                          <span className="rounded bg-primary/10 px-2 py-0.5 text-xs text-primary">
                            管理员
                          </span>
                        ) : (
                          <span className="rounded bg-secondary px-2 py-0.5 text-xs text-secondary-foreground">
                            普通用户
                          </span>
                        )}
                      </TableCell>
                      <TableCell>{user.tenantName}</TableCell>
                      <TableCell>
                        {new Date(user.createdAt).toLocaleString('zh-CN')}
                      </TableCell>
                      <TableCell>
                        {user.username !== currentUser?.username ? (
                          <div className="flex gap-2">
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => handleEditUser(user)}
                            >
                              编辑
                            </Button>
                            <Button
                              variant="destructive"
                              size="sm"
                              onClick={() => {
                                setSelectedUser(user);
                                setIsDeleteDialogOpen(true);
                              }}
                            >
                              删除
                            </Button>
                          </div>
                        ) : (
                          <span className="text-xs text-muted-foreground">当前用户</span>
                        )}
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
                确定要删除用户 &quot;{selectedUser?.username}&quot; 吗？此操作不可撤销。
              </DialogDescription>
            </DialogHeader>
            <DialogFooter>
              <Button variant="outline" onClick={() => setIsDeleteDialogOpen(false)}>
                取消
              </Button>
              <Button variant="destructive" onClick={handleDeleteUser}>
                删除
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>

        {/* 编辑用户对话框 */}
        <Dialog open={isEditDialogOpen} onOpenChange={setIsEditDialogOpen}>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>编辑用户</DialogTitle>
              <DialogDescription>
                修改用户 &quot;{selectedUser?.username}&quot; 的信息
              </DialogDescription>
            </DialogHeader>
            <form onSubmit={handleUpdateUser}>
              <div className="space-y-4 py-4">
                <div className="space-y-2">
                  <Label htmlFor="edit-username">用户名</Label>
                  <Input
                    id="edit-username"
                    value={editFormData.username}
                    onChange={(e) =>
                      setEditFormData({ ...editFormData, username: e.target.value })
                    }
                    required
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="edit-password">新密码（留空则不修改）</Label>
                  <Input
                    id="edit-password"
                    type="password"
                    value={editFormData.password}
                    onChange={(e) =>
                      setEditFormData({ ...editFormData, password: e.target.value })
                    }
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="edit-role">角色</Label>
                  <select
                    id="edit-role"
                    className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                    value={editFormData.role}
                    onChange={(e) =>
                      setEditFormData({
                        ...editFormData,
                        role: e.target.value as UserRole,
                      })
                    }
                  >
                    <option value={UserRole.USER}>普通用户</option>
                    <option value={UserRole.ADMIN}>管理员</option>
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
