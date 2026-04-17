<!-- src/components/Layout/Sidebar.vue -->
<template>
  <div class="sidebar-container">
    <div class="logo">
      <h3 style="color: #fff; text-align: center; padding: 15px 0">赛博车间工具箱</h3>
    </div>
    <el-menu
      :default-active="$route.path"
      background-color="#304156"
      text-color="#bfcbd9"
      active-text-color="#409EFF"
      router
    >
     <el-menu-item index="/upload-resource">
       <i class="el-icon-upload"></i>
       <span slot="title">上传资源清单</span>
     </el-menu-item>

      <el-menu-item index="/resource-check">
        <i class="el-icon-document"></i>
        <span slot="title">资源核查</span>
      </el-menu-item>

    <el-menu-item index="/parking-list">
    <i class="el-icon-truck"></i>
    <span slot="title">车位预约查询</span>
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
        <span slot="title">投票管理</span>
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
}

.el-menu {
  border-right: none;
}
</style>
