<!-- src/components/Layout/Header.vue -->
<template>
  <div class="header-wrapper">
    <div class="announcement-bar">
      仅限内部员工使用，请勿外传
    </div>

    <div class="header-container">
      <div class="title">
        <div
          class="marquee-wrap"
          @mouseenter="marqueePaused = true"
          @mouseleave="marqueePaused = false"
        >
          <div class="marquee-track" :class="{ paused: marqueePaused }">
            <!-- 第一组 -->
            <span v-for="(tip, i) in tips" :key="i" class="marquee-text">
              <span class="highlight">温馨提示：</span>{{ tip }}
              <span class="marquee-sep">·</span>
            </span>
            <!-- 复制一组实现无缝循环 -->
            <span v-for="(tip, i) in tips" :key="'dup'+i" class="marquee-text">
              <span class="highlight">温馨提示：</span>{{ tip }}
              <span class="marquee-sep">·</span>
            </span>
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
      tips: [
        '点击右侧工号，可切换到自己工号，查看个人车位预约状态和预约记录',
        '新服 http://22.188.9.15:8082  旧服 27.79 已关闭',
        '邮箱密码填错或者邮箱容量超过80%，均会导致无法发送邮件'
      ],
      marqueePaused: false,
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

/* ========== 主头部 ========== */
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
  position: relative;
  width: 100%;
  height: 22px;
  overflow: hidden;
  -webkit-mask-image: linear-gradient(to right, transparent 0%, #000 5%, #000 95%, transparent 100%);
  mask-image: linear-gradient(to right, transparent 0%, #000 5%, #000 95%, transparent 100%);
}

.marquee-track {
  position: absolute;
  top: 0;
  left: 0;
  display: flex;
  width: max-content;
  animation: marquee-scroll 25s linear infinite;
  will-change: transform;
}

.marquee-track.paused {
  animation-play-state: paused;
}

.marquee-text {
  display: inline-block;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 500;
  color: #e8f3ff;
  padding-right: 50px;
  flex-shrink: 0;
}

.marquee-sep {
  margin-left: 50px;
  color: rgba(255, 255, 255, 0.3);
}

.marquee-text .highlight {
  color: #ffeeba;
  font-weight: 700;
}

@keyframes marquee-scroll {
  0%   { transform: translateX(0); }
  100% { transform: translateX(-50%); }
}

/* ========== 用户信息区 ========== */
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
