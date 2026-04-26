<!-- src/views/MediaCrawl.vue -->
<template>
  <div class="media-crawl-container">
    <!-- 顶部：标题区 -->
    <div class="header-section">
      <h1 class="page-title">🌐 网页媒体采集系统</h1>
      <p class="page-subtitle">Web Media Collector — 自动抓取网页中的图片与视频资源</p>
    </div>

    <!-- 顶部：添加网址输入区 -->
    <div class="input-section">
      <el-form :inline="true" :model="form" class="crawl-form">
        <el-form-item label="目标网址" class="form-item-url">
          <el-input
            v-model="form.url"
            placeholder="请输入需要采集的网页 URL，例如 https://example.com"
            clearable
            class="url-input"
            style="width: 1000px"
          />
        </el-form-item>
        <el-form-item label="采集类型">
          <el-select v-model="form.crawlType" style="width: 140px">
            <el-option label="仅图片" value="IMAGE" />
            <el-option label="仅视频" value="VIDEO" />
            <el-option label="图片+视频" value="BOTH" />
          </el-select>
        </el-form-item>
        <el-form-item label="最小体积(KB)">
          <el-input-number
            v-model="form.minSizeLimit"
            :min="0"
            :max="99999"
            :step="10"
            style="width: 140px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-plus" @click="handleAddTask" :loading="adding">
            添加任务
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 下方分栏：左右交互区 -->
    <div class="main-section">
      <!-- 左侧：任务列表区 -->
      <div class="left-panel">
        <div class="panel-header">
          <span class="panel-title">📋 任务列表</span>
          <el-button
            size="small"
            icon="el-icon-refresh"
            circle
            @click="handleRefresh"
            :loading="loading"
            title="刷新列表"
          />
        </div>
        <div class="task-list-wrapper">
          <el-table
            :data="taskList"
            style="width: 100%"
            stripe
            highlight-current-row
            @row-click="handleRowClick"
            size="small"
            height="100%"
          >
            <el-table-column prop="createTime" label="添加日期" width="100">
              <template slot-scope="{ row }">
                {{ formatDate(row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90">
              <template slot-scope="{ row }">
                <el-tag :type="statusType(row.status)" size="small" effect="plain">
                  {{ statusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="网页标题" min-width="120" show-overflow-tooltip />
            <el-table-column prop="url" label="网址" min-width="160" show-overflow-tooltip />
            <el-table-column prop="imgCount" label="图片数" width="70" align="center" />
            <el-table-column prop="imgTotalSize" label="图片大小" width="90" align="center">
              <template slot-scope="{ row }">
                {{ formatSize(row.imgTotalSize) }}
              </template>
            </el-table-column>
            <el-table-column prop="videoCount" label="视频数" width="70" align="center" />
            <el-table-column prop="videoTotalSize" label="视频大小" width="90" align="center">
              <template slot-scope="{ row }">
                {{ formatSize(row.videoTotalSize) }}
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div class="pagination-wrapper">
          <el-pagination
            small
            layout="total, prev, pager, next"
            :total="total"
            :page-size="pageSize"
            :current-page.sync="currentPage"
            @current-change="handlePageChange"
          />
        </div>
      </div>

      <!-- 右侧：内容展示区 -->
      <div class="right-panel">
        <template v-if="selectedTask">
          <div class="panel-header">
            <span class="panel-title">🖼️ 媒体资源预览</span>
            <div class="panel-actions">
              <span class="path-label">本地路径：</span>
              <span class="path-value">{{ mediaInfo.imgDirPath || mediaInfo.vidDirPath }}</span>
              <el-button-group style="margin-left: 12px">
                <el-button
                  size="small"
                  :type="displayMode === 'thumbnail' ? 'primary' : 'default'"
                  icon="el-icon-picture"
                  @click="displayMode = 'thumbnail'"
                >
                  缩略图
                </el-button>
                <el-button
                  size="small"
                  :type="displayMode === 'full' ? 'primary' : 'default'"
                  icon="el-icon-full-screen"
                  @click="displayMode = 'full'"
                >
                  完整展示
                </el-button>
              </el-button-group>
            </div>
          </div>

          <div class="media-content" v-loading="mediaLoading">
            <!-- 图片展示区 -->
            <template v-if="mediaInfo.imgFiles && mediaInfo.imgFiles.length > 0">
              <div class="media-section">
                <h4 class="media-type-title">📷 图片 ({{ mediaInfo.imgFiles.length }} 张)</h4>
                <div :class="['media-grid', displayMode === 'full' ? 'full-mode' : 'thumbnail-mode']">
                  <div
                    v-for="(file, index) in mediaInfo.imgFiles"
                    :key="'img-' + index"
                    class="media-item"
                  >
                    <el-image
                      :src="getImgUrl(file)"
                      :preview-src-list="getImgPreviewList()"
                      :initial-index="index"
                      fit="cover"
                      class="media-image"
                    />
                    <p class="media-filename">{{ file }}</p>
                  </div>
                </div>
              </div>
            </template>

            <!-- 视频展示区 -->
            <template v-if="mediaInfo.vidFiles && mediaInfo.vidFiles.length > 0">
              <div class="media-section">
                <h4 class="media-type-title">🎬 视频 ({{ mediaInfo.vidFiles.length }} 个)</h4>
                <div :class="['media-grid', displayMode === 'full' ? 'full-mode' : 'thumbnail-mode']">
                  <div
                    v-for="(file, index) in mediaInfo.vidFiles"
                    :key="'vid-' + index"
                    class="media-item"
                  >
                    <video
                      :src="getVidUrl(file)"
                      controls
                      :class="['media-video', displayMode === 'full' ? 'full-video' : 'thumbnail-video']"
                    />
                    <p class="media-filename">{{ file }}</p>
                  </div>
                </div>
              </div>
            </template>

            <!-- 无媒体文件 -->
            <template v-if="(!mediaInfo.imgFiles || mediaInfo.imgFiles.length === 0) && (!mediaInfo.vidFiles || mediaInfo.vidFiles.length === 0)">
              <el-empty description="暂无媒体资源" />
            </template>
          </div>
        </template>

        <!-- 未选择任务 -->
        <template v-if="!selectedTask">
          <el-empty description="请从左侧列表中选择一个任务查看媒体资源" />
        </template>
      </div>
    </div>
  </div>
</template>

<script>
import {
  addCrawlTask,
  listCrawlTasks,
  getTaskMediaFiles
} from '@/api/media-crawl'

export default {
  name: 'MediaCrawl',
  data() {
    return {
      // 表单
      form: {
        url: '',
        crawlType: 'IMAGE',
        minSizeLimit: 0
      },
      adding: false,

      // 列表
      taskList: [],
      loading: false,
      currentPage: 1,
      pageSize: 20,
      total: 0,

      // 选中任务
      selectedTask: null,
      mediaInfo: {},
      mediaLoading: false,
      displayMode: 'thumbnail'
    }
  },
  mounted() {
    this.fetchTaskList()
  },
  methods: {
    // 获取任务列表
    async fetchTaskList() {
      this.loading = true
      try {
        const res = await listCrawlTasks(this.currentPage, this.pageSize)
        if (res.code === 200) {
          // MyPage 继承 ArrayList，data 本身是数组，同时包含 totalElements 等分页字段
          this.taskList = res.data.content || []
          this.total = res.data.totalElements || 0
        }
      } catch (e) {
        console.error('获取任务列表失败', e)
        this.$message.error('获取任务列表失败')
      } finally {
        this.loading = false
      }
    },

    // 添加任务
    async handleAddTask() {
      if (!this.form.url) {
        this.$message.warning('请输入目标网址')
        return
      }
      // 简单 URL 校验
      if (!/^https?:\/\/.+/.test(this.form.url)) {
        this.$message.warning('请输入有效的网址（以 http:// 或 https:// 开头）')
        return
      }
      this.adding = true
      try {
        const res = await addCrawlTask(
          this.form.url,
          this.form.crawlType,
          this.form.minSizeLimit
        )
        if (res.code === 200) {
          this.$message.success('任务添加成功，系统将自动开始抓取')
          this.form.url = ''
          this.fetchTaskList()
        }
      } catch (e) {
        console.error('添加任务失败', e)
        this.$message.error('添加任务失败')
      } finally {
        this.adding = false
      }
    },

    // 刷新列表
    handleRefresh() {
      this.fetchTaskList()
    },

    // 分页切换
    handlePageChange(page) {
      this.currentPage = page
      this.fetchTaskList()
    },

    // 点击行
    async handleRowClick(row) {
      this.selectedTask = row
      this.mediaLoading = true
      this.mediaInfo = {}
      try {
        const res = await getTaskMediaFiles(row.id)
        if (res.code === 200) {
          this.mediaInfo = res.data
        }
      } catch (e) {
        console.error('获取媒体文件列表失败', e)
        this.$message.error('获取媒体文件列表失败')
      } finally {
        this.mediaLoading = false
      }
    },

    // 获取图片访问 URL
    getImgUrl(fileName) {
      if (!this.mediaInfo.folderName) return ''
      return `/api/images/img/${this.mediaInfo.folderName}/${fileName}`
    },

    // 获取视频访问 URL
    getVidUrl(fileName) {
      if (!this.mediaInfo.folderName) return ''
      return `/api/images/vid/${this.mediaInfo.folderName}/${fileName}`
    },

    // 获取图片预览列表（用于 el-image 预览）
    getImgPreviewList() {
      if (!this.mediaInfo.imgFiles || !this.mediaInfo.folderName) return []
      return this.mediaInfo.imgFiles.map(f => this.getImgUrl(f))
    },

    // 格式化日期
    formatDate(dateStr) {
      if (!dateStr) return '-'
      const d = new Date(dateStr)
      const month = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      const hour = String(d.getHours()).padStart(2, '0')
      const min = String(d.getMinutes()).padStart(2, '0')
      return `${month}-${day} ${hour}:${min}`
    },

    // 格式化文件大小
    formatSize(bytes) {
      if (!bytes || bytes === 0) return '-'
      const units = ['B', 'KB', 'MB', 'GB']
      let idx = 0
      let size = bytes
      while (size >= 1024 && idx < units.length - 1) {
        size /= 1024
        idx++
      }
      return size.toFixed(1) + units[idx]
    },

    // 状态标签
    statusLabel(status) {
      const map = {
        'PENDING': '未处理',
        'PROCESSING': '抓取中',
        'SUCCESS': '已提取',
        'FAILED': '无法访问'
      }
      return map[status] || status
    },

    // 状态类型
    statusType(status) {
      const map = {
        'PENDING': 'info',
        'PROCESSING': 'warning',
        'SUCCESS': 'success',
        'FAILED': 'danger'
      }
      return map[status] || 'info'
    }
  }
}
</script>

<style scoped>
.media-crawl-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px 20px;
  box-sizing: border-box;
  overflow: hidden;
  background: #f3f7ff;
}

/* 顶部标题区 */
.header-section {
  text-align: center;
  padding: 8px 0 12px;
  flex-shrink: 0;
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #1f3555;
  letter-spacing: 2px;
}

.page-subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: #7a8ba8;
}

/* 输入区 */
.input-section {
  flex-shrink: 0;
  background: #fff;
  border-radius: 10px;
  padding: 12px 20px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.crawl-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.form-item-url {
  flex: 1;
  min-width: 300px;
}

.url-input {
  width: 100%;
}

/* 主区域 */
.main-section {
  flex: 1;
  display: flex;
  gap: 12px;
  min-height: 0;
  overflow: hidden;
}

/* 左侧面板 */
.left-panel {
  width: 45%;
  min-width: 420px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border-bottom: 1px solid #ebeef5;
  flex-shrink: 0;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f3555;
}

.panel-actions {
  display: flex;
  align-items: center;
}

.path-label {
  font-size: 12px;
  color: #7a8ba8;
}

.path-value {
  font-size: 12px;
  color: #409eff;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-list-wrapper {
  flex: 1;
  overflow: auto;
  min-height: 0;
}

.pagination-wrapper {
  padding: 8px 16px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid #ebeef5;
  flex-shrink: 0;
}

/* 右侧面板 */
.right-panel {
  flex: 1;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.media-content {
  flex: 1;
  overflow: auto;
  padding: 12px 16px;
  min-height: 0;
}

.media-section {
  margin-bottom: 20px;
}

.media-type-title {
  margin: 0 0 10px;
  font-size: 14px;
  color: #303133;
  font-weight: 600;
}

/* 媒体网格 */
.media-grid {
  display: grid;
  gap: 10px;
}

.thumbnail-mode {
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
}

.full-mode {
  grid-template-columns: 1fr;
}

.media-item {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  overflow: hidden;
  background: #fafafa;
  transition: box-shadow 0.2s;
}

.media-item:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.media-image {
  width: 100%;
  height: 140px;
  display: block;
  object-fit: cover;
}

.full-mode .media-image {
  height: auto;
  max-height: 600px;
  object-fit: contain;
}

.media-video {
  width: 100%;
  display: block;
  background: #000;
}

.thumbnail-video {
  height: 140px;
}

.full-video {
  max-height: 600px;
}

.media-filename {
  margin: 0;
  padding: 4px 8px;
  font-size: 11px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
