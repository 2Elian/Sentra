# Sentra 前端开发文档

## 项目概述

本文档描述 Sentra 图结构文档问答助手的前端应用开发规范和技术架构。前端应用同时支持桌面端和 Web 端，采用 Tauri + Next.js + React 技术栈。桌面端和web端全部采用双颜色系统，一个是黑色的色调，一个是白色的色调。

### 技术栈

- **桌面框架**: Tauri 2.x
- **Web 框架**: Next.js 14.x (App Router)
- **UI 框架**: React 18.x
- **状态管理**: Zustand / Jotai
- **样式方案**: Tailwind CSS
- **组件库**: shadcn/ui
- **HTTP 客户端**: Axios / Fetch API
- **表单处理**: React Hook Form + Zod
- **类型检查**: TypeScript

### 目标平台

1. **桌面应用**: Windows, macOS, Linux (通过 Tauri 打包)
2. **Web 应用**: 现代浏览器 (通过 Next.js 部署)

---

## 项目结构

```
desktop-dev/
├── src-tauri/                 # Tauri Rust 后端
│   ├── src/
│   │   ├── main.rs           # Tauri 主入口
│   │   ├── lib.rs            # 库文件
│   │   └── commands/         # Tauri 命令（与前端交互）
│   ├── Cargo.toml            # Rust 依赖配置
│   ├── tauri.conf.json       # Tauri 配置
│   └── icons/                # 应用图标
│
├── src/                       # Next.js 前端源码
│   ├── app/                  # Next.js App Router
│   │   ├── layout.tsx        # 根布局
│   │   ├── page.tsx          # 首页
│   │   ├── login/            # 登录页面
│   │   ├── dashboard/        # 仪表盘
│   │   ├── knowledge-base/   # 知识库管理
│   │   ├── document/         # 文档管理
│   │   └── qa/               # 问答界面
│   │
│   ├── components/           # React 组件
│   │   ├── ui/              # shadcn/ui 基础组件
│   │   ├── layout/          # 布局组件（Header, Sidebar）
│   │   ├── kb/              # 知识库相关组件
│   │   ├── document/        # 文档相关组件
│   │   └── qa/              # 问答相关组件
│   │
│   ├── lib/                 # 工具库
│   │   ├── api/            # API 客户端
│   │   ├── stores/         # 状态管理
│   │   ├── hooks/          # 自定义 Hooks
│   │   ├── utils/          # 工具函数
│   │   └── constants.ts    # 常量定义
│   │
│   ├── types/              # TypeScript 类型定义
│   │   ├── entity.ts       # 实体类型
│   │   ├── kb.ts           # 知识库类型
│   │   ├── document.ts     # 文档类型
│   │   └── qa.ts           # 问答类型
│   │
│   └── styles/             # 样式文件
│       └── globals.css     # 全局样式
│
├── public/                 # 静态资源
├── package.json            # Node.js 依赖
├── tsconfig.json          # TypeScript 配置
├── next.config.js         # Next.js 配置
├── tailwind.config.js     # Tailwind CSS 配置
└── README.md              # 本文档
```

---

## 核心功能模块

### 1. 用户认证模块

#### 1.1 登录/注册

**页面**: `src/app/login/page.tsx`

**功能**:
- 用户登录（用户名/密码），用户不能自己注册用户，但管理员用户可以注册用户。
- 租户选择（一个用户只能隶属于一个租户、租户无法被创建，只能通过后端代码进行创建）
- JWT Token 存储和管理
- 自动登录（Token 持久化）

**API 交互**:
```
POST /api/v1/auth/login
Request: { username, password }
Response: { token, userId, tenantId, username }
```

**状态管理**:
```typescript
// lib/stores/authStore.ts
interface AuthState {
  user: User | null;
  token: string | null;
  tenantId: string | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  isAuthenticated: () => boolean;
}
```

#### 1.2 权限控制

**中间件**: `src/lib/middleware/auth.ts`

- 保护需要认证的路由
- Token 过期自动跳转登录
- 租户隔离验证
- 普通用户无法创建其他用户，管理员用户能够创建、删除用户和更新用户，并且管理员创建的用户必须与管理员隶属于同一个租户。一个用户不能隶属于多个租户。

---

### 2. 知识库管理模块

#### 2.1 知识库列表

**页面**: `src/app/knowledge-base/page.tsx`

**功能**:
- 展示当前用户的所有知识库，包括公共的知识库
- 搜索和过滤知识库
- 知识库统计信息（文档数量、状态）

**API 交互**:

```
GET /api/v1/kb/list?tenantId={tenantId}
Response: [
  {
    id: string,
    kbId: string,
    kbName: string,
    description: string,
    createdAt: string,
    documentCount: number
  }
]
```

#### 2.2 创建知识库

**组件**: `src/components/kb/CreateKbDialog.tsx`

**功能**:
- 表单输入：知识库名称、描述
- 实体类型模板选择（可选）
- 表单验证（Zod Schema）

**API 交互**:
```
POST /api/v1/kb/build
Request: {
  kbName: string,
  tenantId: string, # 自动填充为当前账号隶属于的租户id
  ownerUserId: string, # 自动填充为当前账号名称
  description: string
}
Response: { kbId, kbName, status }
```

#### 2.3 知识库详情

**页面**: `src/app/knowledge-base/[kbId]/page.tsx`

**功能**:
- 知识库基本信息展示
- 文档列表展示
- 实体类型模板管理
- 知识库删除

---

### 3. 文档管理模块

#### 3.1 文档上传

**组件**: `src/components/document/DocumentUpload.tsx`

**功能**:
- 拖拽上传 PDF 文件
- 实体类型模板选择（可选，覆盖知识库默认模板）
- 上传进度显示
- 文件格式验证（仅支持 PDF）

**API 交互**:
```
POST /api/v1/document/upload
Request: FormData {
  kbId: string,
  entityTemplateId: string (可选),
  file: File
}
Response: {
  id: string,
  filename: string,
  status: "UPLOADED" | "OCR_PROCESSING" | "KB_BUILDING" | "COMPLETED" | "FAILED",
  progress: 0-100
}
```

**技术要点**:
- 使用 `FormData` 上传文件
- 轮询或 WebSocket 获取文档处理进度
- 错误处理和重试机制

#### 3.2 文档列表

**页面**: `src/app/document/page.tsx`

**功能**:
- 按知识库筛选文档
- 状态筛选（处理中、完成、失败）
- 文档搜索（文件名）
- 批量操作（删除）

**API 交互**:
```
GET /api/v1/document/list?kbId={kbId}
Response: [
  {
    id: string,
    kbId: string,
    filename: string,
    fileSize: number,
    status: string,
    progress: number,
    createdAt: string,
    errorMessage: string | null
  }
]
```

#### 3.3 文档详情

**页面**: `src/app/document/[documentId]/page.tsx`

**功能**:
- 文档基本信息展示
- 处理进度实时更新
- OCR 结果预览（Markdown 内容）
- 文档删除

---

### 4. 智能问答模块

#### 4.1 问答界面

**页面**: `src/app/qa/page.tsx`

**功能**:
- 聊天式问答界面
- 知识库选择
- 历史记录展示
- 答案来源引用（文档片段）

**API 交互**:
```
POST /api/v1/qa/ask
Request: {
  kbId: string,
  question: string,
  retrievalMode: "vector" | "graph" | "hybrid"
}
Response: {
  answer: string,
  sources: [
    {
      documentId: string,
      filename: string,
      chunks: [
        {
          content: string,
          score: number
        }
      ]
    }
  ],
  graphPath: [
    {
      entityId: string,
      entityType: string,
      relation: string
    }
  ]
}
```

**技术要点**:
- 流式响应（SSE / WebSocket）
- Markdown 渲染（支持代码高亮）
- 图谱可视化（可选）

#### 4.2 问答历史

**组件**: `src/components/qa/ChatHistory.tsx`

**功能**:
- 会话历史列表
- 会话搜索
- 会话删除
- 会话导出

---

### 5. 实体类型模板管理模块

#### 5.1 模板列表

**页面**: `src/app/entity-templates/page.tsx`

**功能**:
- 展示所有实体类型模板
- 系统模板和用户自定义模板分类
- 模板预览（实体类型列表）

**API 交互**:
```
GET /api/v1/entity-template/list?tenantId={tenantId}
Response: [
  {
    templateId: string,
    templateName: string,
    description: string,
    isSystem: boolean,
    entityTypes: [
      {
        entityTypeName: string,
        entityLabel: string,
        color: string,
        description: string
      }
    ]
  }
]
```

#### 5.2 创建自定义模板

**组件**: `src/components/entity/CreateTemplateDialog.tsx`

**功能**:
- 表单输入：模板名称、描述
- 实体类型定义（动态表单）
- 颜色选择器
- 实体类型预览

---

## 状态管理方案

### Zustand Store 示例

```typescript
// src/lib/stores/kbStore.ts
import { create } from 'zustand';

interface KnowledgeBase {
  id: string;
  kbId: string;
  kbName: string;
  description: string;
}

interface KbStore {
  knowledgeBases: KnowledgeBase[];
  selectedKbId: string | null;
  setKnowledgeBases: (kbs: KnowledgeBase[]) => void;
  selectKb: (kbId: string) => void;
  addKb: (kb: KnowledgeBase) => void;
  updateKb: (kbId: string, updates: Partial<KnowledgeBase>) => void;
  deleteKb: (kbId: string) => void;
}

export const useKbStore = create<KbStore>((set) => ({
  knowledgeBases: [],
  selectedKbId: null,
  setKnowledgeBases: (kbs) => set({ knowledgeBases: kbs }),
  selectKb: (kbId) => set({ selectedKbId: kbId }),
  addKb: (kb) => set((state) => ({ knowledgeBases: [...state.knowledgeBases, kb] })),
  updateKb: (kbId, updates) =>
    set((state) => ({
      knowledgeBases: state.knowledgeBases.map((kb) =>
        kb.kbId === kbId ? { ...kb, ...updates } : kb
      ),
    })),
  deleteKb: (kbId) =>
    set((state) => ({
      knowledgeBases: state.knowledgeBases.filter((kb) => kb.kbId !== kbId),
    })),
}));
```

---

## API 客户端封装

### Axios 实例配置

```typescript
// src/lib/api/axios.ts
import axios from 'axios';
import { useAuthStore } from '@/lib/stores/authStore';

const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器（添加 Token）
apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 响应拦截器（统一错误处理）
apiClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().logout();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default apiClient;
```

### API 服务模块

```typescript
// src/lib/api/kbApi.ts
import apiClient from './axios';
import { KnowledgeBase } from '@/types/kb';

export const kbApi = {
  // 获取知识库列表
  list: (tenantId: string): Promise<KnowledgeBase[]> =>
    apiClient.get('/api/v1/kb/list', { params: { tenantId } }),

  // 创建知识库
  create: (data: {
    kbName: string;
    tenantId: string;
    ownerUserId: string;
    description?: string;
  }): Promise<KnowledgeBase> =>
    apiClient.post('/api/v1/kb/build', data),

  // 删除知识库
  delete: (kbId: string): Promise<void> =>
    apiClient.delete(`/api/v1/kb/${kbId}`),

  // 获取知识库详情
  get: (kbId: string): Promise<KnowledgeBase> =>
    apiClient.get(`/api/v1/kb/${kbId}`),
};
```

---

## Tauri 集成

### Tauri 配置

```json
// src-tauri/tauri.conf.json
{
  "build": {
    "beforeDevCommand": "npm run dev",
    "beforeBuildCommand": "npm run build",
    "devUrl": "http://localhost:3000",
    "frontendDist": "../out"
  },
  "app": {
    "windows": [
      {
        "title": "Sentra 智能文档助手",
        "width": 1280,
        "height": 800,
        "resizable": true,
        "fullscreen": false,
        "transparent": false,
        "decorations": true
      }
    ],
    "security": {
      "csp": "default-src 'self'; connect-src 'self' http://localhost:*"
    }
  },
  "bundle": {
    "active": true,
    "targets": ["msi", "nsis", "dmg", "appimage"],
    "icon": ["icons/32x32.png", "icons/128x128.png", "icons/128x128@2x.png", "icons/icon.icns"]
  }
}
```

### Tauri 命令示例

```rust
// src-tauri/src/commands/mod.rs
use tauri::State;

#[tauri::command]
async fn open_local_file(file_path: String) -> Result<String, String> {
    // 打开本地文件（如 PDF 预览）
    Ok(format!("Opened file: {}", file_path))
}

#[tauri::command]
async fn get_system_info() -> Result<String, String> {
    // 获取系统信息
    Ok("System Info".to_string())
}
```

```typescript
// src/lib/utils/tauri.ts
import { invoke } from '@tauri-apps/api/tauri';

export const tauri = {
  // 打开本地文件
  openLocalFile: (filePath: string) =>
    invoke<string>('open_local_file', { filePath }),

  // 获取系统信息
  getSystemInfo: () =>
    invoke<string>('get_system_info'),
};
```

---

## 样式规范

### Tailwind CSS 配置

```javascript
// tailwind.config.js
/** @type {import('tailwindcss').Config} */
module.exports = {
  darkMode: ['class'],
  content: [
    './src/pages/**/*.{js,ts,jsx,tsx,mdx}',
    './src/components/**/*.{js,ts,jsx,tsx,mdx}',
    './src/app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        border: 'hsl(var(--border))',
        input: 'hsl(var(--input))',
        ring: 'hsl(var(--ring))',
        background: 'hsl(var(--background))',
        foreground: 'hsl(var(--foreground))',
        primary: {
          DEFAULT: 'hsl(var(--primary))',
          foreground: 'hsl(var(--primary-foreground))',
        },
        secondary: {
          DEFAULT: 'hsl(var(--secondary))',
          foreground: 'hsl(var(--secondary-foreground))',
        },
        destructive: {
          DEFAULT: 'hsl(var(--destructive))',
          foreground: 'hsl(var(--destructive-foreground))',
        },
        muted: {
          DEFAULT: 'hsl(var(--muted))',
          foreground: 'hsl(var(--muted-foreground))',
        },
        accent: {
          DEFAULT: 'hsl(var(--accent))',
          foreground: 'hsl(var(--accent-foreground))',
        },
      },
      borderRadius: {
        lg: 'var(--radius)',
        md: 'calc(var(--radius) - 2px)',
        sm: 'calc(var(--radius) - 4px)',
      },
    },
  },
  plugins: [require('tailwindcss-animate')],
};
```

### 主题变量

```css
/* src/app/globals.css */
@tailwind base;
@tailwind components;
@tailwind utilities;

@layer base {
  :root {
    --background: 0 0% 100%;
    --foreground: 222.2 84% 4.9%;
    --primary: 221.2 83.2% 53.3%;
    --primary-foreground: 210 40% 98%;
    --secondary: 210 40% 96.1%;
    --secondary-foreground: 222.2 47.4% 11.2%;
    --muted: 210 40% 96.1%;
    --muted-foreground: 215.4 16.3% 46.9%;
    --accent: 210 40% 96.1%;
    --accent-foreground: 222.2 47.4% 11.2%;
    --destructive: 0 84.2% 60.2%;
    --destructive-foreground: 210 40% 98%;
    --border: 214.3 31.8% 91.4%;
    --input: 214.3 31.8% 91.4%;
    --ring: 221.2 83.2% 53.3%;
    --radius: 0.5rem;
  }

  .dark {
    --background: 222.2 84% 4.9%;
    --foreground: 210 40% 98%;
    --primary: 217.2 91.2% 59.8%;
    --primary-foreground: 222.2 47.4% 11.2%;
    --secondary: 217.2 32.6% 17.5%;
    --secondary-foreground: 210 40% 98%;
    --muted: 217.2 32.6% 17.5%;
    --muted-foreground: 215 20.2% 65.1%;
    --accent: 217.2 32.6% 17.5%;
    --accent-foreground: 210 40% 98%;
    --destructive: 0 62.8% 30.6%;
    --destructive-foreground: 210 40% 98%;
    --border: 217.2 32.6% 17.5%;
    --input: 217.2 32.6% 17.5%;
    --ring: 224.3 76.3% 48%;
  }
}
```

---

## 开发工作流

### 环境准备

```bash
# 1. 安装 Node.js 依赖
npm install

# 2. 安装 Tauri CLI (全局)
npm install -g @tauri-apps/cli

# 3. 安装 Rust (Tauri 依赖)
# 访问 https://www.rust-lang.org/tools/install

# 4. 配置环境变量
cp .env.example .env.local
```

### 开发模式

```bash
# Web 开发模式（仅浏览器）
npm run dev

# 桌面开发模式（Tauri）
npm run tauri dev
```

### 构建生产版本

```bash
# 构建 Web 静态文件
npm run build

# 构建桌面应用
npm run tauri build
```

---

## TypeScript 类型定义

### 核心类型

```typescript
// src/types/entity.ts
export interface EntityType {
  id: string;
  entityTypeName: string;
  entityLabel: string;
  color: string;
  description: string;
}

export interface EntityTypeTemplate {
  templateId: string;
  templateName: string;
  description: string;
  isSystem: boolean;
  tenantId: string | null;
  entityTypes: EntityType[];
  createdAt: string;
  updatedAt: string;
}

// src/types/kb.ts
export interface KnowledgeBase {
  id: string;
  kbId: string;
  kbName: string;
  tenantId: string;
  ownerUserId: string;
  description: string | null;
  createdAt: string;
  updatedAt: string;
}

// src/types/document.ts
export type DocumentStatus =
  | 'UPLOADED'
  | 'OCR_PROCESSING'
  | 'MD_PARSED'
  | 'KB_BUILDING'
  | 'COMPLETED'
  | 'FAILED';

export interface Document {
  id: string;
  kbId: string;
  filename: string;
  fileSize: number;
  fileType: string;
  remoteFilePath: string;
  status: DocumentStatus;
  progress: number;
  errorMessage: string | null;
  entityTemplateId: string | null;
  documentUniqueId: string | null;
  tenantId: string;
  createdAt: string;
  updatedAt: string;
}

// src/types/qa.ts
export interface QARequest {
  kbId: string;
  question: string;
  retrievalMode: 'vector' | 'graph' | 'hybrid';
}

export interface QAResponse {
  answer: string;
  sources: QASource[];
  graphPath: QAGraphPath[];
}

export interface QASource {
  documentId: string;
  filename: string;
  chunks: {
    content: string;
    score: number;
  }[];
}

export interface QAGraphPath {
  entityId: string;
  entityType: string;
  relation: string;
}
```

---

## 性能优化

### 1. 代码分割

```typescript
// 使用 Next.js 动态导入
import dynamic from 'next/dynamic';

const DocumentUpload = dynamic(
  () => import('@/components/document/DocumentUpload'),
  { ssr: false, loading: () => <p>加载中...</p> }
);
```

### 2. 图片优化

```typescript
import Image from 'next/image';

<Image
  src="/logo.png"
  alt="Logo"
  width={200}
  height={50}
  priority
/>
```

### 3. 虚拟滚动

```typescript
// 使用 react-virtual 处理长列表
import { useVirtualizer } from '@tanstack/react-virtual';

// 适用于文档列表、问答历史等
```

---

## 错误处理

### 统一错误处理

```typescript
// src/lib/utils/errorHandler.ts
export class ApiError extends Error {
  constructor(
    public message: string,
    public statusCode: number,
    public details?: any
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

export const handleApiError = (error: any) => {
  if (error instanceof ApiError) {
    // 处理已知 API 错误
    console.error('API Error:', error.message);
    return error.message;
  }

  if (error.response) {
    // Axios 错误
    console.error('Response Error:', error.response.data);
    return error.response.data.message || '请求失败';
  }

  if (error.request) {
    // 网络错误
    console.error('Network Error:', error.message);
    return '网络连接失败，请检查网络';
  }

  // 未知错误
  console.error('Unknown Error:', error);
  return '发生未知错误';
};
```

---

## 测试策略

### 单元测试

```bash
# 安装测试依赖
npm install -D jest @testing-library/react @testing-library/jest-dom

# 运行测试
npm test
```

### E2E 测试

```bash
# 安装 Playwright
npm install -D @playwright/test

# 运行 E2E 测试
npm run test:e2e
```

---

## 部署指南

### Web 部署

```bash
# 构建 Next.js 静态文件
npm run build

# 部署到 Vercel
vercel --prod

# 或部署到 Nginx
# 将 out/ 目录复制到 Nginx 静态文件目录
```

### 桌面应用分发

```bash
# 构建桌面应用
npm run tauri build

# 生成的安装包位于
# - Windows: src-tauri/target/release/bundle/msi/
# - macOS: src-tauri/target/release/bundle/dmg/
# - Linux: src-tauri/target/release/bundle/appimage/
```

---

## 开发规范

### 代码风格

- 使用 ESLint + Prettier
- 遵循 Airbnb React/TypeScript 规范
- 组件命名采用 PascalCase
- 文件命名采用 kebab-case 或 PascalCase（组件）

### Git 提交规范

```
feat: 新功能
fix: 修复 Bug
docs: 文档更新
style: 代码格式调整
refactor: 重构
test: 测试相关
chore: 构建/工具链相关

示例:
feat(kb): 添加知识库创建功能
fix(document): 修复文档上传进度显示错误
```

### 分支管理

```
main          # 主分支（生产环境）
develop       # 开发分支
feature/*     # 功能分支
bugfix/*      # 修复分支
hotfix/*      # 紧急修复分支
```

---

## 常见问题

### Q1: Tauri 开发环境配置失败？

**A**: 确保：
- 已安装 Rust 工具链
- 已安装系统依赖（Windows: WebView2, macOS: Xcode, Linux: webkit2gtk）
- Node.js 版本 >= 18

### Q2: Next.js 开发模式热更新失败？

**A**: 检查：
- 端口 3000 是否被占用
- `.env.local` 配置是否正确
- 清除 `.next` 缓存目录

### Q3: API 请求跨域问题？

**A**: 开发环境配置代理：
```javascript
// next.config.js
module.exports = {
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: 'http://localhost:8080/api/:path*',
      },
    ];
  },
};
```

### Q4: 桌面应用打包后 API 请求失败？

**A**: 检查 `tauri.conf.json` 中的 CSP 配置：
```json
"security": {
  "csp": "default-src 'self'; connect-src 'self' http://localhost:* https://your-api-domain.com"
}
```

---

## 参考资料

- [Tauri 官方文档](https://tauri.app/v1/guides/)
- [Next.js 官方文档](https://nextjs.org/docs)
- [shadcn/ui 组件库](https://ui.shadcn.com/)
- [React Hook Form](https://react-hook-form.com/)
- [Zustand 状态管理](https://docs.pmnd.rs/zustand)

---

## 开发进度

### ✅ 已完成 (Phase 1: 用户认证与基础架构)

#### 1. 项目初始化
- [x] Next.js 14.x 项目配置（App Router）
- [x] TypeScript 配置
- [x] Tailwind CSS + 主题系统（黑白双色调）
- [x] ESLint + Prettier 配置
- [x] 目录结构创建
- [x] .gitignore 配置

#### 2. UI 组件库 (shadcn/ui)
- [x] Button 组件
- [x] Input 组件
- [x] Card 组件
- [x] Label 组件
- [x] Dialog 组件
- [x] Table 组件
- [x] 全局样式和主题变量（支持明暗主题）

#### 3. API 客户端
- [x] Axios 实例配置
- [x] 请求/响应拦截器
- [x] 统一错误处理 (ApiError 类)
- [x] Token 自动注入
- [x] 401 自动跳转登录

#### 4. 用户认证模块
- [x] TypeScript 类型定义 (User, LoginRequest, LoginResponse, UserRole)
- [x] authApi: login, logout, getCurrentUser
- [x] userApi: list, create, update, delete (仅管理员)
- [x] Zustand authStore (状态持久化)
- [x] 登录页面 (`/login`)
- [x] 权限控制中间件

#### 5. 用户管理模块 (仅管理员)
- [x] 用户列表页面 (`/users`)
- [x] 创建用户对话框
- [x] 删除用户确认对话框
- [x] 角色显示（管理员/普通用户）
- [x] 租户隔离（只能创建同租户用户）

#### 6. 布局组件
- [x] Header 组件（用户信息、登出按钮）
- [x] Sidebar 组件（导航菜单）
- [x] MainLayout 组件（权限检查）
- [x] 仪表盘页面 (`/dashboard`)

#### 7. Tauri 配置
- [x] Cargo.toml 配置
- [x] tauri.conf.json 配置
- [x] Rust 主入口文件
- [x] 窗口配置（1280x800）
- [x] 构建脚本配置

### 🚧 进行中

暂无

### 📋 待开发 (Phase 2: 知识库管理)

#### 1. 知识库管理模块
- [ ] 知识库列表页面 (`/knowledge-base`)
- [ ] 创建知识库对话框
- [ ] 知识库详情页面
- [ ] 知识库删除功能
- [ ] 知识库 API 客户端 (kbApi)
- [ ] 知识库 Zustand store

#### 2. 实体类型模板模块
- [ ] 实体类型模板列表 (`/entity-templates`)
- [ ] 创建自定义模板对话框
- [ ] 模板预览组件
- [ ] 实体类型 API 客户端

### 📋 待开发 (Phase 3: 文档管理)

#### 1. 文档管理模块
- [ ] 文档列表页面 (`/document`)
- [ ] 文档上传组件（拖拽上传）
- [ ] 文档详情页面
- [ ] 文档处理进度实时更新
- [ ] 文档删除功能
- [ ] 文档 API 客户端 (documentApi)
- [ ] 文档 Zustand store

### 📋 待开发 (Phase 4: 智能问答)

#### 1. 问答模块
- [ ] 问答界面 (`/qa`)
- [ ] 聊天式 UI 组件
- [ ] 知识库选择器
- [ ] 答案来源引用组件
- [ ] 问答历史记录
- [ ] 问答 API 客户端 (qaApi)
- [ ] 流式响应处理（SSE / WebSocket）
- [ ] Markdown 渲染

### 📋 待开发 (Phase 5: 高级功能)

#### 1. 主题切换
- [ ] 明暗主题切换按钮
- [ ] 主题持久化
- [ ] 系统主题自动检测

#### 2. 图谱可视化
- [ ] Neo4j 图谱展示
- [ ] 实体关系可视化
- [ ] 交互式图谱探索

#### 3. 数据导出
- [ ] 文档导出
- [ ] 问答历史导出
- [ ] 知识库数据导出

### 📋 待优化

#### 1. 性能优化
- [ ] 代码分割（动态导入）
- [ ] 虚拟滚动（长列表）
- [ ] 图片优化
- [ ] 缓存策略

#### 2. 测试
- [ ] 单元测试（Jest + React Testing Library）
- [ ] E2E 测试（Playwright）
- [ ] API 测试

#### 3. 部署
- [ ] Docker 配置
- [ ] CI/CD 配置
- [ ] 生产环境优化

---

## 版本历史

- **v1.0.0** (2026-01-28): 初始版本，核心功能规划
- **v1.1.0** (2026-01-28): Phase 1 完成 - 用户认证与基础架构
