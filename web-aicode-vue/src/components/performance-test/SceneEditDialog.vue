<template>
  <el-dialog title="编辑测试场景" :visible.sync="visible" width="90%" top="5vh">
    <div class="dialog-layout">
      <div class="input-area">
        <div style="margin-bottom: 10px;">
          <el-button type="primary" size="small" icon="el-icon-refresh" @click="initDefaultScenes">初始化默认场景</el-button>
          <el-button type="success" size="small" icon="el-icon-plus" @click="addRow">添加专项场景</el-button>
        </div>
        <el-table :data="list" border size="small">
          <el-table-column label="场景名称"><template slot-scope="scope"><el-input v-model="scope.row.sceneName" size="mini"></el-input></template></el-table-column>
          <el-table-column label="预期TPS" width="90"><template slot-scope="scope"><el-input-number v-model="scope.row.targetTps" :controls="false" size="mini" style="width: 100%;"></el-input-number></template></el-table-column>
          <el-table-column label="预期RT" width="90"><template slot-scope="scope"><el-input-number v-model="scope.row.targetRt" :controls="false" size="mini" style="width: 100%;"></el-input-number></template></el-table-column>
          <el-table-column label="选中" width="50"><template slot-scope="scope"><el-checkbox v-model="scope.row.isSelected" :true-label="1" :false-label="0"></el-checkbox></template></el-table-column>
          <el-table-column label="操作" width="50"><template slot-scope="scope"><el-button type="text" icon="el-icon-delete" @click="list.splice(scope.$index, 1)"></el-button></template></el-table-column>
        </el-table>
        <div class="form-footer">
          <el-button type="primary" @click="handleSave" :loading="loading">保存场景定义</el-button>
        </div>
      </div>
      <div class="guide-area">
        <div class="guide-content">
          <h3>测试场景指南</h3>
          <p><strong>1. 常规场景：</strong> 包含基准、负载、混合、稳定性、极限、批量常规。</p>
          <p><strong>2. 专项场景：</strong> 针对特殊业务需求（如下游承压、挡板模拟）另设专项。</p>
          <el-divider></el-divider>
          <div v-for="i in 15" :key="i" class="placeholder-text">补充说明内容占位...</div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script>
import { saveScenes } from '@/api/performance'

export default {
  name: 'SceneEditDialog',
  data() {
    return {
      visible: false,
      loading: false,
      taskId: null,
      list: []
    }
  },
  methods: {
    init(taskId, scenes) {
      this.visible = true
      this.taskId = taskId
      this.list = JSON.parse(JSON.stringify(scenes || []))
    },
    addRow() {
      this.list.push({ sceneName: '', sceneType: 5, isSelected: 1 })
    },
    initDefaultScenes() {
      const defaults = [
        { sceneName: '单交易基准测试', sceneType: 1, isSelected: 1 },
        { sceneName: '单交易负载测试', sceneType: 2, isSelected: 1 },
        { sceneName: '60%混合负载测试', sceneType: 3, isSelected: 1 },
        { sceneName: '80%混合负载测试', sceneType: 3, isSelected: 1 },
        { sceneName: '100%混合负载测试', sceneType: 3, isSelected: 1 },
        { sceneName: '80%稳定性测试(4小时)', sceneType: 4, isSelected: 1 },
        { sceneName: '极限场景测试', sceneType: 5, isSelected: 1 },
        { sceneName: '批量作业专项测试', sceneType: 6, isSelected: 1 }
      ]
      // 过滤掉已有的默认场景，避免重复
      const existingNames = this.list.map(s => s.sceneName)
      defaults.forEach(d => {
        if (!existingNames.includes(d.sceneName)) {
          this.list.push(d)
        }
      })
    },
    async handleSave() {
      this.loading = true
      const res = await saveScenes(this.taskId, this.list)
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
.dialog-layout { display: flex; height: 70vh; }
.input-area { flex: 2; padding-right: 20px; border-right: 1px solid #ebeef5; overflow-y: auto; }
.guide-area { flex: 1; padding-left: 20px; overflow-y: auto; background-color: #fafafa; }
.form-footer { margin-top: 20px; text-align: right; }
.placeholder-text { color: #c0c4cc; font-size: 12px; margin-top: 10px; }
</style>
