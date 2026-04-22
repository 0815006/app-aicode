<template>
  <el-dialog title="模板文件维护" :visible.sync="visible" width="600px">
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
          <div slot="tip" class="el-upload__tip">上传文件将替换同名文件，建议包含“业务交易调查表”或“批量运行调查表”关键字</div>
        </el-upload>
      </div>

      <el-table :data="fileList" border size="small" style="margin-top: 20px">
        <el-table-column prop="fileName" label="文件名" min-width="200"></el-table-column>
        <el-table-column prop="fileSize" label="大小" width="100">
          <template slot-scope="scope">{{ (scope.row.fileSize / 1024).toFixed(2) }} KB</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template slot-scope="scope">
            <el-button type="text" size="mini" @click="handleDownload(scope.row.fileName)">下载</el-button>
            <el-button type="text" size="mini" style="color: #F56C6C" @click="handleDelete(scope.row.fileName)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </el-dialog>
</template>

<script>
import { listTemplates, deleteTemplate, getTemplateDownloadUrl } from '@/api/performance'

export default {
  name: 'TemplateManageDialog',
  data() {
    return {
      visible: false,
      fileList: [],
      uploadUrl: '/api/performance/file/upload'
    }
  },
  methods: {
    init() {
      this.visible = true
      this.fetchFileList()
    },
    async fetchFileList() {
      const res = await listTemplates()
      if (res.code === 200) {
        this.fileList = res.data
      }
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
        const res = await deleteTemplate(fileName)
        if (res.code === 200) {
          this.$message.success('删除成功')
          this.fetchFileList()
        }
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
