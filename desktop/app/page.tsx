import Link from 'next/link';

export default function HomePage() {
  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden">
      {/* 全屏背景图片 - 沉浸式体验核心 */}
      <div className="fixed inset-0 z-[-1]">
        <img
          src="/Sentra宣传图.png"
          alt="Background"
          className="h-full w-full object-fill opacity-40 dark:opacity-30 transition-opacity duration-700"
        />
      </div>

      {/* 动态光效背景装饰 */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-1/2 -right-1/2 h-[200%] w-[200%] animate-[spin_60s_linear_infinite] opacity-30 dark:opacity-20">
          <div className="absolute inset-0 bg-gradient-to-r from-primary/10 via-transparent to-primary/10" />
        </div>
        <div className="absolute -bottom-1/2 -left-1/2 h-[200%] w-[200%] animate-[spin_80s_linear_infinite_reverse] opacity-30 dark:opacity-20">
          <div className="absolute inset-0 bg-gradient-to-r from-primary/10 via-transparent to-primary/10" />
        </div>
      </div>

      {/* 主要内容 - 毛玻璃卡片 */}
      <div className="relative z-10 container mx-auto px-4 animate-in fade-in zoom-in-95 duration-700 slide-in-from-bottom-4">
        <div className="mx-auto max-w-4xl overflow-hidden rounded-[2rem] border border-white/20 bg-white/5 shadow-2xl backdrop-blur-md dark:border-white/10 dark:bg-black/20">
          <div className="flex flex-col items-center gap-8 px-6 py-16 text-center sm:px-12 sm:py-20">
            
            {/* 顶部标签 */}
            <div className="inline-flex items-center rounded-full border border-primary/20 bg-primary/10 px-3 py-1 text-sm font-medium text-primary backdrop-blur-sm">
              <span className="flex h-2 w-2 rounded-full bg-primary mr-2 animate-pulse"></span>
              Sentra v0.1.0
            </div>

            {/* 标题和描述 */}
            <div className="space-y-6 max-w-3xl">
              <h1 className="text-5xl font-bold tracking-tight sm:text-6xl md:text-7xl">
                <span className="bg-gradient-to-r from-foreground via-foreground to-foreground/70 bg-clip-text text-transparent">
                  Sentra
                </span>
                <span className="block text-3xl sm:text-4xl md:text-5xl mt-3 font-normal text-muted-foreground">
                  重新定义文档问答
                </span>
              </h1>

              <p className="text-lg sm:text-xl text-muted-foreground/90 max-w-2xl mx-auto leading-relaxed">
                基于图结构的智能文档问答系统，让知识检索更高效、信息检索更准确。
                <br className="hidden sm:block" />
                体验最准确的文档问答功能。
              </p>

              {/* 功能特性图标 */}
              <div className="flex flex-wrap gap-3 justify-center pt-6">
                {[
                  { icon: "M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z", text: "智能检索" },
                  { icon: "M13 10V3L4 14h7v7l9-11h-7z", text: "高效性能" },
                  { icon: "M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z", text: "多租户管理" }
                ].map((item, idx) => (
                  <div key={idx} className="flex items-center gap-2 px-4 py-2 rounded-full border border-primary/10 bg-primary/5 text-primary text-sm font-medium transition-colors hover:bg-primary/10">
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d={item.icon} />
                    </svg>
                    {item.text}
                  </div>
                ))}
              </div>
            </div>

            {/* 行动按钮 */}
            <div className="flex flex-col sm:flex-row gap-4 pt-4 w-full justify-center">
              <Link
                href="/login"
                className="group relative inline-flex items-center justify-center px-8 py-3.5 text-base font-semibold text-white transition-all duration-300 bg-gradient-to-r from-blue-600 to-purple-600 rounded-xl hover:scale-[1.02] hover:shadow-lg hover:shadow-purple-500/30 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:ring-offset-2 dark:focus:ring-offset-gray-900"
              >
                <span className="absolute inset-0 rounded-xl bg-gradient-to-r from-blue-600 to-purple-600 opacity-0 group-hover:opacity-100 blur-sm transition-opacity duration-300" />
                <span className="relative flex items-center gap-2">
                  立即开始
                  <svg className="w-5 h-5 transition-transform duration-300 group-hover:translate-x-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7l5 5m0 0l-5 5m5-5H6" />
                  </svg>
                </span>
              </Link>
              
              <Link
                href="/about" 
                className="inline-flex items-center justify-center px-8 py-3.5 text-base font-medium transition-all duration-200 bg-transparent border border-input rounded-xl hover:bg-accent hover:text-accent-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
              >
                了解我们
              </Link>
            </div>

            {/* 底部提示 */}
            <p className="text-xs text-muted-foreground/60 pt-4">
              文档到知识库的问答 · 多租户管理 · 角色权限控制
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
