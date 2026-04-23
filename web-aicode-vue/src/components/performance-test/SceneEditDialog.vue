<template>
  <el-dialog title="编辑测试场景" :visible.sync="visible" width="98%" top="2vh">
    <div class="dialog-layout">
      <div class="input-area" v-loading="loading">
        <div style="margin-bottom: 15px;">
          <el-button type="primary" size="small" icon="el-icon-refresh" @click="handleInitDefault">初始化默认场景</el-button>
          <el-button type="success" size="small" icon="el-icon-plus" @click="addCustomScene">添加自定义场景</el-button>
        </div>

        <!-- 1. 场景主表展示区 -->
        <el-card class="box-card" header="场景概览与方案定义" shadow="never">
          <el-table :data="scenes" border size="mini" highlight-current-row>
            <el-table-column label="场景名称" min-width="150">
              <template slot-scope="scope">
                <el-input v-model="scope.row.sceneName" size="mini"></el-input>
              </template>
            </el-table-column>
            <el-table-column label="测试目的" min-width="200">
              <template slot-scope="scope">
                <el-input type="textarea" :rows="2" v-model="scope.row.testObjective" size="mini"></el-input>
              </template>
            </el-table-column>
            <el-table-column label="实施方法" min-width="200">
              <template slot-scope="scope">
                <el-input type="textarea" :rows="2" v-model="scope.row.implementationMethod" size="mini"></el-input>
              </template>
            </el-table-column>
            <el-table-column label="结束条件" min-width="200">
              <template slot-scope="scope">
                <el-input type="textarea" :rows="2" v-model="scope.row.endCondition" size="mini"></el-input>
              </template>
            </el-table-column>
            <el-table-column label="类型" width="90">
              <template slot-scope="scope">
                <el-select v-model="scope.row.sceneType" size="mini">
                  <el-option :value="1" label="基准"></el-option>
                  <el-option :value="2" label="单负载"></el-option>
                  <el-option :value="3" label="混合负载"></el-option>
                  <el-option :value="4" label="稳定性"></el-option>
                  <el-option :value="5" label="极限"></el-option>
                  <el-option :value="6" label="批量"></el-option>
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="TPS比例(%)" width="90">
              <template slot-scope="scope">
                <el-input-number v-model="scope.row.targetTpsRatio" :controls="false" size="mini" style="width: 100%;" @change="handleRatioChange(scope.row)"></el-input-number>
              </template>
            </el-table-column>
            <el-table-column label="总TPS" width="80" prop="targetTotalTps"></el-table-column>
            <el-table-column label="持续时间(分)" width="90">
              <template slot-scope="scope">
                <el-input-number v-model="scope.row.globalDuration" :controls="false" size="mini" style="width: 100%;"></el-input-number>
              </template>
            </el-table-column>
            <el-table-column label="选中" width="50">
              <template slot-scope="scope">
                <el-checkbox v-model="scope.row.isSelected" :true-label="1" :false-label="0"></el-checkbox>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="50">
              <template slot-scope="scope">
                <el-button type="text" icon="el-icon-delete" style="color: #F56C6C" @click="removeScene(scope.$index)"></el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- 2. 场景明细展示区 (按块叠放) -->
        <div v-for="(scene, sIdx) in scenes" :key="scene.id || sIdx" class="scene-block">
          <el-card shadow="hover">
            <div slot="header" class="scene-header">
              <span class="scene-title">
                <i class="el-icon-s-operation"></i> {{ scene.sceneName || '未命名场景' }} - 交易配置
              </span>
              <div class="scene-meta">
                <span>TPS比例: {{ scene.targetTpsRatio }}%</span>
                <el-divider direction="vertical"></el-divider>
                <span>总TPS: {{ scene.targetTotalTps }}</span>
              </div>
            </div>

            <el-table :data="sceneDetails[sIdx]" border size="mini" stripe>
              <el-table-column label="交易名称" prop="tranName" min-width="150"></el-table-column>
              <el-table-column label="预期TPS" width="90">
                <template slot-scope="scope">
                  <el-input-number v-model="scope.row.targetTps" :controls="false" size="mini" style="width: 100%;" @change="recalcDetail(scope.row, scene)"></el-input-number>
                </template>
              </el-table-column>
              <el-table-column label="预期RT(s)" width="90">
                <template slot-scope="scope">
                  <el-input-number v-model="scope.row.targetRt" :controls="false" size="mini" style="width: 100%;" @change="recalcDetail(scope.row, scene)"></el-input-number>
                </template>
              </el-table-column>
              <el-table-column label="VU" width="80">
                <template slot-scope="scope">
                  <el-input-number v-model="scope.row.vuCount" :controls="false" size="mini" style="width: 100%;"></el-input-number>
                </template>
              </el-table-column>
              <el-table-column label="Ramp-up(s)" width="90">
                <template slot-scope="scope">
                  <el-input-number v-model="scope.row.rampUp" :controls="false" size="mini" style="width: 100%;"></el-input-number>
                </template>
              </el-table-column>
              <el-table-column label="Pacing" width="80">
                <template slot-scope="scope">
                  <el-input-number v-model="scope.row.pacing" :controls="false" size="mini" style="width: 100%;"></el-input-number>
                </template>
              </el-table-column>
              <el-table-column label="吞吐量定时器" width="110">
                <template slot-scope="scope">
                  <el-input-number v-model="scope.row.throughputTimer" :controls="false" size="mini" style="width: 100%;"></el-input-number>
                </template>
              </el-table-column>
              <el-table-column v-if="scene.sceneType === 1" label="迭代次数" width="80">
                <template slot-scope="scope">
                  <el-input-number v-model="scope.row.iterations" :controls="false" size="mini" style="width: 100%;"></el-input-number>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </div>

        <div class="form-footer">
          <el-button type="primary" @click="handleSaveAll" :loading="loading">保存所有场景及配置</el-button>
        </div>
      </div>

      <div class="guide-area">
        <div class="guide-content">
          <h3>场景配置说明</h3>
          <p><strong>1. 自动联动逻辑：</strong></p>
          <ul>
            <li>修改场景主表的 <strong>TPS比例</strong>，会自动重算该场景下所有交易的预期TPS、VU、Ramp-up及定时器。</li>
            <li><strong>VU计算公式：</strong> ROUNDUP(TPS * RT * 1.1, 0)</li>
            <li><strong>Ramp-up计算公式：</strong> ROUNDUP(VU / 5, 0)</li>
            <li><strong>定时器计算公式：</strong> ROUNDUP(TPS * 60 * 1.1, -1)</li>
          </ul>
          <p><strong>2. 场景类型说明：</strong></p>
          <ul>
            <li><strong>基准：</strong> VU固定为1，不设TPS目标，固定迭代100次。</li>
            <li><strong>极限：</strong> 默认生成左值(120%)和右值(140%)两个场景。</li>
          </ul>
          <el-divider></el-divider>
          <div class="placeholder-text">注：初始化默认场景会清空当前任务已有的场景配置，请谨慎操作。</div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script>
import { initScenes, getScenes, getSceneDetails, saveAllScenes } from '@/api/performance'

export default {
  name: 'SceneEditDialog',
  data() {
    return {
      visible: false,
      loading: false,
      taskId: null,
      scenes: [],
      sceneDetails: [], // 二维数组，索引与scenes对应
      trans: [] // 缓存交易基础指标，用于重算
    }
  },
  methods: {
    async init(taskId, trans) {
      this.visible = true
      this.taskId = taskId
      this.trans = trans || []
      await this.fetchData()
    },
    async fetchData() {
      this.loading = true
      try {
        const res = await getScenes(this.taskId)
        if (res.code === 200) {
          this.scenes = res.data
          this.sceneDetails = []
          for (const scene of this.scenes) {
            const dRes = await getSceneDetails(scene.id)
            this.sceneDetails.push(dRes.data || [])
          }
        }
      } finally {
        this.loading = false
      }
    },
    async handleInitDefault() {
      try {
        await this.$confirm('初始化将清空现有场景配置，是否继续？', '提示', { type: 'warning' })
        this.loading = true
        const res = await initScenes(this.taskId)
        if (res.code === 200) {
          this.$message.success('初始化成功')
          await this.fetchData()
        }
      } catch (e) {
        // cancel
      } finally {
        this.loading = false
      }
    },
    addCustomScene() {
      const newScene = {
        taskId: this.taskId,
        sceneName: '自定义场景',
        sceneType: 5,
        targetTpsRatio: 100,
        isSelected: 1,
        globalDuration: 10,
        targetTotalTps: 0,
        testObjective: '',
        implementationMethod: '',
        endCondition: ''
      }
      this.scenes.push(newScene)
      const details = this.trans.map(t => {
        const detail = {
          tranId: t.id,
          tranName: t.tranName,
          targetRt: t.targetRt,
          targetSuccessRate: t.targetSuccessRate,
          targetTps: t.targetTps
        }
        this.recalcDetail(detail, newScene)
        return detail
      })
      this.sceneDetails.push(details)
      this.updateTotalTps(this.scenes.length - 1)
    },
    removeScene(index) {
      this.scenes.splice(index, 1)
      this.sceneDetails.splice(index, 1)
    },
    handleRatioChange(scene) {
      const idx = this.scenes.indexOf(scene)
      if (idx === -1) return
      
      const details = this.sceneDetails[idx]
      details.forEach(detail => {
        const originalTran = this.trans.find(t => t.id === detail.tranId)
        if (originalTran) {
          const ratio = scene.targetTpsRatio / 100
          detail.targetTps = originalTran.targetTps ? (originalTran.targetTps * ratio).toFixed(2) * 1 : 0
          this.recalcDetail(detail, scene)
        }
      })
      this.updateTotalTps(idx)
    },
    recalcDetail(detail, scene) {
      if (scene.sceneType === 1) {
        detail.vuCount = 1
        detail.rampUp = 0
        detail.iterations = 100
        detail.targetTps = null
        detail.throughputTimer = null
      } else {
        const tps = detail.targetTps || 0
        const rt = detail.targetRt || 0.5
        detail.vuCount = Math.ceil(tps * rt * 1.1) || 1
        detail.rampUp = Math.ceil(detail.vuCount / 5)
        const timerRaw = tps * 66
        detail.throughputTimer = Math.ceil(timerRaw / 10) * 10
      }
    },
    updateTotalTps(idx) {
      const details = this.sceneDetails[idx]
      const total = details.reduce((sum, d) => sum + (d.targetTps * 1 || 0), 0)
      this.scenes[idx].targetTotalTps = total.toFixed(2) * 1
    },
    async handleSaveAll() {
      if (!this.taskId) {
        this.$message.error('任务ID缺失')
        return
      }
      this.loading = true
      try {
        const data = (this.scenes || []).map((scene, index) => {
          return {
            scene: scene,
            details: (this.sceneDetails && this.sceneDetails[index]) ? this.sceneDetails[index] : []
          }
        })
        console.log('Saving scenes data:', data)
        const res = await saveAllScenes(this.taskId, data)
        if (res && res.code === 200) {
          this.$message.success('全部保存成功')
          this.$emit('refresh')
          this.visible = false
        } else {
          this.$message.error(res ? res.message : '保存失败')
        }
      } catch (e) {
        console.error('Save scenes error:', e)
        this.$message.error('保存失败: ' + (e.message || '未知错误'))
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.dialog-layout { display: flex; height: 85vh; }
.input-area { flex: 4; padding-right: 20px; border-right: 1px solid #ebeef5; overflow-y: auto; }
.guide-area { flex: 1; padding-left: 20px; overflow-y: auto; background-color: #fafafa; }
.box-card { margin-bottom: 20px; }
.scene-block { margin-bottom: 25px; }
.scene-header { display: flex; justify-content: space-between; align-items: center; }
.scene-title { font-weight: bold; color: #409EFF; }
.scene-meta { font-size: 13px; color: #606266; }
.form-footer { margin-top: 30px; text-align: center; padding-bottom: 20px; }
.placeholder-text { color: #F56C6C; font-size: 12px; margin-top: 10px; font-weight: bold; }
.demo-table-expand { font-size: 0; }
.demo-table-expand label { width: 90px; color: #99a9bf; }
.demo-table-expand .el-form-item { margin-right: 0; margin-bottom: 0; width: 100%; display: flex; align-items: center; }
</style>
