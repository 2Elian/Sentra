import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import { User, UserRole } from '@/types/user';
import { authApi } from '@/lib/api/userApi';

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;

  // Actions
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  getCurrentUser: () => Promise<void>;
  clearError: () => void;
  isAdmin: () => boolean;
  initializeAuth: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      token: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,

      // 从 localStorage 初始化认证状态
      initializeAuth: () => {
        if (typeof window === 'undefined') return;

        try {
          const token = localStorage.getItem('token');
          const userStr = localStorage.getItem('user');

          console.log('initializeAuth - token:', token);
          console.log('initializeAuth - userStr:', userStr);

          if (token && userStr) {
            const user = JSON.parse(userStr) as User;
            console.log('initializeAuth - parsed user:', user);
            console.log('initializeAuth - user.role:', user?.role);

            set({
              user,
              token,
              isAuthenticated: true,
            });
          }
        } catch (error) {
          console.error('Failed to initialize auth from localStorage:', error);
          // 清除无效的数据
          localStorage.removeItem('token');
          localStorage.removeItem('user');
        }
      },

      login: async (username: string, password: string) => {
        set({ isLoading: true, error: null });
        try {
          const response = await authApi.login({ username, password });

          // 打印响应数据用于调试
          console.log('Login Response:', response);
          console.log('User object:', response.user);
          console.log('User role:', response.user?.role);

          // 保存到状态
          set({
            user: response.user,
            token: response.token,
            isAuthenticated: true,
            isLoading: false,
            error: null,
          });

          // 保存到 localStorage
          if (typeof window !== 'undefined') {
            localStorage.setItem('token', response.token);
            localStorage.setItem('user', JSON.stringify(response.user));
          }
        } catch (error: any) {
          set({
            isLoading: false,
            error: error.message || '登录失败',
            isAuthenticated: false,
            user: null,
            token: null,
          });
          throw error;
        }
      },

      logout: async () => {
        try {
          await authApi.logout();
        } catch (error) {
          console.error('登出接口调用失败:', error);
        } finally {
          // 清除状态
          set({
            user: null,
            token: null,
            isAuthenticated: false,
            error: null,
          });

          // 清除 localStorage
          if (typeof window !== 'undefined') {
            localStorage.removeItem('token');
            localStorage.removeItem('user');
          }

          // 跳转到登录页
          if (typeof window !== 'undefined') {
            window.location.href = '/login';
          }
        }
      },

      getCurrentUser: async () => {
        set({ isLoading: true, error: null });
        try {
          const user = await authApi.getCurrentUser();
          set({
            user,
            isAuthenticated: true,
            isLoading: false,
            error: null,
          });

          // 更新 localStorage
          if (typeof window !== 'undefined') {
            localStorage.setItem('user', JSON.stringify(user));
          }
        } catch (error: any) {
          set({
            isLoading: false,
            error: error.message || '获取用户信息失败',
            isAuthenticated: false,
            user: null,
            token: null,
          });

          // 清除 localStorage
          if (typeof window !== 'undefined') {
            localStorage.removeItem('token');
            localStorage.removeItem('user');
          }
          throw error;
        }
      },

      clearError: () => {
        set({ error: null });
      },

      isAdmin: () => {
        const { user } = get();
        // 兼容多种可能的格式
        const role = user?.role;
        if (!role) return false;

        // 检查是否为管理员（字符串或枚举）
        return (
          role === UserRole.ADMIN ||
          role === 'ADMIN' ||
          role === 'Admin' ||
          (typeof role === 'string' && role.toUpperCase() === 'ADMIN')
        );
      },
    }),
    {
      name: 'auth-storage',
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({
        user: state.user,
        token: state.token,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);
