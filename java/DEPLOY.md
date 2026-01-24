# Sentra 项目部署与启动指南

本文档详细介绍了如何一键启动 Sentra 后端微服务，以及如何将其打包并部署到云端或 Docker 环境。

---

## 🚀 一键启动（本地开发）

为了方便本地开发时一键启动所有微服务，我们推荐使用 IntelliJ IDEA 的 `Run Dashboard` 或编写简单的批处理脚本。

### 方法一：Windows 批处理脚本 (`start-all.bat`)

在项目根目录创建 `start-all.bat` 文件，内容如下：

```bat
@echo off
echo Starting Sentra Services...

:: 1. 启动 Nacos (请确保已配置环境变量或指定绝对路径)
start "Nacos" cmd /c "startup.cmd -m standalone"

:: 等待 Nacos 启动（根据机器性能调整时间）
timeout /t 10

:: 2. 启动 User Service
start "User Service" java -jar sentra-user-service/target/sentra-user-service-1.0.0-SNAPSHOT.jar

:: 3. 启动 Knowledge Service
start "Knowledge Service" java -jar sentra-knowledge-service/target/sentra-knowledge-service-1.0.0-SNAPSHOT.jar

echo All services started!
pause
```

**使用前准备：**
1. 确保已运行 `mvn clean install` 编译生成了 jar 包。
2. 确保 Nacos、PostgreSQL、Redis 等中间件已在后台运行。

### 方法二：IDEA Run Dashboard (推荐)

1. 在 IDEA 中打开项目。
2. 找到 `Run Dashboard` (View -> Tool Windows -> Services)。
3. 点击 `+` -> `Run Configuration Type` -> `Spring Boot`。
4. 选中 `SentraUserApplication` 和 `SentraKnowledgeApplication`。
5. 点击运行按钮（绿色三角形），即可并行启动所有服务。

---

## 🐳 Docker 容器化部署

### 1. 编写 Dockerfile

为每个服务编写 `Dockerfile`。以 `sentra-user-service` 为例：

**`sentra-user-service/Dockerfile`**
```dockerfile
# 基础镜像
FROM openjdk:17-jdk-slim

# 作者信息
LABEL maintainer="Sentra Team"

# 设置工作目录
WORKDIR /app

# 复制 Jar 包
COPY target/sentra-user-service-1.0.0-SNAPSHOT.jar app.jar

# 暴露端口
EXPOSE 8081

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 2. 编写 docker-compose.yml

在项目根目录创建 `docker-compose.yml`，编排所有服务及中间件：

```yaml
version: '3.8'

services:
  # Nacos
  nacos:
    image: nacos/nacos-server:latest
    container_name: sentra-nacos
    environment:
      - MODE=standalone
    ports:
      - "8848:8848"
    networks:
      - sentra-net

  # User Service
  user-service:
    build: ./sentra-user-service
    container_name: sentra-user-service
    ports:
      - "8081:8081"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR=nacos:8848
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/sentra_user
    depends_on:
      - nacos
      - postgres
    networks:
      - sentra-net

  # Knowledge Service
  knowledge-service:
    build: ./sentra-knowledge-service
    container_name: sentra-knowledge-service
    ports:
      - "8082:8082"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR=nacos:8848
    depends_on:
      - nacos
      - postgres
    networks:
      - sentra-net

  # PostgreSQL
  postgres:
    image: postgres:14
    container_name: sentra-postgres
    environment:
      - POSTGRES_PASSWORD=password
    volumes:
      - pg-data:/var/lib/postgresql/data
    networks:
      - sentra-net

networks:
  sentra-net:
    driver: bridge

volumes:
  pg-data:
```

### 3. 一键打包与运行

```bash
# 1. 编译所有模块
mvn clean package -DskipTests

# 2. 构建并启动 Docker 容器
docker-compose up -d --build
```

---

## ☁️ 云端部署 (CI/CD)

如果要发布到云端（如阿里云、AWS），通常结合 CI/CD 流水线（如 GitHub Actions, Jenkins）：

1.  **代码提交**: 推送代码到 Git 仓库。
2.  **自动构建**: CI 触发 Maven 构建。
3.  **镜像推送**:
    *   构建 Docker 镜像：`docker build -t registry.example.com/sentra-user:v1 .`
    *   推送到私有仓库：`docker push registry.example.com/sentra-user:v1`
4.  **服务部署**:
    *   **K8s**: 更新 Deployment YAML 文件中的镜像版本，执行 `kubectl apply -f deployment.yaml`。
    *   **云服务器**: 登录服务器，拉取新镜像并重启容器 (`docker-compose pull && docker-compose up -d`)。

---

## 📋 注意事项

1.  **配置文件分离**: 生产环境建议使用 Nacos 作为配置中心，将 `application-prod.yml` 中的敏感信息（数据库密码等）放入 Nacos 配置管理，避免硬编码在镜像中。
2.  **网络互通**: 在 Docker 或 K8s 中，服务间调用请使用**服务名**（如 `http://user-service`），而非 `localhost`。
3.  **日志挂载**: 生产环境务必将 `/logs` 目录挂载到宿主机，防止容器重启丢失日志。
