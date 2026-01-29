# Sentra 前端开发快速启动指南

## 🎯 当前开发状态

✅ **Phase 1 已完成**: 用户认证与基础架构
- ✅ Next.js 14 + TypeScript + Tailwind CSS
- ✅ 用户登录/登出
- ✅ 用户管理（仅管理员）
- ✅ 仪表盘
- ✅ Tauri 桌面应用配置

## 📦 安装依赖

### 1. Node.js 依赖

```bash
cd desktop-dev
npm install
```

**主要依赖**:
- `next@14.2.15` - Web 框架
- `react@18.3.1` - UI 框架
- `zustand@5.0.2` - 状态管理
- `axios@1.7.7` - HTTP 客户端
- `@tauri-apps/cli@2.2.0` - Tauri CLI
- `lucide-react@0.462.0` - 图标库
- `class-variance-authority` - CVA 样式变体

### 2. 配置环境变量

```bash
cp .env.example .env.local
```

`.env.local` 内容：
```env
NEXT_PUBLIC_USER_API_URL=http://localhost:8081
NEXT_PUBLIC_KNOWLEDGE_API_URL=http://localhost:8082
NEXT_PUBLIC_PYTHON_API_URL=http://localhost:8000
NODE_ENV=development
NEXT_PUBLIC_APP_NAME=Sentra 智能文档助手
NEXT_PUBLIC_APP_VERSION=1.0.0
```

## 🚀 启动开发服务器

### Web 开发模式（推荐先使用此模式）

```bash
npm run dev
```

访问: http://localhost:3000

**可用页面**:
- `/` - 首页
- `/login` - 登录页
- `/dashboard` - 仪表盘
- `/users` - 用户管理（仅管理员）

### 桌面开发模式（Tauri）

**前提条件**:
1. 安装 Rust: https://www.rust-lang.org/tools/install
2. Windows: 安装 WebView2
3. macOS: 安装 Xcode
4. Linux: 安装 webkit2gtk

```bash
npm run tauri dev
```

## 🧪 测试用户认证

### 1. 启动后端服务

确保后端服务正在运行：
- **用户服务 (sentra-user-service)**: `http://localhost:8081`
- **知识库服务 (sentra-knowledge-service)**: `http://localhost:8082`
- **Python 服务**: `http://localhost:8000`（用于 OCR 和知识图谱处理）

### 2. 登录测试

访问 `http://localhost:3000/login`

使用后端创建的测试账号登录（需要先在后端数据库中创建用户）。

### 3. 测试用户管理（管理员功能）

- 登录后访问 `/users`
- 创建新用户
- 删除用户
- 查看用户列表

## 📁 项目结构

```
desktop-dev/
├── app/                          # Next.js App Router 页面
│   ├── layout.tsx               # 根布局
│   ├── page.tsx                 # 首页
│   ├── login/                   # 登录页
│   ├── dashboard/               # 仪表盘
│   └── users/                   # 用户管理
│
├── components/                   # React 组件
│   ├── ui/                      # shadcn/ui 基础组件
│   │   ├── button.tsx
│   │   ├── input.tsx
│   │   ├── card.tsx
│   │   ├── dialog.tsx
│   │   ├── label.tsx
│   │   └── table.tsx
│   └── layout/                  # 布局组件
│       ├── header.tsx
│       ├── sidebar.tsx
│       └── main-layout.tsx
│
├── lib/                          # 工具库
│   ├── api/                     # API 客户端
│   │   ├── axios.ts             # Axios 配置（支持多服务）
│   │   │                        # - userApiClient: 8081
│   │   │                        # - knowledgeApiClient: 8082
│   │   └── userApi.ts           # 用户 API
│   ├── stores/                  # Zustand 状态管理
│   │   └── authStore.ts         # 认证状态
│   ├── utils/                   # 工具函数
│   │   └── cn.ts                # className 合并
│   └── middleware/              # 中间件
│       └── authMiddleware.ts    # 认证中间件
│
├── types/                        # TypeScript 类型
│   └── user.ts                  # 用户类型定义
│
├── src-tauri/                    # Tauri Rust 代码
│   ├── src/
│   │   ├── main.rs              # Rust 主入口
│   │   └── lib.rs               # Rust 库
│   ├── Cargo.toml               # Rust 依赖
│   └── tauri.conf.json          # Tauri 配置
│
├── package.json                 # Node.js 依赖
├── tsconfig.json                # TypeScript 配置
├── tailwind.config.ts           # Tailwind CSS 配置
├── next.config.mjs              # Next.js 配置
└── .env.local                   # 环境变量（不提交到 Git）
```

## 🔑 关键文件说明

### 1. API 客户端 (`lib/api/axios.ts`)

现在支持多个后端服务：
- `userApiClient` - 连接到用户服务 (8081)
- `knowledgeApiClient` - 连接到知识库服务 (8082)
- `apiClient` - 默认使用 userApiClient

所有实例都配置了：
- 自动添加 JWT Token 到请求头
- 统一错误处理
- 401 自动跳转登录

### 2. 认证状态 (`lib/stores/authStore.ts`)

- Zustand 状态管理
- localStorage 持久化
- 提供 `login`, `logout`, `getCurrentUser` 方法

### 3. 用户 API (`lib/api/userApi.ts`)

- 使用 `userApiClient` (端口 8081)
- `authApi`: login, logout, getCurrentUser
- `userApi`: list, create, update, delete (管理员)

### 4. 知识库 API (待实现 `lib/api/kbApi.ts`)

- 将使用 `knowledgeApiClient` (端口 8082)
- 知识库 CRUD 操作

### 5. 布局组件 (`components/layout/`)

- `Header`: 显示用户信息、登出按钮
- `Sidebar`: 导航菜单
- `MainLayout`: 统一布局，权限检查

## 🎨 样式系统

### 主题变量 (`app/globals.css`)

- 支持明暗主题（黑白双色调）
- 使用 CSS 变量定义颜色
- Tailwind CSS 类名自动响应主题变化

### 组件样式

- 使用 `cn()` 工具函数合并 className
- 支持 `variant` 属性切换样式
- 基于 shadcn/ui 设计系统

## 🔧 开发工具

### 1. ESLint

```bash
npm run lint
```

### 2. TypeScript 类型检查

```bash
npm run build
```

### 3. 构建生产版本

```bash
# Web 构建
npm run build

# 桌面应用构建
npm run tauri build
```

## 📝 下一步开发

### Phase 2: 知识库管理（待开发）

1. 创建 `types/kb.ts` - 知识库类型定义
2. 创建 `lib/api/kbApi.ts` - 知识库 API
3. 创建 `lib/stores/kbStore.ts` - 知识库状态
4. 创建 `app/knowledge-base/page.tsx` - 知识库列表页
5. 创建 `components/kb/CreateKbDialog.tsx` - 创建知识库对话框

### 参考现有代码

- **用户模块** 完整实现了 CRUD 功能
- 可以参考 `userApi.ts` 实现 `kbApi.ts`
- 可以参考 `users/page.tsx` 实现 `knowledge-base/page.tsx`

## 🐛 常见问题

### 1. 端口被占用

```bash
# Windows
netstat -ano | findstr :3000
taskkill /PID <PID> /F

# macOS/Linux
lsof -ti:3000 | xargs kill
```

### 2. 安装依赖失败

```bash
# 清除缓存重新安装
rm -rf node_modules package-lock.json
npm install
```

### 3. 后端 API 连接失败

- 检查 `.env.local` 中的 API 地址
- 确保后端服务正在运行：
  - 用户服务: `http://localhost:8081`
  - 知识库服务: `http://localhost:8082`
  - Python 服务: `http://localhost:8000`
- 检查浏览器控制台的错误信息

### 4. Tauri 构建失败

- 确保已安装 Rust
- 确保 Node.js 版本 >= 18
- Windows: 确保已安装 WebView2

## 📚 参考资料

- [Next.js 文档](https://nextjs.org/docs)
- [Tauri 文档](https://tauri.app/v2/guides/)
- [Zustand 文档](https://docs.pmnd.rs/zustand)
- [Tailwind CSS 文档](https://tailwindcss.com/docs)
- [shadcn/ui 组件库](https://ui.shadcn.com/)

## 🎉 开发完成清单

- [x] ✅ Phase 1: 用户认证与基础架构
- [ ] ⏳ Phase 2: 知识库管理
- [ ] ⏳ Phase 3: 文档管理
- [ ] ⏳ Phase 4: 智能问答
- [ ] ⏳ Phase 5: 高级功能

祝开发顺利！🚀
