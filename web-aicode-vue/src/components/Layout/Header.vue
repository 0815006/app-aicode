<!-- src/components/Layout/Header.vue -->
<template>
  <div class="header-wrapper"> <!-- ✅ 唯一根元素 -->
    <!-- 通告栏 -->
    <div class="announcement-bar">
      仅限自己使用，不对外开放
    </div>

    <!-- 原有头部 -->
    <div class="header-container">
      <div class="title"></div>
      <div class="user-info">
        <!-- 用户：admin -->
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Header'
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
  font-size: 14px;
}
</style>
