<template>
  <div 
    ref="chatContainer"
    class="chat-hall-page" 
    :class="{ fullscreen: isFullscreen }"
  >
    <!-- 左右布局 -->
    <div class="chat-main-content">

      <!-- 左侧：在线用户列表 -->
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
import he from 'he'  // ✅ 加上这一行

// 引入代码高亮样式
import 'highlight.js/styles/github.css'

// 自定义 Tokenizer
const renderer = new marked.Renderer()

// 重写代码块解析（fenced code blocks）
renderer.code = function (code, language) {
  // 如果 language 存在但没有闭合（marked 会正常传入 language）
  // 我们不处理，交给默认逻辑 —— 但我们要防止未闭合导致的问题
  // 实际上 marked 会把未闭合的 ``` 当作段落处理，所以我们只需确保安全输出
  const validLang = language && hljs.getLanguage(language) ? language : 'plaintext'
  let highlighted

  try {
    if (language && hljs.getLanguage(validLang)) {
      highlighted = hljs.highlight(code, { language: validLang }).value
    } else {
      highlighted = hljs.highlightAuto(code).value
    }
  } catch (err) {
    // 高亮失败 → 返回纯文本
    return `<pre><code>${marked.escape(code)}</code></pre>`
  }

  return `<pre><code class="hljs language-${language}">${highlighted}</code></pre>`
}

// 自定义 tokenizer：防止未闭合代码块破坏解析（进阶可选）
// 但 marked 本身对未闭合 ``` 的处理较弱，最简单方式是：确保输出安全

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
      ws: null,
      wsConnected: false,
      isFullscreen: false, // ✅ 控制全屏状态
      onlineUsers: [], // 存放在线用户列表
      displayMode: localStorage.getItem('chatDisplayMode') || 'plain', // 默认纯文本
      hasLoadedHistory: false // ✅ 新增字段
    }
  },
  created() {
    this.initWebSocket()
  },
  beforeDestroy() {
    this.closeWebSocket()
  },
  mounted() {
    // 监听全屏状态变化（兼容 ESC 键退出）
    document.addEventListener('fullscreenchange', this.handleFullscreenChange)
    document.addEventListener('webkitfullscreenchange', this.handleFullscreenChange)
      // ✅ 页面加载时自动调用加载历史消息
    this.loadHistoryMessages()
  },

  beforeDestroy() {
    document.removeEventListener('fullscreenchange', this.handleFullscreenChange)
    document.removeEventListener('webkitfullscreenchange', this.handleFullscreenChange)
  },
  watch: {
    displayMode(newMode) {
      localStorage.setItem('chatDisplayMode', newMode)
    },
    currentUser(newName, oldName) {
      if (this.wsConnected && newName && newName !== oldName) {
        // 发送一个特殊消息，告诉服务器“我改名了”
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
        console.log('📩 收到消息：', event.data) // ✅ 加日志
        const msg = JSON.parse(event.data)
        // 在 onmessage 中添加
        if (msg.type === 'rename') {
          console.log(`【系统】${msg.oldName} 改名为 ${msg.newName}`)
          // 可选：在消息区显示一条系统消息
          this.messages.push({
            username: '📢 系统通知',
            htmlContent: `<i style="color:#999">${msg.oldName} 改名为 ${msg.newName}</i>`,
            time: msg.time
          })
        }else if (msg.type === 'onlineUsers') {
          // 假设后端发送 { type: 'onlineUsers', users: ['张三', '李四'] }
          this.onlineUsers = [...new Set(msg.users)] // ✅ 去重
        } else {
          // 普通消息
          msg.id = msg.id || Date.now() + Math.random() // 防止后端没传 id
          this.messages.push(msg)
          this.$nextTick(() => {
            const container = this.$el.querySelector('.messages')
            if (container) container.scrollTop = container.scrollHeight
          })
        }
      }

      this.ws.onclose = () => {
        console.log('WebSocket 已关闭')
        this.wsConnected = false
        setTimeout(this.initWebSocket, 3000)
      }

      this.ws.onerror = (error) => {
        console.error('WebSocket 错误', error)
      }
    },

    loadHistoryMessages() {
      if (this.hasLoadedHistory) return // ✅ 如果已加载，不重复执行
      const timestamp = this.messages.length > 0 ? this.messages[0].time : Date.now();
      fetch(`/api/chat/history?timestamp=${timestamp}&limit=50`)
        .then(response => response.json())
        .then(data => {
          this.hasLoadedHistory = true // ✅ 标记为已加载
          const historyMessages = Array.isArray(data) ? data : (data && data.data ? data.data : []);
          const newMessages = historyMessages.filter(msg => !this.messages.some(exist => exist.id === msg.id));
          for (const msg of newMessages) {
            // 解析 message 字段中的 JSON 字符串
            try {
              const parsedMessage = JSON.parse(msg.message);
              msg.id = parsedMessage.id;
              msg.username = parsedMessage.username;
              msg.content = parsedMessage.content;
              msg.htmlContent = parsedMessage.htmlContent;
              msg.time = parsedMessage.time;
            } catch (e) {
              console.error('解析历史消息失败:', e);
              // 如果解析失败，使用默认值
              msg.username = '未知用户';
              msg.content = '解析失败';
              msg.htmlContent = '<p>解析失败</p>';
              msg.time = Date.now();
            }
          }
          // 倒序插入消息，确保最新的消息在最下面
          this.messages.push(...newMessages.reverse());

          this.$nextTick(() => {
            const container = this.$el.querySelector('.messages');
            if (container) {
              // ✅ 修改为滚动到底部
              container.scrollTop = container.scrollHeight;
            }
          });
        })
        .catch(error => {
          console.error('加载历史消息失败:', error);
          alert('加载历史消息失败，请稍后再试。');
        });
    },

      // ✅ 新增：安全地渲染消息内容
    renderMessageContent(msg) {
      if (this.displayMode === 'preview') {
        // 确保 htmlContent 有效
        if (msg.htmlContent) {
          return DOMPurify.sanitize(msg.htmlContent);
        } else if (msg.content) {
          // 如果 htmlContent 无效，尝试用 content 渲染
          return DOMPurify.sanitize(this.renderContentToHtml(msg.content));
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
        // 进入全屏
        if (container.requestFullscreen) {
          await container.requestFullscreen()
        } else if (container.webkitRequestFullscreen) {
          await container.webkitRequestFullscreen() // Safari
        }
        this.isFullscreen = true
      } else {
        // 退出全屏
        if (document.exitFullscreen) {
          await document.exitFullscreen()
        } else if (document.webkitExitFullscreen) {
          await document.webkitExitFullscreen()
        }
        this.isFullscreen = false
      }
    },
    // 监听键盘 ESC 退出全屏
    handleFullscreenChange() {
      if (!document.fullscreenElement && !document.webkitFullscreenElement) {
        this.isFullscreen = false
      }
    },

    handleKeydown(event) {
    // 按下 Ctrl + Enter 且内容非空
    if (event.key === 'Enter' && event.ctrlKey) {
      event.preventDefault() // 阻止默认换行（如果需要）
      this.sendMessage()
    }
    // 可选：Shift + Enter 换行
    // 不处理即可，textarea 会自动换行
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

      // ✅ 添加唯一 id（可以用时间戳 + 随机数）
      const msg = {
        id: Date.now() + Math.random(), // 唯一 ID
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

.online-users-sidebar {
  width: 240px;
  padding: 16px 14px;
  overflow-y: auto;
  max-height: 100%;
  border: 1px solid #d9e3f2;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 8px 20px rgba(16, 43, 98, 0.08);
}

.online-users-sidebar h3 {
  margin: 0 0 14px 0;
  font-size: 14px;
  color: #1f2d3d;
}

.online-users-list {
  list-style: none;
  padding: 0;
  margin: 0;
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

.messages::-webkit-scrollbar {
  width: 8px;
}

.messages::-webkit-scrollbar-thumb {
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

.no-messages-tip {
  color: #92a1b7;
  text-align: center;
  padding: 24px;
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

.chat-hall-page.fullscreen .online-users-sidebar {
  width: 240px !important;
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

  .online-users-sidebar,
  .chat-hall-page.fullscreen .online-users-sidebar {
    width: 100% !important;
    max-height: 200px;
  }

  .chat-content-area {
    min-width: 0;
  }
}
</style>
