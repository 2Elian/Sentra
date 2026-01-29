'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { cn } from '@/lib/utils/cn';
import { useAuthStore } from '@/lib/stores/authStore';
import {
  FileText,
  Database,
  MessageSquare,
  Users,
  Settings,
  Home,
  LayoutDashboard,
  Layers,
} from 'lucide-react';

const navItems = [
  {
    title: '仪表盘',
    href: '/dashboard',
    icon: LayoutDashboard,
  },
  {
    title: '知识库管理',
    href: '/knowledge-base',
    icon: Database,
    description: '管理和搜索知识库',
  },
  {
    title: '实体模板',
    href: '/entity-templates',
    icon: Layers,
    description: '管理知识抽取模板',
  },
  {
    title: '智能问答',
    href: '/qa',
    icon: MessageSquare,
    description: 'AI 驱动的问答系统',
  },
  {
    title: '用户管理',
    href: '/users',
    icon: Users,
    requiresAdmin: true,
    description: '管理用户和权限',
  },
  {
    title: '设置',
    href: '/settings',
    icon: Settings,
    description: '系统设置和偏好',
  },
];

export function Sidebar() {
  const pathname = usePathname();
  const { isAdmin } = useAuthStore();

  return (
    <aside className="fixed left-0 top-16 z-40 h-[calc(100vh-4rem)] w-64 bg-background/60 backdrop-blur-xl border-r border-white/5 dark:border-white/5 shadow-[4px_0_24px_-12px_rgba(0,0,0,0.1)] transition-all duration-300">
      <div className="flex h-full flex-col py-4">
        {/* 导航菜单 */}
        <nav className="flex-1 space-y-1.5 px-3 overflow-y-auto custom-scrollbar">
          {navItems.map((item) => {
            // 检查管理员权限
            if (item.requiresAdmin && !isAdmin()) {
              return null;
            }

            const Icon = item.icon;
            const isActive = pathname === item.href || pathname?.startsWith(item.href + '/');

            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  'group relative flex items-center gap-3 rounded-xl px-3 py-3 text-sm font-medium transition-all duration-300 ease-in-out overflow-hidden',
                  isActive
                    ? 'text-primary bg-primary/10 shadow-sm'
                    : 'text-muted-foreground hover:bg-muted/50 hover:text-foreground'
                )}
              >
                {/* 选中状态的左侧光标 */}
                {isActive && (
                  <div className="absolute left-0 top-1/2 -translate-y-1/2 h-8 w-1 rounded-r-full bg-primary animate-in fade-in slide-in-from-left-1 duration-300" />
                )}
                
                {/* 选中状态的背景流光 */}
                {isActive && (
                  <div className="absolute inset-0 bg-gradient-to-r from-primary/10 via-transparent to-transparent opacity-50" />
                )}

                <Icon className={cn(
                  'h-5 w-5 transition-transform duration-300 z-10',
                  isActive ? 'scale-110 text-primary' : 'group-hover:scale-110 group-hover:text-foreground'
                )} />
                
                <span className={cn("flex-1 z-10 transition-colors duration-300", isActive && "font-semibold")}>
                  {item.title}
                </span>

                {isActive && (
                  <div className="h-1.5 w-1.5 rounded-full bg-primary animate-pulse z-10 shadow-[0_0_8px_rgba(var(--primary),0.5)]" />
                )}
              </Link>
            );
          })}
        </nav>

        {/* 底部信息 */}
        <div className="px-3 mt-auto">
          <div className="rounded-xl border border-border/40 bg-gradient-to-br from-muted/50 to-muted/10 p-4 backdrop-blur-sm">
            <div className="flex items-center gap-2.5 mb-2">
              <div className="relative flex h-2 w-2">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-2 w-2 bg-green-500"></span>
              </div>
              <span className="text-xs font-medium text-foreground/80">系统运行正常</span>
            </div>
            <div className="text-[10px] text-muted-foreground font-mono opacity-70">
              Sentra v1.0.0 (Build 2024)
            </div>
          </div>
        </div>
      </div>
    </aside>
  );
}
