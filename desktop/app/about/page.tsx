import Link from 'next/link';

export default function AboutPage() {
  return (
    <div className="min-h-screen bg-background relative overflow-hidden">
      {/* 顶部导航 */}
      <header className="sticky top-0 z-50 w-full border-b border-border/40 bg-background/60 backdrop-blur-xl">
        <div className="container flex h-16 items-center justify-between">
          <div className="flex items-center gap-2">
            <Link href="/" className="flex items-center gap-2">
              <img src="/logo.svg" alt="Sentra Logo" className="h-8 w-8" />
              <span className="text-xl font-bold bg-gradient-to-r from-primary to-purple-600 bg-clip-text text-transparent">Sentra</span>
            </Link>
          </div>
          <nav className="hidden md:flex items-center gap-6">
            <Link href="/" className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">首页</Link>
            <Link href="/about" className="text-sm font-medium text-primary">关于我们</Link>
            <Link href="/login" className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">登录</Link>
          </nav>
        </div>
      </header>

      {/* Hero Section */}
      <div className="relative pt-20 pb-32 overflow-hidden">
        <div className="absolute inset-0 z-[-1]">
           <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[1000px] h-[500px] bg-primary/20 rounded-full blur-[120px] opacity-30" />
           <div className="absolute bottom-0 right-0 w-[800px] h-[600px] bg-purple-500/20 rounded-full blur-[100px] opacity-20" />
        </div>

        <div className="container px-4 mx-auto text-center">
          <div className="inline-flex items-center rounded-full border border-primary/20 bg-primary/5 px-3 py-1 text-sm font-medium text-primary mb-8 animate-in fade-in slide-in-from-bottom-4 duration-700">
            <span className="flex h-2 w-2 rounded-full bg-primary mr-2"></span>
            下一代文档智能管理方案
          </div>
          
          <h1 className="text-5xl md:text-7xl font-bold tracking-tight mb-8 animate-in fade-in slide-in-from-bottom-5 duration-700 delay-100">
            让知识<span className="text-primary">触手可及</span>
          </h1>
          
          <p className="text-xl text-muted-foreground max-w-2xl mx-auto mb-12 animate-in fade-in slide-in-from-bottom-6 duration-700 delay-200">
            Sentra 不仅仅是一个文档库，它是基于先进图结构的智能知识中枢。
            <br />
            我们致力于帮助企业和个人构建连接、高效、可视化的知识体系。
          </p>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-8 max-w-5xl mx-auto mt-20 animate-in fade-in slide-in-from-bottom-8 duration-700 delay-300">
            {[
              {
                title: "图谱可视化",
                desc: "自动提取文档间的关联，构建直观的知识图谱，发现隐性连接。",
                icon: (
                  <svg className="w-8 h-8 text-blue-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19.428 15.428a2 2 0 00-1.022-.547l-2.384-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z" />
                  </svg>
                )
              },
              {
                title: "深度语义检索",
                desc: "超越关键词匹配，理解查询意图，提供精准的段落级答案。",
                icon: (
                  <svg className="w-8 h-8 text-purple-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                )
              },
              {
                title: "智能协作",
                desc: "多人实时协作，版本控制，权限精细化管理，保障数据安全。",
                icon: (
                  <svg className="w-8 h-8 text-green-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 005.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                  </svg>
                )
              }
            ].map((feature, idx) => (
              <div key={idx} className="group relative p-8 rounded-3xl bg-card border border-border/50 hover:border-primary/50 transition-all duration-300 hover:shadow-2xl hover:-translate-y-1">
                <div className="absolute inset-0 bg-gradient-to-br from-primary/5 to-transparent opacity-0 group-hover:opacity-100 rounded-3xl transition-opacity" />
                <div className="relative z-10 flex flex-col items-center">
                  <div className="mb-6 p-4 rounded-2xl bg-muted/50 group-hover:bg-background transition-colors shadow-sm">
                    {feature.icon}
                  </div>
                  <h3 className="text-xl font-bold mb-3">{feature.title}</h3>
                  <p className="text-muted-foreground leading-relaxed">{feature.desc}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>



      {/* Footer */}
      <footer className="py-12 border-t border-border/40 bg-muted/20">
        <div className="container px-4 mx-auto text-center text-sm text-muted-foreground">
          <div className="flex items-center justify-center gap-2 mb-4 opacity-50">
            <img src="/logo.svg" alt="Logo" className="h-6 w-6 grayscale" />
            <span className="font-semibold">Sentra</span>
          </div>
          <p>© 2026 Sentra Inc. All rights reserved.</p>
        </div>
      </footer>
    </div>
  );
}
