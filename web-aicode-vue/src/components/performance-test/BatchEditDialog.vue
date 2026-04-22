<template>
  <el-dialog title="编辑批量作业" :visible.sync="visible" width="98%" top="2vh">
    <div class="dialog-layout">
      <div class="input-area">
        <div style="margin-bottom: 10px; display: flex; justify-content: space-between; align-items: center;">
          <div>
            <el-button type="primary" size="small" icon="el-icon-plus" @click="addRow">添加作业</el-button>
            <el-button type="success" size="small" icon="el-icon-download" @click="handleDownloadTemplate">下载批量运行调查表模板</el-button>
            <el-upload
              style="display: inline-block; margin-left: 10px;"
              action=""
              :auto-upload="false"
              :show-file-list="false"
              :on-change="handleUploadExcel"
              accept=".xls,.xlsx"
            >
              <el-button type="warning" size="small" icon="el-icon-upload2">上传调查表解析</el-button>
            </el-upload>
          </div>
          <div class="table-tip">* 批量作业调研内容</div>
        </div>
        
        <el-table :data="list" border size="mini" height="55vh">
          <el-table-column label="基础属性" align="center">
            <el-table-column label="作业编号" width="100">
              <template slot-scope="scope"><el-input v-model="scope.row.jobNo" size="mini"></el-input></template>
            </el-table-column>
            <el-table-column label="作业名称*" width="150">
              <template slot-scope="scope"><el-input v-model="scope.row.jobName" size="mini"></el-input></template>
            </el-table-column>
            <el-table-column label="作业数" width="80">
              <template slot-scope="scope"><el-input v-model="scope.row.jobCount" size="mini"></el-input></template>
            </el-table-column>
            <el-table-column label="并行方式" width="100">
              <template slot-scope="scope"><el-input v-model="scope.row.jobParallelMode" size="mini"></el-input></template>
            </el-table-column>
            <el-table-column label="功能简述" width="150">
              <template slot-scope="scope"><el-input v-model="scope.row.jobDesc" type="textarea" :rows="1" size="mini"></el-input></template>
            </el-table-column>
            <el-table-column label="触发条件" width="120">
              <template slot-scope="scope"><el-input v-model="scope.row.jobTriggerCond" size="mini"></el-input></template>
            </el-table-column>
            <el-table-column label="前导作业" width="120">
              <template slot-scope="scope"><el-input v-model="scope.row.jobPreName" size="mini"></el-input></template>
            </el-table-column>
            <el-table-column label="执行频率" width="80">
              <template slot-scope="scope"><el-input v-model="scope.row.jobFrequency" size="mini"></el-input></template>
            </el-table-column>
          </el-table-column>

          <el-table-column label="生产调研指标" align="center">
            <el-table-column label="数据类型" width="100">
              <template slot-scope="scope"><el-input v-model="scope.row.jobDataType" size="mini"></el-input></template>
            </el-table-column>
            <el-table-column label="预估数据量" width="120">
              <template slot-scope="scope"><el-input v-model="scope.row.jobDataVolume" size="mini"></el-input></template>
            </el-table-column>
            <el-table-column label="实际运行时长" width="100">
              <template slot-scope="scope"><el-input v-model="scope.row.jobActualDuration" size="mini" placeholder="手工填写"></el-input></template>
            </el-table-column>
            <el-table-column label="预估时长" width="100">
              <template slot-scope="scope"><el-input v-model="scope.row.jobDuration" size="mini"></el-input></template>
            </el-table-column>
            <el-table-column label="执行时间点" width="120">
              <template slot-scope="scope"><el-input v-model="scope.row.jobExecTimePoint" size="mini"></el-input></template>
            </el-table-column>
          </el-table-column>

          <el-table-column label="方案与机制" align="center">
            <el-table-column label="叠加联机" width="80">
              <template slot-scope="scope"><el-input v-model="scope.row.isMixedLink" size="mini"></el-input></template>
            </el-table-column>
            <el-table-column label="重做机制" width="80">
              <template slot-scope="scope"><el-input v-model="scope.row.hasRetry" size="mini"></el-input></template>
            </el-table-column>
          </el-table-column>

          <el-table-column label="操作" width="50" fixed="right">
            <template slot-scope="scope"><el-button type="text" icon="el-icon-delete" style="color: #F56C6C" @click="list.splice(scope.$index, 1)"></el-button></template>
          </el-table-column>
        </el-table>

        <div v-if="summaryData" style="margin-top: 20px;">
          <div style="font-weight: bold; margin-bottom: 10px; border-left: 4px solid #67C23A; padding-left: 10px;">系统级批量指标</div>
          <el-row :gutter="20">
            <el-col :span="6">
              <div class="summary-item">
                <span class="label">整体批量时长:</span>
                <el-input v-model="summaryData.batchTotalDuration" size="small" style="width: 150px;"></el-input>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="summary-item">
                <span class="label">整体数据量:</span>
                <el-input v-model="summaryData.batchTotalDataVolume" size="small" style="width: 150px;"></el-input>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="summary-item">
                <span class="label">并行度:</span>
                <el-input v-model="summaryData.batchParallelDegree" size="small" style="width: 150px;"></el-input>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="summary-item">
                <span class="label">最大并行数:</span>
                <el-input v-model="summaryData.batchMaxParallelCount" size="small" style="width: 150px;"></el-input>
              </div>
            </el-col>
          </el-row>
        </div>

        <div class="form-footer">
          <el-button type="primary" @click="handleSave" :loading="loading">保存批量信息</el-button>
        </div>
      </div>
      <div class="guide-area">
        <div class="guide-content">
          <h3>批量作业调研指南</h3>
          <div class="guide-item">
            <div class="guide-title">☆ 重点关注</div>
            <div class="guide-desc">逻辑复杂、涉及大表关联、有严格时间窗口要求的作业。</div>
          </div>
          <div class="guide-item">
            <div class="guide-title">☆ 系统级指标</div>
            <div class="guide-desc">
              1、整体批量时长：整个批量流程预计运行的总时间；<br/>
              2、整体数据量：批量处理涉及的核心业务数据总量；<br/>
              3、并行度：批量执行时的任务并发策略；<br/>
              4、最大并行数：系统支持的最大同时运行作业数。
            </div>
          </div>
          <el-divider></el-divider>
          <div v-for="i in 10" :key="i" class="placeholder-text">补充说明内容占位...</div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script>
import { saveBatches, getTemplateDownloadByKeywordUrl, parseBatchExcel } from '@/api/performance'

export default {
  name: 'BatchEditDialog',
  data() {
    return {
      visible: false,
      loading: false,
      taskId: null,
      list: [],
      summaryData: {
        batchTotalDuration: '',
        batchTotalDataVolume: '',
        batchParallelDegree: '',
        batchMaxParallelCount: ''
      }
    }
  },
  methods: {
    init(taskId, batches, task) {
      this.visible = true
      this.taskId = taskId
      this.list = JSON.parse(JSON.stringify(batches || []))
      if (task) {
        this.summaryData = {
          batchTotalDuration: task.batchTotalDuration || '',
          batchTotalDataVolume: task.batchTotalDataVolume || '',
          batchParallelDegree: task.batchParallelDegree || '',
          batchMaxParallelCount: task.batchMaxParallelCount || ''
        }
      } else {
        this.summaryData = {
          batchTotalDuration: '',
          batchTotalDataVolume: '',
          batchParallelDegree: '',
          batchMaxParallelCount: ''
        }
      }
    },
    addRow() {
      this.list.push({ 
        jobNo: '',
        jobName: '', 
        jobCount: '1',
        jobParallelMode: '',
        jobDesc: '',
        jobTriggerCond: '',
        jobPreName: '',
        jobConcurrentNames: '',
        jobFrequency: '',
        jobDataType: '',
        jobDataVolume: '',
        jobDuration: '',
        jobExecTimePoint: '',
        isMixedLink: '否',
        mixedTranNames: '',
        hasRetry: '是',
        retryDesc: ''
      })
    },
    handleDownloadTemplate() {
      window.open(getTemplateDownloadByKeywordUrl('批量运行调查表'))
    },
    async handleUploadExcel(file) {
      const formData = new FormData()
      formData.append('file', file.raw)
      this.loading = true
      try {
        const res = await parseBatchExcel(formData)
        if (res.code === 200) {
          this.$message.success('解析成功')
          this.list = res.data.batches
          if (res.data.summary) {
            this.summaryData = {
              batchTotalDuration: res.data.summary.batchTotalDuration || '',
              batchTotalDataVolume: res.data.summary.batchTotalDataVolume || '',
              batchParallelDegree: res.data.summary.batchParallelDegree || '',
              batchMaxParallelCount: res.data.summary.batchMaxParallelCount || ''
            }
          }
        }
      } catch (e) {
        this.$message.error('解析失败')
      }
      this.loading = false
    },
    async handleSave() {
      this.loading = true
      const res = await saveBatches(this.taskId, {
        batches: this.list,
        summary: this.summaryData
      })
      if (res.code === 200) {
        this.$message.success('保存成功')
        this.$emit('refresh')
        this.visible = false
      }
      this.loading = false
    }
  }
}
</script>

<style scoped>
.dialog-layout { display: flex; height: 85vh; }
.input-area { flex: 3; padding-right: 20px; border-right: 1px solid #ebeef5; overflow-y: auto; }
.guide-area { flex: 1; padding-left: 20px; overflow-y: auto; background-color: #fafafa; }
.form-footer { margin-top: 20px; text-align: right; }
.placeholder-text { color: #c0c4cc; font-size: 12px; margin-top: 10px; }
.table-tip { font-size: 12px; color: #F56C6C; }
.summary-item { display: flex; align-items: center; margin-bottom: 10px; }
.summary-item .label { font-size: 13px; color: #606266; margin-right: 10px; width: 110px; text-align: right; }
.guide-item { margin-bottom: 15px; line-height: 1.6; }
.guide-title { font-weight: bold; color: #303133; font-size: 13px; margin-bottom: 4px; }
.guide-desc { color: #606266; font-size: 12px; padding-left: 10px; }
</style>
