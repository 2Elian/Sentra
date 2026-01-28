'use client';

import { useEffect } from 'react';
import { useAuthStore } from '@/lib/stores/authStore';

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const initializeAuth = useAuthStore((state) => state.initializeAuth);

  useEffect(() => {
    // 应用启动时从 localStorage 初始化认证状态
    initializeAuth();
  }, [initializeAuth]);

  return <>{children}</>;
}
