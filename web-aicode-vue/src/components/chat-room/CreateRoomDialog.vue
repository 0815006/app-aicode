<template>
  <el-dialog
    title="创建聊天房间"
    :visible.sync="dialogVisible"
    width="420px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form ref="form" :model="form" label-width="80px">
      <el-form-item label="房间标题" required>
        <el-input
          v-model="form.title"
          placeholder="请输入房间标题"
          maxlength="50"
          show-word-limit
        ></el-input>
      </el-form-item>
    </el-form>
    <span slot="footer">
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">创建</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { createRoom } from '@/api/chat-room'

export default {
  name: 'CreateRoomDialog',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    creator: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      form: { title: '' },
      loading: false
    }
  },
  computed: {
    dialogVisible: {
      get() { return this.visible },
      set(val) { this.$emit('update:visible', val) }
    }
  },
  methods: {
    handleClose() {
      this.form.title = ''
      this.$emit('update:visible', false)
    },
    handleConfirm() {
      if (!this.form.title.trim()) {
        this.$message.warning('请输入房间标题')
        return
      }
      this.loading = true
      createRoom({ title: this.form.title.trim(), creator: this.creator }).then(res => {
        if (res.code === 200) {
          this.$message.success('房间创建成功')
          this.$emit('created', res.data)
          this.handleClose()
        } else {
          this.$message.error(res.message || '创建失败')
        }
      }).catch(() => {
        this.$message.error('创建失败')
      }).finally(() => {
        this.loading = false
      })
    }
  }
}
</script>
