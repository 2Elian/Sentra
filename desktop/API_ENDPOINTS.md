# Sentra 后端服务端口配置说明

## 📡 服务端口映射

### Java 后端服务

| 服务名称 | 端口 | 用途 | 前端 Axios 实例 |
|---------|------|------|----------------|
| **sentra-user-service** | 8081 | 用户认证、用户管理 | `userApiClient` |
| **sentra-knowledge-service** | 8082 | 知识库管理、文档管理、OCR | `knowledgeApiClient` |

### Python 后端服务

| 服务名称 | 端口 | 用途 |
|---------|------|------|
| **Python 服务** | 8000 | OCR 解析、Markdown 重构、知识图谱处理 |

## 🔌 前端 API 客户端配置

### 1. Axios 实例定义

```typescript
// lib/api/axios.ts

// 用户服务 Axios 实例
export const userApiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_USER_API_URL || 'http://localhost:8081',
  timeout: 30000,
});

// 知识库服务 Axios 实例
export const knowledgeApiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_KNOWLEDGE_API_URL || 'http://localhost:8082',
  timeout: 30000,
});

// 默认实例（使用用户服务）
export const apiClient = userApiClient;
```

### 2. 环境变量配置

```env
# .env.local
NEXT_PUBLIC_USER_API_URL=http://localhost:8081
NEXT_PUBLIC_KNOWLEDGE_API_URL=http://localhost:8082
NEXT_PUBLIC_PYTHON_API_URL=http://localhost:8000
```

### 3. Next.js Rewrite 规则

```javascript
// next.config.mjs
async rewrites() {
  return [
    {
      source: '/api/user/:path*',
      destination: 'http://localhost:8081/api/:path*',
    },
    {
      source: '/api/knowledge/:path*',
      destination: 'http://localhost:8082/api/:path*',
    },
    {
      source: '/api/python/:path*',
      destination: 'http://localhost:8000/api/:path*',
    },
  ];
}
```

## 📂 API 端点映射

### 用户服务 (8081)

| 功能 | HTTP 方法 | 端点 | 前端 API |
|------|-----------|------|----------|
| 用户登录 | POST | `/v1/auth/login` | `authApi.login()` |
| 用户登出 | POST | `/v1/auth/logout` | `authApi.logout()` |
| 获取当前用户 | GET | `/v1/auth/me` | `authApi.getCurrentUser()` |
| 获取用户列表 | GET | `/v1/user/list` | `userApi.list()` |
| 创建用户 | POST | `/v1/user/create` | `userApi.create()` |
| 更新用户 | PUT | `/v1/user/:id` | `userApi.update()` |
| 删除用户 | DELETE | `/v1/user/:id` | `userApi.delete()` |

### 知识库服务 (8082)

| 功能 | HTTP 方法 | 端点 | 前端 API |
|------|-----------|------|----------|
| 获取知识库列表 | GET | `/v1/kb/list` | 待实现 |
| 创建知识库 | POST | `/v1/kb/build` | 待实现 |
| 删除知识库 | DELETE | `/v1/kb/:id` | 待实现 |
| 获取知识库详情 | GET | `/v1/kb/:id` | 待实现 |
| 上传文档 | POST | `/v1/document/upload` | 待实现 |
| 获取文档列表 | GET | `/v1/document/list` | 待实现 |
| 删除文档 | DELETE | `/v1/document/:id` | 待实现 |

### Python 服务 (8000)

| 功能 | HTTP 方法 | 端点 | 用途 |
|------|-----------|------|------|
| Markdown 解析 | POST | `/api/v1/md_parse` | 重构 Markdown 内容 |
| 问答接口 | POST | `/api/v1/qa/ask` | 智能问答 |
| 知识库构建 | POST | `/api/v1/kb/build` | 构建知识图谱 |

## 🔐 认证机制

所有 API 实例都配置了相同的拦截器：

1. **请求拦截器**: 自动从 localStorage 读取 Token 并添加到请求头
   ```typescript
   config.headers.Authorization = `Bearer ${token}`
   ```

2. **响应拦截器**:
   - 401 状态码: 自动跳转登录页
   - 其他错误: 统一错误处理，显示错误信息

## 🚀 使用示例

### 用户服务示例

```typescript
import { userApiClient } from '@/lib/api/axios';

// 直接使用 userApiClient
const response = await userApiClient.get('/api/v1/user/list', {
  params: { tenantId: 'xxx' }
});

// 或使用封装好的 API
import { authApi, userApi } from '@/lib/api/userApi';
const user = await authApi.login({ username, password });
```

### 知识库服务示例（待实现）

```typescript
import { knowledgeApiClient } from '@/lib/api/axios';

// 直接使用 knowledgeApiClient
const kbList = await knowledgeApiClient.get('/api/v1/kb/list', {
  params: { tenantId: 'xxx' }
});

// 或使用封装好的 API（待实现）
import { kbApi } from '@/lib/api/kbApi';
const kbs = await kbApi.list(tenantId);
```

## ⚠️ 注意事项

1. **端口冲突**: 确保后端服务已启动且端口未被占用
2. **CORS 配置**: 开发环境使用 Next.js rewrite 解决 CORS
3. **Token 管理**: Token 存储在 localStorage，所有请求自动携带
4. **多实例切换**: 根据业务需求选择正确的 Axios 实例

## 📝 开发建议

1. **新增 API 时**:
   - 用户相关: 使用 `userApiClient`
   - 知识库相关: 使用 `knowledgeApiClient`
   - 文档相关: 使用 `knowledgeApiClient`
   - OCR 相关: 后端会调用 Python 服务

2. **API 封装**:
   - 在 `lib/api/` 下创建对应的 API 文件（如 `kbApi.ts`）
   - 封装具体的业务接口
   - 使用 TypeScript 类型定义

3. **错误处理**:
   - 使用 `ApiError` 类统一处理错误
   - 在组件中通过 try-catch 捕获并显示错误
