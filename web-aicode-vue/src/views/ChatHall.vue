<template>
  <div 
    ref="chatContainer"
    class="chat-hall-page" 
    :class="{ fullscreen: isFullscreen }"
  >
    <!-- 左右布局 -->
    <div class="chat-main-content">

      <!-- 左侧：在线用户列表 + 文件列表 -->
      <div class="sidebar-container">
        <!-- 上面：在线用户列表 -->
        <div class="online-users-sidebar">
          <h3>👥 在线人员</h3>
          <ul class="online-users-list">
            <li
              v-for="user in onlineUsers"
              :key="user"
            >
              <span class="online-dot">●</span>
              <span>{{ user }}</span>
            </li>
          </ul>
        </div>

        <!-- 下面：文件列表 -->
        <div class="file-list-sidebar">
          <div class="file-list-header">
            <h3>📁 文件共享</h3>
            <el-upload
              class="file-upload-btn"
              action="/api/chat/file/upload"
              :data="{ uploaderId: currentEmpNo, uploaderName: currentUser }"
              :show-file-list="false"
              :on-success="handleFileUploadSuccess"
              :on-error="handleFileUploadError"
              :before-upload="beforeFileUpload"
            >
              <el-button size="mini" type="primary" icon="el-icon-upload2" circle title="上传文件"></el-button>
            </el-upload>
          </div>
          <div class="file-list-content" v-loading="fileLoading">
            <div v-if="fileList.length === 0" class="no-files-tip">暂无文件</div>
            <ul class="file-items-list">
              <li v-for="file in fileList" :key="file.id" class="file-item">
                <div class="file-info">
                  <div class="file-name" :title="file.fileName">{{ file.fileName }}</div>
                  <div class="file-meta">
                    {{ file.uploaderName }} · {{ formatFileSize(file.fileSize) }}
                  </div>
                </div>
                <div class="file-actions">
                  <el-link type="primary" :underline="false" icon="el-icon-download" @click="handleFileDownload(file)"></el-link>
                  <el-link 
                    v-if="file.uploaderId === currentEmpNo" 
                    type="danger" 
                    :underline="false" 
                    icon="el-icon-delete" 
                    @click="handleFileDelete(file)"
                    style="margin-left: 8px;"
                  ></el-link>
                </div>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <!-- 右侧：原聊天内容 -->
      <div class="chat-content-area">
        <!-- 标题 + 全屏按钮 -->
        <div class="chat-header">
          <div>
            <h2>
              💬 聊天大厅
            </h2>
            <p class="chat-subtitle">
              欢迎来到聊天大厅，支持 <strong>Markdown</strong> 格式！
            </p>
          </div>
          <div
            @click="toggleFullscreen"
            :title="isFullscreen ? '退出全屏' : '全屏'"
            class="fullscreen-button"
          >
            {{ isFullscreen ? '✕' : '⛶' }}
          </div>
        </div>

        <!-- 连接状态 -->
        <div class="connection-status-bar">
          <span>状态：{{ wsConnected ? '🟢 已连接' : '🔴 未连接' }}</span>

          <!-- 切换按钮 -->
          <div class="display-mode-toggle">
            <button
              @click="displayMode = 'plain'"
              :class="{ active: displayMode === 'plain' }"
            >
              📄 纯文本
            </button>
            <button
              @click="displayMode = 'preview'"
              :class="{ active: displayMode === 'preview' }"
            >
              🔍 .md预览
            </button>
          </div>
        </div>

        <!-- 消息区 -->
        <div
          class="messages"
        >
          <!-- 加载历史消息按钮 -->
          <div class="load-history-container">
            <button @click="loadHistoryMessages" class="load-history-button">加载更多记录</button>
          </div>

          <div
            v-for="(msg) in messages"
            :key="msg.id"
            :class="['message-item', { 'message-self': msg.username === currentUser }]"
          >
            <div class="message-meta">
              {{ msg.username }} · {{ formatTime(msg.time) }}
            </div>
            <div
              class="markdown-content"
              v-html="renderMessageContent(msg)"
            />
          </div>

        <!-- 暂无消息提示 -->
        <div v-if="messages.length === 0" class="no-messages-tip">
          暂无消息
        </div>

        </div>

        <!-- 输入区：三列等高布局 -->
        <div class="input-area">

          <!-- 左侧：用户名输入 -->
          <div class="username-input-wrapper">
            <label class="username-label">
              当前用户名
            </label>
            <input
              v-model="currentUser"
              placeholder="名字"
              class="username-input"
            />
          </div>

          <!-- 中间：消息输入框 -->
          <div class="message-input-wrapper">
            <textarea
              v-model="inputMessage"
              placeholder="支持 Markdown"
              @keydown="handleKeydown"
              class="message-textarea"
            ></textarea>
          </div>

          <!-- 右侧：发送按钮（等高 + 居中） -->
          <div class="send-button-wrapper">
            <button
              @click="sendMessage"
              :disabled="!inputMessage.trim() || !wsConnected"
              class="send-button"
              title="按 Ctrl + Enter 发送"
            >
              <span class="send-button-text">发送</span>
              <span class="send-button-shortcut">Ctrl+Enter</span>
            </button>
          </div>
        </div>


        <!-- 语法提示 -->
        <div class="syntax-tip">
          支持语法：<code>**加粗**</code> <code>*斜体*</code> <code>[链接](url)</code> <code>`代码`</code> <code>```代码块```</code>
        </div>

      </div>
    </div>
  </div>
</template>

<script>
import marked from 'marked'
import hljs from 'highlight.js'
import DOMPurify from 'dompurify'
import he from 'he'
import { getFileList, deleteFile, getDownloadUrl } from '@/api/chat'
import { getCurrentEmpNo } from '@/utils/currentUser'

// 引入代码高亮样式
import 'highlight.js/styles/github.css'

// 自定义 Tokenizer
const renderer = new marked.Renderer()

// 重写代码块解析（fenced code blocks）
renderer.code = function (code, language) {
  const validLang = language && hljs.getLanguage(language) ? language : 'plaintext'
  let highlighted

  try {
    if (language && hljs.getLanguage(validLang)) {
      highlighted = hljs.highlight(code, { language: validLang }).value
    } else {
      highlighted = hljs.highlightAuto(code).value
    }
  } catch (err) {
    return `<pre><code>${marked.escape(code)}</code></pre>`
  }

  return `<pre><code class="hljs language-${language}">${highlighted}</code></pre>`
}

// 配置 marked
marked.setOptions({
  gfm: true,
  breaks: false,
  smartLists: true,
  smartypants: true,
  renderer: renderer,
  highlight: function (code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(code, { language: lang }).value
      } catch (err) {
        console.error('代码高亮失败', err)
      }
    }
    return hljs.highlightAuto(code).value
  }
})

export default {
  name: 'ChatHall',
  data() {
    return {
      messages: [],
      inputMessage: '',
      currentUser: localStorage.getItem('chatUsername') || '游客' + Math.floor(Math.random() * 1000),
      currentEmpNo: getCurrentEmpNo(),
      ws: null,
      wsConnected: false,
      isFullscreen: false,
      onlineUsers: [],
      displayMode: localStorage.getItem('chatDisplayMode') || 'plain',
      hasLoadedHistory: false,
      fileList: [],
      fileLoading: false
    }
  },
  created() {
    this.initWebSocket()
  },
  mounted() {
    document.addEventListener('fullscreenchange', this.handleFullscreenChange)
    document.addEventListener('webkitfullscreenchange', this.handleFullscreenChange)
    this.loadHistoryMessages()
    this.fetchFileList()
  },
  beforeDestroy() {
    document.removeEventListener('fullscreenchange', this.handleFullscreenChange)
    document.removeEventListener('webkitfullscreenchange', this.handleFullscreenChange)
    this.closeWebSocket()
  },
  watch: {
    displayMode(newMode) {
      localStorage.setItem('chatDisplayMode', newMode)
    },
    currentUser(newName, oldName) {
      if (this.wsConnected && newName && newName !== oldName) {
        const msg = {
          type: 'rename',
          oldName: oldName,
          newName: newName,
          time: Date.now()
        }
        this.ws.send(JSON.stringify(msg))
      }
    }
  },
  methods: {
    initWebSocket() {
      const wsUrl = `ws://localhost:8089/ws/chat?username=${encodeURIComponent(this.currentUser)}`
      this.ws = new WebSocket(wsUrl)

      this.ws.onopen = () => {
        console.log('WebSocket 连接成功')
        this.wsConnected = true
      }

      this.ws.onmessage = (event) => {
        const msg = JSON.parse(event.data)
        if (msg.type === 'rename') {
          this.messages.push({
            username: '📢 系统通知',
            htmlContent: `<i style="color:#999">${msg.oldName} 改名为 ${msg.newName}</i>`,
            time: msg.time
          })
        } else if (msg.type === 'onlineUsers') {
          this.onlineUsers = [...new Set(msg.users)]
        } else {
          msg.id = msg.id || Date.now() + Math.random()
          this.messages.push(msg)
          this.$nextTick(() => {
            const container = this.$el.querySelector('.messages')
            if (container) container.scrollTop = container.scrollHeight
          })
        }
      }

      this.ws.onclose = () => {
        this.wsConnected = false
        setTimeout(this.initWebSocket, 3000)
      }

      this.ws.onerror = (error) => {
        console.error('WebSocket 错误', error)
      }
    },

    loadHistoryMessages() {
      if (this.hasLoadedHistory) return
      const timestamp = this.messages.length > 0 ? this.messages[0].time : Date.now();
      fetch(`/api/chat/history?timestamp=${timestamp}&limit=50`)
        .then(response => response.json())
        .then(data => {
          this.hasLoadedHistory = true
          const historyMessages = Array.isArray(data) ? data : (data && data.data ? data.data : []);
          const newMessages = historyMessages.filter(msg => !this.messages.some(exist => exist.id === msg.id));
          for (const msg of newMessages) {
            try {
              const parsedMessage = JSON.parse(msg.message);
              msg.id = parsedMessage.id;
              msg.username = parsedMessage.username;
              msg.content = parsedMessage.content;
              msg.htmlContent = parsedMessage.htmlContent;
              msg.time = parsedMessage.time;
            } catch (e) {
              msg.username = '未知用户';
              msg.content = '解析失败';
              msg.htmlContent = '<p>解析失败</p>';
              msg.time = Date.now();
            }
          }
          this.messages.push(...newMessages.reverse());
          this.$nextTick(() => {
            const container = this.$el.querySelector('.messages');
            if (container) container.scrollTop = container.scrollHeight;
          });
        })
        .catch(error => {
          console.error('加载历史消息失败:', error);
        });
    },

    renderMessageContent(msg) {
      if (this.displayMode === 'preview') {
        if (msg.htmlContent) {
          return DOMPurify.sanitize(msg.htmlContent);
        } else if (msg.content) {
          return DOMPurify.sanitize(marked.parse(msg.content));
        } else {
          return 'Invalid';
        }
      } else {
        return `<div class="plain-text-mode">${DOMPurify.sanitize(he.escape(msg.content || ''))}</div>`;
      }
    },

    async toggleFullscreen() {
      const container = this.$refs.chatContainer
      if (!this.isFullscreen) {
        if (container.requestFullscreen) {
          await container.requestFullscreen()
        } else if (container.webkitRequestFullscreen) {
          await container.webkitRequestFullscreen()
        }
        this.isFullscreen = true
      } else {
        if (document.exitFullscreen) {
          await document.exitFullscreen()
        } else if (document.webkitExitFullscreen) {
          await document.webkitExitFullscreen()
        }
        this.isFullscreen = false
      }
    },

    handleFullscreenChange() {
      if (!document.fullscreenElement && !document.webkitFullscreenElement) {
        this.isFullscreen = false
      }
    },

    handleKeydown(event) {
      if (event.key === 'Enter' && event.ctrlKey) {
        event.preventDefault()
        this.sendMessage()
      }
    },

    sendMessage() {
      const content = this.inputMessage.trim()
      if (!content || !this.wsConnected) return

      const html = marked.parse(content)
      const htmlContent = DOMPurify.sanitize(html, {
        USE_PROFILES: { html: true },
        ADD_TAGS: ['pre', 'code'],
        ADD_ATTR: ['class']
      })

      const msg = {
        id: Date.now() + Math.random(),
        username: this.currentUser,
        content,
        htmlContent,
        time: Date.now()
      }

      this.ws.send(JSON.stringify(msg))
      this.inputMessage = ''
      localStorage.setItem('chatUsername', this.currentUser)

      this.$nextTick(() => {
        const container = this.$el.querySelector('.messages')
        if (container) container.scrollTop = container.scrollHeight
      })
    },

    closeWebSocket() {
      if (this.ws) {
        this.ws.close()
      }
    },

    formatTime(time) {
      return new Date(time).toTimeString().substr(0, 8)
    },

    // 文件相关方法
    fetchFileList() {
      this.fileLoading = true
      getFileList().then(res => {
        if (res.code === 200) {
          this.fileList = res.data
        }
      }).finally(() => {
        this.fileLoading = false
      })
    },

    beforeFileUpload(file) {
      const isLt500M = file.size / 1024 / 1024 < 500
      if (!isLt500M) {
        this.$message.error('上传文件大小不能超过 500MB!')
      }
      return isLt500M
    },

    handleFileUploadSuccess(res) {
      if (res.code === 200) {
        this.$message.success('文件上传成功')
        this.fetchFileList()
      } else {
        this.$message.error(res.message || '文件上传失败')
      }
    },

    handleFileUploadError() {
      this.$message.error('文件上传失败')
    },

    handleFileDownload(file) {
      const url = getDownloadUrl(file.id)
      window.open(url)
    },

    handleFileDelete(file) {
      this.$confirm('确定要删除该文件吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        deleteFile(file.id, this.currentEmpNo).then(res => {
          if (res.code === 200) {
            this.$message.success('删除成功')
            this.fetchFileList()
          } else {
            this.$message.error(res.message || '删除失败')
          }
        })
      }).catch(() => {})
    },

    formatFileSize(size) {
      if (size === 0) return '0 B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
      const i = Math.floor(Math.log(size) / Math.log(k))
      return parseFloat((size / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    }
  }
}
</script>

<style scoped>
.chat-hall-page {
  padding: 20px;
  height: 100%;
  max-width: 100%;
  margin: 0 auto;
  transition: all 0.25s ease;
  background: linear-gradient(180deg, #f5f8ff 0%, #eef3fb 100%);
  box-sizing: border-box;
  overflow: hidden;
}

.chat-main-content {
  display: flex;
  gap: 18px;
  height: 100%;
}

.sidebar-container {
  width: 260px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  height: 100%;
}

.online-users-sidebar, .file-list-sidebar {
  padding: 16px 14px;
  border: 1px solid #d9e3f2;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 8px 20px rgba(16, 43, 98, 0.08);
  display: flex;
  flex-direction: column;
}

.online-users-sidebar {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

.file-list-sidebar {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.online-users-sidebar h3, .file-list-header h3 {
  margin: 0;
  font-size: 14px;
  color: #1f2d3d;
}

.file-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  flex-shrink: 0;
}

.online-users-list {
  list-style: none;
  padding: 0;
  margin: 14px 0 0 0;
  font-size: 14px;
  color: #51627a;
}

.online-users-list li {
  display: flex;
  align-items: center;
  padding: 8px 10px;
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  border-radius: 10px;
  background: #f5f9ff;
}

.online-dot {
  color: #2bc870;
  margin-right: 8px;
  font-size: 12px;
}

.file-list-content {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
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

.no-files-tip, .no-messages-tip {
  color: #92a1b7;
  text-align: center;
  padding: 20px;
  font-size: 13px;
}

.chat-content-area {
  flex: 1;
  min-width: 500px;
  min-height: 0;
  max-height: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  border: 1px solid #d9e3f2;
  border-radius: 16px;
  padding: 16px;
  background: #ffffff;
  box-shadow: 0 12px 24px rgba(16, 43, 98, 0.08);
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 14px;
}

.chat-header h2 {
  margin: 0;
  display: flex;
  align-items: center;
  font-size: 22px;
  color: #1f2d3d;
}

.chat-subtitle {
  color: #6d7a90;
  margin: 8px 0 0 0;
  font-size: 13px;
}

.fullscreen-button {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #4aa8ff, #3588ff);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 16px;
  box-shadow: 0 8px 16px rgba(64, 158, 255, 0.32);
  transition: all 0.2s ease;
}

.fullscreen-button:hover {
  transform: translateY(-1px);
}

.connection-status-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  font-size: 12px;
  color: #607089;
  padding: 10px 12px;
  background: #f5f8ff;
  border: 1px solid #dfebff;
  border-radius: 10px;
}

.display-mode-toggle {
  display: flex;
  gap: 8px;
  font-size: 12px;
}

.display-mode-toggle button {
  padding: 5px 10px;
  border: 1px solid #d6e0ef;
  background: #ffffff;
  color: #4f6078;
  border-radius: 8px;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.18s ease;
}

.display-mode-toggle button:hover {
  border-color: #98c2ff;
  background: #f3f8ff;
}

.display-mode-toggle button.active {
  border: 1px solid #409eff;
  background: #eaf4ff;
  color: #2f79d9;
}

.messages {
  flex: 1;
  min-height: 260px;
  overflow-y: auto;
  border: 1px solid #d9e3f2;
  border-radius: 12px;
  padding: 14px;
  background: linear-gradient(180deg, #f9fbff 0%, #f4f8ff 100%);
  margin-bottom: 14px;
}

.messages::-webkit-scrollbar, 
.file-list-content::-webkit-scrollbar,
.online-users-sidebar::-webkit-scrollbar {
  width: 6px;
}

.messages::-webkit-scrollbar-thumb, 
.file-list-content::-webkit-scrollbar-thumb,
.online-users-sidebar::-webkit-scrollbar-thumb {
  background: #c8d8f0;
  border-radius: 8px;
}

.load-history-container {
  text-align: center;
  margin-bottom: 12px;
}

.load-history-button {
  border: none;
  padding: 7px 14px;
  border-radius: 999px;
  font-size: 12px;
  cursor: pointer;
  color: #ffffff;
  background: linear-gradient(135deg, #4aa8ff, #3588ff);
  box-shadow: 0 6px 12px rgba(64, 158, 255, 0.25);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.load-history-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 16px rgba(64, 158, 255, 0.3);
}

.message-item {
  margin-bottom: 14px;
  text-align: left;
}

.message-item.message-self {
  text-align: right;
}

.message-meta {
  font-size: 12px;
  color: #7a879a;
  margin-bottom: 5px;
}

.markdown-content {
  display: inline-block;
  padding: 10px 14px;
  border-radius: 14px 14px 14px 6px;
  background-color: #ffffff;
  border: 1px solid #e4ebf7;
  box-shadow: 0 4px 12px rgba(28, 53, 96, 0.06);
  color: #1f2d3d;
  max-width: 80%;
  word-wrap: break-word;
  line-height: 1.6;
}

.message-self .markdown-content {
  background: linear-gradient(135deg, #53acff, #3588ff);
  color: #ffffff;
  border: none;
  border-radius: 14px 14px 6px 14px;
  box-shadow: 0 6px 14px rgba(53, 136, 255, 0.28);
}

.input-area {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: stretch;
  margin-bottom: 10px;
}

.username-input-wrapper {
  display: flex;
  flex-direction: column;
  min-width: 140px;
  justify-content: space-between;
}

.username-label {
  font-size: 12px;
  color: #6d7a90;
}

.username-input {
  padding: 9px 10px;
  border: 1px solid #d6e0ef;
  border-radius: 8px;
  font-size: 14px;
  margin-top: 4px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.username-input:focus,
.message-textarea:focus {
  outline: none;
  border-color: #75b5ff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.14);
}

.message-input-wrapper {
  flex: 1;
  min-width: 220px;
  display: flex;
  flex-direction: column;
}

.message-textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid #d6e0ef;
  border-radius: 10px;
  height: 86px;
  resize: vertical;
  line-height: 1.45;
  font-size: 14px;
  box-sizing: border-box;
  flex: 1;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.send-button-wrapper {
  display: flex;
  flex-direction: column;
  min-width: 90px;
  justify-content: center;
  align-items: center;
}

.send-button {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  width: 72px;
  height: 72px;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  font-size: 14px;
  padding: 0;
  color: #ffffff;
  background: linear-gradient(135deg, #4aa8ff, #3588ff);
  box-shadow: 0 10px 18px rgba(53, 136, 255, 0.28);
  transition: transform 0.2s ease, box-shadow 0.2s ease, opacity 0.2s ease;
}

.send-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 12px 22px rgba(53, 136, 255, 0.35);
}

.send-button:disabled {
  cursor: not-allowed;
  opacity: 0.58;
  box-shadow: none;
}

.send-button-text {
  font-weight: 700;
}

.send-button-shortcut {
  font-size: 10px;
  margin-top: 2px;
  opacity: 0.92;
}

.syntax-tip {
  margin-top: 0;
  margin-bottom: 0;
  font-size: 12px;
  color: #8b97ab;
  background: #f7faff;
  border: 1px dashed #d6e5fb;
  border-radius: 8px;
  padding: 8px 10px;
}

.chat-hall-page.fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw !important;
  height: 100vh !important;
  margin: 0 !important;
  padding: 12px !important;
  max-width: none !important;
  border-radius: 0 !important;
  background: #eef3fb;
  z-index: 9999;
  overflow: hidden;
}

.chat-hall-page.fullscreen .messages {
  min-height: 0;
  padding: 14px;
}

.chat-hall-page.fullscreen [title="退出全屏"] {
  background: linear-gradient(135deg, #f47171, #eb4f4f) !important;
}

.chat-hall-page.fullscreen .sidebar-container {
  width: 260px !important;
}

.markdown-content code {
  font-family: 'Courier New', monospace;
}

.markdown-content pre {
  background: #202734;
  color: #f8f8f2;
  padding: 12px;
  border-radius: 8px;
  overflow: auto;
  margin: 12px 0;
  font-size: 0.95em;
  line-height: 1.4;
}

.markdown-content pre code {
  background: none;
  color: inherit;
  padding: 0;
  font-size: inherit;
}

.markdown-content a {
  color: #2976dc;
  text-decoration: none;
}

.markdown-content a:hover {
  text-decoration: underline;
}

.markdown-content blockquote {
  border-left: 4px solid #409eff;
  margin: 10px 0;
  padding: 6px 10px;
  background: #edf6ff;
  color: #4c5e78;
  border-radius: 4px;
}

.plain-text-mode {
  font-family: 'Courier New', monospace;
  background: #f5f7fb;
  padding: 8px;
  border-radius: 6px;
  border: 1px dashed #c8d6eb;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-size: 14px;
  color: #4f6078;
}

@media (max-width: 960px) {
  .chat-main-content {
    flex-direction: column;
    height: auto;
  }

  .sidebar-container,
  .chat-hall-page.fullscreen .sidebar-container {
    width: 100% !important;
    max-height: 400px;
  }

  .chat-content-area {
    min-width: 0;
  }
}
</style>
