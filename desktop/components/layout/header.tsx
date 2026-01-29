'use client';

import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/lib/stores/authStore';
import { Button } from '@/components/ui/button';
import { ThemeToggle } from '@/components/ui/theme-toggle';

export function Header() {
  const router = useRouter();
  const { user, logout, isAdmin } = useAuthStore();

  const handleLogout = async () => {
    await logout();
  };

  return (
    <header className="sticky top-0 z-50 w-full border-b border-white/5 bg-background/60 backdrop-blur-xl shadow-sm transition-all duration-300">
      <div className="flex h-16 items-center justify-between px-4 w-full">
        <div className="flex items-center space-x-4">
          <div className="flex items-center gap-3 group cursor-pointer" onClick={() => router.push('/')}>
            <div className="h-9 w-9 rounded-xl bg-gradient-to-br from-primary/20 to-primary/5 flex items-center justify-center border border-primary/10 shadow-sm transition-transform group-hover:scale-105">
              <svg className="w-5 h-5 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
            </div>
            <div>
              <h1 className="text-lg font-bold bg-gradient-to-r from-foreground to-foreground/70 bg-clip-text text-transparent tracking-tight">
                Sentra
              </h1>
              <p className="text-[10px] text-muted-foreground font-medium tracking-wider uppercase">Intelligent Docs</p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-4">
          <ThemeToggle />

          <div className="hidden sm:flex items-center gap-3 pl-4 border-l border-border/20">
            <div className="flex items-center gap-3 px-1 py-1 pr-4 rounded-full bg-muted/30 border border-white/5 hover:bg-muted/50 transition-colors cursor-default">
              <div className="h-8 w-8 rounded-full bg-gradient-to-br from-primary to-purple-600 p-[1px] shadow-sm">
                <div className="h-full w-full rounded-full bg-background flex items-center justify-center">
                   <span className="text-xs font-bold text-primary">{user?.username?.substring(0, 2).toUpperCase() || 'US'}</span>
                </div>
              </div>
              <div className="flex flex-col">
                <span className="text-xs font-semibold leading-none">{user?.username || 'Guest'}</span>
                <span className="text-[10px] text-muted-foreground leading-none mt-1">
                  {isAdmin() ? 'Administrator' : 'User'}
                </span>
              </div>
            </div>

            <Button 
              variant="ghost" 
              size="icon" 
              onClick={handleLogout} 
              className="h-9 w-9 rounded-full hover:bg-destructive/10 hover:text-destructive transition-colors"
              title="退出登录"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
              </svg>
            </Button>
          </div>
        </div>
      </div>
    </header>
  );
}
