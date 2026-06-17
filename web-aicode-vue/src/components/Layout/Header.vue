<!-- src/components/Layout/Header.vue -->
<template>
  <div class="header-wrapper">
    <div class="announcement-bar">
      仅限内部员工使用，请勿外传
    </div>

    <div class="header-container">
      <div class="title">
        <div class="marquee-wrap">
          <div class="marquee-track">
            <span class="marquee-text"><span class="highlight">温馨提示：</span>点击右侧工号，可切换到自己工号，查看个人车位预约状态和预约记录  👉</span>
            <span class="marquee-text marquee-clone"><span class="highlight">温馨提示：</span>点击右侧工号，可切换到自己工号，查看个人车位预约状态和预约记录  👉</span>
          </div>
        </div>
      </div>
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
.header-wrapper {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.announcement-bar {
  background: linear-gradient(90deg, #2f8ff0 0%, #4aa8ff 100%);
  color: #eaf4ff;
  font-size: 13px;
  font-weight: 500;
  text-align: center;
  line-height: 28px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.15);
}

.header-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 22px;
  min-height: 48px;
  flex: 1;
  color: #fff;
  font-size: 15px;
  font-weight: 500;
  background: linear-gradient(90deg, #2f8ff0 0%, #4aa8ff 100%);
}

.title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.marquee-wrap {
  overflow: hidden;
}

.marquee-track {
  display: flex;
  width: fit-content;
  animation: marquee-scroll 30s linear infinite;
}

.marquee-text {
  display: inline-block;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 500;
  color: #e8f3ff;
  padding-right: 60px;
  flex-shrink: 0;
}

.marquee-clone {
  padding-right: 0;
}

.marquee-text .highlight {
  color: #ffeeba;
  font-weight: 700;
}

@keyframes marquee-scroll {
  0%   { transform: translateX(0); }
  100% { transform: translateX(-50%); }
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  flex-wrap: wrap;
  justify-content: flex-end;
  flex-shrink: 0;
}

.label {
  color: #e8f3ff;
  white-space: nowrap;
}

.emp-no {
  cursor: pointer;
  user-select: none;
  max-width: 100%;
}

.emp-input {
  width: 140px;
}
</style>
