<!-- src/components/Layout/index.vue -->
<template>
  <div class="layout-wrapper">
    <aside class="sidebar">
      <Sidebar />
    </aside>

    <header class="header">
      <Header />
    </header>

    <div class="status-bar">
      <span class="status-item">系统时间：{{ currentTime }}</span>
      <span v-if="clientIp" class="status-item">登录IP：{{ clientIp }}</span>
    </div>

    <main class="content">
      <router-view />
    </main>
  </div>
</template>

<script>
import Sidebar from './Sidebar'
import Header from './Header'
import { getClientIp } from '@/api/system'

export default {
  name: 'Layout',
  components: {
    Sidebar,
    Header
  },
  data() {
    return {
      currentTime: '',
      clientIp: '',
      clockTimer: null
    }
  },
  mounted() {
    this.updateCurrentTime()
    this.clockTimer = setInterval(this.updateCurrentTime, 1000)
    this.fetchClientIp()
  },
  beforeDestroy() {
    if (this.clockTimer) {
      clearInterval(this.clockTimer)
      this.clockTimer = null
    }
  },
  methods: {
    updateCurrentTime() {
      const now = new Date()
      this.currentTime = now.toLocaleString()
    },
    async fetchClientIp() {
      try {
        const res = await getClientIp()
        if (res.code === 200) {
          this.clientIp = res.data || ''
        }
      } catch (e) {
        this.clientIp = ''
        console.error('获取IP失败', e)
      }
    }
  }
}
</script>

<style scoped>
.layout-wrapper {
  display: grid;
  grid-template-columns: 240px 1fr;
  grid-template-rows: auto 1fr 34px;
  height: 100dvh;
  width: 100%;
  background: #eef2f8;
  overflow: hidden;
}

.sidebar {
  grid-column: 1;
  grid-row: 1 / 4;
  background: linear-gradient(180deg, #1f3555 0%, #1a2d49 100%);
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  overflow: hidden;
}

.header {
  grid-column: 2;
  grid-row: 1;
  min-height: 76px;
  background: transparent;
  z-index: 2;
}

.status-bar {
  grid-column: 1 / 3;
  grid-row: 3;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 28px;
  height: 34px;
  padding: 0 18px;
  background: linear-gradient(90deg, #1e2f4f 0%, #23528c 50%, #1e2f4f 100%);
  color: #eaf4ff;
  font-size: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.status-item {
  white-space: nowrap;
}

.content {
  grid-column: 2;
  grid-row: 2;
  min-height: 0;
  overflow: hidden;
  background: #f3f7ff;
}
</style>