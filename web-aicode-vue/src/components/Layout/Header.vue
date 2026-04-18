<!-- src/components/Layout/Header.vue -->
<template>
  <div class="header-wrapper">
    <div class="announcement-bar">
      仅限自己使用，不对外开放
    </div>

    <div class="header-container">
      <div class="title"></div>
      <div class="user-info">
        <span class="label">当前员工号：</span>
        <template v-if="!editing">
          <el-tag class="emp-no" effect="dark" @click="startEdit">{{ empNo }}</el-tag>
        </template>
        <template v-else>
          <el-input
            v-model.trim="editingEmpNo"
            size="mini"
            maxlength="7"
            class="emp-input"
            placeholder="7位员工号"
            @keyup.enter.native="switchEmpNo"
          />
          <el-button type="warning" size="mini" @click="switchEmpNo">切换员工号</el-button>
          <el-button size="mini" @click="cancelEdit">取消</el-button>
        </template>
      </div>
    </div>
  </div>
</template>

<script>
import { getCurrentEmpNo, isEmpNoValid, setCurrentEmpNo } from '@/utils/currentUser'

export default {
  name: 'Header',
  data() {
    return {
      empNo: getCurrentEmpNo(),
      editingEmpNo: '',
      editing: false
    }
  },
  methods: {
    startEdit() {
      this.editingEmpNo = this.empNo
      this.editing = true
    },
    cancelEdit() {
      this.editing = false
      this.editingEmpNo = ''
    },
    switchEmpNo() {
      const target = (this.editingEmpNo || '').trim()
      if (!isEmpNoValid(target)) {
        this.$message.warning('员工号必须是7位数字')
        return
      }
      setCurrentEmpNo(target)
      this.empNo = target
      this.editing = false
      this.$message.success('已切换当前员工号')
    }
  }
}
</script>

<style scoped>
.announcement-bar {
  background-color: #409eff; /* 原主题色 */
  color: #2528d9;
  font-size: 14px;
  font-weight: 500;
  text-align: center;
  padding: 6px 0;
  border-bottom: 1px solid  #409eff; /* 原主题色 */
}

.header-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  height: 50px; /* 原高度 */
  color: #fff;
  font-size: 16px;
  font-weight: 500;
  background-color: #409eff; /* 原主题色 */
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.label {
  color: #e8f3ff;
}

.emp-no {
  cursor: pointer;
  user-select: none;
}

.emp-input {
  width: 130px;
}
</style>
