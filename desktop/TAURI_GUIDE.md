# Tauri 桌面应用开发指南

## ⚠️ 重要提示

由于 Next.js 静态导出的限制，**桌面端和 Web 端的 API 请求方式不同**。

## 🌐 Web 开发模式（推荐用于开发）

Web 模式支持 API 代理重写，开发体验更好。

```bash
npm run dev
```

**特点**:
- ✅ 支持 API 代理（通过 Next.js rewrites）
- ✅ 热更新速度快
- ✅ 支持中间件
- ✅ 更好的开发体验

## 🖥️ 桌面开发模式（Tauri）

桌面模式用于打包和测试桌面应用。

```bash
npm run tauri dev
```

### 重要配置

### 1. Next.js 配置 (`next.config.mjs`)

```javascript
// Tauri 需要静态导出
output: 'export',
images: {
  unoptimized: true,  // 静态导出不支持图片优化
},
```

**注意**: `output: 'export'` 会导致：
- ❌ 不支持 Next.js 中间件（`middleware.ts`）
- ❌ 不支持 API 路由
- ❌ 不支持图片优化
- ❌ 不支持 rewrites（仅在开发模式有效）

### 2. Tauri 配置 (`src-tauri/tauri.conf.json`)

```json
{
  "build": {
    "beforeDevCommand": "npm run dev",
    "devUrl": "http://localhost:3000",
    "beforeBuildCommand": "npm run build",
    "frontendDist": "../out"  // Next.js 静态导出目录
  }
}
```

### 3. 认证方式

由于没有中间件，需要在**每个组件**中检查认证：

```typescript
'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/lib/stores/authStore';

export default function ProtectedPage() {
  const router = useRouter();
  const { isAuthenticated } = useAuthStore();

  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/login');
    }
  }, [isAuthenticated, router]);

  // 页面内容...
}
```

## 🔄 API 请求差异

### Web 开发模式

在开发模式下，可以使用 Next.js rewrites 代理 API 请求：

```javascript
// next.config.mjs (仅在开发模式有效)
async rewrites() {
  return [
    {
      source: '/api/user/:path*',
      destination: 'http://localhost:8081/api/:path*',
    },
  ];
}
```

前端请求：
```typescript
// 可以使用相对路径
const response = await fetch('/api/user/list?tenantId=xxx');
```

### 桌面模式

由于是静态 HTML，**rewrites 不生效**，必须使用完整的 API URL：

```typescript
// 必须使用完整 URL
const response = await fetch('http://localhost:8081/api/user/list?tenantId=xxx');
```

或者使用配置的环境变量：

```typescript
const API_URL = process.env.NEXT_PUBLIC_USER_API_URL || 'http://localhost:8081';
const response = await fetch(`${API_URL}/api/user/list?tenantId=xxx`);
```

## 🚀 推荐的开发流程

### 1. Web 开发（主要开发阶段）

```bash
npm run dev
```

- 在浏览器中开发和测试
- 使用 Next.js rewrites 代理 API
- 享受热更新和更好的开发体验

### 2. 桌面测试（打包前测试）

```bash
npm run tauri dev
```

- 测试桌面应用功能
- 检查窗口大小、布局等
- 验证 API 请求是否正常

### 3. 桌面打包（生产版本）

```bash
npm run tauri build
```

## 🐛 常见问题

### 1. Tauri 启动失败：找不到 frontendDist

**原因**: Next.js 还没有构建静态文件

**解决**:
```bash
# 先构建一次 Next.js
npm run build

# 然后再启动 Tauri
npm run tauri dev
```

### 2. API 请求失败（桌面模式）

**原因**: 静态导出后，Next.js rewrites 不生效

**解决**: 在代码中使用完整的 API URL，而不是相对路径

### 3. 路由跳转不工作

**原因**: 静态导出后，Next.js 路由行为不同

**解决**: 使用 `<a href="">` 而不是 `Link` 组件，或确保使用客户端路由

### 4. 登录后刷新页面，用户状态丢失

**原因**: Zustand persist 中间件需要正确配置

**解决**: 确保使用了 `localStorage` 持久化

```typescript
// lib/stores/authStore.ts
import { persist } from 'zustand/middleware';

export const useAuthStore = create(
  persist(
    (set) => ({ /* ... */ }),
    { name: 'auth-storage' }
  )
);
```

## 📝 最佳实践

### 1. API 客户端封装

```typescript
// lib/api/axios.ts
const API_BASE_URL = process.env.NEXT_PUBLIC_USER_API_URL || 'http://localhost:8081';

export const userApiClient = axios.create({
  baseURL: API_BASE_URL,  // 使用完整 URL
  timeout: 30000,
});
```

### 2. 组件认证检查

每个需要认证的页面都添加检查：

```typescript
useEffect(() => {
  if (!isAuthenticated) {
    router.push('/login');
  }
}, [isAuthenticated, router]);
```

### 3. 环境变量配置

确保 `.env.local` 配置正确：

```env
NEXT_PUBLIC_USER_API_URL=http://localhost:8081
NEXT_PUBLIC_KNOWLEDGE_API_URL=http://localhost:8082
NEXT_PUBLIC_PYTHON_API_URL=http://localhost:8000
```

## 📚 总结

- **开发优先使用 Web 模式** (`npm run dev`)
- **桌面模式用于打包和最终测试** (`npm run tauri dev`)
- **注意 API 请求方式的差异**
- **每个需要认证的页面都要添加认证检查**
- **使用完整 URL 而不是相对路径**

这样可以在开发时享受 Next.js 的便利，打包时得到原生的桌面应用体验。
