# 自主建模规则驱动造数引擎 (MetaGen) — 改造文档

## 1. 改造概述

本次改造新增了"自主建模规则驱动造数引擎"（MetaGen）模块。该模块允许用户通过手动定义元数据（Metadata）来构建文件骨架、业务规则与逻辑依赖，从而实现高可靠、强校验的金融级报文批量生产。

系统采用"**完全自主建模**"路径，弃用样板解析推断模式。核心设计理念为：

- **手动建模驱动**：用户定义字段规则（FIXED/DATE/ENUM/REF_FILE/SEQ/SUM/COUNT/RANDOM/AMOUNT/EXPRESSION），引擎按照规则执行生成
- **单向引用约束**：字段只能引用前面已定义的字段，从物理上杜绝循环依赖
- **二层嵌套结构**：字段支持父子层级（`5.0 → 5.1, 5.2`），定长模式下子字段长度之和必须严格等于父字段
- **预览沙箱化**：预览不更新序列号/游标，保护正式生产环境
- **流式写盘 + 原子重命名**：大数据量采用 `BufferedWriter` 流式写盘，先生成 `.tmp` 文件，成功后 `rename` 为正式文件

---

## 2. 文件变更清单

### 2.1 后端新增文件（20个）

| 层级 | 文件名 | 说明 |
|------|--------|------|
| Entity | `MetaFileModel.java` | 模型主表实体 |
| Entity | `MetaFieldDefinition.java` | 字段定义实体，含 `sort_index` / `rule_config_json` |
| Entity | `MetaEnumLibrary.java` | 枚举库实体 |
| Entity | `MetaRefFile.java` | 引用素材文件实体 |
| Entity | `MetaSequenceTracker.java` | 序列号/游标追踪实体 |
| Entity | `MetaEntityFile.java` | 生成历史记录实体 |
| Mapper | `MetaFileModelMapper.java` | 模型表 Mapper |
| Mapper | `MetaFieldDefinitionMapper.java` | 字段表 Mapper |
| Mapper | `MetaEnumLibraryMapper.java` | 枚举库 Mapper |
| Mapper | `MetaRefFileMapper.java` | 引用文件 Mapper |
| Mapper | `MetaSequenceTrackerMapper.java` | 序列号追踪 Mapper |
| Mapper | `MetaEntityFileMapper.java` | 生成记录 Mapper |
| Service | `MetaFileModelService.java` + impl | 模型 CRUD、发布、共享、权限 |
| Service | `MetaFieldDefinitionService.java` + impl | 字段批量保存、单向引用校验、SUM 目标查询 |
| Service | `MetaEnumLibraryService.java` + impl | 枚举库管理 |
| Service | `MetaRefFileService.java` + impl | 引用文件上传与配置 |
| Service | `MetaSequenceTrackerService.java` + impl | 序列号锁控（preview 只读不写） |
| Service | `MetaEntityFileService.java` + impl | 生成记录管理 |
| Service | `MetaGenEngineService.java` + impl | **核心引擎**：预览/异步生成/流式写盘/金额 DSL/表达式 DSL |
| Controller | `MetaManagerController.java` | 管理端（`/api/v1/meta`）：模型 CRUD、字段校验、枚举库、引用文件、发布/共享 |
| Controller | `MetaGenController.java` | 执行端（`/api/v1/meta`）：预览、批量生成、状态查询、历史、下载、序列号重置 |

### 2.2 前端新增文件（5个）

| 层级 | 文件名 | 说明 |
|------|--------|------|
| API | `src/api/meta-gen.js` | 全部接口函数集中管理 |
| Component | `src/components/meta-gen/FieldRuleDialog.vue` | 字段规则配置弹窗（10 种规则类型 + 金额 DSL 构造器） |
| Component | `src/components/meta-gen/EnumManageDialog.vue` | 枚举库管理弹窗 |
| Component | `src/components/meta-gen/RefFileDialog.vue` | 引用文件上传与列映射配置弹窗 |
| View | `src/views/MetaGen.vue` | 主页面：三栏布局 + 字段编排 + 字节标尺预览 + 历史记录 |

### 2.3 前端修改文件（1个）

| 文件 | 修改内容 |
|------|----------|
| `src/router/index.js` | 新增 `/meta-gen` 路由，指向 `MetaGen.vue` |

---

## 3. 数据库建表 DDL

以下 6 张表需要在目标数据库（MySQL 8.0，`utf8mb4`）中执行创建。

### 3.1 模型主表

```
CREATE TABLE `meta_file_model` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `model_name` VARCHAR(100) NOT NULL COMMENT '模型名称',
    `has_header` TINYINT(1) DEFAULT 1,
    `has_footer` TINYINT(1) DEFAULT 1,
    `split_type` VARCHAR(20) NOT NULL COMMENT 'DELIMITER 或 FIXED',
    `delimiter` VARCHAR(10) DEFAULT NULL COMMENT '分隔符内容',
    `line_ending_char` VARCHAR(10) DEFAULT NULL COMMENT '行结尾固定符号',
    `encoding` VARCHAR(20) DEFAULT 'UTF-8' COMMENT 'UTF-8/GBK，影响长度计算',
    `max_rows_limit` INT DEFAULT 100000 COMMENT '记录数安全阈值',
    `owner_id` VARCHAR(50) NOT NULL COMMENT '创建人工号',
    `status` VARCHAR(20) DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/DISABLED',
    `model_version` INT DEFAULT 1 COMMENT '模型版本号',
    `shared_with` VARCHAR(100) DEFAULT NULL COMMENT '共享用户工号，逗号分隔',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='文件模型主表';
```

### 3.2 字段定义表

```
CREATE TABLE `meta_field_definition` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `model_id` BIGINT NOT NULL,
    `section` VARCHAR(20) NOT NULL COMMENT 'FILENAME/HEADER/BODY/FOOTER',
    `parent_id` BIGINT DEFAULT NULL COMMENT '仅支持两层，子字段填父ID',
    `field_key` VARCHAR(50) NOT NULL COMMENT '字段唯一变量名',
    `field_name` VARCHAR(100) COMMENT '业务描述',
    `sort_order` VARCHAR(10) NOT NULL COMMENT '前端显示用: 5.0, 5.1',
    `sort_index` INT NOT NULL COMMENT '后端真实排序依据',
    `level` TINYINT DEFAULT 1 COMMENT '1或2',
    `length` INT NOT NULL COMMENT '定长模式下的字节长度',
    `is_required` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否必填：1-是，0-否',
    `padding_direction` VARCHAR(10) DEFAULT 'NONE' COMMENT 'LEFT/RIGHT/NONE',
    `padding_char` VARCHAR(5) DEFAULT ' ',
    `rule_type` VARCHAR(30) NOT NULL COMMENT 'FIXED/DATE/ENUM/REF_FILE/REF_FIELD/SEQ/SUM/COUNT/RANDOM/AMOUNT/EXPRESSION',
    `ref_field_key` VARCHAR(50) COMMENT '引用的同报文内字段Key',
    `ref_enum_key` VARCHAR(50) COMMENT '引用的枚举库Key',
    `ref_sequence_key` VARCHAR(50) COMMENT '引用的序列号Key',
    `ref_file_id` BIGINT COMMENT '引用的素材文件ID',
    `rule_config_json` JSON NOT NULL COMMENT '包含金额DSL、表达式DSL、随机规则等',
    INDEX `idx_model_sort` (`model_id`, `section`, `sort_index`)
) ENGINE=InnoDB COMMENT='字段定义明细表';
```

### 3.3 枚举库表

```
CREATE TABLE `meta_enum_library` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `enum_key` VARCHAR(50) UNIQUE NOT NULL COMMENT '如 sex',
    `enum_name` VARCHAR(100),
    `items` JSON NOT NULL COMMENT '存储: [{"val":"0", "desc":"男"}, {"val":"1", "desc":"女"}]'
) ENGINE=InnoDB COMMENT='枚举库表';
```

### 3.4 引用素材文件表

```
CREATE TABLE `meta_ref_file` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `ref_name` VARCHAR(50) UNIQUE NOT NULL,
    `file_path` VARCHAR(255) NOT NULL,
    `parse_type` VARCHAR(20) NOT NULL COMMENT 'DELIMITER 或 FIXED',
    `delimiter` VARCHAR(10),
    `column_mapping` JSON COMMENT '存储列索引与Key的映射: {"userName":1, "age":2}'
) ENGINE=InnoDB COMMENT='引用素材文件表';
```

### 3.5 序列号与游标追踪表

```
CREATE TABLE `meta_sequence_tracker` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `target_type` VARCHAR(20) NOT NULL COMMENT 'SEQ 或 REF_FILE',
    `target_id` VARCHAR(100) NOT NULL COMMENT 'modelId:fieldKey 或 refFileId',
    `current_value` BIGINT DEFAULT 0,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_target` (`target_type`, `target_id`)
) ENGINE=InnoDB COMMENT='并发锁控与游标表';
```

### 3.6 生成历史表

```
CREATE TABLE `meta_entity_file` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `model_id` BIGINT NOT NULL,
    `file_name` VARCHAR(255) NOT NULL,
    `file_type` VARCHAR(20) NOT NULL COMMENT 'PREVIEW 或 FORMAL',
    `storage_path` VARCHAR(255) NOT NULL,
    `row_count` INT DEFAULT 0,
    `status` VARCHAR(20) DEFAULT 'RUNNING' COMMENT 'SUCCESS/FAILED/RUNNING',
    `create_user` VARCHAR(50),
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `duration_ms` INT COMMENT '耗时（毫秒）',
    `error_msg` TEXT,
    `temp_path` VARCHAR(255) COMMENT '生成中的临时路径'
) ENGINE=InnoDB COMMENT='生成历史与任务表';
```

---

## 4. 配置与部署

### 4.1 后端 application.yml 配置

在 [`application.yml`](java-aicode-server/src/main/resources/application.yml) 中新增以下配置：

```yaml
meta:
  storage:
    preview: ./storage/preview   # 预览文件目录
    formal: ./storage/formal     # 正式文件目录
```

### 4.2 存储目录创建

确保应用运行用户对以下目录有读写权限，首次启动时会自动创建：

- `./storage/preview/` — 预览文件存放
- `./storage/formal/` — 正式文件存放
- `./storage/formal/tmp/` — 生成过程中的临时文件
- `./storage/uploads/` — 引用素材上传目录

### 4.3 异步任务支持

确保 Spring Boot 已启用异步任务支持（[`Application.java`](java-aicode-server/src/main/java/com/bocfintech/allstar/Application.java) 中已有 `@EnableAsync` 注解）。

### 4.4 前端路由访问

前端页面路由为 `/meta-gen`，完整路径：`http://{host}:{port}/#/meta-gen`。

如需在左侧导航栏显示入口，请在 [`Sidebar.vue`](web-aicode-vue/src/components/Layout/Sidebar.vue) 中添加对应菜单项。

---

## 5. 用户操作指南

### 5.1 入口

访问 `/meta-gen` 页面，界面分为三个区域：

| 区域 | 宽度 | 功能 |
|------|------|------|
| **左侧导航栏** | 220px | 展示所有模型列表，支持搜索、新建 |
| **中间配置区** | 自适应 | 模型属性、字段编排、预览、历史记录 |
| **右侧资源区** | 200px | 枚举库和引用文件的快捷查看入口 |

### 5.2 创建模型

1. 点击左侧面板的"新建模型"按钮
2. 在中间面板的"模型属性"区域填写：
   - **名称**：模型的业务名称
   - **编码**：UTF-8 / GBK / ASCII（定长模式强制使用 GBK，禁止 UTF-8）
   - **格式**：定长 (FIXED) 或分隔符 (DELIMITER)
   - **分隔符**：仅在分隔符模式下填写（如 `,` 或 `|`）
   - **换行符**：行结束符（默认 `\r\n`）
   - **最大行数**：安全阈值，防止误操作生成超大文件（默认 100,000）
   - **文件头/尾**：是否包含 Header/Footer 区块
3. 点击"保存模型"

### 5.3 定义字段规则

1. 选择要编辑的区块（文件名 / 文件头 / 文件体 / 文件尾）
2. 点击"新增字段"，弹出规则配置弹窗
3. 填写字段属性：

| 属性 | 说明 |
|------|------|
| **区块** | FILENAME/HEADER/BODY/FOOTER |
| **变量名 (Key)** | 全局唯一标识，用于其他字段引用（如 `userName`） |
| **字段描述** | 业务含义说明 |
| **层级** | 一级字段或二级子字段 |
| **长度 (字节)** | 字段占用的字节数（Unit: Bytes） |
| **补齐方向** | 左补齐/右补齐/不补齐 |
| **补齐字符** | 空格或 0 |

4. 选择规则类型，填写对应配置：

| 规则类型 | 说明 | 配置内容 |
|----------|------|----------|
| `FIXED` | 固定值 | 变量名本身作为值 |
| `DATE` | 日期 | 生成当前日期 |
| `ENUM` | 枚举 | 选择枚举库 Key |
| `REF_FILE` | 引用文件 | 选择已上传的素材文件 |
| `REF_FIELD` | 引用字段 | 下拉列表仅显示当前字段之前定义的 Key |
| `SEQ` | 序列号 | 自动递增，支持带头循环 |
| `SUM` | 汇总金额 | Body 所有行的金额合计（Header/Footer 用） |
| `COUNT` | 统计行数 | Body 总行数（Header/Footer 用） |
| `RANDOM` | 随机值 | 数字/汉字/UUID |
| `AMOUNT` | 金额 | DSL 配置：精度、整数位、小数模式、符号位 |
| `EXPRESSION` | 表达式 | CONCAT 拼接表达式 |

5. 点击"确定"添加到字段列表
6. 点击"保存字段"持久化到数据库

### 5.4 字段层级与排序

- **缩进/反缩进**：点击行右侧的箭头按钮，在一级和二级之间切换
- **上移/下移**：同级字段位置互换，一级字段会带着子字段整体移动
- **行号自动计算**：前端根据 `sort_index` 动态渲染 `1.0 → 1.1, 1.2` 等行号

> ⚠️ 定长模式下，子字段的长度之和**必须严格等于**父字段总长度，保存时会进行校验。

### 5.5 预览

1. 完成字段配置后，点击"刷新预览"
2. 预览区（暗色 Monokai 主题框）将显示生成的 3 行 Body 文本
3. 顶部字节标尺帮助检查定长对齐效果

> ⚠️ 预览模式**不会更新**序列号/游标，对正式生产环境完全透明。

### 5.6 发布模型

1. 确认字段配置无误后，点击"发布"按钮
2. 系统执行单向引用校验，通过后状态变为 `PUBLISHED`
3. **已发布模型字段默认只读**，需修改时先点击"退回草稿"

### 5.7 批量生成

1. 确保模型状态为 `PUBLISHED`
2. 在"生成历史"区域点击"批量生成"
3. 输入生成行数和可选批次名称
4. 系统异步执行：
   - 生成 Body → 流式写入 `body.tmp`
   - 生成 Footer → 汇总值写入 `footer.tmp`
   - 生成 Header → 回填汇总值到 `header.tmp`
   - 合并三个临时文件 → 重命名为正式文件
   - 更新数据库状态为 `SUCCESS`
5. 可在历史列表查看进度和下载文件

### 5.8 共享模型

点击"共享"按钮，输入目标员工工号（多个用逗号分隔），被共享者获得模型的修改权限。

### 5.9 资源管理

- **枚举库**：点击右侧面板或弹窗进入，支持 Key-Value 枚举项管理
- **引用文件**：上传素材文件并配置列映射（如 `{"userName": 1, "age": 2}`）

---

## 6. 注意事项

### 6.1 定长模式限制

- 定长模式（`FIXED`）**强制禁止 UTF-8 编码**，只能使用 GBK 或 ASCII
- 原因：UTF-8 中文占 3 字节，极易导致变长偏移，破坏定长报文结构
- 前端在选择定长模式时自动将编码切换为 GBK

### 6.2 字节长度严格校验

- 所有 `length` 字段代表**字节数**，非字符数
- 生成时若内容字节长度超出定义长度，**直接报错停止**，不自动截断
- 补齐字符必须为单字节字符

### 6.3 单向引用规则

| 区块 | 可引用的范围 |
|------|------------|
| FILENAME | 常量、日期 |
| HEADER | FILENAME 字段、BODY 的 SUM/COUNT 虚拟变量 |
| BODY | 当前行之前已生成的 BODY 字段、FILENAME、HEADER |
| FOOTER | 以上所有 + BODY 汇总值 |

前端在配置 REF_FIELD 规则时，下拉列表**仅显示当前位置之前的字段**，从 UI 层面杜绝非法引用。

### 6.4 并发与性能

- 序列号获取使用 `@Transactional` 保护，预览模式下只读不写
- 大批量生成（10 万+）采用每 1000 行 `flush()` 一次，防止内存溢出
- 不建议在一个事务中持有 `meta_sequence_tracker` 锁直到生成结束

### 6.5 文件安全

- 生成中使用 `.tmp` 临时文件，全部完成后才 `rename` 为正式文件
- 生成失败时立即删除 `.tmp` 文件，防止脏数据残留
- 下载接口校验文件是否存在，正在生成中的文件不可下载

### 6.6 模型状态机

```
DRAFT → PUBLISHED → DISABLED
  ↑        |
  └────────┘ (退回草稿)
```

- `DRAFT`：可自由修改，不可用于正式生成
- `PUBLISHED`：只读，可用于生成；修改前需退草稿
- `DISABLED`：历史保留，不可再发起任务

---

## 7. API 接口清单

所有接口前缀：`/api/meta`

### 7.1 模型管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/models` | 分页查询当前用户可见的模型 |
| POST | `/models` | 创建模型 |
| GET | `/models/{id}` | 获取模型详情 + 字段列表 |
| PUT | `/models/{id}` | 更新模型（已发布模型禁止修改） |
| DELETE | `/models/{id}` | 删除模型及关联字段 |
| POST | `/models/{id}/publish` | 发布模型（含单向引用校验） |
| POST | `/models/{id}/share` | 共享给其他用户 |

### 7.2 字段管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/models/{modelId}/fields` | 批量保存字段 |
| POST | `/fields/validate` | 校验字段合法性 |
| GET | `/fields/sum-targets/{modelId}` | 获取 SUM/COUNT 目标字段列表 |

### 7.3 枚举库

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/enums` | 获取所有枚举 |
| GET | `/enums/keys` | 获取所有枚举 Key |
| POST | `/enums` | 创建/更新枚举 |
| DELETE | `/enums/{id}` | 删除枚举 |

### 7.4 引用文件

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/resources` | 获取所有引用文件 |
| POST | `/resources/upload` | 上传引用文件 |
| POST | `/resources/define` | 配置解析规则和列映射 |

### 7.5 执行与生成

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/execute/preview/{modelId}` | 同步预览（3 行） |
| POST | `/execute/generate` | 异步批量生成 |
| GET | `/execute/status/{taskId}` | 查询生成进度 |
| GET | `/execute/history` | 获取生成历史 |
| GET | `/execute/download/{fileId}` | 下载文件 |
| DELETE | `/execute/file/{fileId}` | 删除生成记录及物理文件 |

### 7.6 系统辅助

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/sys/sequence/reset` | 重置序列号 |
| POST | `/sys/clean-temp` | 手动清理预览临时文件 |

---

## 8. 上线检查清单

- [ ] 6 张 Meta 表已在目标数据库创建
- [ ] `application.yml` 已添加 `meta.storage.preview` 和 `meta.storage.formal` 配置
- [ ] 存储目录（`storage/preview`、`storage/formal`、`storage/uploads`）已创建并授予写权限
- [ ] `@EnableAsync` 注解已启用（`Application.java`）
- [ ] 前端 `router/index.js` 已添加 `/meta-gen` 路由
- [ ] 侧边栏菜单已添加"造数引擎"入口（`Sidebar.vue`）
- [ ] 打包部署并验证页面可正常访问
