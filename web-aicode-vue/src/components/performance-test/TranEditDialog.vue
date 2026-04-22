<template>
  <el-dialog title="编辑联机交易" :visible.sync="visible" width="98%" top="2vh">
    <div class="dialog-layout">
      <div class="input-area">
        <div style="margin-bottom: 10px; display: flex; justify-content: space-between; align-items: center;">
          <div>
            <el-button type="primary" size="small" icon="el-icon-plus" @click="addRow">添加交易</el-button>
            <el-button type="success" size="small" icon="el-icon-download" @click="handleDownloadTemplate">下载业务交易调查表模板</el-button>
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
          <div class="table-tip">* 带星号字段为必填项</div>
        </div>
        
        <el-table :data="list" border size="mini" height="60vh">
          <el-table-column label="基本信息" align="center">
            <el-table-column label="模块名称*" width="120">
              <template slot-scope="scope"><el-input v-model="scope.row.moduleName" size="mini"></el-input></template>
            </el-table-column>
            <el-table-column label="交易名称*" width="150">
              <template slot-scope="scope"><el-input v-model="scope.row.tranName" size="mini"></el-input></template>
            </el-table-column>
            <el-table-column label="交易代码" width="120">
              <template slot-scope="scope"><el-input v-model="scope.row.tranCode" size="mini"></el-input></template>
            </el-table-column>
            <el-table-column label="接口类型*" width="120">
              <template slot-scope="scope"><el-input v-model="scope.row.interfaceType" size="mini"></el-input></template>
            </el-table-column>
          </el-table-column>

          <el-table-column label="1. 生产现状调研情况" align="center">
            <el-table-column label="日峰值(万)*" width="90">
              <template slot-scope="scope"><el-input-number v-model="scope.row.tranDailyVol" :controls="false" size="mini" style="width: 100%;"></el-input-number></template>
            </el-table-column>
            <el-table-column label="高峰时段(万)*" width="90">
              <template slot-scope="scope"><el-input-number v-model="scope.row.tranPeakHourVol" :controls="false" size="mini" style="width: 100%;"></el-input-number></template>
            </el-table-column>
            <el-table-column label="最大TPS*" width="80">
              <template slot-scope="scope"><el-input-number v-model="scope.row.tranPeakTps" :controls="false" size="mini" style="width: 100%;"></el-input-number></template>
            </el-table-column>
            <el-table-column label="平均RT(s)*" width="80">
              <template slot-scope="scope"><el-input-number v-model="scope.row.tranAvgRt" :controls="false" size="mini" style="width: 100%;"></el-input-number></template>
            </el-table-column>
          </el-table-column>

          <el-table-column label="2. 业务交易指标明细要求" align="center">
            <el-table-column label="目标日峰值*" width="90">
              <template slot-scope="scope"><el-input-number v-model="scope.row.targetDailyVol" :controls="false" size="mini" style="width: 100%;"></el-input-number></template>
            </el-table-column>
            <el-table-column label="目标TPS*" width="80">
              <template slot-scope="scope"><el-input-number v-model="scope.row.targetTps" :controls="false" size="mini" style="width: 100%;"></el-input-number></template>
            </el-table-column>
            <el-table-column label="目标RT(s)*" width="80">
              <template slot-scope="scope"><el-input-number v-model="scope.row.targetRt" :controls="false" size="mini" style="width: 100%;"></el-input-number></template>
            </el-table-column>
            <el-table-column label="成功率%*" width="80">
              <template slot-scope="scope"><el-input-number v-model="scope.row.targetSuccessRate" :controls="false" size="mini" style="width: 100%;"></el-input-number></template>
            </el-table-column>
          </el-table-column>

          <el-table-column label="3. 交易选择结果" align="center">
            <el-table-column label="选中*" width="60">
              <template slot-scope="scope"><el-checkbox v-model="scope.row.isSelected" :true-label="1" :false-label="0"></el-checkbox></template>
            </el-table-column>
            <el-table-column label="选取原因" width="150">
              <template slot-scope="scope"><el-input v-model="scope.row.selectReason" size="mini"></el-input></template>
            </el-table-column>
          </el-table-column>

          <el-table-column label="操作" width="50" fixed="right">
            <template slot-scope="scope"><el-button type="text" icon="el-icon-delete" style="color: #F56C6C" @click="list.splice(scope.$index, 1)"></el-button></template>
          </el-table-column>
        </el-table>

        <div v-if="summaryData" style="margin-top: 20px;">
          <div style="font-weight: bold; margin-bottom: 10px; border-left: 4px solid #409EFF; padding-left: 10px;">系统汇总统计</div>
          <el-row :gutter="20">
            <el-col :span="6">
              <div class="summary-item">
                <span class="label">用户数总数:</span>
                <el-input-number v-model="summaryData.totalUserCount" :controls="false" size="small" style="width: 120px;"></el-input-number>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="summary-item">
                <span class="label">日均在线用户数:</span>
                <el-input-number v-model="summaryData.dailyOnlineUserCount" :controls="false" size="small" style="width: 120px;"></el-input-number>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="summary-item">
                <span class="label">日交易峰值TPS*:</span>
                <el-input-number v-model="summaryData.dailyPeakTps" :controls="false" size="small" style="width: 120px;"></el-input-number>
              </div>
            </el-col>
            <el-col :span="6">
              <div class="summary-item">
                <span class="label">年交易峰值TPS*:</span>
                <el-input-number v-model="summaryData.annualPeakTps" :controls="false" size="small" style="width: 120px;"></el-input-number>
              </div>
            </el-col>
          </el-row>
        </div>

        <div class="form-footer">
          <el-button type="primary" @click="handleSave" :loading="loading">保存交易信息</el-button>
        </div>
      </div>
      <div class="guide-area">
        <div class="guide-content">
          <h3>联机交易调研指南</h3>
          <div class="guide-item">
            <div class="guide-title">☆ 产品/模块名称</div>
            <div class="guide-desc">将应用系统先按模块或产品进行划分，也可继续细分子模块，例如：国内支付</div>
          </div>
          <div class="guide-item">
            <div class="guide-title">☆ 交易名称</div>
            <div class="guide-desc">例如：无折转客户帐</div>
          </div>
          <div class="guide-item">
            <div class="guide-title">☆ 交易代码</div>
            <div class="guide-desc">例如：B353。注：对于查询交易，按不同查询条件进行查询的应分开按不同交易进行填写，例如：客户信息查询（按客户号查询），客户信息查询（按客户姓名查询）</div>
          </div>
          <div class="guide-item">
            <div class="guide-title">☆ 生产现状调研情况</div>
            <div class="guide-desc">
              已投产系统生产现状数据：<br/>
              1、生产日峰值交易量：生产上各交易每日峰值交易量；<br/>
              2、生产高峰时段交易量：生产上高峰时段每小时峰值交易量；<br/>
              3、生产每秒最大交易量TPS：生产上各交易每秒最大交易量；<br/>
              4、生产交易平均响应时间：生产上各交易的平均响应时间；<br/>
              5、生产交易最大响应时间：生产上各交易的最大响应时间；
            </div>
          </div>
          <div class="guide-item">
            <div class="guide-title">☆ 业务交易明细要求</div>
            <div class="guide-desc">
              业务交易的性能要求和性能场景的设置依据：<br/>
              1、日峰值交易量：各交易每日峰值交易量；<br/>
              2、高峰时段交易量：高峰时段每小时峰值交易量；<br/>
              3、每秒最大交易量TPS：各交易高峰时间段每秒最大交易量，如不填写，将根据高峰时段交易量进行推算；<br/>
              4、交易平均响应时间：用户希望的交易响应时间（单步骤的平均响应时间）；<br/>
              5、交易最大响应时间：用户能容忍的最大交易响应时间（最大响应时间）；<br/>
              6、交易成功率：用户希望的交易成功率；<br/>
              7、用户平均操作时间：从用户进入交易画面填写交易要素信息开始，到将交易提交后台所花费的时间（交易被处理及返回的时间不含在内），平台类系统不适用可不填；
            </div>
          </div>
          <div class="guide-item">
            <div class="guide-title">☆ 交易选择结果</div>
            <div class="guide-desc">
              由测试项目组主导评估填写：<br/>
              1、是否选为性能测试交易：是否在性能测试场景设置中包含此交易；
            </div>
          </div>
          <div class="guide-item">
            <div class="guide-title">☆ 系统汇总统计</div>
            <div class="guide-desc">
              一般情况下被测交易只是系统交易总量的一部分，需要填写被测系统整体的统计信息：<br/>
              1、用户数总数：有权限登陆该系统的用户总数，平台类系统无需填写；<br/>
              2、日均在线用户数：系统每日平均同时在线用户数均值，平台类系统无需填写；<br/>
              3、日交易峰值TPS：系统普通日（不包括国庆、春节、电商促销等特殊日）的TPS；<br/>
              4、年交易峰值TPS：系统年峰值（包括国庆、春节、电商促销等特殊日）的TPS；
            </div>
          </div>
          <div class="guide-item" style="color: #F56C6C; font-weight: bold; margin-top: 20px;">
            ☆ 带*号为必填项
          </div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script>
import { saveTrans, getTemplateDownloadByKeywordUrl, parseTranExcel } from '@/api/performance'

export default {
  name: 'TranEditDialog',
  data() {
    return {
      visible: false,
      loading: false,
      taskId: null,
      list: [],
      summaryData: {
        totalUserCount: 0,
        dailyOnlineUserCount: 0,
        dailyPeakTps: 0,
        annualPeakTps: 0
      }
    }
  },
  methods: {
    init(taskId, trans, task) {
      this.visible = true
      this.taskId = taskId
      this.list = JSON.parse(JSON.stringify(trans || []))
      // 从任务对象中提取汇总信息
      if (task) {
        this.summaryData = {
          totalUserCount: task.totalUserCount || 0,
          dailyOnlineUserCount: task.dailyOnlineUserCount || 0,
          dailyPeakTps: task.dailyPeakTps || 0,
          annualPeakTps: task.annualPeakTps || 0
        }
      } else {
        this.summaryData = {
          totalUserCount: 0,
          dailyOnlineUserCount: 0,
          dailyPeakTps: 0,
          annualPeakTps: 0
        }
      }
    },
    addRow() {
      this.list.push({ 
        moduleName: '', 
        tranName: '', 
        tranCode: '', 
        interfaceType: '',
        tranDailyVol: 0,
        tranPeakHourVol: 0,
        tranPeakTps: 0,
        tranAvgRt: 0,
        targetDailyVol: 0,
        targetTps: 0,
        targetRt: 0,
        targetSuccessRate: 100,
        isSelected: 1, 
        dataSource: 1 
      })
    },
    handleDownloadTemplate() {
      window.open(getTemplateDownloadByKeywordUrl('业务交易调查表'))
    },
    async handleUploadExcel(file) {
      const formData = new FormData()
      formData.append('file', file.raw)
      this.loading = true
      try {
        const res = await parseTranExcel(formData)
        if (res.code === 200) {
          this.$message.success('解析成功')
          this.list = res.data.trans
          if (res.data.summary) {
            this.summaryData = res.data.summary
          }
        }
      } catch (e) {
        this.$message.error('解析失败')
      }
      this.loading = false
    },
    async handleSave() {
      this.loading = true
      const res = await saveTrans(this.taskId, {
        trans: this.list,
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
.dialog-layout { display: flex; height: 80vh; }
.input-area { flex: 3; padding-right: 20px; border-right: 1px solid #ebeef5; overflow-y: auto; }
.guide-area { flex: 1; padding-left: 20px; overflow-y: auto; background-color: #fafafa; }
.form-footer { margin-top: 20px; text-align: right; }
.placeholder-text { color: #c0c4cc; font-size: 12px; margin-top: 10px; }
.table-tip { font-size: 12px; color: #F56C6C; }
.summary-item { display: flex; align-items: center; margin-bottom: 10px; }
.summary-item .label { font-size: 13px; color: #606266; margin-right: 10px; width: 100px; text-align: right; }
.guide-item { margin-bottom: 15px; line-height: 1.6; }
.guide-title { font-weight: bold; color: #303133; font-size: 13px; margin-bottom: 4px; }
.guide-desc { color: #606266; font-size: 12px; padding-left: 10px; }
</style>
