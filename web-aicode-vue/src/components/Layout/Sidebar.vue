<!-- src/components/Layout/Sidebar.vue -->
<template>
  <div class="sidebar-container" :class="{ 'sidebar-collapsed': collapsed }">
    <div class="logo" :class="{ 'logo-collapsed': collapsed }">
      <div class="logo-title">
        <img src="/toolbox-favicon.svg" class="logo-icon" alt="toolbox" />
        <h3 v-show="!collapsed">赛博车间</h3>
      </div>
      <p v-show="!collapsed">Workspace Portal</p>
      <div v-show="!collapsed" class="miniapp-float" title="微信小程序" @click="showMiniapp = true">
        <svg class="miniapp-icon" viewBox="0 0 24 24" width="22" height="22">
          <circle cx="12" cy="12" r="11" fill="none" stroke="currentColor" stroke-width="1.5"/>
          <text x="12" y="17" text-anchor="middle" font-size="14" font-weight="700" fill="currentColor" font-style="italic">S</text>
        </svg>
      </div>
    </div>
    <el-menu
      :default-active="$route.path"
      background-color="transparent"
      text-color="#c7d8ee"
      active-text-color="#ffffff"
      router
      :collapse="collapsed"
    >
      <el-menu-item index="/resource-check">
        <i class="el-icon-document"></i>
        <span slot="title">资源核查</span>
      </el-menu-item>

      <el-menu-item index="/parking-list">
        <i class="el-icon-truck"></i>
        <span slot="title">车位预约</span>
      </el-menu-item>

      <!-- ✅ 新增：停车大屏 -->
      <el-menu-item index="/parking-screen">
        <i class="el-icon-monitor"></i>
        <span slot="title">停车大屏</span>
      </el-menu-item>

          <!-- ✅ 新增：文件共享 -->
      <el-menu-item index="/file-share">
        <i class="el-icon-folder-opened"></i>
        <span slot="title">文件共享</span>
      </el-menu-item>

      <!-- ✅ 新增：聊天大厅 -->
      <el-menu-item index="/chat">
        <i class="el-icon-chat-line-round"></i>
        <span slot="title">聊天大厅</span>
      </el-menu-item>

      <!-- ✅ 新增：聊天房间 -->
      <el-menu-item index="/chat-room">
        <i class="el-icon-s-home"></i>
        <span slot="title">聊天房间</span>
      </el-menu-item>

      <el-menu-item index="/data-migration">
        <i class="el-icon-data-line"></i>
        <span slot="title">数据迁移</span>
      </el-menu-item>

      <!-- ✅ 投票功能保留，暂不开启 -->
      <!-- <el-menu-item index="/vote">
        <i class="el-icon-star-off"></i>
        <span slot="title">投票功能</span>
      </el-menu-item> -->

      <el-menu-item index="/xmind-converter">
        <i class="el-icon-refresh"></i>
        <span slot="title">思维导图</span>
      </el-menu-item>

      <el-menu-item index="/text-comparison">
        <i class="el-icon-document"></i>
        <span slot="title">文本比对</span>
      </el-menu-item>

      <el-menu-item index="/text-extract">
        <i class="el-icon-reading"></i>
        <span slot="title">文本提取</span>
      </el-menu-item>

      <!-- ✅ 媒体抓取功能保留，暂不开启 -->
      <!-- <el-menu-item index="/media-crawl">
        <i class="el-icon-download"></i>
        <span slot="title">媒体抓取</span>
      </el-menu-item> -->

      <el-menu-item index="/performance">
        <i class="el-icon-s-opportunity"></i>
        <span slot="title">性能测试</span>
      </el-menu-item>

      <!-- ✅ 新增：造数引擎 -->
      <el-menu-item index="/meta-gen">
        <i class="el-icon-s-marketing"></i>
        <span slot="title">造数引擎</span>
      </el-menu-item>

      <el-menu-item index="/format-json">
        <i class="el-icon-s-data"></i>
        <span slot="title">格式化JSON</span>
      </el-menu-item>

      <el-menu-item index="/format-xml">
        <i class="el-icon-s-order"></i>
        <span slot="title">格式化XML</span>
      </el-menu-item>

      <el-menu-item index="/format-html">
        <i class="el-icon-s-grid"></i>
        <span slot="title">格式化HTML</span>
      </el-menu-item>

      <el-menu-item index="/format-sql">
        <i class="el-icon-s-tools"></i>
        <span slot="title">格式化SQL</span>
      </el-menu-item>

      <el-menu-item index="/encode-decode">
        <i class="el-icon-refresh"></i>
        <span slot="title">编码解码</span>
      </el-menu-item>

      <el-menu-item index="/encrypt-tool">
        <i class="el-icon-key"></i>
        <span slot="title">加密解密</span>
      </el-menu-item>

    </el-menu>

    <!-- 折叠/展开切换按钮 -->
    <div class="collapse-toggle" @click="toggleCollapse" :title="collapsed ? '展开菜单' : '折叠菜单'">
      <svg viewBox="0 0 24 24" width="18" height="18" class="toggle-arrow" :class="{ 'arrow-collapsed': collapsed }">
        <path d="M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z" fill="currentColor"/>
      </svg>
    </div>

    <!-- 小程序码弹窗 -->
    <el-dialog
      :visible.sync="showMiniapp"
      title="微信小程序"
      width="360px"
      :close-on-click-modal="true"
      center
    >
      <div class="miniapp-dialog-body">
        <img
          src="/miniapp.png"
          alt="食堂菜单小程序码"
          class="miniapp-qr-img"
        />
        <p class="miniapp-tip">微信扫一扫，查看今日食堂菜单</p>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'Sidebar',
  data() {
    return {
      showMiniapp: false,
      collapsed: false
    }
  },
  methods: {
    toggleCollapse() {
      this.collapsed = !this.collapsed
      this.$emit('collapse-change', this.collapsed)
    }
  }
}
</script>

<style scoped>
.sidebar-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
}

/* 折叠态：logo 仅显示图标并居中 */
.logo-collapsed {
  padding: 14px 0 10px !important;
  display: flex;
  justify-content: center;
}

.logo-collapsed .logo-title {
  justify-content: center;
  gap: 0;
}

.logo-collapsed .logo-icon {
  margin: 0;
}

/* 折叠态：菜单图标居中 */
.sidebar-collapsed .el-menu {
  padding: 10px 0;
}

.sidebar-collapsed ::v-deep .el-menu-item {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 !important;
}

.sidebar-collapsed ::v-deep .el-menu-item i {
  margin-right: 0 !important;
}

.logo {
  padding: 18px 14px 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.logo-icon {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
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

/* ========== 小程序码悬浮入口 ========== */
.logo {
  position: relative;
}

.miniapp-float {
  position: absolute;
  right: 16px;
  bottom: 4px;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: transparent;
  border-radius: 6px;
  transition: background 0.25s, opacity 0.25s, transform 0.25s;
  z-index: 1;
}

.miniapp-float:hover {
  background: rgba(255, 255, 255, 0.12);
  transform: scale(1.15);
}

.miniapp-float:active {
  transform: scale(0.95);
}

.miniapp-icon {
  color: rgba(255, 255, 255, 0.45);
  transition: color 0.25s;
  display: block;
}

.miniapp-float:hover .miniapp-icon {
  color: rgba(255, 255, 255, 0.85);
}

/* ========== 弹窗样式 ========== */
.miniapp-dialog-body {
  text-align: center;
  padding: 10px 0;
}

.miniapp-qr-img {
  width: 200px;
  height: 200px;
  display: block;
  margin: 0 auto 16px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.miniapp-tip {
  margin: 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
}

/* ========== 折叠切换按钮 ========== */
.collapse-toggle {
  position: absolute;
  bottom: 10px;
  right: 10px;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: background 0.25s, border-color 0.25s, right 0.3s;
  z-index: 10;
}

.collapse-toggle:hover {
  background: rgba(94, 170, 255, 0.25);
  border-color: rgba(94, 170, 255, 0.45);
}

.toggle-arrow {
  color: rgba(255, 255, 255, 0.5);
  transition: color 0.25s, transform 0.3s;
}

.collapse-toggle:hover .toggle-arrow {
  color: rgba(255, 255, 255, 0.9);
}

/* 折叠态：箭头翻转 */
.arrow-collapsed {
  transform: rotate(180deg);
}

/* 折叠态：按钮居中 */
.sidebar-collapsed .collapse-toggle {
  right: 50%;
  transform: translateX(50%);
}
</style>
