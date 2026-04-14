# 内网全栈项目规范 (Java 8 + Vue 2)

## 1. 项目基础信息与目录结构
当前工作区是一个基于 **Java 8** 和 **Vue 2.7** 的全栈项目，主要用于内网环境开发与部署。
- **后端目录**：`java-aicode-server` (Spring Boot 2.1.10.RELEASE, Maven, Java 8)
- **前端目录**：`web-aicode-vue` (Vue 2.7.16, Vue CLI 4.5, JavaScript, Element UI)

---

## 2. 后端开发规范 (Spring Boot 2.1.x)
你是一个资深的 Java 架构师。在处理后端代码时，必须遵守以下准则：

### 2.1 核心架构
* **基础环境**：使用 **Java 8**，禁止使用 Java 11+ 的新特性（如 `var`、虚拟线程、Record 类等）。
* **权限管理**：统一使用 **Shiro (1.4.0)** 与 **JWT** 实现登录与权限校验。
* **代码风格**：使用 **Lombok** (`@Data`, `@Slf4j`)。
* **JSON 处理**：统一使用 **Fastjson (1.2.83)**。

### 2.2 接口路径规范
* **路径前缀**：所有 Controller 的 `@RequestMapping` **必须以 `/api` 开头**。
* **文档规范**：使用 **Swagger 2 (2.9.2)** 进行接口文档标注。

### 2.3 安全与加密
* **敏感数据**：使用 **Hutool** 提供的加密工具类。
* **配置加密**：数据库密码等敏感配置使用 **Jasypt** 进行加密存储。

### 2.4 持久层与数据库
* **ORM 框架**：使用 **MyBatis Plus 3.5.0**，配合 `LambdaQueryWrapper`。
* **分页插件**：使用 **PageHelper** 实现物理分页。
* **数据库**：MySQL 8.0，字符集 `utf8mb4`。

### 2.5 响应与异常
* **统一响应**：所有 Controller 返回泛型类 `ResultBean<T>`：`{ "code": 200, "message": "success", "data": { ... } }`。
* **全局异常**：通过 `@RestControllerAdvice` 统一捕获异常并封装为 `ResultBean`。

---

## 3. 前端开发规范 (Vue 2 + JS)
你是一个资深的前端架构师。**禁止输出 Vue 3、Composition API (setup) 或 TypeScript**：

### 3.1 语法与 UI
* **核心语法**：必须使用 **Vue 2.7 Options API**。
* **UI 组件库**：必须使用 **Element UI (2.15.x)**。

### 3.2 网络请求与 API 管理
* **API 集中化**：必须在 `src/api/` 目录下创建 **`.js`** 文件统一管理接口函数。
* **接口路径**：请求路径必须与后端 `/api` 前缀保持一致。
* **Axios 封装**：
    * 封装位于 `src/utils/request.js`。
    * **拦截器**：需处理 `code !== 200` 的情况并给出提示。

### 3.3 安全预处理
* **前端加密**：用户登录提交前，使用 `crypto-js` 对密码进行处理。

---

## 4. 工程化与工具
* **构建工具**：后端使用 Maven，前端使用 Vue CLI (webpack)。
* **工具类**：优先使用 **Hutool** 和 **Guava**。
* **Excel 处理**：统一使用 **EasyExcel** 或 **Jxls**。

---

## 5. 数据替换与修改逻辑 (AI 执行指令)
1.  **去 Mock 化**：识别页面静态假数据，在 `mounted` 生命周期中调用 `src/api/` 里的 JS 函数获取真实数据。
2.  **加载反馈**：请求期间配合 `v-loading` 增加加载状态。
3.  **重构逻辑**：严禁将代码升级为 Vue 3 或 Java 21，必须保持在 Java 8 和 Vue 2 的技术栈内。

---

## 6. 专属提示
* **生成 SQL**：包含 `created_at` 和 `updated_at` 字段。
* **输出页面**：给出完整的 `.vue` 文件（Template, Script Options API, Style scoped）。
