<template>
  <el-dialog title="编辑数据准备方案" :visible.sync="visible" width="98%" top="2vh">
    <div class="dialog-layout">
      <!-- 左侧：明细表格 -->
      <div class="input-area">
        <div style="margin-bottom: 10px; display: flex; justify-content: space-between; align-items: center;">
          <div>
            <el-button type="primary" size="small" icon="el-icon-plus" @click="addRow">添加表</el-button>
            <el-button type="success" size="small" icon="el-icon-download" @click="handleDownloadTemplate">下载数据准备明细模板</el-button>
            <el-upload
              style="display: inline-block; margin-left: 10px;"
              action=""
              :auto-upload="false"
              :show-file-list="false"
              :on-change="handleUploadExcel"
              accept=".xls,.xlsx"
            >
              <el-button type="warning" size="small" icon="el-icon-upload2">上传明细解析</el-button>
            </el-upload>
          </div>
          <div class="table-tip">单位：万行</div>
        </div>
        
        <el-table :data="list" border size="mini" height="65vh">
          <el-table-column label="数据分类" width="100">
            <template slot-scope="scope">
              <el-select v-model="scope.row.dataType" size="mini">
                <el-option :value="1" label="核心业务表"></el-option>
                <el-option :value="2" label="基础数据"></el-option>
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="英文表名*" width="150">
            <template slot-scope="scope"><el-input v-model="scope.row.tableNameEn" size="mini"></el-input></template>
          </el-table-column>
          <el-table-column label="中文表名*" width="150">
            <template slot-scope="scope"><el-input v-model="scope.row.tableNameCn" size="mini"></el-input></template>
          </el-table-column>
          <el-table-column label="生产存量" width="100">
            <template slot-scope="scope"><el-input-number v-model="scope.row.tableRowsCount" :controls="false" size="mini" style="width: 100%"></el-input-number></template>
          </el-table-column>
          <el-table-column label="年增长率%" width="90">
            <template slot-scope="scope"><el-input-number v-model="scope.row.tableGrowthRate" :controls="false" size="mini" style="width: 100%"></el-input-number></template>
          </el-table-column>
          <el-table-column label="目标造数*" width="100">
            <template slot-scope="scope"><el-input-number v-model="scope.row.targetRowsCount" :controls="false" size="mini" style="width: 100%"></el-input-number></template>
          </el-table-column>
          <el-table-column label="数据特征分布" min-width="150">
            <template slot-scope="scope"><el-input v-model="scope.row.dataDistDesc" type="textarea" :rows="1" size="mini"></el-input></template>
          </el-table-column>
          <el-table-column label="准备方式" width="120">
            <template slot-scope="scope">
              <el-input v-model="scope.row.prepMethod" size="mini"></el-input>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="50" fixed="right">
            <template slot-scope="scope"><el-button type="text" icon="el-icon-delete" style="color: #F56C6C" @click="list.splice(scope.$index, 1)"></el-button></template>
          </el-table-column>
        </el-table>

        <div class="form-footer">
          <el-button type="primary" @click="handleSaveDetails" :loading="loading">保存明细数据</el-button>
        </div>
      </div>

      <!-- 右侧：定性描述方案 -->
      <div class="plan-area">
        <div class="plan-content">
          <h3>数据准备定性描述</h3>
          <el-form :model="planForm" label-position="top" size="small">
            <el-form-item label="2.1 相关数据内容分析">
              <el-input type="textarea" :rows="4" v-model="planForm.modelAnalysis" placeholder="推荐：分析系统核心业务实体及其关联关系..."></el-input>
            </el-form-item>
            <el-form-item label="2.4 数据约束说明">
              <el-input type="textarea" :rows="3" v-model="planForm.dataConstraint" placeholder="推荐：说明主外键约束、唯一性索引要求等..."></el-input>
            </el-form-item>
            <el-form-item label="3.1 数据来源说明">
              <el-input type="textarea" :rows="3" v-model="planForm.dataSourceDesc" placeholder="推荐：生产环境脱敏借数、自动化脚本自造等..."></el-input>
            </el-form-item>
            <el-form-item label="3.2 数据构造/准备方法描述">
              <el-input type="textarea" :rows="4" v-model="planForm.prepMethodDesc" placeholder="推荐：详细描述造数脚本逻辑或脱敏工具使用流程..."></el-input>
            </el-form-item>
            <el-form-item label="3.3 数据脱敏/清洗规则">
              <el-input type="textarea" :rows="3" v-model="planForm.cleaningRule" placeholder="推荐：敏感字段（姓名、证件号）的掩码规则..."></el-input>
            </el-form-item>
          </el-form>
          <div class="form-footer">
            <el-button type="success" @click="handleSavePlan" :loading="loading">保存定性方案</el-button>
          </div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script>
import { saveDataDetails, getDataPlan, saveDataPlan, getTemplateDownloadByKeywordUrl, parseDataExcel } from '@/api/performance'

export default {
  name: 'DataEditDialog',
  data() {
    return {
      visible: false,
      loading: false,
      taskId: null,
      list: [],
      planForm: {
        id: null,
        taskId: null,
        modelAnalysis: '',
        dataConstraint: '',
        dataSourceDesc: '',
        prepMethodDesc: '',
        cleaningRule: ''
      }
    }
  },
  methods: {
    async init(taskId, details) {
      this.visible = true
      this.taskId = taskId
      this.list = JSON.parse(JSON.stringify(details || []))
      this.planForm.taskId = taskId
      
      // 获取定性方案
      const res = await getDataPlan(taskId)
      if (res.code === 200 && res.data) {
        this.planForm = res.data
      } else {
        // 默认推荐填充
        this.planForm.modelAnalysis = '本系统核心数据模型包含客户信息、账户信息及交易流水。数据间存在强关联，需保证业务链路完整性。'
        this.planForm.dataConstraint = '1. 证件号与客户号唯一性约束；\n2. 账户余额与流水汇总一致性；\n3. 状态机流转约束。'
        this.planForm.dataSourceDesc = '核心业务数据采用生产脱敏借数方式；基础码表及配置数据采用全量同步方式。'
        this.planForm.prepMethodDesc = '1. 使用ETL工具从生产环境抽取数据；\n2. 经过脱敏平台进行关键字段掩码；\n3. 编写SQL脚本进行数据偏移处理以适配测试日期。'
        this.planForm.cleaningRule = '姓名：保留姓氏，名字掩码；\n证件号：保留前6后4位，中间掩码；\n手机号：保留前3后4位。'
      }
    },
    addRow() {
      this.list.push({ 
        dataType: 1,
        tableNameEn: '',
        tableNameCn: '',
        tableRowsCount: 0,
        tableGrowthRate: 0,
        targetRowsCount: 0,
        dataDistDesc: '',
        prepMethod: '脚本自造'
      })
    },
    handleDownloadTemplate() {
      window.open(getTemplateDownloadByKeywordUrl('数据准备'))
    },
    async handleUploadExcel(file) {
      const formData = new FormData()
      formData.append('file', file.raw)
      this.loading = true
      try {
        const res = await parseDataExcel(formData)
        if (res.code === 200) {
          this.$message.success('解析成功')
          this.list = res.data
        }
      } catch (e) {
        this.$message.error('解析失败')
      }
      this.loading = false
    },
    async handleSaveDetails() {
      this.loading = true
      const res = await saveDataDetails(this.taskId, this.list)
      if (res.code === 200) {
        this.$message.success('明细保存成功')
        this.$emit('refresh')
      }
      this.loading = false
    },
    async handleSavePlan() {
      this.loading = true
      const res = await saveDataPlan(this.planForm)
      if (res.code === 200) {
        this.$message.success('定性方案保存成功')
      }
      this.loading = false
    }
  }
}
</script>

<style scoped>
.dialog-layout { display: flex; height: 85vh; }
.input-area { flex: 3; padding-right: 20px; border-right: 1px solid #ebeef5; overflow-y: auto; }
.plan-area { flex: 2; padding-left: 20px; overflow-y: auto; background-color: #fcfcfc; }
.form-footer { margin-top: 20px; text-align: right; }
.table-tip { font-size: 12px; color: #909399; }
.plan-content h3 { font-size: 15px; color: #303133; margin-bottom: 20px; border-bottom: 1px solid #eee; padding-bottom: 10px; }
</style>
