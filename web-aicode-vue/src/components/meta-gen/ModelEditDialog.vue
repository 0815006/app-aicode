<template>
  <el-dialog :title="isCreate ? '新建模型' : '编辑模型'" :visible.sync="visible" width="600px" append-to-body @close="handleClose">
    <el-form :model="form" label-width="100px" size="small">
      <el-form-item label="模型名称" required>
        <el-input v-model="form.modelName" placeholder="如：代发工资批量文件" />
      </el-form-item>
      <el-form-item label="编码" required>
        <el-select v-model="form.encoding">
          <el-option label="UTF-8" value="UTF-8" />
          <el-option label="GBK" value="GBK" />
          <el-option label="ASCII" value="ASCII" />
        </el-select>
        <span v-if="form.splitType === 'FIXED'" style="color:#e6a23c;font-size:11px">定长模式推荐使用GBK</span>
      </el-form-item>
      <el-form-item label="格式" required>
        <el-select v-model="form.splitType" @change="onSplitTypeChange">
          <el-option label="定长 (FIXED)" value="FIXED" />
          <el-option label="分隔符 (DELIMITER)" value="DELIMITER" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="form.splitType === 'DELIMITER'" label="分隔符">
        <el-input v-model="form.delimiter" placeholder="如 , 或 |" style="width:120px" />
      </el-form-item>
      <el-form-item label="换行符">
        <el-input v-model="form.lineEndingChar" placeholder="默认 \r\n" style="width:120px" />
      </el-form-item>
      <el-form-item label="最大行数">
        <el-input-number v-model="form.maxRowsLimit" :min="1" :max="9999999" style="width:100%" />
        <span style="color:#909399;font-size:11px;margin-left:8px">安全阈值，防止误操作生成海量数据</span>
      </el-form-item>
      <el-form-item label="文件头">
        <el-switch v-model="form.hasHeader" :active-value="1" :inactive-value="0" />
      </el-form-item>
      <el-form-item label="文件尾">
        <el-switch v-model="form.hasFooter" :active-value="1" :inactive-value="0" />
      </el-form-item>
      <el-form-item label="共享给">
        <el-input v-model="form.sharedWith" placeholder="输入员工工号，多个用逗号分隔，如 100001,100002" />
      </el-form-item>
    </el-form>
    <div slot="footer">
      <el-button size="small" @click="visible = false">取消</el-button>
      <el-button size="small" type="primary" @click="handleSave" :loading="loading">
        {{ isCreate ? '创建' : '保存' }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import { createModel, updateModel } from '@/api/meta-gen'

export default {
  name: 'ModelEditDialog',
  data() {
    return {
      visible: false,
      loading: false,
      isCreate: false,
      activeModelId: null,
      form: {}
    }
  },
  methods: {
    init(modelData) {
      this.loading = false
      if (modelData) {
        // 编辑
        this.isCreate = false
        this.activeModelId = modelData.id
        this.form = { ...modelData }
        if (this.form.hasHeader === undefined) this.form.hasHeader = 1
        if (this.form.hasFooter === undefined) this.form.hasFooter = 1
        if (!this.form.maxRowsLimit) this.form.maxRowsLimit = 100
        if (!this.form.encoding) this.form.encoding = 'UTF-8'
        if (!this.form.splitType) this.form.splitType = 'DELIMITER'
      } else {
        // 新建
        this.isCreate = true
        this.activeModelId = null
        this.form = {
          modelName: '新模型',
          splitType: 'DELIMITER',
          delimiter: ',',
          encoding: 'UTF-8',
          maxRowsLimit: 100,
          hasHeader: 1,
          hasFooter: 1,
          lineEndingChar: '',
          sharedWith: ''
        }
      }
      this.visible = true
    },
    onSplitTypeChange(val) {
      // UTF-8 现在支持定长和分隔符模式，无需强制切换编码
    },
    async handleSave() {
      if (!this.form.modelName || !this.form.modelName.trim()) {
        this.$message.warning('请输入模型名称')
        return
      }
      this.loading = true
      try {
        if (this.isCreate) {
          await createModel({ ...this.form })
          this.$message.success('模型创建成功')
        } else {
          await updateModel(this.activeModelId, { ...this.form })
          this.$message.success('保存成功')
        }
        this.visible = false
        this.$emit('refresh')
        this.$emit('listRefresh')
      } finally {
        this.loading = false
      }
    },
    handleClose() {
      this.visible = false
    }
  }
}
</script>
