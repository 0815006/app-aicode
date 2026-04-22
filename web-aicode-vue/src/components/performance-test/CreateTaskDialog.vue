<template>
  <el-dialog title="新建性能测试任务" :visible.sync="visible" width="500px" @close="handleClose">
    <el-form :model="form" :rules="rules" ref="taskForm" label-width="100px" size="small">
      <el-form-item label="批次" prop="batchNo">
        <el-input v-model="form.batchNo" placeholder="如：2606"></el-input>
      </el-form-item>
      <el-form-item label="产品" prop="productId">
        <el-input v-model="form.productId" placeholder="如：BPS-D-AUTO"></el-input>
      </el-form-item>
      <el-form-item label="填报人员" prop="recorderRange">
        <el-input type="textarea" v-model="form.recorderRange" placeholder="7位员工号，逗号分隔"></el-input>
      </el-form-item>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button @click="visible = false">取 消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="loading">确 定</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { saveTask } from '@/api/performance'
import { getCurrentEmpNo } from '@/utils/currentUser'

export default {
  name: 'CreateTaskDialog',
  data() {
    return {
      visible: false,
      loading: false,
      form: {
        batchNo: '',
        productId: '',
        recorderRange: '',
        creatorId: getCurrentEmpNo(),
        status: 10
      },
      rules: {
        batchNo: [{ required: true, message: '请输入批次', trigger: 'blur' }],
        productId: [{ required: true, message: '请输入产品标识', trigger: 'blur' }]
      }
    }
  },
  methods: {
    init() {
      this.visible = true
      this.form.batchNo = ''
      this.form.productId = ''
      this.form.recorderRange = ''
      this.$nextTick(() => {
        this.$refs.taskForm.clearValidate()
      })
    },
    async handleSubmit() {
      try {
        await this.$refs.taskForm.validate()
        this.loading = true
        const res = await saveTask(this.form)
        if (res.code === 200) {
          this.$message.success('创建成功')
          this.visible = false
          this.$emit('refresh')
        }
      } catch (error) {
        console.error(error)
      } finally {
        this.loading = false
      }
    },
    handleClose() {
      this.$refs.taskForm.resetFields()
    }
  }
}
</script>
