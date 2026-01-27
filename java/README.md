# Sentra - 企业级知识问答与知识图谱平台

Sentra 是一个面向多租户的企业级知识问答与知识图谱平台，支持文档上传、解析、结构化存储以及基于知识库的问答。

```bash
   $env:JAVA_HOME = "D:\java\jdk17"
   $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

   cd nacos/bin
   .\startup.cmd -m standalone
   sh startup.sh -m standalone
```

## 🛠 技术栈

- **开发语言**: Java 17
- **框架**: Spring Boot 3.2.0, Spring Cloud 2023.0.0, Spring Cloud Alibaba 2023.0.1.0
- **数据库**:
    - PostgreSQL (核心关系数据)
    - MongoDB (文档存储)
    - Neo4j (知识图谱)
    - Redis (缓存 & Session)
    - Elasticsearch (全文检索)
- **消息队列**: RabbitMQ
- **注册中心/配置中心**: Nacos
- **鉴权**: Sa-Token

## 📂 模块说明

- `sentra-common`: 公共模块，包含通用工具、基类、全局异常处理及多租户上下文。
- `sentra-user-service`: 用户与租户服务，负责租户管理、用户认证与权限控制。
- `sentra-knowledge-service`: 知识库服务，负责文档上传、解析任务调度及知识图谱构建。
- `sentra-agent-service`: (预留) Agent 推理服务。

## 🚀 快速开始

### 1. 环境准备

请确保本地或服务器已安装以下中间件：

- **Nacos**: 2.x (默认端口 8848)
- **PostgreSQL**: 14+ (默认端口 5432)
- **MongoDB**: 5.0+ (默认端口 27017)
- **Neo4j**: 5.x (默认端口 7687)
- **RabbitMQ**: 3.9+ (默认端口 5672)
- **Elasticsearch**: 8.x (默认端口 9200)

### 2. 数据库初始化

#### 2.1 创建PostgreSQL数据库

请在 PostgreSQL 中创建以下数据库：

```sql
CREATE DATABASE sentra_user;
CREATE DATABASE sentra_knowledge;
```

如果使用Docker部署PostgreSQL，可以通过以下命令创建：

```bash
# 连接到PostgreSQL容器
docker exec -it postgres-sentra psql -U postgres

# 创建数据库
CREATE DATABASE sentra_user;
CREATE DATABASE sentra_knowledge;

# 验证
\l

# 退出
\q
```

#### 2.2 初始化数据库表结构

创建完数据库后，需要执行SQL初始化脚本来创建表结构和初始数据。

**方式一：通过Docker命令执行（推荐）**

```bash
# Windows PowerShell/CMD
docker exec -i postgres-sentra psql -U postgres -d sentra_knowledge < "G:\项目成果打包\基于图结构的文档问答助手\dev\init_db.sql"

# Linux/Mac
docker exec -i postgres-sentra psql -U postgres -d sentra_knowledge < /path/to/init_db.sql
```

**方式二：交互式执行**

```bash
# 连接到数据库
docker exec -it postgres-sentra psql -U postgres -d sentra_knowledge

# 然后复制粘贴 init_db.sql 文件内容执行
```

**SQL脚本说明：**

`init_db.sql` 脚本会创建以下表结构并插入初始数据：

- `t_entity_type_template` - 实体类型模板表（如：合同领域、论文领域）
- `t_entity_type_definition` - 实体类型定义表（具体的实体类型）
- `t_knowledge_base` - 知识库表
- `t_document` - 文档表
- 系统预置数据：
  - 合同领域模板（包含18种实体类型：合同主体、金额、日期条款等）
  - 论文领域模板（包含8种实体类型：作者、机构、关键词等）

> **注意**：`sentra_user` 数据库的表结构会在首次启动 `sentra-user-service` 时由JPA自动创建（`ddl-auto: update`配置）。

### 3. 配置修改

修改各服务 `src/main/resources/application.yml` 中的数据源配置：

- **sentra-user-service**: 修改 PostgreSQL 连接信息。
- **sentra-knowledge-service**: 修改 PostgreSQL, MongoDB, Neo4j, Elasticsearch, RabbitMQ 连接信息。

### 4. 编译构建

在项目根目录 (`sentra`) 执行：
```bash
mvn clean install
```

### 5. 启动服务

请按照以下顺序启动服务：

1.  **启动 Nacos**
2.  **启动 SentraUserApplication** (`sentra-user-service`)
    - 端口: 8081
3.  **启动 SentraKnowledgeApplication** (`sentra-knowledge-service`)
    - 端口: 8082

## 🔌 接口测试

### 用户/租户服务 (Port: 8081)

**1. 创建租户**
```http
POST /v1/tenant
Content-Type: application/json

{
  "name": "测试租户",
  "type": "FREE"
}
```

**2. 登录 (示例)**
```http
POST /v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password"
}
```

### 知识库服务 (Port: 8082)

**1. 创建知识库**
```http
POST /v1/kb
Content-Type: application/json
X-Tenant-Id: {tenant_id}

{
  "name": "公司制度库",
  "scope": "TENANT",
  "type": "DOCUMENT"
}
```

**2. 上传文档**
```http
POST /v1/document/upload
Content-Type: multipart/form-data
X-Tenant-Id: {tenant_id}

kbId: {kb_id}
file: (选择文件)
```
