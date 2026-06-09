# Release Notes — aicode v1.0.0

**版本号**: v1.0.0  
**发布日期**: 2026-06-10  
**技术栈**: Java 8 + Spring Boot 2.1.10 / Vue 2.7 + Element UI / MySQL 8.0  
**发布类型**: 首次正式完整版本 (First Official Release)

---

## 1. 版本概述

aicode v1.0.0 是首个正式完整版本，涵盖后端 20 个 Controller、前端 15 个功能页面，提供从数据造数、性能测试方案生成、数据比对迁移、即时通讯到格式化工具等全栈功能支撑。系统遵循内网全栈项目规范，基于 Spring Boot 2.1.10 + Vue 2.7 技术栈构建，采用 Shiro + JWT 统一鉴权，MyBatis Plus 3.5.0 作为 ORM 框架，Element UI 2.15.x 提供前端 UI 组件。

---

## 2. 新功能 / 模块清单

### 2.1 即时通讯（聊天大厅 & 聊天房间）

- **ChatHall 聊天大厅**：支持创建/加入聊天房间，多用户实时沟通。
- **ChatRoom 聊天房间**：基于 WebSocket 的实时消息推送，支持文本消息、文件发送与历史记录查询。
- 后端 WebSocket 服务 (`ChatRoomWebSocketServer`) 支持房间内广播与点对点消息。

### 2.2 投票管理

- 创建投票任务，配置投票选项（单选/多选），实时查看投票结果。
- 后端基于 `VoteTask`、`VoteOption`、`VoteRecord` 三张表实现完整投票生命周期。

### 2.3 资源核查

- 支持按产品 ID 与批次号查询硬件资源申请信息（CPU、内存、存储、中间件等）。
- 提供资源核查结果的汇总与明细展示。

### 2.4 车位预约查询

- 车位预约 (`ParkingBook`) 与预约记录 (`ParkingRecord`) 的查询与管理。
- 后端包含定时任务 (`ParkBookTask`) 支持自动预约逻辑。

### 2.5 数据迁移

- 跨数据库的数据迁移工具，支持源库到目标库的表结构同步与数据迁移。
- 基于 `DbConnectionConfig` 管理数据库连接配置，支持 MySQL、GaussDB 等多种数据库方言。

### 2.6 XMind 转换器

- 上传 XMind 思维导图文件，解析并转换为结构化数据或文本输出。

### 2.7 性能测试方案生成

- **核心引擎 PoiEngine**：基于 Apache POI XWPF 原生操作的自研 Word 模板引擎，替代旧版 poi-tl。
  - 支持三种占位符语法：`${key}` / `$[key]` / `{{key}}`
  - 动态表格行循环：`${列表名.字段名}` 点号占位符自动识别，无需 ForEach 标签
  - 图片自动插入：base64 自动解码（`IMAGE_*` 前缀）与 `ImageInfo` 对象两种方式
  - 空值安全处理：null 替换为空字符串，缺失 key 替换为"未获取到数据"（蓝色字体提示）
  - 跨 Run 断裂修复：段落级文本重组算法自动修复被 Word 拆分格式的占位符
- 从 8 张数据库表（`perf_task`、`perf_task_tran`、`perf_task_batch`、`perf_data_plan` 等）自动查询并组装数据，填充 Word 模板生成完整的性能测试方案文档。
- 支持联机交易表、批量作业表、数据明细表、硬件资源表等的动态生成。

### 2.8 文本比对

- 两段文本的逐行差异比较与高亮显示，适用于配置对比、代码审查等场景。

### 2.9 媒体抓取

- 基于配置的 URL 规则抓取网页媒体内容（图片、视频等），支持抓取任务管理与结果浏览。

### 2.10 自主建模规则驱动造数引擎 (MetaGen) ⭐

- **完全自主建模路径**：用户手动定义元数据（字段规则、引用关系、枚举库）构建文件骨架，引擎按照规则生成高度可靠的金融级报文文件。
- **10 种字段规则类型**：FIXED（固定值）、DATE（日期）、ENUM（枚举）、REF_FILE（引用外部文件）、REF_FIELD（引用同报文内前置字段）、SEQ（序列号）、SUM（汇总金额）、COUNT（统计行数）、RANDOM（随机值）、AMOUNT（金额 DSL）、EXPRESSION（表达式拼接）。
- **单向引用约束**：字段只能引用前面已定义的字段，从物理上杜绝循环依赖。
- **二层嵌套结构**：字段支持父子层级，定长模式下子字段长度之和必须严格等于父字段。
- **流式写盘 + 原子重命名**：大数据量采用 `BufferedWriter` 流式写盘，先生成 `.tmp` 临时文件，成功后再 `rename` 为正式文件，保障文件一致性。
- **预览沙箱化**：预览模式不更新序列号/游标，保护正式生产环境。
- 包含 6 张业务表（`meta_file_model`、`meta_field_definition`、`meta_enum_library`、`meta_ref_file`、`meta_sequence_tracker`、`meta_entity_file`）。
- 前端提供三栏布局（模型列表 + 字段编排 + 资源管理），支持字节标尺定长预览、字段拖拽排序、规则配置弹窗等。

### 2.11 格式化 JSON

- JSON 字符串的格式化、压缩与语法校验。

### 2.12 格式化 XML

- XML 字符串的格式化、压缩与语法校验。

### 2.13 文本提取

- 从混合文本中按正则规则提取关键信息（如电话号码、邮箱、身份证号等）。

---

## 3. 技术架构

| 维度 | 技术选型 |
|------|----------|
| 后端框架 | Spring Boot 2.1.10.RELEASE |
| Java 版本 | Java 8 |
| 构建工具 | Maven |
| ORM | MyBatis Plus 3.5.0 + PageHelper |
| 权限管理 | Shiro 1.4.0 + JWT |
| JSON 处理 | Fastjson 1.2.83 |
| 代码简化 | Lombok |
| 工具库 | Hutool + Guava |
| API 文档 | Swagger 2 (2.9.2) |
| 前端框架 | Vue 2.7.16 |
| UI 组件库 | Element UI 2.15.x |
| 前端构建 | Vue CLI 4.5 (webpack) |
| 数据库 | MySQL 8.0 (utf8mb4) |
| 实时通信 | WebSocket (Spring WebSocket) |
| Word 模板引擎 | PoiEngine (自研，基于 Apache POI XWPF) |
| Excel 处理 | EasyExcel / Jxls |

---

## 4. 后端 API 概览

所有接口前缀统一为 `/api`。

### 4.1 Controller 总览

| Controller | 路径前缀 | 功能描述 |
|------------|----------|----------|
| `ChatRoomController` | `/api/chat-room` | 聊天房间管理 |
| `ChatMessageController` | `/api/chat-message` | 聊天消息管理 |
| `ChatFileController` | `/api/chat-file` | 聊天文件管理 |
| `VoteController` | `/api/vote` | 投票任务与选项管理 |
| `ResourceController` | `/api/resource` | 资源核查 |
| `ParkingBookController` | `/api/park` | 车位预约 |
| `ParkingRecordController` | `/api/park` | 预约记录 |
| `DataMigrationController` | `/api/migration` | 数据迁移 |
| `DbConnectionConfigController` | `/api/db-config` | 数据库连接配置 |
| `XmindConverterController` | `/api/xmind` | XMind 文件转换 |
| `PerformanceController` | `/api/performance` | 性能测试任务管理 |
| `PerformanceDocController` | `/api/performance/doc` | 性能测试方案文档生成 |
| `PerformanceFileController` | `/api/performance/file` | 性能测试文件管理 |
| `PerformanceTranController` | `/api/performance/tran` | 联机交易管理 |
| `TaskController` | `/api/task` | 任务调度管理 |
| `DataCompareTaskController` | `/api/data-compare` | 数据比对任务 |
| `DataCompareResultController` | `/api/data-compare` | 数据比对结果 |
| `MetaManagerController` | `/api/meta` | 造数引擎模型管理 |
| `MetaGenController` | `/api/meta` | 造数引擎执行与生成 |
| `MediaCrawlController` | `/api/media-crawl` | 媒体抓取任务 |
| `SystemController` | `/api/system` | 系统辅助接口 |
| `FileController` | `/api/file` | 通用文件上传下载 |
| `ExtractFileController` | `/api/extract` | 文件提取 |

---

## 5. 前端页面清单

| 路由路径 | 页面名称 | 功能说明 |
|----------|----------|----------|
| `/chat` | 聊天大厅 | 聊天房间列表与创建 |
| `/chat-room` | 聊天房间 | 实时聊天消息 |
| `/vote` | 投票管理 | 投票创建与参与 |
| `/resource-check` | 资源核查 | 硬件资源核查 |
| `/parking-list` | 车位预约查询 | 车位预约记录 |
| `/data-migration` | 数据迁移 | 跨库数据迁移 |
| `/xmind-converter` | XMind 转换 | 思维导图转换 |
| `/performance` | 性能测试方案 | 测试方案生成与管理 |
| `/text-comparison` | 文本比对 | 文本差异对比 |
| `/media-crawl` | 媒体抓取 | 网页媒体抓取 |
| `/meta-gen` | 造数引擎 | 自主建模规则驱动造数 |
| `/format-json` | 格式化 JSON | JSON 格式化/压缩 |
| `/format-xml` | 格式化 XML | XML 格式化/压缩 |
| `/text-extract` | 文本提取 | 正则文本提取 |

---

## 6. 数据库

- 数据库类型：MySQL 8.0
- 字符集：`utf8mb4`
- 初始化脚本：`java-aicode-server/init.sql`、`java-aicode-server/meta_init.sql`、`java-aicode-server/performance_init.sql`
- 增量更新脚本：`java-aicode-server/update-20260517.sql`、`java-aicode-server/update-20260518.sql`、`java-aicode-server/update-20260606.sql`、`java-aicode-server/update-chat-room.sql`

核心业务表涵盖：聊天（`chat_room`、`chat_message`、`chat_file`）、投票（`vote_task`、`vote_option`、`vote_record`）、车位预约（`parking_book`、`parking_record`）、性能测试（`perf_task`、`perf_task_tran`、`perf_task_batch`、`perf_data_plan`、`perf_data_detail`、`performance_resource_info` 等）、造数引擎（`meta_file_model`、`meta_field_definition`、`meta_enum_library`、`meta_ref_file`、`meta_sequence_tracker`、`meta_entity_file`）、数据比对（`data_compare_task`、`data_compare_result`）、数据库配置（`db_connection_config`）、媒体抓取（`media_crawl_task`）等。

---

## 7. 部署说明

### 7.1 后端部署

1. 确保 Java 8 运行环境已安装。
2. 在 `application.yml` 中配置数据库连接（密码使用 Jasypt 加密）。
3. 执行 `init.sql`、`meta_init.sql`、`performance_init.sql` 及增量更新脚本建表。
4. 创建存储目录并授予写权限：
   - `./storage/preview/`
   - `./storage/formal/`
   - `./storage/formal/tmp/`
   - `./storage/uploads/`
5. 使用 Maven 打包：`mvn clean package -DskipTests`。
6. 启动应用：`java -jar target/aicode-1.0-SNAPSHOT.jar`。

### 7.2 前端部署

1. 确保 Node.js 环境已安装。
2. 安装依赖：`npm install`。
3. 开发模式运行：`npm run serve`。
4. 生产构建：`npm run build`，将 `dist/` 目录部署到 Nginx 或内网 Web 服务器。

### 7.3 关键配置项

| 配置项 | 说明 |
|--------|------|
| `meta.storage.preview` | 造数引擎预览文件存储目录 |
| `meta.storage.formal` | 造数引擎正式文件存储目录 |
| `jasypt.encryptor.password` | Jasypt 加密主密钥 |
| 数据库连接 | Spring DataSource 配置 |

---

## 8. 已知限制与注意事项

1. **Java 版本**：严格限制 Java 8，不得使用 Java 11+ 特性。
2. **Vue 版本**：严格限制 Vue 2.7 Options API，不得使用 Vue 3 Composition API。
3. **定长模式编码**：造数引擎定长模式（FIXED）强制使用 GBK 编码，禁止 UTF-8（防止变长偏移破坏报文结构）。
4. **预览沙箱**：造数引擎预览模式不更新序列号/游标，对正式生产环境完全透明。
5. **文件安全**：生成过程中使用 `.tmp` 临时文件，失败时自动清理，防止脏数据残留。
6. **并发控制**：数据库连接配置等敏感操作需注意事务隔离与并发安全。
7. **前端加密**：用户登录密码提交前需使用 crypto-js 进行加密处理。

---

## 9. 后续规划

- 完善代码单元测试与集成测试覆盖率。
- 优化大文件生成场景下的内存占用与响应速度。
- 扩展造数引擎支持更多规则类型（如加权随机、正态分布等）。
- 增强数据迁移模块对更多数据库方言（Oracle、SQL Server）的支持。
- 增加操作审计日志功能。
- 完善 Swagger 接口文档注解覆盖率。

---

## 10. 项目资源

- **工作区**: `d:\GitHub\app-aicode`
- **后端**: `java-aicode-server/`
- **前端**: `web-aicode-vue/`
- **文档**: `docs/`
- **数据库脚本**: `java-aicode-server/*.sql`

---

*本文档为 aicode 项目 v1.0.0 首次正式完整版本的 Release Notes，涵盖所有已完成的功能模块、技术架构、部署说明及已知限制。*