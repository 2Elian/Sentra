'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { MainLayout } from '@/components/layout/main-layout';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { useAuthStore } from '@/lib/stores/authStore';
import { userApi } from '@/lib/api/userApi';
import { knowledgeApi } from '@/lib/api/knowledgeApi';

export default function DashboardPage() {
  const router = useRouter();
  const { user, isAuthenticated, isAdmin } = useAuthStore();

  // 统计数据
  const [userCount, setUserCount] = useState<number>(0);
  const [kbCount, setKbCount] = useState<number>(0);
  const [isLoading, setIsLoading] = useState(true);

  // 未登录则跳转到登录页
  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/login');
    }
  }, [isAuthenticated, router]);

  // 获取统计数据
  useEffect(() => {
    const fetchStats = async () => {
      if (!isAuthenticated) return;

      try {
        // 获取用户列表（仅管理员）
        if (isAdmin()) {
          const users = await userApi.list();
          setUserCount(users.length);
        }

        // 获取知识库列表
        let kbs;
        if (isAdmin()) {
          kbs = await knowledgeApi.listByTenant();
        } else {
          kbs = await knowledgeApi.listByOwner(user!.username);
        }
        setKbCount(kbs.length);
      } catch (error) {
        console.error('Failed to fetch stats:', error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchStats();
  }, [isAuthenticated, isAdmin, user]);

  return (
    <MainLayout>
      <div className="space-y-6">
        <div>
          <h1 className="text-3xl font-bold">仪表盘</h1>
          <p className="text-muted-foreground">欢迎回来，{user?.username}</p>
        </div>

        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">知识库总数</CardTitle>
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                className="h-4 w-4 text-muted-foreground"
              >
                <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" />
                <polyline points="3.27 6.96 12 12.01 20.73 6.96" />
                <line x1="12" y1="22.08" x2="12" y2="12" />
              </svg>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{isLoading ? '-' : kbCount}</div>
              <p className="text-xs text-muted-foreground">当前租户的知识库</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">文档总数</CardTitle>
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                className="h-4 w-4 text-muted-foreground"
              >
                <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z" />
                <polyline points="14 2 14 8 20 8" />
              </svg>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">0</div>
              <p className="text-xs text-muted-foreground">所有知识库的文档</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">处理中</CardTitle>
              <svg
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                className="h-4 w-4 text-muted-foreground"
              >
                <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83" />
              </svg>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">0</div>
              <p className="text-xs text-muted-foreground">正在处理的文档</p>
            </CardContent>
          </Card>

          {isAdmin() && (
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-sm font-medium">用户总数</CardTitle>
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2"
                  className="h-4 w-4 text-muted-foreground"
                >
                  <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                  <circle cx="9" cy="7" r="4" />
                  <path d="M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" />
                </svg>
              </CardHeader>
              <CardContent>
                <div className="text-2xl font-bold">{isLoading ? '-' : userCount}</div>
                <p className="text-xs text-muted-foreground">当前租户的用户</p>
              </CardContent>
            </Card>
          )}
        </div>

        <div className="grid gap-6 md:grid-cols-2">
          <Card>
            <CardHeader>
              <CardTitle>快速操作</CardTitle>
              <CardDescription>常用功能快捷入口</CardDescription>
            </CardHeader>
            <CardContent className="space-y-2">
              <a
                href="/knowledge-base"
                className="block rounded-lg border p-4 hover:bg-accent"
              >
                <div className="font-medium">创建知识库</div>
                <div className="text-sm text-muted-foreground">创建一个新的知识库</div>
              </a>
              <a
                href="/document"
                className="block rounded-lg border p-4 hover:bg-accent"
              >
                <div className="font-medium">上传文档</div>
                <div className="text-sm text-muted-foreground">上传 PDF 文档到知识库</div>
              </a>
              <a
                href="/qa"
                className="block rounded-lg border p-4 hover:bg-accent"
              >
                <div className="font-medium">智能问答</div>
                <div className="text-sm text-muted-foreground">向知识库提问</div>
              </a>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>系统信息</CardTitle>
              <CardDescription>当前用户和租户信息</CardDescription>
            </CardHeader>
            <CardContent className="space-y-2">
              <div className="flex justify-between">
                <span className="text-muted-foreground">用户名</span>
                <span className="font-medium">{user?.username}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">角色</span>
                <span className="font-medium">{user?.role === 'ADMIN' ? '管理员' : '普通用户'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">租户ID</span>
                <span className="font-medium text-xs">{user?.tenantId}</span>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </MainLayout>
  );
}
