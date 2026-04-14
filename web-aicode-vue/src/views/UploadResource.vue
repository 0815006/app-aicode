<!-- src/views/UploadResource.vue -->
<template>
  <div class="upload-container">
    <h2 class="page-title">上传环境资源清单</h2>

    <el-form :model="form" label-width="100px" :rules="rules" ref="uploadForm">
      <!-- 产品标识 -->
      <el-form-item label="产品标识" prop="productId">
        <el-input
          v-model="form.productId"
          placeholder="请输入产品标识，如：BPS-D-AUTO"
          style="width: 300px"
          clearable
        />
      </el-form-item>

      <!-- 文件上传 -->
      <el-form-item label="Excel文件" prop="file">
        <el-upload
          ref="upload"
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

      <!-- 上传按钮 -->
      <el-form-item>
        <el-button type="primary" @click="submitUpload" :loading="uploading">
          {{ uploading ? '上传中...' : '立即上传' }}
        </el-button>
        <el-button @click="resetForm">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 上传结果 -->
    <el-alert
      v-if="result"
      :title="result"
      :type="result.includes('成功') ? 'success' : 'error'"
      show-icon
      :closable="false"
    />
  </div>
</template>

<script>
// 先导入
import { uploadResource } from '@/api/resource'

export default {
  name: 'UploadResource',
  data() {
    return {
      form: {
        productId: 'BPS-D-AUTO'
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
        file: [{ required: true, validator: () => !!this.file, message: '请上传文件', trigger: 'change' }]
      }
    }
  },
  methods: {
    handleFileChange(file, fileList) {
      const { name } = file
      if (!/\.(xlsx|xls)$/.test(name)) {
        this.$message.error('只支持上传 Excel 文件（.xls, .xlsx）')
        this.fileList = []
        this.file = null
        return
      }
      this.file = file.raw // 保存原始文件对象
    },
    async submitUpload() {
      await this.$refs.uploadForm.validate(valid => {
        if (!valid) {
          this.$message.warning('请填写完整信息')
          return
        }
      })

      const formData = new FormData()
      formData.append('productId', this.form.productId)
      formData.append('file', this.file)

      this.uploading = true
      this.result = ''

      try {
        // ✅ 使用您封装的接口（不要用 this.$http）
        const res = await uploadResource(this.form.productId, this.file)

        if (res.data && res.data.code === 200) {
          this.result = '✅ 上传成功！'
          this.$message.success('文件上传成功')
        } else {
          this.result = '❌ ' + (res.data.message || '上传失败')
        }
      } catch (error) {
        this.result = '❌ 网络错误或接口异常'
        this.$message.error('请求失败，请检查接口是否可达')
        console.error(error)
      } finally {
        this.uploading = false
      }
    },
    resetForm() {
      this.$refs.uploadForm.resetFields()
      this.fileList = []
      this.file = null
      this.result = ''
    }
  }
}
</script>

<style scoped>
.upload-container {
  max-width: 680px;
  margin: 24px auto;
  padding: 22px;
  border-radius: 14px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  border: 1px solid #dbe7f7;
  box-shadow: 0 10px 22px rgba(21, 52, 105, 0.08);
}

.page-title {
  margin: 0 0 18px;
  font-size: 22px;
  color: #1f2d3d;
}

::v-deep .el-form-item {
  margin-bottom: 18px;
}

::v-deep .el-input__inner {
  border-radius: 8px;
}

::v-deep .el-upload {
  display: block;
}

::v-deep .el-upload-list__item {
  border-radius: 8px;
}

::v-deep .el-button {
  border-radius: 8px;
}

::v-deep .el-alert {
  margin-top: 10px;
  border-radius: 8px;
}
</style>
