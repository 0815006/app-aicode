<template>
  <div
    ref="chatContainer"
    class="chat-room-page"
    :class="{ fullscreen: isFullscreen }"
  >
    <div class="chat-main-content">
      <!-- 左侧：用户信息 + 房间列表 -->
      <div class="sidebar-container">
        <!-- 用户信息区 -->
        <div class="user-info-sidebar">
          <h3>👤 当前用户</h3>
          <div class="user-info-row">
            <input
              v-model="currentUser"
              placeholder="你的名字"
              class="username-input"
            />
            <el-button
              size="mini"
              type="primary"
              icon="el-icon-refresh"
              circle
              title="刷新我的房间列表"
              @click="refreshMyRooms"
            ></el-button>
          </div>
        </div>

        <!-- 房间列表 -->
        <div class="room-list-sidebar">
          <div class="room-list-header">
            <h3>📋 房间列表（{{ roomList.length }}）</h3>
            <el-button
              size="mini"
              type="primary"
              icon="el-icon-plus"
              circle
              title="创建房间"
              @click="showCreateDialog = true"
            ></el-button>
          </div>
          <div class="room-list-content" v-loading="roomLoading">
            <div v-if="roomList.length === 0" class="no-rooms-tip">暂无房间</div>
            <div
              v-for="room in roomList"
              :key="room.id"
              class="room-item"
            >
              <div class="room-title-row">
                <span
                  class="room-expand-icon"
                  @click="toggleRoomExpand(room.id)"
                >{{ expandedRoomId === room.id ? '▼' : '▶' }}</span>
                <span
                  class="room-name"
                  :class="{ 'room-name-active': room.iAmIn }"
                  @click="clickRoom(room)"
                >{{ room.title }}</span>
                <span class="room-member-count">{{ room.memberCount || 0 }}人</span>
                <template v-if="room.iAmIn">
                  <el-button size="mini" type="warning" plain @click.stop="leaveRoom(room)">退出</el-button>
                </template>
                <template v-else>
                  <el-button size="mini" type="primary" plain @click.stop="joinRoom(room)">进入</el-button>
                </template>
              </div>
              <!-- 展开成员列表 -->
              <div v-if="expandedRoomId === room.id" class="room-members">
                <div
                  v-for="member in room.members"
                  :key="member"
                  class="room-member-item"
                >
                  <span class="online-dot">●</span>
                  <span>{{ member }}</span>
                </div>
                <div v-if="!room.members || room.members.length === 0" class="no-members-tip">暂无成员</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：聊天区域 -->
      <div class="chat-content-area">
        <!-- 标题 + 全屏按钮 -->
        <div class="chat-header">
          <div>
            <h2>
              💬 {{ currentRoomTitle || '聊天房间' }}
            </h2>
            <p class="chat-subtitle" v-if="currentRoomId">
              当前在房间中，支持 <strong>Markdown</strong> 格式！
            </p>
            <p class="chat-subtitle" v-else>
              请先进入一个房间开始聊天
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
          <span>状态：{{ wsConnected ? '🟢 已连接' : currentRoomId ? '🔴 未连接' : '⚪ 未进入房间' }}</span>
          <div class="display-mode-toggle">
            <button
              @click="displayMode = 'plain'"
              :class="{ active: displayMode === 'plain' }"
            >📄 纯文本</button>
            <button
              @click="displayMode = 'preview'"
              :class="{ active: displayMode === 'preview' }"
            >🔍 .md预览</button>
          </div>
        </div>

        <!-- 消息区 -->
        <div class="messages">
          <div class="load-history-container">
            <button @click="loadHistoryMessages" class="load-history-button">加载更多记录</button>
          </div>

          <div
            v-for="msg in messages"
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

          <div v-if="messages.length === 0" class="no-messages-tip">
            暂无消息
          </div>
        </div>

        <!-- 输入区 -->
        <div class="input-area">
          <div class="message-input-wrapper">
            <textarea
              v-model="inputMessage"
              placeholder="支持 Markdown（需要先进入房间）"
              @keydown="handleKeydown"
              :disabled="!currentRoomId"
              class="message-textarea"
            ></textarea>
          </div>
          <div class="send-button-wrapper">
            <button
              @click="sendMessage"
              :disabled="!inputMessage.trim() || !wsConnected || !currentRoomId"
              class="send-button"
              title="按 Ctrl + Enter 发送"
            >
              <span class="send-button-text">发送</span>
              <span class="send-button-shortcut">Ctrl+Enter</span>
            </button>
          </div>
        </div>

        <div class="syntax-tip">
          支持语法：<code>**加粗**</code> <code>*斜体*</code> <code>[链接](url)</code> <code>`代码`</code> <code>```代码块```</code>
        </div>
      </div>
    </div>

    <!-- 创建房间弹窗 -->
    <CreateRoomDialog
      :visible.sync="showCreateDialog"
      :creator="currentUser"
      @created="handleRoomCreated"
    />
  </div>
</template>

<script>
import marked from 'marked'
import hljs from 'highlight.js'
import DOMPurify from 'dompurify'
import he from 'he'
import CreateRoomDialog from '@/components/chat-room/CreateRoomDialog.vue'
import { getRoomList, joinRoom as apiJoinRoom, leaveRoom as apiLeaveRoom, getRoomHistory, getRoomMembers } from '@/api/chat-room'
import { WS_BASE_URL } from '@/config/websocket'

import 'highlight.js/styles/github.css'

const renderer = new marked.Renderer()
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
    return '<pre><code>' + marked.escape(code) + '</code></pre>'
  }
  return '<pre><code class="hljs language-' + language + '">' + highlighted + '</code></pre>'
}

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
      } catch (err) { /* ignore */ }
    }
    return hljs.highlightAuto(code).value
  }
})

export default {
  name: 'ChatRoom',
  components: { CreateRoomDialog },
  data() {
    return {
      messages: [],
      inputMessage: '',
      currentUser: localStorage.getItem('chatRoomUsername') || '游客' + Math.floor(Math.random() * 1000),
      ws: null,
      wsConnected: false,
      isFullscreen: false,
      displayMode: localStorage.getItem('chatRoomDisplayMode') || 'plain',
      hasLoadedHistory: false,

      // 房间相关
      roomList: [],
      roomLoading: false,
      currentRoomId: null,
      currentRoomTitle: '',
      expandedRoomId: null,
      showCreateDialog: false,
      roomOnlineMembers: []
    }
  },
  created() {
    this.fetchRoomList()
  },
  mounted() {
    document.addEventListener('fullscreenchange', this.handleFullscreenChange)
    document.addEventListener('webkitfullscreenchange', this.handleFullscreenChange)
  },
  beforeDestroy() {
    document.removeEventListener('fullscreenchange', this.handleFullscreenChange)
    document.removeEventListener('webkitfullscreenchange', this.handleFullscreenChange)
    this.closeWebSocket()
  },
  watch: {
    displayMode(newMode) {
      localStorage.setItem('chatRoomDisplayMode', newMode)
    },
    currentUser(newName, oldName) {
      if (newName && newName !== oldName) {
        localStorage.setItem('chatRoomUsername', newName)
        // 改名后通知房间 WebSocket
        if (this.wsConnected && this.currentRoomId) {
          const msg = {
            type: 'rename',
            oldName: oldName,
            newName: newName,
            time: Date.now()
          }
          this.ws.send(JSON.stringify(msg))
        }
        // 刷新房间列表以匹配新名字
        this.fetchRoomList()
      }
    }
  },
  methods: {
    // ===== 房间列表 =====
    fetchRoomList() {
      this.roomLoading = true
      getRoomList(this.currentUser).then(res => {
        if (res.code === 200) {
          this.roomList = res.data || []
        }
      }).finally(() => {
        this.roomLoading = false
      })
    },

    refreshMyRooms() {
      // 刷新按钮：重新拉房间列表+房间成员
      this.fetchRoomList()
      this.$message.success('已刷新房间列表')
      // 如果当前在房间中，重新获取成员
      if (this.currentRoomId) {
        getRoomMembers(this.currentRoomId).then(res => {
          if (res.code === 200) {
            if (this.wsConnected) {
              this.ws.send(JSON.stringify({ type: 'roomListUpdate' }))
            }
          }
        })
      }
    },

    handleRoomCreated(room) {
      this.fetchRoomList()
    },

    toggleRoomExpand(roomId) {
      this.expandedRoomId = this.expandedRoomId === roomId ? null : roomId
    },

    clickRoom(room) {
      if (this.currentRoomId === room.id) return
      this.switchRoom(room.id, room.title)
    },

    // ===== 加入/退出房间 =====
    joinRoom(room) {
      apiJoinRoom({ roomId: room.id, username: this.currentUser }).then(res => {
        if (res.code === 200) {
          this.$message.success('已加入房间：' + room.title)
          this.fetchRoomList()
          this.switchRoom(room.id, room.title)
        } else {
          this.$message.error(res.message || '加入失败')
        }
      })
    },

    leaveRoom(room) {
      this.$confirm('确定要退出房间「' + room.title + '」吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        apiLeaveRoom({ roomId: room.id, username: this.currentUser }).then(res => {
          if (res.code === 200) {
            this.$message.success(res.data || '已退出房间')
            if (this.currentRoomId === room.id) {
              this.closeWebSocket()
              this.currentRoomId = null
              this.currentRoomTitle = ''
              this.messages = []
              this.hasLoadedHistory = false
            }
            this.fetchRoomList()
          } else {
            this.$message.error(res.message || '退出失败')
          }
        })
      }).catch(() => {})
    },

    // ===== 切换房间 =====
    switchRoom(roomId, title) {
      if (this.currentRoomId === roomId && this.wsConnected) return

      this.closeWebSocket()
      this.currentRoomId = roomId
      this.currentRoomTitle = title
      this.messages = []
      this.hasLoadedHistory = false

      this.initWebSocket(roomId)
      this.$nextTick(() => {
        this.loadHistoryMessages()
      })
    },

    // ===== WebSocket =====
    initWebSocket(roomId) {
      const wsUrl = `${WS_BASE_URL}/ws/chat/room/${roomId}?username=${encodeURIComponent(this.currentUser)}`
      this.ws = new WebSocket(wsUrl)

      this.ws.onopen = () => {
        console.log('【房间WebSocket】连接成功, roomId:', roomId)
        this.wsConnected = true
      }

      this.ws.onmessage = (event) => {
        const msg = JSON.parse(event.data)
        if (msg.type === 'rename') {
          this.messages.push({
            username: '📢 系统通知',
            htmlContent: '<i style="color:#999">' + msg.oldName + ' 改名为 ' + msg.newName + '</i>',
            time: msg.time
          })
        } else if (msg.type === 'roomOnlineMembers') {
          this.roomOnlineMembers = msg.users || []
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
      }

      this.ws.onerror = (error) => {
        console.error('【房间WebSocket】错误', error)
      }
    },

    closeWebSocket() {
      if (this.ws) {
        this.ws.close()
        this.ws = null
        this.wsConnected = false
      }
    },

    // ===== 消息 =====
    loadHistoryMessages() {
      if (this.hasLoadedHistory || !this.currentRoomId) return
      const timestamp = this.messages.length > 0 ? this.messages[0].time : Date.now()
      getRoomHistory(this.currentRoomId, { timestamp, limit: 50 }).then(res => {
        this.hasLoadedHistory = true
        if (res.code === 200) {
          const historyMessages = Array.isArray(res.data) ? res.data : []
          const newMessages = historyMessages.filter(msg => !this.messages.some(exist => exist.id === msg.id))
          for (const msg of newMessages) {
            try {
              const parsed = JSON.parse(msg.message)
              msg.id = parsed.id
              msg.username = parsed.username
              msg.content = parsed.content
              msg.htmlContent = parsed.htmlContent
              msg.time = parsed.time
            } catch (e) {
              msg.username = msg.username || '未知用户'
              msg.content = msg.message || ''
              msg.htmlContent = '<p>解析失败</p>'
              msg.time = msg.timestamp || Date.now()
            }
          }
          this.messages = newMessages.reverse().concat(this.messages)
          this.$nextTick(() => {
            const container = this.$el.querySelector('.messages')
            if (container) container.scrollTop = container.scrollHeight
          })
        }
      }).catch(() => {
        console.error('加载历史消息失败')
      })
    },

    renderMessageContent(msg) {
      if (this.displayMode === 'preview') {
        if (msg.htmlContent) {
          return DOMPurify.sanitize(msg.htmlContent)
        } else if (msg.content) {
          return DOMPurify.sanitize(marked.parse(msg.content))
        } else {
          return 'Invalid'
        }
      } else {
        return '<div class="plain-text-mode">' + DOMPurify.sanitize(he.escape(msg.content || '')) + '</div>'
      }
    },

    sendMessage() {
      const content = this.inputMessage.trim()
      if (!content || !this.wsConnected || !this.currentRoomId) return

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

      this.$nextTick(() => {
        const container = this.$el.querySelector('.messages')
        if (container) container.scrollTop = container.scrollHeight
      })
    },

    handleKeydown(event) {
      if (event.key === 'Enter' && event.ctrlKey) {
        event.preventDefault()
        this.sendMessage()
      }
    },

    // ===== 全屏 =====
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

    formatTime(time) {
      return new Date(time).toTimeString().substr(0, 8)
    }
  }
}
</script>

<style scoped>
.chat-room-page {
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
  width: 280px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  height: 100%;
}

.user-info-sidebar {
  padding: 14px;
  border: 1px solid #d9e3f2;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 8px 20px rgba(16, 43, 98, 0.08);
  flex-shrink: 0;
}

.user-info-sidebar h3 {
  margin: 0 0 10px 0;
  font-size: 14px;
  color: #1f2d3d;
}

.user-info-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-info-row .username-input {
  flex: 1;
  padding: 8px 10px;
  border: 1px solid #d6e0ef;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.2s ease;
}

.user-info-row .username-input:focus {
  outline: none;
  border-color: #75b5ff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.14);
}

.room-list-sidebar {
  flex: 1;
  padding: 16px 14px;
  border: 1px solid #d9e3f2;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 8px 20px rgba(16, 43, 98, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.room-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  flex-shrink: 0;
}

.room-list-header h3 {
  margin: 0;
  font-size: 14px;
  color: #1f2d3d;
}

.room-list-content {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

.no-rooms-tip {
  color: #92a1b7;
  text-align: center;
  padding: 20px;
  font-size: 13px;
}

.room-item {
  margin-bottom: 6px;
  border-radius: 10px;
  background: #f8faff;
  overflow: hidden;
}

.room-title-row {
  display: flex;
  align-items: center;
  padding: 8px 10px;
  gap: 6px;
}

.room-expand-icon {
  cursor: pointer;
  font-size: 10px;
  color: #7a879a;
  flex-shrink: 0;
  width: 14px;
  text-align: center;
}

.room-name {
  flex: 1;
  font-size: 13px;
  color: #2c3e50;
  cursor: pointer;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.room-name:hover {
  color: #409eff;
}

.room-name-active {
  color: #2bc870 !important;
  font-weight: 700;
}

.room-member-count {
  font-size: 11px;
  color: #909399;
  flex-shrink: 0;
}

.room-members {
  padding: 6px 10px 10px 28px;
}

.room-member-item {
  display: flex;
  align-items: center;
  padding: 4px 0;
  font-size: 12px;
  color: #51627a;
}

.online-dot {
  color: #2bc870;
  margin-right: 6px;
  font-size: 10px;
}

.no-members-tip {
  color: #92a1b7;
  font-size: 12px;
}

/* ===== 右侧聊天区域 ===== */
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
.room-list-content::-webkit-scrollbar {
  width: 6px;
}

.messages::-webkit-scrollbar-thumb,
.room-list-content::-webkit-scrollbar-thumb {
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

.message-textarea:focus {
  outline: none;
  border-color: #75b5ff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.14);
}

.message-textarea:disabled {
  background: #f0f2f5;
  cursor: not-allowed;
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

.chat-room-page.fullscreen {
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

.chat-room-page.fullscreen .messages {
  min-height: 0;
  padding: 14px;
}

.chat-room-page.fullscreen .sidebar-container {
  width: 280px !important;
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

.no-messages-tip {
  color: #92a1b7;
  text-align: center;
  padding: 20px;
  font-size: 13px;
}

@media (max-width: 960px) {
  .chat-main-content {
    flex-direction: column;
    height: auto;
  }

  .sidebar-container,
  .chat-room-page.fullscreen .sidebar-container {
    width: 100% !important;
    max-height: 400px;
  }

  .chat-content-area {
    min-width: 0;
  }
}
</style>
