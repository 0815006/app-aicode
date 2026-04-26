<!-- src/components/Layout/Sidebar.vue -->
<template>
  <div class="sidebar-container">
    <div class="logo">
      <h3>赛博车间工具箱</h3>
      <p>Workspace Portal</p>
    </div>
    <el-menu
      :default-active="$route.path"
      background-color="transparent"
      text-color="#c7d8ee"
      active-text-color="#ffffff"
      router
    >
      <el-menu-item index="/resource-check">
        <i class="el-icon-document"></i>
        <span slot="title">资源核查</span>
      </el-menu-item>

      <el-menu-item index="/parking-list">
        <i class="el-icon-truck"></i>
        <span slot="title">车位预约</span>
      </el-menu-item>
    
      <!-- ✅ 新增：聊天大厅 -->
      <el-menu-item index="/chat">
        <i class="el-icon-chat-line-round"></i>
        <span slot="title">聊天大厅</span>
      </el-menu-item>

      <el-menu-item index="/data-migration">
        <i class="el-icon-data-line"></i>
        <span slot="title">数据迁移</span>
      </el-menu-item>

      <el-menu-item index="/vote">
        <i class="el-icon-star-off"></i>
        <span slot="title">投票功能</span>
      </el-menu-item>

      <el-menu-item index="/xmind-converter">
        <i class="el-icon-refresh"></i>
        <span slot="title">思维导图</span>
      </el-menu-item>

      <el-menu-item index="/performance">
        <i class="el-icon-s-opportunity"></i>
        <span slot="title">性能测试</span>
      </el-menu-item>

      <el-menu-item index="/text-comparison">
        <i class="el-icon-document"></i>
        <span slot="title">文本比对</span>
      </el-menu-item>

      <el-menu-item index="/media-crawl">
        <i class="el-icon-download"></i>
        <span slot="title">媒体抓取</span>
      </el-menu-item>

    </el-menu>
  </div>
</template>

<script>
export default {
  name: 'Sidebar'
}
</script>

<style scoped>
.sidebar-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.logo {
  padding: 18px 14px 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo h3 {
  margin: 0;
  text-align: center;
  color: #f2f7ff;
  font-size: 18px;
  font-weight: 700;
}

.logo p {
  margin: 6px 0 0;
  text-align: center;
  color: #9fb4cf;
  font-size: 12px;
}

.el-menu {
  flex: 1;
  padding: 10px 8px;
  border-right: none;
  overflow-y: auto;
}

.el-menu-item {
  border-radius: 10px;
  margin-bottom: 6px;
}

.el-menu-item:hover {
  background: rgba(94, 170, 255, 0.2) !important;
}

.el-menu-item.is-active {
  background: linear-gradient(90deg, #2f8ff0 0%, #4aa8ff 100%) !important;
  box-shadow: 0 6px 14px rgba(46, 145, 255, 0.32);
}
</style>
