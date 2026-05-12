<template>
  <el-dialog title="引用文件管理" :visible.sync="visible" width="600px" append-to-body @close="handleClose">
    <div style="margin-bottom:12px">
      <el-button type="primary" size="small" icon="el-icon-upload" @click="triggerUpload">上传引用文件</el-button>
      <input ref="fileInput" type="file" style="display:none" @change="handleFileChange" />
    </div>
    <el-table :data="refList" border size="small" v-loading="loading">
      <el-table-column prop="refName" label="文件名称" width="180" />
      <el-table-column label="解析模式" width="100">
        <template slot-scope="scope">{{ scope.row.parseType === 'FIXED' ? '定长' : '分隔符' }}</template>
      </el-table-column>
      <el-table-column prop="delimiter" label="分隔符" width="80" />
      <el-table-column prop="filePath" label="路径" show-overflow-tooltip />
      <el-table-column label="操作" width="100">
        <template slot-scope="scope">
          <el-button type="text" size="small" @click="handleDefine(scope.row)">配置映射</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 配置映射弹窗 -->
    <el-dialog title="配置列映射" :visible.sync="mapVisible" width="450px" append-to-body>
      <el-form :model="mapForm" label-width="100px" size="small">
        <el-form-item label="解析模式">
          <el-select v-model="mapForm.parseType" style="width:100%">
            <el-option label="分隔符 (DELIMITER)" value="DELIMITER" />
            <el-option label="定长 (FIXED)" value="FIXED" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="mapForm.parseType === 'DELIMITER'" label="分隔符">
          <el-input v-model="mapForm.delimiter" placeholder="如 , 或 |" />
        </el-form-item>
        <el-form-item label="列映射 (JSON)">
          <el-input v-model="mapForm.columnMapping" type="textarea" :rows="4" placeholder='如 {"userName":1,"age":2}' />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button size="small" @click="mapVisible = false">取消</el-button>
        <el-button size="small" type="primary" @click="handleSaveDefine">保存</el-button>
      </div>
    </el-dialog>
  </el-dialog>
</template>

<script>
import { listRefFiles, uploadRefFile, defineRefFile } from '@/api/meta-gen'

export default {
  name: 'RefFileDialog',
  props: { dialogVisible: Boolean },
  data() {
    return {
      refList: [],
      loading: false,
      mapVisible: false,
      mapForm: {},
      uploadFile: null
    }
  },
  computed: {
    visible: {
      get() { return this.dialogVisible },
      set(v) { this.$emit('update:dialogVisible', v) }
    }
  },
  watch: {
    dialogVisible(v) { if (v) this.loadData() }
  },
  methods: {
    loadData() {
      this.loading = true
      listRefFiles().then(res => { this.refList = res.data || [] }).finally(() => { this.loading = false })
    },
    triggerUpload() { this.$refs.fileInput.click() },
    handleFileChange(e) {
      const file = e.target.files[0]
      if (!file) return
      const formData = new FormData()
      formData.append('file', file)
      formData.append('refName', file.name)
      uploadRefFile(formData).then(() => {
        this.$message.success('上传成功')
        this.loadData()
        this.$emit('changed')
      })
    },
    handleDefine(row) {
      this.mapForm = { ...row }
      if (!this.mapForm.columnMapping) this.mapForm.columnMapping = ''
      this.mapVisible = true
    },
    handleSaveDefine() {
      defineRefFile(this.mapForm).then(() => {
        this.$message.success('配置保存成功')
        this.mapVisible = false
        this.loadData()
      })
    },
    handleClose() {
      this.$emit('update:dialogVisible', false)
    }
  }
}
</script>
