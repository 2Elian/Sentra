'use client';

import { ReactNode, useEffect, useState } from 'react';
import { useAuthStore } from '@/lib/stores/authStore';
import { Header } from './header';
import { Sidebar } from './sidebar';

interface MainLayoutProps {
  children: ReactNode;
  requireAdmin?: boolean;
}

export function MainLayout({ children, requireAdmin = false }: MainLayoutProps) {
  const { isAuthenticated, isAdmin, user } = useAuthStore();
  const [isMounted, setIsMounted] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  // 确保只在客户端挂载后才渲染，避免水合错误
  useEffect(() => {
    // 等待一小段时间确保 auth store 已经初始化
    const timer = setTimeout(() => {
      setIsMounted(true);
      setIsLoading(false);
    }, 100);

    return () => clearTimeout(timer);
  }, []);

  // 调试：打印用户信息
  console.log('MainLayout - User:', user);
  console.log('MainLayout - isAdmin():', isAdmin());
  console.log('MainLayout - user.role:', user?.role);
  console.log('MainLayout - isMounted:', isMounted);

  // 还没挂载或正在加载时显示加载状态，避免水合不匹配
  if (!isMounted || isLoading) {
    return (
      <div className="flex h-screen items-center justify-center bg-background">
        <div className="flex flex-col items-center gap-4">
          <div className="h-10 w-10 animate-spin rounded-full border-4 border-primary border-t-transparent" />
          <p className="text-muted-foreground text-sm font-medium animate-pulse">系统加载中...</p>
        </div>
      </div>
    );
  }

  // 检查权限（只在客户端挂载后检查）
  if (requireAdmin && !isAdmin()) {
    return (
      <div className="flex h-screen items-center justify-center bg-background">
        <div className="text-center space-y-4 max-w-md mx-auto p-8 rounded-2xl border border-border/50 bg-card shadow-xl">
          <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-destructive/10">
            <svg className="h-8 w-8 text-destructive" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          </div>
          <div>
             <h1 className="text-2xl font-bold tracking-tight">权限不足</h1>
             <p className="mt-2 text-muted-foreground">您没有访问此页面的权限</p>
          </div>
          <div className="pt-2">
            <span className="inline-flex items-center rounded-full bg-muted px-3 py-1 text-xs font-medium text-muted-foreground">
              当前角色: {user?.role || '未知'}
            </span>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      {/* 顶部导航 */}
      <Header />
      
      <div className="flex">
        {/* 侧边栏 */}
        <Sidebar />
        
        {/* 主内容区域 */}
        <main className="ml-64 flex-1 p-6 relative">
          {/* 背景装饰：极淡的渐变，增加层次感 */}
          <div className="absolute inset-0 z-[-1] bg-[radial-gradient(ellipse_at_top_left,_var(--tw-gradient-stops))] from-primary/5 via-background to-background pointer-events-none" />
          
          {/* 内容容器：添加入场动画 */}
          <div className="animate-in fade-in slide-in-from-bottom-4 duration-500 ease-out">
             {children}
          </div>
        </main>
      </div>
    </div>
  );
}
