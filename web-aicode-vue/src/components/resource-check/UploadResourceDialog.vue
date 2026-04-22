<!-- src/views/UploadResourceDialog.vue -->
<template>
  <el-dialog
    title="上传环境资源清单"
    :visible.sync="visible"
    width="550px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form :model="form" label-width="100px" :rules="rules" ref="uploadForm" size="small">
      <!-- 产品标识 -->
      <el-form-item label="产品标识" prop="productId">
        <el-input
          v-model="form.productId"
          placeholder="请输入产品标识，如：BPS-D-AUTO"
          style="width: 100%"
          clearable
        />
      </el-form-item>

      <!-- 文件来源 -->
      <el-form-item label="文件来源" prop="fileSource">
        <el-select v-model="form.fileSource" placeholder="请选择文件来源" style="width: 100%">
          <el-option label="部署方案" value="部署方案" />
          <el-option label="资源申请表" value="资源申请表" />
        </el-select>
      </el-form-item>

      <!-- 文件上传 -->
      <el-form-item label="Excel文件" prop="file">
        <el-upload
          ref="upload"
          action=""
          :auto-upload="false"
          :on-change="handleFileChange"
          :file-list="fileList"
          :limit="1"
          accept=".xlsx,.xls"
          :show-file-list="true"
        >
          <el-button slot="trigger" size="small" type="primary">选取文件</el-button>
          <div slot="tip" class="el-upload__tip">支持 .xls、.xlsx 格式，大小不超过 10MB</div>
        </el-upload>
      </el-form-item>
    </el-form>

    <!-- 上传结果 -->
    <el-alert
      v-if="result"
      :title="result"
      :type="result.includes('成功') ? 'success' : 'error'"
      show-icon
      :closable="false"
      style="margin-top: 15px"
    />

    <div slot="footer" class="dialog-footer">
      <el-button @click="visible = false" size="small">取 消</el-button>
      <el-button type="primary" @click="submitUpload" :loading="uploading" size="small">
        {{ uploading ? '上传中...' : '立即上传' }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import { uploadResource } from '@/api/resource'

export default {
  name: 'UploadResourceDialog',
  data() {
    return {
      visible: false,
      form: {
        productId: '',
        fileSource: '部署方案'
      },
      fileList: [],
      file: null,
      uploading: false,
      result: ''
    }
  },
  computed: {
    rules() {
      return {
        productId: [{ required: true, message: '请输入产品标识', trigger: 'blur' }],
        fileSource: [{ required: true, message: '请选择文件来源', trigger: 'change' }],
        file: [{ required: true, validator: (rule, value, callback) => {
          if (!this.file) {
            callback(new Error('请上传文件'))
          } else {
            callback()
          }
        }, trigger: 'change' }]
      }
    }
  },
  methods: {
    init(productId) {
      this.visible = true
      this.form.productId = productId || ''
      this.form.fileSource = '部署方案'
      this.fileList = []
      this.file = null
      this.result = ''
      this.$nextTick(() => {
        if (this.$refs.uploadForm) {
          this.$refs.uploadForm.clearValidate()
        }
      })
    },
    handleFileChange(file, fileList) {
      const { name } = file
      if (!/\.(xlsx|xls)$/.test(name)) {
        this.$message.error('只支持上传 Excel 文件（.xls, .xlsx）')
        this.fileList = []
        this.file = null
        return
      }
      this.file = file.raw
      this.fileList = fileList
    },
    async submitUpload() {
      try {
        await this.$refs.uploadForm.validate()
      } catch (error) {
        return
      }

      this.uploading = true
      this.result = ''

      try {
        const res = await uploadResource(this.form.productId, this.file, this.form.fileSource)

        if (res.code === 200) {
          this.result = '✅ 上传成功！'
          this.$message.success('文件上传成功')
          this.$emit('refresh')
          setTimeout(() => {
            this.visible = false
          }, 1500)
        } else {
          this.result = '❌ ' + (res.message || '上传失败')
        }
      } catch (error) {
        this.result = '❌ 网络错误或接口异常'
        this.$message.error('请求失败，请检查接口是否可达')
        console.error(error)
      } finally {
        this.uploading = false
      }
    },
    handleClose() {
      this.resetForm()
    },
    resetForm() {
      if (this.$refs.uploadForm) {
        this.$refs.uploadForm.resetFields()
      }
      this.fileList = []
      this.file = null
      this.result = ''
    }
  }
}
</script>

<style scoped>
::v-deep .el-dialog {
  border-radius: 12px;
  overflow: hidden;
}
::v-deep .el-dialog__header {
  background: #f6f9ff;
  border-bottom: 1px solid #e6eef9;
}
::v-deep .el-input__inner,
::v-deep .el-button {
  border-radius: 8px;
}
</style>
