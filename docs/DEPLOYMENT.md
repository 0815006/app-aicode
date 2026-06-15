# AICode 全栈平台 — 部署手册

> **适用版本**：1.0-SNAPSHOT  
> **最后更新**：2026-06-15  
> **适用环境**：Windows / Linux / macOS

---

## 1. 两种部署模式概览

本项目支持两种部署方式，分别对应不同的使用场景：

| 模式 | 目标环境 | 核心文件 | 用途 |
|------|---------|---------|------|
| **模式 A：全家桶一键部署** | 本地开发机（Docker Desktop） | [`deploy/docker-compose.yml`](deploy/docker-compose.yml) | 本地开发/测试，一键拉起全部服务 |
| **模式 B：腾讯云服务器部署** | 腾讯云 CVM（129.211.9.238） | [`deploy/deploy_cloud_backend.bat`](deploy/deploy_cloud_backend.bat) + [`deploy/deploy_cloud_frontend.bat`](deploy/deploy_cloud_frontend.bat) | 生产环境，前后端分离部署 |

---

## 2. 系统架构

### 2.1 模式 A 架构（Docker Compose 本地全家桶）

```
                    ┌──────────────┐
                    │   浏览器       │
                    │  localhost:80  │
                    └──────┬───────┘
                           │
                    ┌──────▼───────┐
                    │  Nginx :80   │  ← web-aicode-vue/Dockerfile 构建
                    │  (容器内)     │    静态文件 + /api 反向代理 + WebSocket
                    └──────┬───────┘
                           │ /api → app:8092
                    ┌──────▼───────┐
                    │ Spring Boot  │  ← java-aicode-server/Dockerfile 多阶段构建
                    │  :8092       │    Java 8 + Playwright Chromium
                    └──────┬───────┘
                           │
                    ┌──────▼───────┐
                    │  MySQL 8.0   │  ← 仅 aicode-internal 桥接网络
                    │   :3306      │     stack_db
                    └──────────────┘
```

- 所有服务在同一个 `docker-compose.yml` 中编排
- MySQL **仅在 `aicode-internal` 桥接网络内部暴露**，开发阶段映射 3306 到宿主机便于数据库工具连接
- 数据持久化：MySQL 通过命名卷 `mysql-data`，文件存储通过命名卷 `aicode-data`
- **不含 Redis**：本平台无需缓存中间件，WebSocket 聊天直接通过 Spring Boot 实现

### 2.2 模式 B 架构（腾讯云生产环境）

```
  用户浏览器
  (realapex.site:8082 或 IP:8082)
       │
  ┌────▼──────────────────────────────────┐
  │  腾讯云 CVM (129.211.9.238)            │
  │                                       │
  │  ┌─────────────────────────────────┐ │
  │  │  Nginx (宿主机直接安装)           │ │
  │  │  listen 8082 ssl                │ │
  │  │  / → /var/www/app-aicode/dist/  │ │
  │  │  /api/ → proxy 127.0.0.1:8092   │ │
  │  │  /ws/ → WebSocket proxy         │ │
  │  └──────────┬──────────────────────┘ │
  │             │                         │
  │  ┌──────────▼──────────────────────┐ │
  │  │  Docker 容器: app-aicode         │ │
  │  │  Port: 8092 (宿主机映射)         │ │
  │  │  cloud-dockerfile 单阶段构建     │ │
  │  │  Playwright 基础镜像 + JRE 8    │ │
  │  └──────────┬──────────────────────┘ │
  │             │ host.docker.internal    │
  │  ┌──────────▼──────────────────────┐ │
  │  │  MySQL 8.0 (宿主机直接安装)      │ │
  │  │  Port: 3306, DB: stack_db       │ │
  │  └─────────────────────────────────┘ │
  │                                       │
  │  /data/aicode-v1 (宿主机数据目录)      │
  └───────────────────────────────────────┘
```

- 后端以 **Docker 容器** 运行（Playwright 基础镜像 + JRE 8），MySQL 和 Nginx 在宿主机直接安装
- DB 连接通过 `--add-host=host.docker.internal:host-gateway` 从容器访问宿主机 MySQL 3306
- 数据目录 `/data/aicode-v1` 挂载到容器，用于文件上传、模板、文档生成、抓取媒体等持久化
- 前端通过 [`deploy/nginx-aicode.conf`](deploy/nginx-aicode.conf) 配置 Nginx（含 HTTPS）

---

## 3. 核心组件

| 组件 | 版本 | 用途 |
|------|------|------|
| Nginx | alpine（模式A）/ 宿主机 1.27（模式B） | 前端静态文件托管 + `/api` 反向代理 + WebSocket |
| Java | **8** (OpenJDK) | 后端运行时（Java 8 项目强制要求） |
| Spring Boot | **2.1.10.RELEASE** | 后端框架 |
| MySQL | **8.0** | 主数据库 (`stack_db`)，utf8mb4 |
| Playwright | **1.41.0** | 浏览器自动化（邮件发送 / 媒体抓取 / 网页爬取） |
| Node.js | 16-alpine（仅构建阶段） | 前端 Vue CLI 编译 |

### 关键端口一览

| 服务 | 容器内端口 | 对外端口（模式A） | 对外端口（模式B） |
|------|----------|-----------------|------------------|
| Nginx | 80 | **80** | **8082** |
| Spring Boot | 8092 | **8092**（调试） | **8092**（Docker映射） |
| MySQL | 3306 | 3306（调试可关） | 3306（宿主机） |

---

## 4. 环境要求

### 4.1 模式 A 所需工具

| 工具 | 最低版本 | 验证命令 |
|------|---------|----------|
| Docker | 24+ | `docker --version` |
| Docker Compose | 2.24+ | `docker compose version` |

**无需本地安装** JDK / Maven / Node.js / MySQL，全部由 Docker 镜像提供（后端多阶段构建包含 Maven 编译）。

### 4.2 模式 B 所需工具（本地开发机）

| 工具 | 最低版本 | 说明 |
|------|---------|------|
| JDK | **8** | Maven 编译后端 |
| Maven | 3.6+ | 后端打包 |
| Node.js | 16+ | 前端 Vue CLI 构建 |
| npm | 8+ | 依赖管理 |
| SSH 客户端 | — | `ssh` 和 `scp` 命令可用，连接腾讯云 |
| 腾讯云服务器 | — | 已安装 Docker、MySQL 8.0、Nginx 1.27 |

---

## 5. 模式 A：本地全家桶一键部署

### 5.1 快速启动

在项目根目录执行：

```bash
docker compose -f deploy/docker-compose.yml up -d
```

此命令将自动：
1. 拉取 `mysql:8.0` 镜像
2. 通过多阶段构建编译后端（Maven 3.6 + JDK 8 编译 → Playwright 基础镜像 + JRE 8 运行）
3. 通过多阶段构建编译前端（Node 16 编译 Vue 2 → Nginx Alpine 运行）
4. 创建 `aicode-internal` 桥接网络
5. 按依赖顺序启动：MySQL → App（等待 MySQL healthy） → Nginx
6. 挂载 MySQL 数据卷 `mysql-data` 和应用数据卷 `aicode-data`

### 5.2 启动后的验证

```bash
# 检查容器状态
docker compose -f deploy/docker-compose.yml ps

# 预期输出（healthy 状态关键）
# NAME             STATUS
# aicode-mysql     healthy
# aicode-server    healthy
# aicode-nginx     running
```

```bash
# 健康检查
curl http://localhost:8092/api/system/get-ip
# 返回: {"code":200,"message":"success","data":"..."}（IP 地址）

# 前端页面（通过 Nginx）
curl -I http://localhost
# 返回: HTTP/1.1 200 OK
```

### 5.3 常用命令

```bash
# 查看日志（所有服务）
docker compose -f deploy/docker-compose.yml logs -f

# 查看特定服务日志
docker compose -f deploy/docker-compose.yml logs -f app
docker compose -f deploy/docker-compose.yml logs -f nginx

# 重启单个服务
docker compose -f deploy/docker-compose.yml restart app

# 重新构建并启动（代码变更后）
docker compose -f deploy/docker-compose.yml up -d --build

# 停止所有服务
docker compose -f deploy/docker-compose.yml down

# 停止并删除数据卷（⚠️ 清除数据库和文件数据）
docker compose -f deploy/docker-compose.yml down -v
```

### 5.4 环境变量

后端 [`application.yml`](java-aicode-server/src/main/resources/application.yml) 通过环境变量配置，[`docker-compose.yml`](deploy/docker-compose.yml) 已为 `app` 服务注入：

| 变量 | 默认值 | Docker Compose 覆盖值 | 说明 |
|------|--------|----------------------|------|
| `SERVER_PORT` | `8092` | `8092` | 后端服务端口 |
| `DB_HOST` | `localhost` | `mysql`（容器名） | 数据库主机 |
| `DB_PORT` | `3306` | `3306` | 数据库端口 |
| `DB_NAME` | `stack_db` | `stack_db` | 数据库名称 |
| `DB_USER` | `root` | `root` | 数据库用户名 |
| `DB_PASSWORD` | `root` | `root` | 数据库密码 |
| `DATA_ROOT` | `D:/data/aicode-v1` | `/data/aicode-v1` | 文件存储根目录 |
| `CHROME_EXECUTABLE_PATH` | (Windows路径) | `""`（空） | Docker 内由 Playwright 自行管理浏览器 |
| `FILE_MAX_SIZE` | `500MB` | `500MB` | 文件上传大小限制 |

> 生产环境建议使用 Docker secrets 或 `.env` 文件管理敏感信息，不要将密码硬编码在 compose 文件中。

---

## 6. 模式 B：腾讯云服务器部署

### 6.1 部署流程总览

```
  本地开发机 (Windows)                    腾讯云 CVM (129.211.9.238)
  ──────────────────                     ───────────────────────────
  ① Maven 编译 → target/aicode-1.0-SNAPSHOT.jar
  ② npm run build → dist/               ──scp/ssh──→  /var/www/app-aicode/
  ③ 执行 .bat 脚本                                                │
                                                           ④ Docker build & run
                                                           ⑤ Nginx reload
```

### 6.2 云端前置准备

在腾讯云服务器上一次性完成以下安装（如果尚未配置）：

```bash
# 安装 Docker
curl -fsSL https://get.docker.com | bash
systemctl enable docker && systemctl start docker

# 安装 MySQL 8.0（通过 apt/yum 安装，或使用 Docker）
# 确保创建数据库:
# CREATE DATABASE IF NOT EXISTS stack_db
#   CHARACTER SET utf8mb4
#   COLLATE utf8mb4_unicode_ci;

# 安装 Nginx 1.27
apt install nginx

# 创建数据持久化目录
mkdir -p /data/aicode-v1
```

### 6.3 后端部署

执行 [`deploy/deploy_cloud_backend.bat`](deploy/deploy_cloud_backend.bat)，该脚本自动完成以下步骤：

| 步骤 | 操作 | 说明 |
|------|------|------|
| [1/4] | `mvn clean package -Dmaven.test.skip=true` | 在本地编译后端 JAR（跳过测试） |
| [2/4] | `scp target/aicode-1.0-SNAPSHOT.jar → app.jar` | 上传并重命名 JAR 到服务器 |
| [2/4] | `scp deploy/cloud-dockerfile → Dockerfile` | 上传云端专用 Dockerfile |
| [3/4] | `mkdir -p /data/aicode-v1` | 确保数据目录存在 |
| [4/4] | `docker build -t app-aicode .` | 在服务器构建 Playwright + JRE 8 镜像 |
| [4/4] | `docker stop/rm && docker run` | 重建容器并启动 |

**Docker Run 参数解析**（第42行）：

```bash
docker run -d \
  --name app-aicode \                              # 容器名
  -p 8092:8092 \                                    # 端口映射（宿主机:容器）
  --restart always \                                # 自动重启
  --add-host=host.docker.internal:host-gateway \    # 容器内可访问宿主机 MySQL
  -v /data/aicode-v1:/data/aicode-v1 \              # 数据目录挂载
  -e DB_HOST=host.docker.internal \                 # 数据库指向宿主机
  -e DB_PORT=3306 \
  -e DB_NAME=stack_db \
  -e DB_USER=root \
  -e DB_PASSWORD=root \
  -e DATA_ROOT=/data/aicode-v1 \
  -e SERVER_PORT=8092 \
  app-aicode
```

关键架构决策：
- 后端容器基于 **`mcr.microsoft.com/playwright:v1.41.0-focal`**（已预装 Chromium + 全部系统依赖），仅需补装 `openjdk-8-jre-headless`
- 后端容器通过 `host.docker.internal` 访问**宿主机上的 MySQL**（非容器内 MySQL）
- 数据目录挂载到宿主机 `/data/aicode-v1`，确保容器重启后文件不丢失

### 6.4 前端部署

执行 [`deploy/deploy_cloud_frontend.bat`](deploy/deploy_cloud_frontend.bat)，该脚本自动完成以下步骤：

| 步骤 | 操作 | 说明 |
|------|------|------|
| [1/3] | `npm run build` | 本地 Vue CLI 生产构建 → `dist/` |
| [2/3] | `scp -r dist/* → /var/www/app-aicode/dist/` | 上传静态资源 |
| [3/3] | `scp nginx-aicode.conf → /etc/nginx/conf.d/app-aicode.conf` | 上传 Nginx 配置 |
| [3/3] | `nginx -t && nginx -s reload` | 验证配置并热重载 |

**Nginx 配置** ([`deploy/nginx-aicode.conf`](deploy/nginx-aicode.conf))：

- 监听端口 **8082**（HTTPS），域名 `realapex.site` + IP
- `/` → 静态文件 `/var/www/app-aicode/dist/`（Vue Router History 模式）
- `/api/` → 反向代理到 `http://127.0.0.1:8092/api/`（Docker 后端容器）
- `/ws/` → WebSocket 反向代理到 `http://127.0.0.1:8092/ws/`
- `client_max_body_size 500m` 与后端文件上传大小一致

### 6.5 验证云端部署

```bash
# 后端健康检查
curl http://129.211.9.238:8092/api/system/get-ip

# 前端页面
curl -I http://129.211.9.238:8082
# 返回: HTTP/1.1 200 OK

# 通过域名访问
curl -I https://realapex.site:8082
```

### 6.6 `cloud-dockerfile` 说明

[`deploy/cloud-dockerfile`](deploy/cloud-dockerfile) 与后端源码目录下的 [`java-aicode-server/Dockerfile`](java-aicode-server/Dockerfile) 不同：

| 特性 | 本地 Dockerfile | 云端 cloud-dockerfile |
|------|----------------|----------------------|
| 构建阶段 | **多阶段**（Maven 编译 + Playwright 运行） | **单阶段**（仅 Playwright 运行） |
| 基础镜像 | `maven:3.6.3-jdk-8-slim` → `playwright:v1.41.0-focal` | `playwright:v1.41.0-focal` |
| JAR 来源 | 镜像内 Maven 编译 | 本地编译 → scp 上传 `app.jar` |
| 镜像体积 | 较大（含 Maven 编译层） | 较小（仅运行时） |
| 用途 | 本地无 JDK/Maven 环境时一键构建 | 生产环境瘦镜像快速部署 |

---

## 7. 数据库初始化

### 7.1 创建数据库

```sql
CREATE DATABASE IF NOT EXISTS stack_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci
  COMMENT 'AICode 全栈平台主数据库';
```

> **模式 A**：`docker-compose.yml` 中 `MYSQL_DATABASE: stack_db` 会自动创建数据库，无需手动执行。  
> **模式 B**：需要在云端宿主机 MySQL 中手动执行上述 SQL。

### 7.2 SQL 脚本手工执行

本项目的数据库表结构变更采用**手工 SQL 脚本管理**，不使用 Flyway 等自动迁移工具。所有 SQL 脚本位于 [`java-aicode-server/`](java-aicode-server/) 根目录：

| 脚本文件 | 用途 |
|---------|------|
| [`init.sql`](java-aicode-server/init.sql) | 初始表结构（核心业务表） |
| [`meta_init.sql`](java-aicode-server/meta_init.sql) | 造数引擎相关表 |
| [`performance_init.sql`](java-aicode-server/performance_init.sql) | 性能测试相关表 |
| [`update-20260517.sql`](java-aicode-server/update-20260517.sql) | 增量更新（2026-05-17） |
| [`update-20260518.sql`](java-aicode-server/update-20260518.sql) | 增量更新（2026-05-18） |
| [`update-20260606.sql`](java-aicode-server/update-20260606.sql) | 增量更新（2026-06-06） |
| [`update-chat-room.sql`](java-aicode-server/update-chat-room.sql) | 聊天室功能表 |

**执行顺序**：基础脚本 → 增量更新脚本（按日期先后）。

### 7.3 MySQL 配置建议

在服务器 `my.cnf` 中添加：

```ini
[mysqld]
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci
default-time-zone=+08:00
innodb_buffer_pool_size=512M
max_connections=200
```

---

## 8. 本地开发模式（不依赖 Docker）

如果不想使用 Docker，可以直接在本地运行各个服务。

### 8.1 数据库

确保本地 MySQL 8.0 已启动，执行 [7.1 节](#71-创建数据库) 的建库 SQL，然后按顺序执行 [7.2 节](#72-sql-脚本手工执行) 的 SQL 脚本。

### 8.2 后端启动

```bash
cd java-aicode-server

# 安装依赖并编译
mvn clean install -DskipTests

# 启动（开发模式，热重载需配合 spring-boot-devtools）
mvn spring-boot:run

# 或直接运行 JAR
java -jar target/aicode-1.0-SNAPSHOT.jar
```

验证：

```bash
curl http://localhost:8092/api/system/get-ip
# {"code":200,"message":"success","data":"0:0:0:0:0:0:0:1"}
```

**关于 Playwright**：本地开发需要确保 Chromium 浏览器已安装。如果使用项目配置的 `chrome.executable-path`（`application.yml` 中的 `CHROME_EXECUTABLE_PATH`），需指向有效的 Chrome/Chromium 可执行文件路径。也可通过 Maven profile 安装：

```bash
mvn test -Pinstall-playwright
```

### 8.3 前端启动

```bash
cd web-aicode-vue

# 安装依赖
npm install

# 开发模式启动（热更新）
npm run dev
```

默认端口 **8082**，Vue CLI 代理配置见 [`vue.config.js`](web-aicode-vue/vue.config.js)：

- `/api` → `http://localhost:8092`

访问：`http://localhost:8082`

### 8.4 前端生产构建

```bash
cd web-aicode-vue

# 生产构建
npm run build

# 产物在 dist/ 目录，可直接部署到 Nginx
```

---

## 9. Playwright 专项说明

本项目使用 **Playwright 1.41.0** 进行浏览器自动化，主要应用于以下场景：

| 功能模块 | 对应 Service | 场景 |
|---------|-------------|------|
| 163 邮箱自动发送 | [`BankEmailPlaywrightService`](java-aicode-server/src/main/java/com/bocfintech/allstar/service/impl/BankEmailPlaywrightService.java) | 通过 Chrome CDP 连接已打开的浏览器发送邮件 |
| 媒体资源抓取 | [`CrawlEngineServiceImpl`](java-aicode-server/src/main/java/com/bocfintech/allstar/service/impl/CrawlEngineServiceImpl.java) | 独立启动 Chromium 实例抓取网页媒体 |

### 9.1 本地开发环境（Windows）

- 使用 `chrome.executable-path` 配置指定 Chrome/Chromium 路径
- 邮件发送通过 CDP 调试端口 `9222` 连接已打开的 Chrome 实例
- 媒体抓取由 Playwright 自行启动 managed Chromium 实例

### 9.2 Docker 环境

- 基础镜像 [`mcr.microsoft.com/playwright:v1.41.0-focal`](https://hub.docker.com/_/microsoft-playwright) 已预装 Chromium + 全部系统依赖（libgbm、libasound、libnspr 等）
- `CHROME_EXECUTABLE_PATH` 留空，由 Playwright 自动管理
- 媒体抓取使用 headless Chromium 模式运行

---

## 10. 生产环境安全建议

| 项 | 当前值 | 建议生产值 |
|----|--------|------------|
| MySQL Root 密码 | `root` | 强密码，通过环境变量注入 |
| MySQL 端口 | 对外暴露 | 仅内网访问 |
| 数据目录 | `/data/aicode-v1` | 确保磁盘空间充足，定期备份 |
| Docker Restart Policy | `always`（模式B） | 保持，确保服务自动恢复 |
| Nginx 日志 | 默认 | 配置日志轮转 |
| HTTPS 证书 | `/etc/nginx/cert/` | 确保证书有效期，配置自动续期 |

---

## 11. 常见问题排查

### 11.1 Docker Compose 启动失败

```bash
# 查看完整日志
docker compose -f deploy/docker-compose.yml logs app --tail=100

# 常见原因：
# 1. MySQL 未就绪 → 等待 healthcheck 通过（最多 50 秒）
# 2. 端口被占用 → netstat -ano | findstr "80 8092 3306"（Windows）
# 3. Maven 构建失败 → 检查 pom.xml 依赖是否完整、网络是否可访问 Maven 中央仓库
# 4. Playwright 镜像拉取失败 → 检查 Docker Hub 网络连通性
```

### 11.2 腾讯云端部署失败

```bash
# SSH 到服务器查看容器日志
ssh root@129.211.9.238 "docker logs app-aicode --tail=100"

# 常见原因：
# 1. MySQL 连接失败 → 确认宿主机 MySQL 已启动，root 密码正确，stack_db 已创建
# 2. 端口冲突 → netstat -tlnp | grep 8092
# 3. 容器未启动 → docker ps -a 查看退出状态
# 4. SSH 权限 → 确认已配置免密登录或输入密码
# 5. Playwright 依赖缺失 → 确认使用的是 mcr.microsoft.com/playwright 基础镜像
```

### 11.3 数据库表不存在

```
java.sql.SQLSyntaxErrorException: Table 'stack_db.xxx' doesn't exist
```

确认已按 [7.2 节](#72-sql-脚本手工执行) 顺序执行所有 SQL 脚本。检查方法：

```sql
USE stack_db;
SHOW TABLES;
```

### 11.4 前端代理 404

开发模式下前端无法请求后端 API 时：

1. 确认后端已启动：`curl http://localhost:8092/api/system/get-ip`
2. 检查 Vue CLI 代理目标：查看 [`vue.config.js`](web-aicode-vue/vue.config.js) 中 `devServer.proxy` 的 target
3. Docker 模式下检查 Nginx 配置中 `proxy_pass http://app:8092` 是否正确

### 11.5 端口冲突

```bash
# Windows
netstat -ano | findstr "80 8082 8092 3306"

# Linux
lsof -i :80 -i :8082 -i :8092 -i :3306
```

修改端口：
- 后端：修改 [`application.yml`](java-aicode-server/src/main/resources/application.yml) `SERVER_PORT` 环境变量或默认值
- 前端开发模式：修改 [`vue.config.js`](web-aicode-vue/vue.config.js) `devServer.port`
- Docker Compose：修改 [`docker-compose.yml`](deploy/docker-compose.yml) `ports` 映射
- 云端 Nginx：修改 [`nginx-aicode.conf`](deploy/nginx-aicode.conf) `listen` 端口

### 11.6 云端文件存储不可用

确认容器内挂载正确：

```bash
ssh root@129.211.9.238 "docker exec app-aicode ls /data/aicode-v1"
```

如果目录不存在，在宿主机创建：

```bash
ssh root@129.211.9.238 "mkdir -p /data/aicode-v1"
```

---

## 12. 目录结构速查

```
app-aicode/
├── deploy/                                  # 部署相关文件
│   ├── docker-compose.yml                   # 模式 A：本地全家桶编排
│   ├── cloud-dockerfile                     # 模式 B：云端单阶段构建
│   ├── deploy_cloud_backend.bat             # 模式 B：后端部署脚本（Windows）
│   ├── deploy_cloud_frontend.bat            # 模式 B：前端部署脚本（Windows）
│   └── nginx-aicode.conf                    # 模式 B：云端 Nginx 配置（HTTPS）
├── docs/
│   ├── DEPLOYMENT.md                        # 本文档
│   ├── RELEASE_NOTES.md
│   ├── METAGEN.md
│   └── 163邮箱Playwright自动发送邮件说明.md
├── java-aicode-server/                      # 后端服务
│   ├── Dockerfile                           # 模式 A：多阶段构建（Maven + Playwright）
│   ├── pom.xml                              # Maven 配置（Java 8, Spring Boot 2.1.10）
│   ├── init.sql                             # 初始表结构
│   ├── meta_init.sql                        # 造数引擎表结构
│   ├── performance_init.sql                 # 性能测试表结构
│   ├── update-*.sql                         # 增量更新脚本
│   └── src/main/
│       ├── resources/
│       │   ├── application.yml              # 主配置（端口 8092，参数化）
│       │   └── mapper/                      # MyBatis XML
│       └── java/com/bocfintech/allstar/
│           ├── Application.java             # 启动类
│           ├── bean/ResultBean.java         # 统一响应
│           ├── config/                      # WebConfig, RestTemplateConfig
│           ├── controller/                  # REST 控制器（/api 前缀）
│           ├── entity/                      # 实体类
│           ├── mapper/                      # MyBatis Mapper 接口
│           ├── service/                     # 服务接口
│           │   └── impl/                    # 服务实现（含 Playwright）
│           ├── task/                        # 定时任务
│           ├── util/                        # 工具类
│           └── websocket/                   # WebSocket（聊天室）
└── web-aicode-vue/                          # 前端应用
    ├── Dockerfile                           # 模式 A：多阶段构建（Node 16 → Nginx）
    ├── nginx-default.conf                   # 模式 A：容器内 Nginx 配置
    ├── vue.config.js                        # Vue CLI 开发配置（端口 8082）
    ├── package.json
    └── src/
        ├── api/                             # API 函数（axios 封装）
        ├── components/                      # 组件（按业务目录组织）
        ├── router/                          # Vue Router
        ├── utils/                           # request.js, currentUser.js
        └── views/                           # 页面组件
```

---

> 📌 **关键提醒**  
> 1. 模式 A（Docker Compose）适合本地开发，一条命令拉起全部服务。  
> 2. 模式 B（腾讯云）前端走 Nginx HTTPS 端口 **8082**，后端 Docker 容器端口 **8092**。  
> 3. 数据库密码通过环境变量注入，不要硬编码在配置文件中。  
> 4. 数据库表结构变更通过手工 SQL 脚本管理，**按日期顺序执行**。  
> 5. 云端数据目录 `/data/aicode-v1` 需提前创建并确保磁盘空间充足。  
> 6. Playwright 依赖官方基础镜像，本地开发需确保 Chromium 路径正确或使用 CDP 连接。  
> 7. Windows 下执行 `.bat` 部署脚本前，确保 `ssh` 和 `scp` 命令可用（建议使用 Git Bash 或 OpenSSH）。
