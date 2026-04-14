<!-- src/components/Layout/index.vue -->
<template>
  <div class="layout-wrapper">
    <!-- 左侧菜单 -->
    <div class="sidebar">
      <Sidebar />
    </div>

    <!-- 右侧主内容 -->
    <div class="main-container">
      <div class="header">
        <Header />
      </div>
      <div class="content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script>
import Sidebar from './Sidebar'
import Header from './Header'

export default {
  name: 'Layout',
  components: {
    Sidebar,
    Header
  }
}
</script>

<style scoped>
.layout-wrapper {
  display: flex;
  height: 100vh;
  width: 100%;
  /* ❌ 不要设置 gap */
}

.sidebar {
  width: 200px;
  background-color: #304156;
  color: #fff;
  height: 100%;
  overflow-y: auto;
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  /* ❌ 不要设置 margin-left */
}

.header {
  height: 60px;
  background-color: #1890ff;
  color: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0\.08);
}

.content {
  flex: 1;
  padding: 0;        /* ✅ 关键：去掉 padding */
  overflow: auto;
  background-color: #f5f7fa;
}
</style>