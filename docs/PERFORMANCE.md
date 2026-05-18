# 性能测试方案模板 — poi-tl 标签参考文档

## 概述

模板文件: `性能测试方案模板.docx`  
模板路径: `D:/data/aicode-v1/uploads/performance/templates/性能测试方案模板.docx`  
生成引擎: poi-tl 1.12.2 (`com.deepoove:poi-tl`)

生成文档时，后端从以下 8 张数据库表查询数据，通过 poi-tl 填充到模板中：

| 表名 | 用途 | 查询条件 |
|------|------|----------|
| `perf_task` | 任务主信息 | `id = taskId` |
| `perf_task_tran` | 联机交易 | `task_id = taskId` |
| `perf_task_batch` | 批量作业 | `task_id = taskId` |
| `perf_task_scene` | 测试场景 | `task_id = taskId` |
| `perf_task_scene_detail` | 场景交易明细 | `scene_id = scene.id` |
| `perf_data_plan` | 数据方案定性 | `task_id = taskId` |
| `perf_data_detail` | 数据方案明细 | `task_id = taskId` |
| `performance_resource_info` | 资源申请 | `product_id + batch_no + file_source='资源申请表'` |

---

## ⚠️ 重要：表格行循环与字段写法

### 循环行的正确写法（✅ 必须这样做）

poi-tl 1.12.x 中，表格行循环使用标准 **ForEach** 语法：

1. **只保留一行数据行**（删除其余所有数据行，保留表头行）
2. 在该行**第一个单元格**写入开始标签：
   ```
   {{#tran_list}}
   ```
3. 在该行**最后一个单元格**末尾写入结束标签：
   ```
   {{/tran_list}}
   ```
4. 中间各单元格写字段占位符（推荐用 `{{=fieldName}}` 或 `{{fieldName}}`）：
   ```
   {{=moduleName}}   {{=tranName}}   {{=tranCode}}   ...
   ```

> **poi-tl 会为列表中每条记录复制一行**，每次迭代时单元格内的字段标签以该条记录的 Map 为作用域解析。

### 字段写法三种等价形式

| 写法 | 示例 | 说明 |
|---|---|---|
| 标准写法 | `{{moduleName}}` | poi-tl 默认文本渲染，在迭代作用域内直接取字段值 |
| `=` 前缀 | `{{=moduleName}}` | 等价于标准写法，代码已注册 `=` 插件处理 |
| `==` 前缀 | `{{==moduleName}}` | 等价于标准写法，`=` 插件自动剥除多余前缀 |

### 错误写法（❌ 不要这样做）

| 错误示例 | 原因 |
|---|---|
| `{{tran_list.moduleName}}` | 列表变量是 List，List 对象没有 `moduleName` 属性 |
| `{{#tran_list}}` 与 `{{/tran_list}}` 不在同一行 | 必须在同一表格行内完成 ForEach 开/闭 |

### 支持 `{{=xxx}}` / `{{==xxx}}` 写法（单值字段同样适用）

代码已注册 `=` 前缀插件，单值字段和循环内字段均可使用：

```
{{=task_name}}    →  等价于  {{task_name}}
{{==batch_no}}    →  等价于  {{batch_no}}
{{=moduleName}}   →  在 {{#tran_list}} 迭代内等价于当前行的 moduleName 字段
```

---

## 在 Word 中插入 poi-tl 标签的操作方法

1. 在 Word 中打开 `性能测试方案模板.docx`
2. 选中要替换的文本，直接键入 poi-tl 标签（如 `{{task_name}}`）
3. **关键**: 确保标签在同一个"格式块"内 — 建议先在记事本中打出标签，再整体粘贴到 Word 中
4. 对于表格行循环，操作步骤：
   - 保留表头行不变
   - **只留一行数据行**
   - 在该行**第一格**写 `{{#tran_list}}`
   - 在该行**最后一格**末写 `{{/tran_list}}`
   - 中间各格写字段名：`{{=moduleName}}`、`{{=tranName}}` 等（或不加 `=` 前缀也可）

---

## 标签详解

### 一、封面 / 页眉区域

这些是独立的文本字段，出现在文档封面的固定位置，直接替换为对应标签即可：

| 当前位置（示例值） | poi-tl 标签 | 数据来源 | 类型 |
|---|---|---|---|
| 项目标题 (如 `2510批次"新一代客服系统..."`) | `{{task_name}}` | perf_task.task_name | String |
| 测试任务编号 (如 `PT-2510-CS-0017_TS_01`) | `{{test_task_no}}` | perf_task.test_task_no | String |
| 生产任务编号 | `{{prod_task_no}}` | perf_task.prod_task_no | String |
| 牵头组件/产品标识 (如 `OBTM`) | `{{product_id}}` | perf_task.product_id | String |
| 批次 (如 `2510`) | `{{batch_no}}` | perf_task.batch_no | String |
| 需求编号 (如 `FR-202312-53445`) | `{{req_no}}` | perf_task.req_no | String |
| 项目名称 | `{{proj_name}}` | perf_task.proj_name | String |
| 项目编号 (如 `M-202504-00006`) | `{{proj_no}}` | perf_task.proj_no | String |
| 测试部门 | `{{test_dept}}` | perf_task.test_dept | String |
| 开发部门 | `{{dev_dept}}` | perf_task.dev_dept | String |
| 测试经理 | `{{perf_manager}}` | perf_task.perf_manager | String |
| 测试架构师 | `{{test_arch}}` | perf_task.test_arch | String |
| 项目经理 | `{{project_manager}}` | perf_task.project_manager | String |
| 开始时间 | `{{start_time}}` | perf_task.start_time (格式化 yyyy-MM-dd HH:mm:ss) | String |
| 结束时间 | `{{end_time}}` | perf_task.end_time (格式化) | String |

---

### 二、第1章 概述 — 测试环境与资源

#### 2.1 表1.4.1 硬件资源清单（循环表）

**改造方法**:
1. 保留表头行（序号/服务名称/平台类型/CPU核数/内存/独占存储/SAN存储/NAS存储/备注）
2. **只保留第一行数据行**（删除其余所有数据行）
3. 在该数据行的**第一个单元格**开头写 `{{#resource_list}}`
4. 在该数据行的**最后一个单元格**末尾写 `{{/resource_list}}`
5. 其余单元格按下表写字段名（推荐用 `{{=字段名}}` 写法）：

| 列 | 单元格内容 | 对应字段 |
|---|---|---|
| 序号 | `{{serialNumber}}` | serial_number（实体自带，无数据时自动生成序号）|
| 服务名称 | `{{serviceName}}` | service_name |
| 系统平台 | `{{systemPlatform}}` | system_platform |
| PAAS平台类型 | `{{paasPlatformType}}` | paas_platform_type |
| CPU核数 | `{{cpuCores}}` | cpu_cores |
| 内存(GB) | `{{memoryGb}}` | memory_gb |
| 独占存储(GB) | `{{dedicatedStorageGb}}` | dedicated_storage_gb |
| SAN存储(GB) | `{{sanStorageGb}}` | san_storage_gb |
| NAS存储(GB) | `{{nasStorageGb}}` | nas_storage_gb |
| 部署地点 | `{{deploymentLocation}}` | deployment_location |
| 网络部署 | `{{networkDeployment}}` | network_deployment |
| 主机名 | `{{hostname}}` | hostname |
| IP地址 | `{{ipAddress}}` | ip_address |
| 备注 | `{{remarks}}` | remarks |

**Word 模板数据行示例（第一行）**:
```
| {{#resource_list}} | {{=serviceName}} | {{=systemPlatform}} | {{=cpuCores}} | {{=memoryGb}} | {{=dedicatedStorageGb}} | {{=sanStorageGb}} | {{=nasStorageGb}} | {{=remarks}} {{/resource_list}} |
```

#### 2.2 表1.4.2 系统软件版本清单（循环表，复用 resource_list）

同样改造为循环表，第一格写 `{{#resource_list}}`，最后一格末写 `{{/resource_list}}`（两张表可以分别出现在模板不同位置，poi-tl 各自独立循环渲染）：

| 列 | 单元格内容 | 对应字段 |
|---|---|---|
| 服务名称 | `{{serviceName}}` | service_name |
| 操作系统版本 | `{{operatingSystem}}` | operating_system |
| 中间件/平台版本 | `{{middleware}}` | middleware |
| 低于基线原因说明 | `{{middlewareReasonBelowBaseline}}` | middleware_reason_below_baseline |

---

### 三、第2章 联机交易调研与方案

#### 3.1 联机交易指标表（循环表）

**当前表头**: 序号 / 模块 / 接口方式 / 交易名称 / 交易代码 / TPS(笔/秒) / 平均响应时间(秒) / 成功率(%)

**改造方法**:
1. 保留表头
2. **只保留第一行数据行**
3. 第一格写 `{{#tran_list}}`，最后一格末写 `{{/tran_list}}`，其余格填 `{{=字段名}}`（见下表）

| 列 | 单元格内容 | 对应字段 |
|---|---|---|
| 序号 | `{{serialNumber}}` | 自动生成 (1, 2, 3...) |
| 模块 | `{{moduleName}}` | module_name |
| 接口方式 | `{{interfaceType}}` | interface_type |
| 交易名称 | `{{tranName}}` | tran_name |
| 交易代码 | `{{tranCode}}` | tran_code |
| 目标TPS | `{{targetTps}}` | target_tps |
| 目标平均响应时间 | `{{targetRt}}` | target_rt |
| 目标成功率 | `{{targetSuccessRate}}` | target_success_rate |

**Word 模板数据行示例（第一行）**:
```
| {{#tran_list}} | {{=moduleName}} | {{=interfaceType}} | {{=tranName}} | {{=tranCode}} | {{=targetTps}} | {{=targetRt}} | {{=targetSuccessRate}} {{/tran_list}} |
```

#### 3.2 联机交易汇总（文本段落）

以下汇总值可直接放在方案文本段落中：

| 描述文案模板 | poi-tl 标签 | 来源 |
|---|---|---|
| 总用户数 X 万 | `{{total_user_count}}` | perf_task.total_user_count |
| 日均在线用户数 X 万 | `{{daily_online_user_count}}` | perf_task.daily_online_user_count |
| 日峰值 TPS X | `{{daily_peak_tps}}` | perf_task.daily_peak_tps |
| 年峰值 TPS X | `{{annual_peak_tps}}` | perf_task.annual_peak_tps |
| 选中交易 TPS 之和 X | `{{selected_tran_tps_sum}}` | perf_task.selected_tran_tps_sum |

---

### 四、批量作业调研与方案

#### 4.1 批量作业指标表（循环表）

**改造方法**: 同联机交易表改造方式，第一格写 `{{#batch_list}}`，最后一格末写 `{{/batch_list}}`

| 列 | 单元格内容 | 对应字段 |
|---|---|---|
| 序号 | `{{serialNumber}}` | 自动生成 |
| 作业名称 | `{{jobName}}` | job_name |
| 处理时长要求 | `{{jobDuration}}` | job_duration |
| 数据量级 | `{{jobDataVolume}}` | job_data_volume |
| 作业编号 | `{{jobNo}}` | job_no |
| 作业数 | `{{jobCount}}` | job_count |
| 并行方式 | `{{jobParallelMode}}` | job_parallel_mode |
| 功能描述 | `{{jobDesc}}` | job_desc |
| 选取原因 | `{{selectReason}}` | select_reason |

**Word 模板数据行示例（第一行）**:
```
| {{#batch_list}} | {{=jobName}} | {{=jobDuration}} | {{=jobDataVolume}} | {{=jobNo}} | {{=selectReason}} {{/batch_list}} |
```

#### 4.2 批量作业汇总（文本段落）

| 描述 | poi-tl 标签 | 来源 |
|---|---|---|
| 预估整体批量时长 | `{{batch_total_duration}}` | perf_task.batch_total_duration |
| 预估整体数据量 | `{{batch_total_data_volume}}` | perf_task.batch_total_data_volume |
| 并行度 | `{{batch_parallel_degree}}` | perf_task.batch_parallel_degree |
| 最大并行数 | `{{batch_max_parallel_count}}` | perf_task.batch_max_parallel_count |

---

### 五、数据准备方案

#### 5.1 数据方案定性描述（文本段落）

该部分对应 `perf_data_plan` 表，使用独立字段（非循环表）：

| 章节/段落 | poi-tl 标签 | 数据来源字段 |
|---|---|---|
| 2.1 相关数据内容分析 | `{{model_analysis}}` | perf_data_plan.model_analysis |
| 2.4 数据约束说明 | `{{data_constraint}}` | perf_data_plan.data_constraint |
| 3.1 数据来源说明 | `{{data_source_desc}}` | perf_data_plan.data_source_desc |
| 3.2 数据构造/准备方法 | `{{prep_method_desc}}` | perf_data_plan.prep_method_desc |
| 3.3 数据脱敏/清洗规则 | `{{cleaning_rule}}` | perf_data_plan.cleaning_rule |

#### 5.2 数据准备明细表（循环表）

第一格写 `{{#data_detail_list}}`，最后一格末写 `{{/data_detail_list}}`，其余格填 `{{=字段名}}`：

| 列 | 单元格内容 | 对应字段 |
|---|---|---|
| 序号 | `{{serialNumber}}` | 自动生成 |
| 分类 | `{{dataType}}` | data_type (1:核心业务表, 2:基础数据/码表) |
| 英文表名 | `{{tableNameEn}}` | table_name_en |
| 中文表名 | `{{tableNameCn}}` | table_name_cn |
| 生产存量(万行) | `{{tableRowsCount}}` | table_rows_count |
| 目标造数(万行) | `{{targetRowsCount}}` | target_rows_count |
| 准备方式 | `{{prepMethod}}` | prep_method |
| 数据特征分布 | `{{dataDistDesc}}` | data_dist_desc |

**Word 模板数据行示例（第一行）**:
```
| {{#data_detail_list}} | {{=dataType}} | {{=tableNameEn}} | {{=tableNameCn}} | {{=tableRowsCount}} | {{=targetRowsCount}} | {{=prepMethod}} | {{=dataDistDesc}} {{/data_detail_list}} |
```

---

### 六、测试场景定义

#### 6.1 场景列表

数据来源: `perf_task_scene` + `perf_task_scene_detail`，通过 `scene_list` 变量注入。

> **注意**: `scene_list` 目前直接放入 dataModel，场景部分暂不做行循环。
> 场景部分建议在 Word 模板中以**段落文本**方式呈现，使用以下字段：

**场景对象字段（通过 `scene_list` 中 `SceneWithDetails` 的 `scene` 属性）**:

| 字段 | 数据来源字段 |
|---|---|
| scene.sceneName | perf_task_scene.scene_name |
| scene.sceneType | scene_type (1:基准,2:单负载,3:混合,4:稳定性,5:极限) |
| scene.targetTpsRatio | target_tps_ratio |
| scene.targetTotalTps | target_total_tps |
| scene.testObjective | test_objective |
| scene.implementationMethod | implementation_method |
| scene.endCondition | end_condition |
| scene.globalDuration | global_duration |

---

### 七、不需修改的部分

以下内容保持不变（手工调整或在 Word 模板中写死）：

- **1.1 项目目标**: 文字描述段模板，生成后手工微调
- **1.4.3/1.4.5 初始化参数**: 内容因项目而异，保持静态
- **3.1 测试文档列表**: 标准参考文档，保持不变
- **3.2 测试地点**: 保持 `上海测试中心测试部` 等静态内容
- **3.3/3.4 架构图**: 图片，保持占位或手工插入
- **3.5 测试人员与资源**: 角色职责表，静态内容
- **4.1 测试工具**: 工具版本描述，静态内容
- **4.2 测试标准/通过标准**: 可在 Word 模板中写死通用标准文字，个别项目手工修改
- **4.4 测试任务构成**: 部分自动化（使用上述标签），部分手工
- **5 测试进度计划**: 部分自动化（使用 start_time/end_time），具体任务条目手工调整
- **6 风险**: 手工填写
- **7 附录**: 附件引用列表，保持静态

---

## 完整标签速查表

### 单值标签（直接替换文本）

| 标签 | 来源 |
|------|------|
| `{{task_name}}` | perf_task.task_name |
| `{{test_task_no}}` | perf_task.test_task_no |
| `{{prod_task_no}}` | perf_task.prod_task_no |
| `{{product_id}}` | perf_task.product_id |
| `{{batch_no}}` | perf_task.batch_no |
| `{{req_no}}` | perf_task.req_no |
| `{{proj_name}}` | perf_task.proj_name |
| `{{proj_no}}` | perf_task.proj_no |
| `{{test_dept}}` | perf_task.test_dept |
| `{{dev_dept}}` | perf_task.dev_dept |
| `{{perf_manager}}` | perf_task.perf_manager |
| `{{test_arch}}` | perf_task.test_arch |
| `{{project_manager}}` | perf_task.project_manager |
| `{{start_time}}` | perf_task.start_time |
| `{{end_time}}` | perf_task.end_time |
| `{{total_user_count}}` | perf_task.total_user_count |
| `{{daily_online_user_count}}` | perf_task.daily_online_user_count |
| `{{daily_peak_tps}}` | perf_task.daily_peak_tps |
| `{{annual_peak_tps}}` | perf_task.annual_peak_tps |
| `{{selected_tran_tps_sum}}` | perf_task.selected_tran_tps_sum |
| `{{batch_total_duration}}` | perf_task.batch_total_duration |
| `{{batch_total_data_volume}}` | perf_task.batch_total_data_volume |
| `{{batch_parallel_degree}}` | perf_task.batch_parallel_degree |
| `{{batch_max_parallel_count}}` | perf_task.batch_max_parallel_count |
| `{{model_analysis}}` | perf_data_plan.model_analysis |
| `{{data_constraint}}` | perf_data_plan.data_constraint |
| `{{data_source_desc}}` | perf_data_plan.data_source_desc |
| `{{prep_method_desc}}` | perf_data_plan.prep_method_desc |
| `{{cleaning_rule}}` | perf_data_plan.cleaning_rule |

### 循环列表标签（表格行迭代）

> **写法规则**:
> - 第一格写 `{{#列表名}}`，最后一格末写 `{{/列表名}}`（必须在**同一行**）
> - 中间各格写 `{{=字段名}}` 或直接 `{{字段名}}`（不加列表名前缀）
> - poi-tl 的 `ForEachRenderPolicy` 会为每条记录复制一整行

| 列表变量 | 第一格开始标签 | 最后一格结束标签 | 来源表 |
|---|---|---|---|
| resource_list | `{{#resource_list}}` | `{{/resource_list}}` | performance_resource_info |
| tran_list | `{{#tran_list}}` | `{{/tran_list}}` | perf_task_tran |
| batch_list | `{{#batch_list}}` | `{{/batch_list}}` | perf_task_batch |
| data_detail_list | `{{#data_detail_list}}` | `{{/data_detail_list}}` | perf_data_detail |

### `=` 前缀标签（等价于普通文本标签）

代码已注册 `=` 前缀插件，任何单值字段均可加 `=` 或 `==` 前缀：

| 写法 | 等价于 |
|---|---|
| `{{=task_name}}` | `{{task_name}}` |
| `{{==batch_no}}` | `{{batch_no}}` |
| `{{=moduleName}}` | `{{moduleName}}` |

---

## 手工操作检查清单

在 Word 中打开 `性能测试方案模板.docx`，按以下顺序操作：

- [ ] 封面：替换项目标题为 `{{task_name}}`，替换其他元数据为对应标签
- [ ] 1.2 参考文档表：替换需求编号为 `{{req_no}}`，项目名称为 `{{proj_name}}`
- [ ] 1.3 被测系统表：替换需求编号、生产任务编号、产品标识为对应标签
- [ ] **1.4.1 硬件资源表**：只留一行数据行，第一格写 `{{#resource_list}}`，最后一格末写 `{{/resource_list}}`，中间格写 `{{=serviceName}}` 等
- [ ] **1.4.2 软件版本表**：只留一行数据行，第一格写 `{{#resource_list}}`，最后一格末写 `{{/resource_list}}`，中间格写 `{{=operatingSystem}}` `{{=middleware}}` 等
- [ ] **联机交易表**：只留一行数据行，第一格写 `{{#tran_list}}`，最后一格末写 `{{/tran_list}}`，中间格写 `{{=moduleName}}` `{{=tranName}}` 等
- [ ] **批量作业表**：只留一行数据行，第一格写 `{{#batch_list}}`，最后一格末写 `{{/batch_list}}`，中间格写 `{{=jobName}}` `{{=jobDuration}}` 等
- [ ] 数据准备段落：替换为 `{{model_analysis}}` 等标签
- [ ] **数据明细表**：只留一行数据行，第一格写 `{{#data_detail_list}}`，最后一格末写 `{{/data_detail_list}}`，中间格写 `{{=tableNameEn}}` 等
- [ ] 3.5/4.4 人员与任务表：替换人员姓名为标签
- [ ] 5 进度计划表：替换日期为 `{{start_time}}` / `{{end_time}}`
- [ ] 全文检查：确保标签内无空格，拼写与本文档一致；`{{#xxx}}` 与 `{{/xxx}}` 必须在**同一表格行**首尾单元格
