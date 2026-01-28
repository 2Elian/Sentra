# Sentra 前端应用

基于 Tauri + Next.js + React 的桌面端和 Web 端前端应用。

## 技术栈

- **桌面框架**: Tauri 2.x
- **Web 框架**: Next.js 14.x (App Router)
- **UI 框架**: React 18.x
- **状态管理**: Zustand
- **样式方案**: Tailwind CSS
- **组件库**: shadcn/ui
- **类型检查**: TypeScript

## 快速开始

### 环境要求

- Node.js >= 18
- Rust (用于 Tauri)
- 系统依赖:
  - Windows: WebView2
  - macOS: Xcode
  - Linux: webkit2gtk

### 后端服务端口

确保以下后端服务正在运行：

| 服务 | 端口 | 说明 |
|------|------|------|
| sentra-user-service | 8081 | 用户认证和管理 |
| sentra-knowledge-service | 8082 | 知识库和文档管理 |
| Python 服务 | 8000 | OCR 和知识图谱处理 |

详见 [API_ENDPOINTS.md](./API_ENDPOINTS.md)

### 安装依赖

```bash
npm install
cp .env.example .env.local
```

### 开发模式

**⚠️ 重要提示**：
- **推荐使用 Web 开发模式**进行日常开发（更好的开发体验）
- 桌面模式用于打包和最终测试
- 详见 [TAURI_GUIDE.md](./TAURI_GUIDE.md)

```bash
# Web 开发模式（推荐，浏览器）
npm run dev

# 或使用启动脚本
./START.bat          # Windows
./START.sh web       # Linux/macOS

# 桌面开发模式（Tauri，用于打包测试）
npm run tauri dev

# 或使用启动脚本
./START.bat desktop  # Windows
./START.sh desktop   # Linux/macOS
```

### 构建生产版本

```bash
# 构建 Web 静态文件
npm run build

# 构建桌面应用
npm run tauri build
```

## 项目结构

```
desktop-dev/
├── src-tauri/           # Tauri Rust 后端
├── src/                 # Next.js 前端源码
│   ├── app/            # Next.js App Router
│   ├── components/     # React 组件
│   ├── lib/            # 工具库
│   ├── types/          # TypeScript 类型
│   └── styles/         # 样式文件
├── public/             # 静态资源
└── FRONTEND_DEVELOPMENT.md  # 详细开发文档
```

## 核心功能

### ✅ 已实现 (Phase 1)

#### 1. 用户认证
- ✅ 登录/登出
- ✅ JWT Token 管理
- ✅ 租户隔离
- ✅ 权限控制（管理员/普通用户）

#### 2. 用户管理（管理员）
- ✅ 用户列表
- ✅ 创建用户
- ✅ 删除用户
- ✅ 角色管理

### 🚧 待开发

#### 3. 知识库管理
- ⏳ 知识库列表
- ⏳ 创建/删除知识库
- ⏳ 知识库详情

#### 4. 文档管理
- ⏳ PDF 文档上传
- ⏳ 文档列表和搜索
- ⏳ 处理进度跟踪

#### 5. 智能问答
- ⏳ 聊天式问答界面
- ⏳ 答案来源引用
- ⏳ 问答历史

#### 6. 实体类型模板
- ⏳ 模板管理
- ⏳ 自定义模板创建

## 文档

- **[GETTING_STARTED.md](./GETTING_STARTED.md)** - 快速启动指南
- **[FRONTEND_DEVELOPMENT.md](./FRONTEND_DEVELOPMENT.md)** - 详细开发文档和进度追踪
- **[API_ENDPOINTS.md](./API_ENDPOINTS.md)** - 后端服务端口配置和 API 映射
- **[TAURI_GUIDE.md](./TAURI_GUIDE.md)** - Tauri 桌面应用开发指南（必读）

## 后端 API

后端服务地址（开发环境）:
- **用户服务**: http://localhost:8081
- **知识库服务**: http://localhost:8082
- **Python 服务**: http://localhost:8000

## 常见问题

### 1. 后端服务未启动？

确保以下服务正在运行：
- 用户服务 (8081)
- 知识库服务 (8082)
- Python 服务 (8000)

查看 [API_ENDPOINTS.md](./API_ENDPOINTS.md) 了解详情。

### 2. Tauri 环境配置
确保已安装 Rust 和系统依赖。详见 [FRONTEND_DEVELOPMENT.md](./FRONTEND_DEVELOPMENT.md#q1-tauri-开发环境配置失败)

### 3. API 跨域问题
开发环境通过 Next.js rewrites 代理 API 请求，生产环境需配置后端 CORS。

## License

MIT
