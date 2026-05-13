<template>
  <el-dialog title="模板文件维护" :visible.sync="visible" width="700px" append-to-body>
    <div class="template-manage-container">
      <div class="upload-section">
        <el-upload
          class="upload-demo"
          :action="uploadUrl"
          :on-success="handleUploadSuccess"
          :on-error="handleUploadError"
          :show-file-list="false"
        >
          <el-button size="small" type="primary" icon="el-icon-upload">上传新模板</el-button>
          <div slot="tip" class="el-upload__tip">上传模型/字段定义模板文件，支持 .json /.xlsx 格式</div>
        </el-upload>
      </div>

      <el-table :data="fileList" border size="small" v-loading="loading" style="margin-top: 20px">
        <el-table-column prop="fileName" label="文件名" min-width="200"></el-table-column>
        <el-table-column prop="fileSize" label="大小" width="100">
          <template slot-scope="scope">{{ (scope.row.fileSize / 1024).toFixed(2) }} KB</template>
        </el-table-column>
        <el-table-column prop="lastModified" label="修改时间" width="160">
          <template slot-scope="scope">{{ formatTime(scope.row.lastModified) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template slot-scope="scope">
            <el-button type="text" size="mini" @click="handleDownload(scope.row.fileName)">下载</el-button>
            <el-button type="text" size="mini" style="color: #F56C6C" @click="handleDelete(scope.row.fileName)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="fileList.length === 0 && !loading" style="text-align:center;color:#909399;padding:20px">暂无模板文件</div>
    </div>
  </el-dialog>
</template>

<script>
import { listTemplates, deleteTemplate, getTemplateDownloadUrl } from '@/api/meta-gen'

export default {
  name: 'TemplateManageDialog',
  data() {
    return {
      visible: false,
      loading: false,
      fileList: [],
      uploadUrl: '/api/meta/template/upload'
    }
  },
  methods: {
    init() {
      this.visible = true
      this.fetchFileList()
    },
    async fetchFileList() {
      this.loading = true
      try {
        const res = await listTemplates()
        this.fileList = res.data || []
      } finally {
        this.loading = false
      }
    },
    formatTime(timestamp) {
      if (!timestamp) return '-'
      const d = new Date(timestamp)
      const pad = n => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
    },
    handleUploadSuccess() {
      this.$message.success('上传成功')
      this.fetchFileList()
    },
    handleUploadError() {
      this.$message.error('上传失败')
    },
    handleDownload(fileName) {
      window.open(getTemplateDownloadUrl(fileName))
    },
    async handleDelete(fileName) {
      try {
        await this.$confirm(`确定删除模板文件 ${fileName} 吗？`, '提示', { type: 'warning' })
        await deleteTemplate(fileName)
        this.$message.success('删除成功')
        this.fetchFileList()
      } catch (e) {
        // cancel
      }
    }
  }
}
</script>

<style scoped>
.template-manage-container {
  padding: 10px;
}
.upload-section {
  text-align: right;
}
</style>
