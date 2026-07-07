<template>
  <div class="file-share-page" v-loading="loading">
    <!-- 顶部操作栏 -->
    <div class="page-header">
      <h2>📁 文件共享</h2>
      <div class="header-actions">
        <el-button v-if="isAdmin" type="primary" icon="el-icon-plus" size="small" @click="showCreateDirDialog">新建子目录</el-button>
      </div>
    </div>

    <!-- 子目录卡片区域，flex布局，高度占满整个空白屏幕 -->
    <div class="directory-cards-wrapper">
      <div v-if="directories.length === 0 && !loading" class="empty-hint">
        <i class="el-icon-folder-opened"></i>
        <p>暂无子目录，请点击"新建子目录"创建</p>
      </div>
      <div
        v-for="dir in directories"
        :key="dir.name"
        class="directory-card"
        :class="{ active: activeDir === dir.name }"
        @click="selectDir(dir.name)"
      >
        <div class="card-header">
          <div class="card-title">
            <i class="el-icon-folder"></i>
            <span class="dir-name">{{ dir.name }}</span>
            <span class="file-count">({{ dir.fileCount }} 个文件)</span>
          </div>
          <div class="card-header-actions">
            <el-button
              v-if="activeDir === dir.name"
              type="primary"
              size="mini"
              icon="el-icon-upload2"
              @click.stop="triggerUpload"
              circle
              title="上传文件"
            ></el-button>
            <el-button
              v-if="isAdmin"
              type="danger"
              size="mini"
              icon="el-icon-delete"
              circle
              @click.stop="confirmDeleteDir(dir.name)"
              title="删除目录"
            ></el-button>
          </div>
        </div>

        <!-- 隐藏的上传input -->
        <input
          type="file"
          :ref="'uploadInput_' + dir.name"
          style="display: none"
          @change="handleFileSelect($event, dir.name)"
        />

        <!-- 文件列表区域 -->
        <div class="file-list-wrapper" v-if="activeDir === dir.name">
          <div v-if="fileList.length === 0" class="no-files-tip">
            <span>暂无文件，点击上传按钮添加</span>
          </div>
          <ul v-else class="file-items-list">
            <li
              v-for="file in fileList"
              :key="file.id"
              class="file-item"
            >
              <div class="file-info">
                <div class="file-name" :title="file.fileName">{{ file.fileName }}</div>
                <div class="file-meta">
                  {{ file.uploaderId }} · {{ formatSize(file.fileSize) }}
                </div>
              </div>
              <div class="file-actions">
                <el-link type="primary" :underline="false" icon="el-icon-download" @click="downloadFile(file)"></el-link>
                <el-link
                  type="danger"
                  :underline="false"
                  icon="el-icon-delete"
                  @click="confirmDeleteFile(file)"
                  style="margin-left: 8px;"
                ></el-link>
              </div>
            </li>
          </ul>
        </div>
      </div>
    </div>

    <!-- 新建子目录对话框 -->
    <el-dialog
      title="新建子目录"
      :visible.sync="createDirDialogVisible"
      width="420px"
      :close-on-click-modal="false"
    >
      <el-form label-width="100px" @submit.native.prevent="doCreateDir">
        <el-form-item label="子目录名称" required>
          <el-input
            v-model="newDirName"
            placeholder="请输入子目录名称"
            maxlength="50"
            show-word-limit
          ></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="createDirDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="doCreateDir" :loading="creating">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  listDirectories,
  createDirectory,
  deleteDirectory,
  listFiles,
  deleteFile,
  getDownloadUrl
} from '@/api/file-share'
import { getCurrentEmpNo } from '@/utils/currentUser'

export default {
  name: 'FileShare',
  data() {
    return {
      loading: false,
      directories: [],
      activeDir: '',
      fileList: [],
      currentEmpNo: getCurrentEmpNo(),
      createDirDialogVisible: false,
      newDirName: '',
      creating: false
    }
  },
  computed: {
    isAdmin() {
      return this.currentEmpNo === '2036377'
    }
  },
  mounted() {
    this.loadDirectories()
  },
  methods: {
    async loadDirectories() {
      this.loading = true
      try {
        const res = await listDirectories()
        this.directories = res.data || []
        if (this.activeDir) {
          const exists = this.directories.find(d => d.name === this.activeDir)
          if (exists) {
            this.loadFileList(this.activeDir)
          } else {
            this.activeDir = ''
            this.fileList = []
          }
        }
      } catch (e) {
        this.$message.error('加载子目录失败: ' + e.message)
      } finally {
        this.loading = false
      }
    },

    selectDir(dirName) {
      if (this.activeDir === dirName) {
        return
      }
      this.activeDir = dirName
      this.loadFileList(dirName)
    },

    async loadFileList(dirName) {
      try {
        const res = await listFiles(dirName)
        this.fileList = res.data || []
      } catch (e) {
        this.$message.error('加载文件列表失败: ' + e.message)
      }
    },

    triggerUpload() {
      const refName = 'uploadInput_' + this.activeDir
      const input = this.$refs[refName]
      if (input) {
        const el = Array.isArray(input) ? input[0] : input
        el.value = ''
        el.click()
      }
    },

    async handleFileSelect(event, dirName) {
      const file = event.target.files[0]
      if (!file) return

      const uploaderId = getCurrentEmpNo()
      const formData = new FormData()
      formData.append('file', file)
      formData.append('subDirectoryName', dirName)
      formData.append('uploaderId', uploaderId)

      try {
        const axios = (await import('@/utils/request')).default
        await axios({
          url: '/file-share/upload',
          method: 'post',
          data: formData,
          headers: { 'Content-Type': 'multipart/form-data' }
        })
        this.$message.success('上传成功')
        this.loadFileList(dirName)
        this.loadDirectories()
      } catch (e) {
        this.$message.error('上传失败: ' + (e.message || '未知错误'))
      }
    },

    downloadFile(file) {
      const url = getDownloadUrl(file.id)
      window.open(url, '_blank')
      setTimeout(() => {
        this.loadFileList(this.activeDir)
      }, 1000)
    },

    async confirmDeleteFile(file) {
      try {
        await this.$confirm('确认删除文件「' + file.fileName + '」？', '提示', {
          type: 'warning'
        })
        await deleteFile(file.id, getCurrentEmpNo())
        this.$message.success('删除成功')
        this.loadFileList(this.activeDir)
        this.loadDirectories()
      } catch (e) {
        if (e !== 'cancel') {
          this.$message.error('删除失败: ' + (e.message || '未知错误'))
        }
      }
    },

    showCreateDirDialog() {
      this.newDirName = ''
      this.createDirDialogVisible = true
    },

    async doCreateDir() {
      const name = this.newDirName.trim()
      if (!name) {
        this.$message.warning('请输入子目录名称')
        return
      }
      this.creating = true
      try {
        await createDirectory(name)
        this.$message.success('创建成功')
        this.createDirDialogVisible = false
        await this.loadDirectories()
      } catch (e) {
        this.$message.error('创建失败: ' + (e.message || '未知错误'))
      } finally {
        this.creating = false
      }
    },

    async confirmDeleteDir(dirName) {
      try {
        await this.$confirm('确认删除子目录「' + dirName + '」及其所有文件？', '警告', {
          type: 'warning',
          confirmButtonText: '确认删除',
          confirmButtonClass: 'el-button--danger'
        })
        await deleteDirectory(dirName)
        this.$message.success('删除成功')
        if (this.activeDir === dirName) {
          this.activeDir = ''
          this.fileList = []
        }
        this.loadDirectories()
      } catch (e) {
        if (e !== 'cancel') {
          this.$message.error('删除失败: ' + (e.message || '未知错误'))
        }
      }
    },

    formatSize(bytes) {
      if (bytes == null) return '-'
      if (bytes < 1024) return bytes + ' B'
      if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
      if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
      return (bytes / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
    }
  }
}
</script>

<style scoped>
.file-share-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 20px;
  box-sizing: border-box;
  background: linear-gradient(180deg, #f9fbff 0%, #f4f8ff 100%);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-shrink: 0;
}

.page-header h2 {
  margin: 0;
  display: flex;
  align-items: center;
  font-size: 22px;
  color: #1f2d3d;
}

/* 卡片容器 — 填满整个屏幕剩余高度 */
.directory-cards-wrapper {
  flex: 1;
  display: flex;
  gap: 16px;
  overflow-x: auto;
  overflow-y: hidden;
  min-height: 0;
  padding-bottom: 4px;
}

.empty-hint {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #92a1b7;
  font-size: 14px;
}

.empty-hint i {
  font-size: 56px;
  margin-bottom: 14px;
  color: #c8d8f0;
}

/* 卡片 — 参照聊天大厅文件共享面板样式 */
.directory-card {
  min-width: 320px;
  max-width: 400px;
  flex-shrink: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px 14px;
  border: 1px solid #d9e3f2;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 8px 20px rgba(16, 43, 98, 0.08);
  cursor: pointer;
  transition: box-shadow 0.2s, border-color 0.2s;
  box-sizing: border-box;
}

.directory-card:hover {
  box-shadow: 0 12px 24px rgba(16, 43, 98, 0.12);
}

.directory-card.active {
  border-color: #409eff;
  box-shadow: 0 8px 20px rgba(64, 158, 255, 0.18);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  margin-bottom: 14px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #1f2d3d;
  min-width: 0;
}

.card-title i {
  color: #409eff;
  font-size: 16px;
  flex-shrink: 0;
}

.dir-name {
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 600;
}

.file-count {
  color: #909399;
  font-weight: 400;
  font-size: 12px;
  white-space: nowrap;
}

.card-header-actions {
  display: flex;
  gap: 4px;
  align-items: center;
  flex-shrink: 0;
}

/* 文件列表 — 参照聊天大厅 file-list-content */
.file-list-wrapper {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

.file-list-wrapper::-webkit-scrollbar {
  width: 6px;
}

.file-list-wrapper::-webkit-scrollbar-thumb {
  background: #c8d8f0;
  border-radius: 8px;
}

.file-list-wrapper::-webkit-scrollbar-thumb:hover {
  background: #b0c4de;
}

.file-items-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.file-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  border-radius: 10px;
  background: #f8faff;
  margin-bottom: 8px;
  transition: background 0.2s;
}

.file-item:hover {
  background: #f0f5ff;
}

.file-info {
  flex: 1;
  min-width: 0;
  margin-right: 10px;
}

.file-name {
  font-size: 13px;
  color: #2c3e50;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-weight: 500;
}

.file-meta {
  font-size: 11px;
  color: #909399;
  margin-top: 4px;
}

.file-actions {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.no-files-tip {
  color: #92a1b7;
  text-align: center;
  padding: 20px;
  font-size: 13px;
}
</style>
