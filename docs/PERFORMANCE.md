# 性能测试方案文档生成 — PoiEngine 标签参考文档

## 1. 概述

### 1.1 架构

```
PerformanceDocServiceImpl.generateDoc()
  ├── assembleData()      → 从 8 张数据库表查询数据，组装 DocGenerateContext
  ├── buildDataModel()    → 构建 Map<String, Object> 统一数据模型
  └── PoiEngine.fillModel(is, os, dataModel)
        ├── handleImageBase64FromSingleData()  → IMAGE_ 开头 base64 → 图片
        ├── replaceImagePlaceholders()         → ImageInfo → 图片
        ├── handleDynamicTable()               → 点号占位符 → 表格行循环
        ├── handleStaticTable()                → 单值占位符 → 表格内文本替换
        └── replaceParagraphPlaceholders()     → 单值占位符 → 段落文本替换
```

### 1.2 引擎

| 项目 | 旧引擎 (poi-tl) | 新引擎 (PoiEngine) |
|------|----------------|---------------------|
| 底层库 | poi-tl 1.12.2 | Apache POI XWPF（原生） |
| 单值占位符 | `{{key}}` | `${key}` / `$[key]` / `{{key}}` 三种语法 |
| 表格循环 | `{{#list}}` … `{{/list}}` ForEach 标签 | `${list.field}` 点号占位符，自动识别 |
| 图片支持 | PictureRenderPolicy | `IMAGE_*` base64 自动解码 + `ImageInfo` 对象 |
| 空值处理 | 原样保留或报错 | 自动替换为"未获取到数据"（蓝色） |
| 跨 Run 断裂 | 不支持 | 段落级文本重组算法自动修复 |

### 1.3 数据来源

| 表名 | 用途 | dataModel key | 查询条件 |
|------|------|---------------|----------|
| `perf_task` | 任务主信息 | 单值（见 §3） | `id = taskId` |
| `perf_task_tran` | 联机交易 | `tran_list` | `task_id = taskId` |
| `perf_task_batch` | 批量作业 | `batch_list` | `task_id = taskId` |
| `perf_task_scene` + `perf_task_scene_detail` | 测试场景 | `scene_list` | `task_id = taskId` |
| `perf_data_plan` | 数据方案定性 | 单值（见 §3） | `task_id = taskId` |
| `perf_data_detail` | 数据方案明细 | `data_detail_list` | `task_id = taskId` |
| `performance_resource_info` | 资源申请 | `resource_list` | `product_id + batch_no + file_source='资源申请表'` |

---

## 2. 占位符语法规范

PoiEngine 统一支持**三种等价语法**，模板中可混用：

| 语法 | 示例 | 说明 |
|------|------|------|
| `${key}` | `${task_name}` | 推荐，最清晰 |
| `$[key]` | `$[task_name]` | 备选 |
| `{{key}}` | `{{task_name}}` | 兼容 poi-tl 旧模板 |

### 2.1 单值替换

占位符 key **不含点号**，直接替换为 `dataModel` 中对应值：

```
${task_name}   →  "2510批次新一代客服系统..."
${batch_no}    →  "2510"
```

### 2.2 表格行循环（动态表格）

占位符 key **含点号**，格式为 `列表名.字段名`。引擎自动检测表格第 2 行（数据模板行）中的点号占位符，提取列表名，根据列表行数复制模板行并逐一填充。

**模板写法**：

| 列 | 单元格内容 |
|----|-----------|
| 序号 | `${tran_list.serialNumber}` |
| 模块 | `${tran_list.moduleName}` |
| 接口方式 | `${tran_list.interfaceType}` |
| 交易名称 | `${tran_list.tranName}` |
| 交易代码 | `${tran_list.tranCode}` |
| 目标TPS | `${tran_list.targetTps}` |
| 目标响应时间 | `${tran_list.targetRt}` |
| 目标成功率 | `${tran_list.targetSuccessRate}` |

> **关键规则**：
> - 表格至少保留 2 行（表头 + 1 行数据模板行）
> - 数据模板行的每个单元格使用 `列表名.字段名` 格式
> - 引擎自动根据数据行数复制模板行
> - **不需要** `{{#list}}` / `{{/list}}` 开始/结束标签

### 2.3 图片占位符

详见 [§4 图片支持](#4-图片支持)。

---

## 3. 完整标签速查表

### 3.1 单值标签（段落/表格通用）

#### 封面与任务信息

| 标签 | 数据来源 | 说明 |
|------|---------|------|
| `${task_name}` | perf_task.task_name | 项目标题 |
| `${test_task_no}` | perf_task.test_task_no | 测试任务编号 |
| `${prod_task_no}` | perf_task.prod_task_no | 生产任务编号 |
| `${product_id}` | perf_task.product_id | 牵头组件/产品标识 |
| `${batch_no}` | perf_task.batch_no | 批次 |
| `${req_no}` | perf_task.req_no | 需求编号 |
| `${proj_name}` | perf_task.proj_name | 项目名称 |
| `${proj_no}` | perf_task.proj_no | 项目编号 |
| `${test_dept}` | perf_task.test_dept | 测试部门 |
| `${dev_dept}` | perf_task.dev_dept | 开发部门 |
| `${perf_manager}` | perf_task.perf_manager | 测试经理 |
| `${test_arch}` | perf_task.test_arch | 测试架构师 |
| `${project_manager}` | perf_task.project_manager | 项目经理 |
| `${start_time}` | perf_task.start_time | 开始时间 (yyyy-MM-dd HH:mm:ss) |
| `${end_time}` | perf_task.end_time | 结束时间 (yyyy-MM-dd HH:mm:ss) |

#### 联机交易汇总

| 标签 | 数据来源 |
|------|---------|
| `${total_user_count}` | perf_task.total_user_count |
| `${daily_online_user_count}` | perf_task.daily_online_user_count |
| `${daily_peak_tps}` | perf_task.daily_peak_tps |
| `${annual_peak_tps}` | perf_task.annual_peak_tps |
| `${selected_tran_tps_sum}` | perf_task.selected_tran_tps_sum |

#### 批量作业汇总

| 标签 | 数据来源 |
|------|---------|
| `${batch_total_duration}` | perf_task.batch_total_duration |
| `${batch_total_data_volume}` | perf_task.batch_total_data_volume |
| `${batch_parallel_degree}` | perf_task.batch_parallel_degree |
| `${batch_max_parallel_count}` | perf_task.batch_max_parallel_count |

#### 数据方案定性（段落文本）

| 标签 | 数据来源 | 典型章节 |
|------|---------|---------|
| `${model_analysis}` | perf_data_plan.model_analysis | 数据内容分析 |
| `${data_constraint}` | perf_data_plan.data_constraint | 数据约束说明 |
| `${data_source_desc}` | perf_data_plan.data_source_desc | 数据来源说明 |
| `${prep_method_desc}` | perf_data_plan.prep_method_desc | 数据构造/准备方法 |
| `${cleaning_rule}` | perf_data_plan.cleaning_rule | 数据脱敏/清洗规则 |

### 3.2 表格循环标签

#### 联机交易表 (`tran_list`)

| 单元格内容 | 字段 | 来源 |
|-----------|------|------|
| `${tran_list.serialNumber}` | 序号 | 自动生成 (1, 2, 3…) |
| `${tran_list.moduleName}` | 模块 | perf_task_tran.module_name |
| `${tran_list.interfaceType}` | 接口方式 | perf_task_tran.interface_type |
| `${tran_list.tranName}` | 交易名称 | perf_task_tran.tran_name |
| `${tran_list.tranCode}` | 交易代码 | perf_task_tran.tran_code |
| `${tran_list.targetTps}` | 目标TPS | perf_task_tran.target_tps |
| `${tran_list.targetRt}` | 目标响应时间 | perf_task_tran.target_rt |
| `${tran_list.targetSuccessRate}` | 目标成功率 | perf_task_tran.target_success_rate |

#### 批量作业表 (`batch_list`)

| 单元格内容 | 字段 | 来源 |
|-----------|------|------|
| `${batch_list.serialNumber}` | 序号 | 自动生成 |
| `${batch_list.jobName}` | 作业名称 | perf_task_batch.job_name |
| `${batch_list.jobDuration}` | 处理时长要求 | perf_task_batch.job_duration |
| `${batch_list.jobDataVolume}` | 数据量级 | perf_task_batch.job_data_volume |
| `${batch_list.jobNo}` | 作业编号 | perf_task_batch.job_no |
| `${batch_list.jobCount}` | 作业数 | perf_task_batch.job_count |
| `${batch_list.jobParallelMode}` | 并行方式 | perf_task_batch.job_parallel_mode |
| `${batch_list.jobDesc}` | 功能描述 | perf_task_batch.job_desc |
| `${batch_list.selectReason}` | 选取原因 | perf_task_batch.select_reason |

#### 数据准备明细表 (`data_detail_list`)

| 单元格内容 | 字段 | 来源 |
|-----------|------|------|
| `${data_detail_list.serialNumber}` | 序号 | 自动生成 |
| `${data_detail_list.dataType}` | 分类 | perf_data_detail.data_type |
| `${data_detail_list.tableNameEn}` | 英文表名 | perf_data_detail.table_name_en |
| `${data_detail_list.tableNameCn}` | 中文表名 | perf_data_detail.table_name_cn |
| `${data_detail_list.tableRowsCount}` | 生产存量(万行) | perf_data_detail.table_rows_count |
| `${data_detail_list.targetRowsCount}` | 目标造数(万行) | perf_data_detail.target_rows_count |
| `${data_detail_list.prepMethod}` | 准备方式 | perf_data_detail.prep_method |
| `${data_detail_list.dataDistDesc}` | 数据特征分布 | perf_data_detail.data_dist_desc |

#### 硬件资源清单表 (`resource_list`)

| 单元格内容 | 字段 | 来源 |
|-----------|------|------|
| `${resource_list.serialNumber}` | 序号 | performance_resource_info.serial_number |
| `${resource_list.serviceName}` | 服务名称 | performance_resource_info.service_name |
| `${resource_list.systemPlatform}` | 系统平台 | performance_resource_info.system_platform |
| `${resource_list.paasPlatformType}` | PAAS平台类型 | performance_resource_info.paas_platform_type |
| `${resource_list.cpuCores}` | CPU核数 | performance_resource_info.cpu_cores |
| `${resource_list.memoryGb}` | 内存(GB) | performance_resource_info.memory_gb |
| `${resource_list.dedicatedStorageGb}` | 独占存储(GB) | performance_resource_info.dedicated_storage_gb |
| `${resource_list.sanStorageGb}` | SAN存储(GB) | performance_resource_info.san_storage_gb |
| `${resource_list.nasStorageGb}` | NAS存储(GB) | performance_resource_info.nas_storage_gb |
| `${resource_list.operatingSystem}` | 操作系统 | performance_resource_info.operating_system |
| `${resource_list.middleware}` | 中间件/平台 | performance_resource_info.middleware |
| `${resource_list.middlewareReasonBelowBaseline}` | 低于基线原因 | performance_resource_info.middleware_reason_below_baseline |
| `${resource_list.deploymentLocation}` | 部署地点 | performance_resource_info.deployment_location |
| `${resource_list.networkDeployment}` | 网络部署 | performance_resource_info.network_deployment |
| `${resource_list.hostname}` | 主机名 | performance_resource_info.hostname |
| `${resource_list.ipAddress}` | IP地址 | performance_resource_info.ip_address |
| `${resource_list.remarks}` | 备注 | performance_resource_info.remarks |

---

## 4. 图片支持

PoiEngine 支持两种图片插入方式。

### 4.1 方式一：base64 自动解码（推荐）

在 `dataModel` 中放入以 `IMAGE_` 开头的 key，value 为 base64 编码字符串，引擎自动解码为图片并插入。

**Java 代码**：

```java
Map<String, Object> dataModel = new LinkedHashMap<>();
// ... 其他数据 ...
dataModel.put("IMAGE_ARCH", "iVBORw0KGgoAAAANSUhEUg...");  // 架构图 base64
dataModel.put("IMAGE_DESC_ARCH", "系统架构图");               // 图片描述
```

**Word 模板**：

```
${IMAGE_ARCH}                    ← 占位符，替换为实际图片
${IMAGE_DESC_ARCH}               ← 占位符，替换为"系统架构图"
```

**机制**：

1. `handleImageBase64FromSingleData()` 在段落替换前执行
2. 扫描所有 `IMAGE_` 开头（非 `IMAGE_DESC`、`IMAGE_PLACEHOLDER`）的 key
3. value 通过 `Base64.getDecoder().decode()` 解码
4. 通过魔术头自动检测图片格式（PNG / JPEG / GIF / BMP）
5. 图片以 450×320 EMU（约 47×34 mm）尺寸插入
6. `IMAGE_DESC_*` 在段落替换阶段替换为描述文本

> **约束**：当前图片固定以 450×320 EMU 尺寸插入。如需自定义尺寸，使用方式二。

### 4.2 方式二：ImageInfo 对象（精确控制）

```java
Map<String, PoiEngine.ImageInfo> imageData = new LinkedHashMap<>();
ImageInfo info = new ImageInfo();
info.setImageBytes(imageBytes);
info.setFileName("架构图.png");
info.setSuffix("png");
info.setWidthMm(120);       // 宽度 mm
info.setHeightMm(80);       // 高度 mm
info.setDescription("系统架构图");
imageData.put("ARCH", info);

PoiEngine.fillModel(is, os, dataModel, imageData);
```

**Word 模板**：

```
${ARCH}          → 占位符，替换为图片（120×80mm）
${ARCH_DESC}     → 占位符，替换为"系统架构图"
```

---

## 5. 空值与异常处理

### 5.1 空值/null

| 场景 | 行为 |
|------|------|
| dataModel 中 value 为 null | 替换为空字符串 |
| dataModel 中不存在对应 key | 替换为 **"未获取到数据"**（蓝色字体 #0000FF） |
| 列表数据为空 | 模板行中所有点号占位符替换为"未获取到数据"（蓝色） |
| 图片 base64 为 null/空 | 跳过，不插入 |
| `IMAGE_DESC_*` 无对应值 | 替换为空字符串 |

### 5.2 图片插入失败

- base64 解码失败或图片格式不支持 → 占位符替换为 **"图片加载失败"**（红色字体 #C00000）
- ImageInfo 方式同样会捕获异常并显示红色提示

### 5.3 跨 Run 断裂占位符

Word 编辑时可能将一个占位符拆散到多个 XWPFRun 中（如 `${task_n` 和 `ame}` 被分割）。PoiEngine 的 `getParagraphFullText()` 会在段落级别重新拼接文本后再匹配正则，确保断裂占位符也能被识别和替换。

---

## 6. Word 模板制作指南

### 6.1 准备工作

模板文件：`性能测试方案模板.docx`
模板路径：`D:/data/aicode-v1/uploads/performance/templates/性能测试方案模板.docx`

### 6.2 单值占位符操作

1. 在 Word 中打开模板文件
2. 选中要替换的文本
3. 直接键入占位符（如 `${task_name}`）
4. **建议**：先在记事本中打出完整占位符，再整体粘贴到 Word，避免被 Word 自动拆分格式

### 6.3 表格循环行操作（⚠️ 与 poi-tl 完全不同）

**旧方式 (poi-tl)**：第一格 `{{#tran_list}}`，最后一格 `{{/tran_list}}`，中间格 `{{fieldName}}`

**新方式 (PoiEngine)**：

1. 保留表头行不变
2. **只留一行数据模板行**（删除其余数据行）
3. 每个单元格直接写 `列表名.字段名` 格式的占位符
4. **不需要**任何开始/结束标签

**示例 — 联机交易表模板行**：

| 序号 | 模块 | 接口方式 | 交易名称 | 交易代码 | TPS | 响应时间 | 成功率 |
|------|------|---------|---------|---------|-----|---------|--------|
| `${tran_list.serialNumber}` | `${tran_list.moduleName}` | `${tran_list.interfaceType}` | `${tran_list.tranName}` | `${tran_list.tranCode}` | `${tran_list.targetTps}` | `${tran_list.targetRt}` | `${tran_list.targetSuccessRate}` |

> **核心区别**：不用 `{{#tran_list}}` / `{{/tran_list}}`，只需每个格写 `${tran_list.字段名}` 即可。引擎通过点号自动识别这是动态表格行。

### 6.4 表格行检测机制

PoiEngine 遍历每个表格的第 2 行（索引 1），检测其中是否包含 `listName.fieldName` 格式的点号占位符。如果检测到且 `listName` 在 `listData` 中存在，则将该表格视为动态表格：

1. 提取模板行的单元格数量 `N`
2. 为 `listData[listName]` 中每条数据创建新行（`table.createRow()`）
3. 复制模板行样式，替换 `${listName.xxx}` → 当前行数据
4. 删除原始模板行
5. 如果列表为空，模板行替换为"未获取到数据"（蓝色）

---

## 7. 测试场景 (`scene_list`)

数据来源：`perf_task_scene` + `perf_task_scene_detail`，通过 `SceneWithDetails` 对象注入 `dataModel`。

> `scene_list` 当前不在 PoiEngine 的表格循环范围内（因为其数据结构是嵌套对象而非 `List<Map>`），建议在 Word 模板中以段落文本方式呈现，或后续扩展支持。

---

## 8. 手工操作检查清单

在 Word 中打开 `性能测试方案模板.docx`，按顺序操作：

- [ ] 封面：替换项目标题为 `${task_name}`，替换其他元数据为对应标签
- [ ] 1.2 参考文档表：替换需求编号为 `${req_no}`，项目名称为 `${proj_name}`
- [ ] 1.3 被测系统表：替换需求编号、生产任务编号、产品标识为对应标签
- [ ] **1.4.1 硬件资源表**：只留一行数据行，每格写 `${resource_list.字段名}`（见 §3.2）
- [ ] **1.4.2 软件版本表**：只留一行数据行，每格写 `${resource_list.operatingSystem}`、`${resource_list.middleware}` 等
- [ ] **联机交易表**：只留一行数据行，每格写 `${tran_list.字段名}`
- [ ] **批量作业表**：只留一行数据行，每格写 `${batch_list.字段名}`
- [ ] 数据准备段落：替换为 `${model_analysis}` 等标签
- [ ] **数据明细表**：只留一行数据行，每格写 `${data_detail_list.字段名}`
- [ ] 3.3/3.4 架构图：如需自动插入图片，使用 `${IMAGE_ARCH}` 占位符（需后端配合传 base64）
- [ ] 5 进度计划表：替换日期为 `${start_time}` / `${end_time}`
- [ ] 全文检查：
  - 占位符语法正确（`${key}` / `$[key]` / `{{key}}` 均可）
  - 表格循环使用点号格式（`${列表名.字段名}`），**不要**用 `{{#list}} {{/list}}`
  - 占位符拼写与本文档完全一致（区分大小写）
  - 标签内无多余空格

---

## 9. 不需要修改的部分

以下内容保持静态（手工填写或在 Word 模板中写死）：

- **1.1 项目目标**：文字描述段，生成后手工微调
- **1.4.3/1.4.5 初始化参数**：内容因项目而异
- **3.1 测试文档列表**：标准参考文档
- **3.2 测试地点**：静态内容（上海测试中心测试部）
- **3.3/3.4 架构图**：可用 `${IMAGE_ARCH}` 自动插入，或保持静态
- **3.5 测试人员与资源**：角色职责表，静态内容
- **4.1 测试工具**：工具版本描述，静态内容
- **4.2 测试标准/通过标准**：通用标准文字
- **4.4 测试任务构成**：部分可自动化，部分手工
- **5 测试进度计划**：使用 `${start_time}` / `${end_time}`，具体条目手工调整
- **6 风险**：手工填写
- **7 附录**：附件引用列表，静态

---

## 10. 从 poi-tl 模板迁移指南

如果已有基于 poi-tl 的 Word 模板，需要做以下修改：

| 旧写法 (poi-tl) | 新写法 (PoiEngine) | 说明 |
|----------------|---------------------|------|
| `{{task_name}}` | `${task_name}` | 单值标签，`{{}}` 语法仍兼容，推荐改为 `${}` |
| `{{=task_name}}` | `${task_name}` | `=` 前缀不再需要 |
| `{{#tran_list}}` … `{{/tran_list}}` | 删除，每格改为 `${tran_list.字段名}` | 不再需要 ForEach 开始/结束标签 |
| `{{moduleName}}`（在循环行内） | `${tran_list.moduleName}` | 需要加上列表名前缀 |
| 图片 `{{@img}}` | `${IMAGE_xxx}` | base64 自动解码或 ImageInfo 对象 |

**迁移步骤**：

1. 全局替换 `{{=` → `${`，`}}` → `}`
2. 删除所有 `{{#列表名}}` 和 `{{/列表名}}` 标签
3. 表格模板行中的字段名加上列表名前缀：`{{field}}` → `${列表名.field}`
4. 图片标签改为 `${IMAGE_xxx}` 格式
