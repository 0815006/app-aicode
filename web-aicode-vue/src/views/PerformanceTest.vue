<template>
  <div class="performance-page">
    <div class="performance-main-content">
      <!-- 左侧任务列表 -->
      <div class="sidebar-container">
        <div class="task-list-sidebar">
          <div class="sidebar-header">
            <h3>📋 性能测试任务</h3>
            <div class="header-actions">
              <el-button 
                type="info" 
                size="mini" 
                icon="el-icon-setting" 
                circle
                title="模板维护"
                @click="handleManageTemplates"
              ></el-button>
              <el-button 
                type="primary" 
                size="mini" 
                icon="el-icon-plus" 
                circle
                title="新建任务"
                @click="handleCreateTask"
              ></el-button>
            </div>
          </div>
          <div class="task-list-content" v-loading="loading">
            <div v-if="taskList.length === 0" class="no-tasks-tip">暂无任务</div>
            <ul class="task-items-list">
              <li 
                v-for="item in taskList" 
                :key="item.id" 
                class="task-item"
                :class="{ active: activeTaskId === item.id.toString() }"
                @click="handleSelectTask(item.id.toString())"
              >
                <div class="task-info">
                  <div class="task-name" :title="item.taskName">{{ item.taskName || '未命名任务' }}</div>
                  <div class="task-meta">
                    {{ item.batchNo }} · {{ item.productId }}
                  </div>
                </div>
                <i class="el-icon-arrow-right"></i>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <!-- 右侧详情展示区 -->
      <div class="detail-content-area">
        <div v-if="!activeTaskId" class="empty-state">
          <el-empty description="请选择或新建一个性能测试任务"></el-empty>
        </div>
        <div v-else class="detail-scroll-container">
          <!-- 任务信息区块 -->
          <el-card class="info-block" shadow="hover">
            <div slot="header" class="card-header">
              <span class="block-title"><i class="el-icon-info"></i> 任务信息</span>
              <el-button v-if="isCreator" type="primary" size="mini" icon="el-icon-edit" @click="openTaskInfoEdit">编辑</el-button>
            </div>
            <el-descriptions :column="3" border size="small">
              <el-descriptions-item label="任务名称">{{ taskInfo.taskName }}</el-descriptions-item>
              <el-descriptions-item label="测试任务编号">{{ taskInfo.testTaskNo }}</el-descriptions-item>
              <el-descriptions-item label="生产任务编号">{{ taskInfo.prodTaskNo }}</el-descriptions-item>
              <el-descriptions-item label="产品/牵头组件">{{ taskInfo.productId }}</el-descriptions-item>
              <el-descriptions-item label="批次">{{ taskInfo.batchNo }}</el-descriptions-item>
              <el-descriptions-item label="需求编号">{{ taskInfo.reqNo }}</el-descriptions-item>
              <el-descriptions-item label="项目名称">{{ taskInfo.projName }}</el-descriptions-item>
              <el-descriptions-item label="项目编号">{{ taskInfo.projNo }}</el-descriptions-item>
              <el-descriptions-item label="测试部门">{{ taskInfo.testDept }}</el-descriptions-item>
              <el-descriptions-item label="开发部门">{{ taskInfo.devDept }}</el-descriptions-item>
              <el-descriptions-item label="开始时间">{{ taskInfo.startTime }}</el-descriptions-item>
              <el-descriptions-item label="结束时间">{{ taskInfo.endTime }}</el-descriptions-item>
              <el-descriptions-item label="性能测试经理">{{ taskInfo.perfManager }}</el-descriptions-item>
              <el-descriptions-item label="测试架构师">{{ taskInfo.testArch }}</el-descriptions-item>
              <el-descriptions-item label="项目经理">{{ taskInfo.projectManager }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- 联机交易区块 -->
          <el-card class="info-block" shadow="hover">
            <div slot="header" class="card-header">
              <span class="block-title"><i class="el-icon-connection"></i> 联机交易调研与方案</span>
              <el-button v-if="canEdit" type="primary" size="mini" icon="el-icon-edit" @click="openTranEdit">编辑</el-button>
            </div>
            <div v-if="taskInfo.totalUserCount || taskInfo.dailyPeakTps || taskInfo.selectedTranTpsSum" class="summary-display">
              <el-tag size="small" type="success">用户总数: {{ taskInfo.totalUserCount }}</el-tag>
              <el-tag size="small" type="success">日均在线: {{ taskInfo.dailyOnlineUserCount }}</el-tag>
              <el-tag size="small" type="warning">日峰值TPS: {{ taskInfo.dailyPeakTps }}</el-tag>
              <el-tag size="small" type="warning">年峰值TPS: {{ taskInfo.annualPeakTps }}</el-tag>
              <el-tag size="small" type="danger" effect="dark">选中交易TPS之和: {{ taskInfo.selectedTranTpsSum || 0 }}</el-tag>
            </div>
            <el-table :data="trans" border stripe size="small">
              <el-table-column prop="moduleName" label="模块" width="100" show-overflow-tooltip></el-table-column>
              <el-table-column prop="tranName" label="交易名称" min-width="150" show-overflow-tooltip></el-table-column>
              <el-table-column prop="tranCode" label="交易代码" width="100" show-overflow-tooltip></el-table-column>
              <el-table-column label="生产现状" align="center">
                <el-table-column prop="tranPeakTps" label="最大TPS" width="80"></el-table-column>
                <el-table-column prop="tranAvgRt" label="平均RT" width="80"></el-table-column>
              </el-table-column>
              <el-table-column label="指标要求" align="center">
                <el-table-column prop="targetTps" label="目标TPS" width="80"></el-table-column>
                <el-table-column prop="targetRt" label="目标RT" width="80"></el-table-column>
                <el-table-column prop="targetSuccessRate" label="成功率%" width="70"></el-table-column>
              </el-table-column>
              <el-table-column label="选中" width="60" align="center">
                <template slot-scope="scope">
                  <i v-if="scope.row.isSelected" class="el-icon-success" style="color: #67C23A"></i>
                  <i v-else class="el-icon-info" style="color: #909399"></i>
                </template>
              </el-table-column>
            </el-table>
          </el-card>

          <!-- 批量作业区块 -->
          <el-card class="info-block" shadow="hover">
            <div slot="header" class="card-header">
              <span class="block-title"><i class="el-icon-s-order"></i> 批量作业调研与方案</span>
              <el-button v-if="canEdit" type="primary" size="mini" icon="el-icon-edit" @click="openBatchEdit">编辑</el-button>
            </div>
            <div v-if="taskInfo.batchTotalDuration" class="summary-display">
              <el-tag size="small" type="info">整体时长: {{ taskInfo.batchTotalDuration }}</el-tag>
              <el-tag size="small" type="info">整体数据量: {{ taskInfo.batchTotalDataVolume }}</el-tag>
              <el-tag size="small" type="info">并行度: {{ taskInfo.batchParallelDegree }}</el-tag>
              <el-tag size="small" type="info">最大并行数: {{ taskInfo.batchMaxParallelCount }}</el-tag>
            </div>
            <el-table :data="batches" border stripe size="small">
              <el-table-column prop="jobNo" label="编号" width="70"></el-table-column>
              <el-table-column prop="jobName" label="作业名称" min-width="150" show-overflow-tooltip></el-table-column>
              <el-table-column prop="jobDataType" label="数据类型" width="90" show-overflow-tooltip></el-table-column>
              <el-table-column prop="jobDataVolume" label="预估数据量" width="100" show-overflow-tooltip></el-table-column>
              <el-table-column prop="jobActualDuration" label="实际运行时长" width="120"></el-table-column>
              <el-table-column prop="jobDuration" label="预估时长" min-width="120" show-overflow-tooltip></el-table-column>
              <el-table-column prop="jobExecTimePoint" label="执行时间点" min-width="120" show-overflow-tooltip></el-table-column>
              <el-table-column label="混合/重做" width="90" align="center">
                <template slot-scope="scope">
                  <el-tooltip :content="'叠加联机: ' + scope.row.isMixedLink" placement="top">
                    <el-tag :type="scope.row.isMixedLink === '是' ? 'warning' : 'info'" size="mini" style="margin-right: 5px">混</el-tag>
                  </el-tooltip>
                  <el-tooltip :content="'重做机制: ' + scope.row.hasRetry" placement="top">
                    <el-tag :type="scope.row.hasRetry === '是' ? 'success' : 'info'" size="mini">重</el-tag>
                  </el-tooltip>
                </template>
              </el-table-column>
            </el-table>
          </el-card>

          <!-- 数据准备区块 -->
          <el-card class="info-block" shadow="hover">
            <div slot="header" class="card-header">
              <span class="block-title"><i class="el-icon-coin"></i> 数据准备方案</span>
              <el-button v-if="canEdit" type="primary" size="mini" icon="el-icon-edit" @click="openDataEdit">编辑</el-button>
            </div>
            <el-table :data="datas" border stripe size="small">
              <el-table-column label="分类" width="90">
                <template slot-scope="scope">
                  <el-tag :type="scope.row.dataType === 2 ? 'info' : 'primary'" size="mini">
                    {{ scope.row.dataType === 2 ? '基础数据' : '核心业务' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="tableNameEn" label="英文表名" min-width="120" show-overflow-tooltip></el-table-column>
              <el-table-column prop="tableNameCn" label="中文表名" min-width="120" show-overflow-tooltip></el-table-column>
              <el-table-column label="生产存量(万)" width="100" align="right">
                <template slot-scope="scope">{{ scope.row.tableRowsCount }}</template>
              </el-table-column>
              <el-table-column label="目标造数(万)" width="100" align="right">
                <template slot-scope="scope">{{ scope.row.targetRowsCount }}</template>
              </el-table-column>
              <el-table-column prop="prepMethod" label="准备方式" width="100"></el-table-column>
              <el-table-column prop="dataDistDesc" label="数据特征分布" show-overflow-tooltip></el-table-column>
            </el-table>
          </el-card>

          <!-- 测试场景区块 -->
          <el-card class="info-block" shadow="hover">
            <div slot="header" class="card-header">
              <span class="block-title"><i class="el-icon-video-play"></i> 测试场景定义</span>
              <el-button v-if="canEdit" type="primary" size="mini" icon="el-icon-edit" @click="openSceneEdit">编辑</el-button>
            </div>
            <el-table :data="scenes" border stripe size="small">
              <el-table-column prop="sceneName" label="场景名称" min-width="180"></el-table-column>
              <el-table-column prop="targetTpsRatio" label="TPS比例(%)" width="100"></el-table-column>
              <el-table-column prop="targetTotalTps" label="预期总TPS" width="100"></el-table-column>
              <el-table-column prop="globalDuration" label="持续时间(分)" width="100"></el-table-column>
              <el-table-column label="状态" width="80">
                <template slot-scope="scope">
                  <el-tag :type="scope.row.isSelected ? 'primary' : 'info'" size="mini">{{ scope.row.isSelected ? '已选' : '未选' }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </div>
      </div>
    </div>

    <!-- 弹窗组件 -->
    <create-task-dialog ref="createTaskDialog" @refresh="fetchTaskList" />
    <task-info-edit-dialog ref="taskInfoEditDialog" @refresh="refreshDetail" />
    <tran-edit-dialog ref="tranEditDialog" @refresh="refreshDetail" />
    <batch-edit-dialog ref="batchEditDialog" @refresh="refreshDetail" />
    <data-edit-dialog ref="dataEditDialog" @refresh="refreshDetail" />
    <scene-edit-dialog ref="sceneEditDialog" @refresh="refreshDetail" />
    <template-manage-dialog ref="templateManageDialog" />
  </div>
</template>

<script>
import { listTasks, getTaskDetail } from '@/api/performance'
import { getCurrentEmpNo } from '@/utils/currentUser'
import CreateTaskDialog from '@/components/performance-test/CreateTaskDialog.vue'
import TaskInfoEditDialog from '@/components/performance-test/TaskInfoEditDialog.vue'
import TranEditDialog from '@/components/performance-test/TranEditDialog.vue'
import BatchEditDialog from '@/components/performance-test/BatchEditDialog.vue'
import DataEditDialog from '@/components/performance-test/DataEditDialog.vue'
import SceneEditDialog from '@/components/performance-test/SceneEditDialog.vue'
import TemplateManageDialog from '@/components/performance-test/TemplateManageDialog.vue'

export default {
  name: 'PerformanceTest',
  components: {
    CreateTaskDialog,
    TaskInfoEditDialog,
    TranEditDialog,
    BatchEditDialog,
    DataEditDialog,
    SceneEditDialog,
    TemplateManageDialog
  },
  data() {
    return {
      taskList: [],
      activeTaskId: null,
      taskInfo: {},
      trans: [],
      batches: [],
      datas: [],
      scenes: [],
      currentUser: getCurrentEmpNo(),
      loading: false
    }
  },
  computed: {
    isCreator() {
      return this.taskInfo.creatorId === this.currentUser
    },
    canEdit() {
      if (this.isCreator) return true
      if (!this.taskInfo.recorderRange) return false
      const range = this.taskInfo.recorderRange.split(',').map(s => s.trim())
      return range.includes(this.currentUser)
    }
  },
  created() {
    this.fetchTaskList()
  },
  methods: {
    async fetchTaskList() {
      this.loading = true
      try {
        const res = await listTasks()
        if (res.code === 200) {
          this.taskList = res.data
        }
      } finally {
        this.loading = false
      }
    },
    async handleSelectTask(taskId) {
      this.activeTaskId = taskId
      const res = await getTaskDetail(taskId)
      if (res.code === 200) {
        this.taskInfo = res.data.task
        this.trans = res.data.trans
        this.batches = res.data.batches
        this.datas = res.data.datas
        this.scenes = res.data.scenes
      }
    },
    refreshDetail() {
      if (this.activeTaskId) {
        this.handleSelectTask(this.activeTaskId)
      }
    },
    handleCreateTask() {
      this.$refs.createTaskDialog.init()
    },
    handleManageTemplates() {
      this.$refs.templateManageDialog.init()
    },
    openTaskInfoEdit() {
      this.$refs.taskInfoEditDialog.init(this.taskInfo)
    },
    openTranEdit() {
      this.$refs.tranEditDialog.init(this.activeTaskId, this.trans, this.taskInfo)
    },
    openBatchEdit() {
      this.$refs.batchEditDialog.init(this.activeTaskId, this.batches, this.taskInfo)
    },
    openDataEdit() {
      this.$refs.dataEditDialog.init(this.activeTaskId, this.datas)
    },
    openSceneEdit() {
      this.$refs.sceneEditDialog.init(this.activeTaskId, this.trans)
    }
  }
}
</script>

<style scoped>
.performance-page {
  padding: 20px;
  height: 100%;
  background: linear-gradient(180deg, #f5f8ff 0%, #eef3fb 100%);
  box-sizing: border-box;
  overflow: hidden;
}

.performance-main-content {
  display: flex;
  gap: 18px;
  height: 100%;
}

.sidebar-container {
  width: 280px;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.task-list-sidebar {
  padding: 16px 14px;
  border: 1px solid #d9e3f2;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 8px 20px rgba(16, 43, 98, 0.08);
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  flex-shrink: 0;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 14px;
  color: #1f2d3d;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.task-list-content {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

.task-list-content::-webkit-scrollbar {
  width: 6px;
}

.task-list-content::-webkit-scrollbar-thumb {
  background: #c8d8f0;
  border-radius: 8px;
}

.task-items-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.task-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-radius: 10px;
  background: #f8faff;
  margin-bottom: 10px;
  transition: all 0.2s;
  cursor: pointer;
  border: 1px solid transparent;
}

.task-item:hover {
  background: #f0f5ff;
  transform: translateY(-1px);
}

.task-item.active {
  background: #eaf4ff;
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
}

.task-info {
  flex: 1;
  min-width: 0;
  margin-right: 10px;
}

.task-name {
  font-size: 13px;
  color: #2c3e50;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 600;
}

.task-meta {
  font-size: 11px;
  color: #909399;
  margin-top: 4px;
}

.task-item i {
  color: #c0c4cc;
  font-size: 12px;
}

.task-item.active i {
  color: #409eff;
}

.no-tasks-tip {
  color: #92a1b7;
  text-align: center;
  padding: 20px;
  font-size: 13px;
}

.detail-content-area {
  flex: 1;
  min-width: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  border: 1px solid #d9e3f2;
  border-radius: 16px;
  padding: 20px;
  background: #ffffff;
  box-shadow: 0 12px 24px rgba(16, 43, 98, 0.08);
  overflow: hidden;
}

.detail-scroll-container {
  flex: 1;
  overflow-y: auto;
  padding-right: 5px;
}

.detail-scroll-container::-webkit-scrollbar {
  width: 6px;
}

.detail-scroll-container::-webkit-scrollbar-thumb {
  background: #c8d8f0;
  border-radius: 8px;
}

.info-block {
  margin-bottom: 20px;
  border-radius: 12px;
  border: 1px solid #ebeef5;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.block-title {
  font-weight: bold;
  color: #303133;
  font-size: 15px;
}

.block-title i {
  margin-right: 6px;
  color: #409eff;
}

.empty-state {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.summary-display {
  margin-bottom: 15px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 10px;
  background-color: #f8f9fb;
  border-radius: 8px;
}

@media (max-width: 960px) {
  .performance-main-content {
    flex-direction: column;
  }

  .sidebar-container {
    width: 100%;
    height: 300px;
  }
}
</style>
