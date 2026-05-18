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

## 在 Word 中插入 poi-tl 标签的操作方法

1. 在 Word 中打开 `性能测试方案模板.docx`
2. 选中要替换的文本，直接键入 poi-tl 标签（如 `{{task_name}}`）
3. **关键**: 确保标签在同一个"格式块"内 — 建议先在记事本中打出标签，再整体粘贴到 Word 中
4. 对于表格行循环（如 `{{#tran_list}}`），需要先在表格中只保留一行数据行，然后在第一个单元格开头加 `{{#xxx}}`，最后一个单元格末尾加 `{{/xxx}}`

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

#### 2.1 表1.4.1 硬件资源清单（需改造为循环表）

**当前状态**: 一个 35 行的静态表格，列出每台服务器的配置

**改造方法**:
1. 保留表头行（序号/服务名称/平台类型/节点数量/CPU核数/内存/独占存储/SAN存储/NAS存储/备注）
2. **只保留第一行数据行**（删除其余 33 行数据）
3. 在该数据行的第一个单元格开头添加 `{{#resource_list}}`
4. 在该数据行的最后一个单元格末尾添加 `{{/resource_list}}`
5. 按如下映射替换每个单元格：

| 列 | 当前值示例 | poi-tl 标签 | 数据来源字段 |
|---|---|---|---|
| 序号 | `1` | `{{resource_list.serialNumber}}` | serial_number |
| 服务名称 | `FTP服务器` | `{{resource_list.serviceName}}` | service_name |
| 平台类型 | `IAAS8.2.1` | `{{resource_list.systemPlatform}}` | system_platform |
| 节点数量 | `2` | `{{resource_list.hostCount}}` | (固定为1，每个资源记录代表一台机器) |
| CPU核数 | `4` | `{{resource_list.cpuCores}}` | cpu_cores |
| 内存(GB) | `8` | `{{resource_list.memoryGb}}` | memory_gb |
| 独占存储(GB) | `80` | `{{resource_list.dedicatedStorageGb}}` | dedicated_storage |
| SAN存储(GB) | `1024` | `{{resource_list.sanStorageGb}}` | san_storage_gb |
| NAS存储(GB) | `-` | `{{resource_list.nasStorageGb}}` | nas_storage_gb |
| 备注 | `-` | `{{resource_list.remarks}}` | remarks |

> **注意**: 部署地点 (`deploymentLocation`) 也可以作为一列加入表头，当前模板没有这一列但数据库中有此字段。

#### 2.2 表1.4.2 系统软件版本清单

**当前状态**: 一个 6 行的静态表格，列出每个服务的操作系统和中间件版本

**改造方法**: 同样改造为循环表，保留表头，留一行数据行。如果需要在同一个模板中复用 resource_list 但展示不同字段（操作系统/中间件），可以再循环一次同样的数据源：

| 列 | poi-tl 标签 | 数据来源字段 |
|---|---|---|
| 服务名称 | `{{resource_list.serviceName}}` | service_name |
| 操作系统版本 | `{{resource_list.operatingSystem}}` | operating_system |
| 中间件/平台版本 | `{{resource_list.middleware}}` | middleware |
| 低于基线原因说明 | `{{resource_list.middlewareReasonBelowBaseline}}` | middleware_reason_below_baseline |

> **技巧**: 如果软件版本表和硬件资源表使用相同的数据源 (`resource_list`)，可以在模板中放置两份，poi-tl 会分别循环渲染。

---

### 三、第2章 联机交易调研与方案

#### 3.1 联机交易指标表（当前模板 Table 6，23行）

**当前表头**: 序号 / 模块 / 接口方式 / 交易名称 / 交易代码 / TPS(笔/秒) / 平均响应时间(秒) / 成功率(%)

**改造方法**:
1. 保留表头
2. 只保留第一行数据行
3. 在该行第一格加 `{{#tran_list}}`，最后一格加 `{{/tran_list}}`

| 列 | poi-tl 标签 | 数据来源字段 |
|---|---|---|
| 序号 | `{{tran_list.serialNumber}}` | (无此字段，可用 index 或去掉) |
| 模块 | `{{tran_list.moduleName}}` | module_name |
| 接口方式 | `{{tran_list.interfaceType}}` | interface_type |
| 交易名称 | `{{tran_list.tranName}}` | tran_name |
| 交易代码 | `{{tran_list.tranCode}}` | tran_code |
| 目标TPS | `{{tran_list.targetTps}}` | target_tps |
| 目标平均响应时间 | `{{tran_list.targetRt}}` | target_rt |
| 目标成功率 | `{{tran_list.targetSuccessRate}}` | target_success_rate |
| 选取原因 | `{{tran_list.selectReason}}` | select_reason |

**补充字段（可在表格右方新增列）**:

| 附加列 | poi-tl 标签 | 数据来源字段 |
|---|---|---|
| 目标最大响应时间 | `{{tran_list.targetMaxRt}}` | target_max_rt |
| 目标日峰值量(万笔/日) | `{{tran_list.targetDailyVol}}` | target_daily_vol |
| 思考时间(秒) | `{{tran_list.targetThinkTime}}` | target_think_time |
| 指标来源 | `{{tran_list.indicatorSource}}` | indicator_source (1:实测, 2:采样折算, 3:经验对标) |
| 指标推算过程 | `{{tran_list.calculationProcess}}` | calculation_process |

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

#### 4.1 批量作业指标表（当前模板 Table 7，16行）

**当前表头**: 序号 / 作业名称 / 指标要求处理时长 / 数量 / 作业编号 / 备注

**改造方法**: 同联机交易表改造方式

| 列 | poi-tl 标签 | 数据来源字段 |
|---|---|---|
| 序号 | (可用表格行号) | - |
| 作业名称 | `{{batch_list.jobName}}` | job_name |
| 处理时长要求 | `{{batch_list.jobDuration}}` | job_duration |
| 数据量级 | `{{batch_list.jobDataVolume}}` | job_data_volume |
| 作业编号 | `{{batch_list.jobNo}}` | job_no |

**补充字段（可在表格右方新增列）**:

| 附加列 | poi-tl 标签 | 数据来源字段 |
|---|---|---|
| 作业数 | `{{batch_list.jobCount}}` | job_count |
| 并行方式 | `{{batch_list.jobParallelMode}}` | job_parallel_mode |
| 功能描述 | `{{batch_list.jobDesc}}` | job_desc |
| 触发条件 | `{{batch_list.jobTriggerCond}}` | job_trigger_cond |
| 前导作业 | `{{batch_list.jobPreName}}` | job_pre_name |
| 并发作业 | `{{batch_list.jobConcurrentNames}}` | job_concurrent_names |
| 执行频率 | `{{batch_list.jobFrequency}}` | job_frequency |
| 执行时间点 | `{{batch_list.jobExecTimePoint}}` | job_exec_time_point |
| 是否叠加联机 | `{{batch_list.isMixedLink}}` | is_mixed_link |
| 是否有重做机制 | `{{batch_list.hasRetry}}` | has_retry |
| 选取原因 | `{{batch_list.selectReason}}` | select_reason |

#### 4.2 批量作业汇总（文本段落）

| 描述 | poi-tl 标签 | 来源 |
|---|---|---|
| 预估整体批量时长 | `{{batch_total_duration}}` | perf_task.batch_total_duration |
| 预估整体数据量 | `{{batch_total_data_volume}}` | perf_task.batch_total_data_volume |
| 并行度 | `{{batch_parallel_degree}}` | perf_task.batch_parallel_degree |
| 最大并行数 | `{{batch_max_parallel_count}}` | perf_task.batch_max_parallel_count |

---

### 五、数据准备方案

#### 5.1 数据方案定性描述（文本框落）

该部分对应 `perf_data_plan` 表，使用独立字段（非循环表）：

| 章节/段落 | poi-tl 标签 | 数据来源字段 |
|---|---|---|
| 2.1 相关数据内容分析 | `{{model_analysis}}` | perf_data_plan.model_analysis |
| 2.4 数据约束说明 | `{{data_constraint}}` | perf_data_plan.data_constraint |
| 3.1 数据来源说明 | `{{data_source_desc}}` | perf_data_plan.data_source_desc |
| 3.2 数据构造/准备方法 | `{{prep_method_desc}}` | perf_data_plan.prep_method_desc |
| 3.3 数据脱敏/清洗规则 | `{{cleaning_rule}}` | perf_data_plan.cleaning_rule |

#### 5.2 数据准备明细表（表级规模）

循环表，数据来源 `perf_data_detail`：

| 列 | poi-tl 标签 | 数据来源字段 |
|---|---|---|
| 分类 | `{{data_detail_list.dataType}}` | data_type (1:核心业务表, 2:基础数据/码表) |
| 英文表名 | `{{data_detail_list.tableNameEn}}` | table_name_en |
| 中文表名 | `{{data_detail_list.tableNameCn}}` | table_name_cn |
| 生产存量(万行) | `{{data_detail_list.tableRowsCount}}` | table_rows_count |
| 目标造数(万行) | `{{data_detail_list.targetRowsCount}}` | target_rows_count |
| 准备方式 | `{{data_detail_list.prepMethod}}` | prep_method |
| 数据特征分布 | `{{data_detail_list.dataDistDesc}}` | data_dist_desc |

---

### 六、测试场景定义

#### 6.1 场景列表（嵌套循环表）

两个层级：
- 外层: `{{#scene_list}}` 遍历场景
- 内层: `{{#sceneDetailList}}` 遍历该场景下的交易明细

数据来源: `perf_task_scene` + `perf_task_scene_detail`

**外层标签（场景级别）**:

| 字段 | poi-tl 标签 | 数据来源字段 |
|---|---|---|
| 场景名称 | `{{scene_list.sceneName}}` | perf_task_scene.scene_name |
| 场景类型 | `{{scene_list.sceneType}}` | scene_type (1:基准,2:单负载,3:混合,4:稳定性,5:极限) |
| 目标TPS百分比 | `{{scene_list.targetTpsRatio}}` | target_tps_ratio |
| 目标总TPS | `{{scene_list.targetTotalTps}}` | target_total_tps |
| 测试目的 | `{{scene_list.testObjective}}` | test_objective |
| 实施方法 | `{{scene_list.implementationMethod}}` | implementation_method |
| 结束条件 | `{{scene_list.endCondition}}` | end_condition |
| 持续时间(分) | `{{scene_list.globalDuration}}` | global_duration |

**内层标签（交易明细级别）**:

| 字段 | poi-tl 标签 | 数据来源字段 |
|---|---|---|
| 交易名称 | `{{sceneDetailList.tranName}}` | perf_task_scene_detail.tran_name |
| 目标TPS | `{{sceneDetailList.targetTps}}` | target_tps |
| 目标RT | `{{sceneDetailList.targetRt}}` | target_rt |
| 成功率 | `{{sceneDetailList.targetSuccessRate}}` | target_success_rate |
| VU数 | `{{sceneDetailList.vuCount}}` | vu_count |
| Ramp-up(秒) | `{{sceneDetailList.rampUp}}` | ramp_up |
| Pacing | `{{sceneDetailList.pacing}}` | pacing |
| Throughput Timer | `{{sceneDetailList.throughputTimer}}` | throughput_timer |
| 迭代次数 | `{{sceneDetailList.iterations}}` | iterations |

#### 6.2 嵌套表格示例结构

在 Word 模板中，可以这样组织：

```
场景名称: {{scene_list.sceneName}}
目标TPS比例: {{scene_list.targetTpsRatio}}%   目标总TPS: {{scene_list.targetTotalTps}}

{{#scene_list.sceneDetailList}}
  交易名: {{sceneDetailList.tranName}}  TPS: {{sceneDetailList.targetTps}}  RT: {{sceneDetailList.targetRt}}  VU: {{sceneDetailList.vuCount}}
{{/scene_list.sceneDetailList}}
```

> **注意**: poi-tl 中，嵌套列表访问子列表时，路径是 `scene_list.sceneDetailList`（即外层对象的属性名）。

---

### 七、不需修改的部分

以下内容保持不变（手工调整或在 Word 模板中写死）：

- **1.1 项目目标**: 文字描述段模板，生成后手工微调
- **1.4.3/1.4.5 初始化参数**: 内容因项目而异，保持静态
- **3.1 测试文档列表**: 标准参考文档，保持不变
- **3.2 测试地点**: 保持 `上��测试中心测试部` 等静态内容
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

| 标签 | 来源表 |
|------|--------|
| `{{#resource_list}}` ... `{{/resource_list}}` | performance_resource_info |
| `{{#tran_list}}` ... `{{/tran_list}}` | perf_task_tran |
| `{{#batch_list}}` ... `{{/batch_list}}` | perf_task_batch |
| `{{#data_detail_list}}` ... `{{/data_detail_list}}` | perf_data_detail |
| `{{#scene_list}}` ... `{{/scene_list}}` | perf_task_scene (外层) |
| `{{#scene_list.sceneDetailList}}` ... `{{/scene_list.sceneDetailList}}` | perf_task_scene_detail (内层) |

---

## 手工操作检查清单

在 Word 中打开 `性能测试方案模板.docx`，按以下顺序操作：

- [ ] 封面：替换项目标题为 `{{task_name}}`，替换其他元数据为对应标签
- [ ] 1.2 参考文档表：替换需求编号为 `{{req_no}}`，项目名称为 `{{proj_name}}`
- [ ] 1.3 被测系统表：替换需求编号、生产任务编号、产品标识为对应标签
- [ ] **1.4.1 硬件资源表**：改造为循环表 `{{#resource_list}}...{{/resource_list}}`
- [ ] **1.4.2 软件版本表**：改造为循环表（复用 resource_list）
- [ ] **联机交易表**：改造为循环表 `{{#tran_list}}...{{/tran_list}}`
- [ ] **批量作业表**：改造为循环表 `{{#batch_list}}...{{/batch_list}}`
- [ ] 数据准备段落：替换为 `{{model_analysis}}` 等标签
- [ ] **数据明细表**：改造为循环表 `{{#data_detail_list}}...{{/data_detail_list}}`
- [ ] **场景定义区**：改造为嵌套循环 `{{#scene_list}}...{{#sceneDetailList}}...`
- [ ] 3.5/4.4 人员与任务表：替换人员姓名为标签
- [ ] 5 进度计划表：替换日期为 `{{start_time}}` / `{{end_time}}`
- [ ] 全文检查：确保标签内无空格、拼写与本文档一致
