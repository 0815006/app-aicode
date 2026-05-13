<template>
  <el-dialog title="引用文件管理" :visible.sync="visible" width="800px" append-to-body @close="handleClose">
    <div style="margin-bottom:12px; text-align: right">
      <el-button type="primary" size="small" icon="el-icon-upload" @click="triggerUpload">上传引用文件</el-button>
      <input ref="fileInput" type="file" style="display:none" @change="handleFileChange" />
    </div>
    <el-table :data="refList" border size="small" v-loading="loading">
      <el-table-column prop="refName" label="文件名称" min-width="180" />
      <el-table-column label="解析模式" width="120">
        <template slot-scope="scope">
          {{ scope.row.parseType === 'FIXED' ? '定长' : '分隔符(' + (scope.row.delimiter || '-') + ')' }}
        </template>
      </el-table-column>
      <el-table-column prop="filePath" label="路径" show-overflow-tooltip />
      <el-table-column label="操作" width="150">
        <template slot-scope="scope">
          <el-button type="text" size="small" @click="handlePreview(scope.row)">
            <i class="el-icon-view"></i>
          </el-button>
          <el-button type="text" size="small" @click="handleDefine(scope.row)">配置映射</el-button>
          <el-button type="text" size="small" style="color: #f56c6c" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 配置映射弹窗 -->
    <el-dialog title="配置列映射" :visible.sync="mapVisible" width="600px" append-to-body>
      <el-form :model="mapForm" label-width="120px" size="small">
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
          <el-input v-model="mapForm.columnMapping" type="textarea" :rows="6" :placeholder="columnMappingPlaceholder" />
        </el-form-item>
      </el-form>
      <!-- 填写指南 -->
      <div v-if="mapForm.parseType === 'FIXED'" style="margin: 10px 0; padding: 10px; background: #f5f7fa; border-radius: 4px; font-size: 12px; color: #606266; line-height: 1.8;">
        <div style="font-weight: bold; margin-bottom: 4px;">填写指南：</div>
        <div>• 定长格式：{"字段名": {"start": 起始位置, "length": 字段长度}}</div>
        <div>• start: 从1开始计算的起始字节位置</div>
        <div>• length: 字段的字节长度</div>
        <div>• 示例：{"name": {"start": 1, "length": 10}, "age": {"start": 11, "length": 3}}</div>
      </div>
      <div v-else style="margin: 10px 0; padding: 10px; background: #f5f7fa; border-radius: 4px; font-size: 12px; color: #606266; line-height: 1.8;">
        <div style="font-weight: bold; margin-bottom: 4px;">填写指南：</div>
        <div>• 分隔符格式：{"字段名": 列序号}</div>
        <div>• 列序号从1开始，对应文件表头的第N列</div>
        <div>• 示例：{"userName": 1, "age": 2, "email": 3}</div>
      </div>
      <div slot="footer">
        <el-button size="small" @click="mapVisible = false">取消</el-button>
        <el-button size="small" type="primary" @click="handleSaveDefine">保存</el-button>
      </div>
    </el-dialog>

    <!-- 文件预览弹窗 -->
    <el-dialog title="文件预览" :visible.sync="previewVisible" width="1000px" append-to-body>
      <div v-if="previewLoading" v-loading="previewLoading"></div>
      <div v-else>
        <div style="margin-bottom: 15px; line-height: 1.8;">
          <div><strong>解析模式：</strong>{{ previewRow.parseType === 'FIXED' ? '定长' : '分隔符(' + (previewRow.delimiter || '-') + ')' }}</div>
          <div><strong>列映射 (JSON)：</strong>{{ previewRow.columnMapping || '-' }}</div>
        </div>
        <el-table :data="previewLines" border size="small" height="300">
          <el-table-column label="行号" width="80" align="center">
            <template slot-scope="scope">{{ scope.$index + 1 }}</template>
          </el-table-column>
          <el-table-column label="内容" min-width="800">
            <template slot-scope="scope">
              <div style="font-family: monospace; white-space: pre-wrap; word-break: break-all;">{{ scope.row }}</div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </el-dialog>
</template>

<script>
import { listRefFiles, uploadRefFile, defineRefFile, deleteRefFile, previewRefFile } from '@/api/meta-gen'

export default {
  name: 'RefFileDialog',
  props: { dialogVisible: Boolean },
  data() {
    return {
      refList: [],
      loading: false,
      mapVisible: false,
      mapForm: {},
      uploadFile: null,
      previewVisible: false,
      previewLines: [],
      previewLoading: false,
      previewRow: {},
      originalParseType: ''
    }
  },
  computed: {
    visible: {
      get() { return this.dialogVisible },
      set(v) { this.$emit('update:dialogVisible', v) }
    },
    columnMappingPlaceholder() {
      if (this.mapForm.parseType === 'FIXED') {
        return '定长格式: {"name":{"start":1,"length":10},"age":{"start":11,"length":3}}'
      }
      return '分隔符格式: {"userName":1,"age":2}'
    }
  },
  watch: {
    dialogVisible(v) { if (v) this.loadData() },
    'mapForm.parseType'(newVal, oldVal) {
      // 当解析模式切换到FIXED时，提供FIXED格式的默认值
      if (newVal === 'FIXED' && oldVal === 'DELIMITER' && !this.mapForm.columnMapping) {
        this.mapForm.columnMapping = '{"name":{"start":1,"length":10},"age":{"start":11,"length":3}}'
      }
    }
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
      // 设置默认值
      if (!this.mapForm.parseType) {
        this.mapForm.parseType = 'DELIMITER'
      }
      if (!this.mapForm.delimiter) {
        this.mapForm.delimiter = ','
      }
      // 记录原始解析模式，用于判断是否需要设置默认值
      this.originalParseType = this.mapForm.parseType
      if (!this.mapForm.columnMapping) {
        if (this.mapForm.parseType === 'FIXED') {
          this.mapForm.columnMapping = '{"name":{"start":1,"length":10},"age":{"start":11,"length":3}}'
        } else {
          this.mapForm.columnMapping = '{"col1":1,"col2":2}'
        }
      }
      this.mapVisible = true
    },
    handleSaveDefine() {
      defineRefFile(this.mapForm).then(() => {
        this.$message.success('配置保存成功')
        this.mapVisible = false
        this.loadData()
      })
    },
    handleDelete(row) {
      this.$confirm('确定要删除该引用文件吗？删除后无法恢复', '确认删除', {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消'
      }).then(() => {
        deleteRefFile(row.id).then(() => {
          this.$message.success('删除成功')
          this.loadData()
          this.$emit('changed')
        })
      }).catch(() => {})
    },
    handlePreview(row) {
      this.previewVisible = true
      this.previewLoading = true
      this.previewLines = []
      this.previewRow = row
      previewRefFile(row.id, 5).then(res => {
        this.previewLines = res.data || []
      }).finally(() => {
        this.previewLoading = false
      })
    },
    handleClose() {
      this.$emit('update:dialogVisible', false)
    }
  }
}
</script>
