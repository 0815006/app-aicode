<!-- src/views/FormatHtml.vue -->
<template>
  <div class="format-html-page">
    <div class="html-container">
      <!-- 左侧：输入区域 -->
      <div class="input-section card">
        <div class="section-header">
          <h3><i class="el-icon-edit"></i> 输入 HTML</h3>
        </div>
        <div class="input-content">
          <el-input
            v-model="inputHtml"
            type="textarea"
            placeholder="请输入HTML代码..."
            :rows="20"
            @input="handleInputChange"
          />
        </div>
      </div>

      <!-- 右侧：输出区域 -->
      <div class="output-section card">
        <div class="section-header">
          <h3><i class="el-icon-s-order"></i> 格式化结果</h3>
          <div class="header-actions">
            <el-button
              type="primary"
              icon="el-icon-refresh"
              @click="formatHtml"
              :disabled="!inputHtml.trim()"
            >
              刷新
            </el-button>
            <el-button
              type="success"
              icon="el-icon-document-copy"
              @click="copyFormattedHtml"
              :disabled="!formattedHtml"
            >
              复制
            </el-button>
          </div>
        </div>
        <div class="output-content">
          <div class="html-wrapper">
            <div v-if="formattedHtml" class="html-display">
              <pre><code>{{ formattedHtml }}</code></pre>
            </div>
            <div v-else class="empty-tip">
              <i class="el-icon-info"></i>
              <p>点击刷新按钮查看格式化结果</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import copyToClipboard from '@/utils/clipboard'

// HTML void elements — 永无闭合标签，不能有子节点
const VOID_ELEMENTS = new Set([
  'area', 'base', 'br', 'col', 'embed', 'hr', 'img', 'input',
  'link', 'meta', 'param', 'source', 'track', 'wbr'
])

// raw text 元素 — 内容不解析，原样保留
const RAW_TEXT_ELEMENTS = new Set([
  'script', 'style', 'textarea', 'title'
])

// 不包含子节点时也应当保持内容的元素（即使不是 raw text）
const PRESERVE_WHITESPACE_ELEMENTS = new Set([
  'pre', 'code'
])

// 常见的 inline 元素 — 内容不换行
const INLINE_ELEMENTS = new Set([
  'a', 'abbr', 'b', 'bdi', 'bdo', 'cite', 'code', 'data', 'dfn',
  'em', 'i', 'kbd', 'mark', 'q', 's', 'samp', 'small', 'span',
  'strong', 'sub', 'sup', 'time', 'u', 'var'
])

export default {
  name: 'FormatHtml',
  data() {
    return {
      inputHtml: '',
      formattedHtml: ''
    }
  },
  methods: {
    handleInputChange() {
      this.formattedHtml = ''
    },
    formatHtml() {
      if (!this.inputHtml.trim()) {
        this.$message.warning('请输入HTML代码')
        return
      }

      try {
        const parser = new DOMParser()
        const doc = parser.parseFromString(this.inputHtml, 'text/html')
        // text/html 不会抛错，只产生 parsererror 在极罕见情况下
        const errorNode = doc.querySelector('parsererror')
        if (errorNode) {
          throw new Error(errorNode.textContent || 'HTML格式不正确')
        }

        const indent = '  '
        const lines = []

        const walk = (node, depth) => {
          if (node.nodeType === Node.TEXT_NODE) {
            const text = node.textContent
            // 检查父元素是否为 raw text 或 preserve whitespace
            const parentTag = node.parentElement
              ? node.parentElement.tagName.toLowerCase()
              : ''
            if (RAW_TEXT_ELEMENTS.has(parentTag) || PRESERVE_WHITESPACE_ELEMENTS.has(parentTag)) {
              if (text) lines.push(text)
            } else {
              const trimmed = text.trim()
              if (trimmed) lines.push(indent.repeat(depth) + trimmed)
            }
            return
          }

          if (node.nodeType === Node.COMMENT_NODE) {
            lines.push(indent.repeat(depth) + '<!--' + node.textContent + '-->')
            return
          }

          if (node.nodeType !== Node.ELEMENT_NODE) return

          const tag = node.tagName.toLowerCase()
          const attrs = this.formatAttributes(node)
          const openTag = attrs ? '<' + tag + ' ' + attrs + '>' : '<' + tag + '>'

          // raw text 元素特殊处理
          if (RAW_TEXT_ELEMENTS.has(tag)) {
            lines.push(indent.repeat(depth) + openTag + node.innerHTML + '</' + tag + '>')
            return
          }

          // void 元素：自闭合形式
          if (VOID_ELEMENTS.has(tag)) {
            lines.push(indent.repeat(depth) + '<' + tag + (attrs ? ' ' + attrs : '') + ' />')
            return
          }

          const children = Array.from(node.childNodes)

          if (children.length === 0) {
            // 空元素 — 完整闭合
            lines.push(indent.repeat(depth) + openTag + '</' + tag + '>')
            return
          }

          // 判断是否为纯文本内容（只有单个文本子节点）
          const isTextOnly = children.length === 1 && children[0].nodeType === Node.TEXT_NODE

          if (isTextOnly) {
            const text = children[0].textContent
            if (INLINE_ELEMENTS.has(tag) || !text.includes('\n') && text.length < 60) {
              // 短文本或 inline 元素 → 一行
              lines.push(indent.repeat(depth) + openTag + text + '</' + tag + '>')
              return
            }
          }

          // 常规块级元素：开始标签 → 子节点 → 闭合标签
          lines.push(indent.repeat(depth) + openTag)
          children.forEach(function (child) { walk(child, depth + 1) })
          lines.push(indent.repeat(depth) + '</' + tag + '>')
        }

        // 从 body 开始遍历，或从 html 开始
        const root = doc.body.childNodes.length > 0 ? doc.body : doc.documentElement
        Array.from(root.childNodes).forEach(function (child) { walk(child, 0) })

        this.formattedHtml = lines.join('\n').trim()
        this.$message.success('格式化成功')
      } catch (error) {
        this.$message.error('HTML格式化失败: ' + (error.message || '请检查输入格式'))
        this.formattedHtml = ''
      }
    },
    formatAttributes(node) {
      if (!node.attributes || node.attributes.length === 0) return ''
      const parts = []
      for (let i = 0; i < node.attributes.length; i++) {
        const attr = node.attributes[i]
        // 布尔属性：只输出属性名
        if (attr.value === '' && this.isBooleanAttr(attr.name)) {
          parts.push(attr.name)
        } else {
          parts.push(attr.name + '="' + this.escapeAttr(attr.value) + '"')
        }
      }
      return parts.join(' ')
    },
    isBooleanAttr(name) {
      const bools = new Set([
        'disabled', 'checked', 'selected', 'readonly', 'required',
        'multiple', 'autofocus', 'novalidate', 'formnovalidate'
      ])
      return bools.has(name.toLowerCase())
    },
    escapeAttr(value) {
      return value
        .replace(/&/g, '&amp;')
        .replace(/"/g, '&quot;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
    },
    copyFormattedHtml() {
      if (!this.formattedHtml) {
        this.$message.warning('没有可复制的内容')
        return
      }
      var self = this
      copyToClipboard(this.formattedHtml)
        .then(function () { self.$message.success('复制成功') })
        .catch(function () { self.$message.error('复制失败') })
    }
  }
}
</script>

<style scoped>
.format-html-page {
  padding: 20px;
  height: 100%;
  background: linear-gradient(180deg, #f5f8ff 0%, #eef3fb 100%);
  box-sizing: border-box;
  overflow: hidden;
}

.html-container {
  display: flex;
  gap: 20px;
  height: 100%;
  align-items: stretch;
}

.card {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #d9e3f2;
  box-shadow: 0 12px 24px rgba(16, 43, 98, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.input-section {
  flex: 1;
  min-width: 0;
}

.output-section {
  flex: 1;
  min-width: 0;
}

.section-header {
  padding: 18px 20px;
  height: 68px;
  flex-shrink: 0;
  border-bottom: 1px solid #f0f4f8;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-sizing: border-box;
}

.section-header h3 {
  margin: 0;
  font-size: 18px;
  color: #1f2d3d;
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.input-content {
  padding: 15px;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.input-content ::v-deep .el-textarea__inner {
  flex: 1;
  resize: none;
  font-family: 'Courier New', Courier, monospace;
  font-size: 14px;
}

.output-content {
  padding: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.html-wrapper {
  flex: 1;
  padding: 15px;
  overflow-y: auto;
}

.html-display {
  background: #282c34;
  color: #abb2bf;
  padding: 15px;
  border-radius: 8px;
  font-family: 'Courier New', Courier, monospace;
  font-size: 13px;
  line-height: 1.5;
  box-sizing: border-box;
  min-height: 100%;
}

.html-display pre {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.empty-tip {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100%;
  color: #909399;
}

.empty-tip i {
  font-size: 48px;
  margin-bottom: 16px;
}

@media (max-width: 1000px) {
  .html-container {
    flex-direction: column;
  }
  .input-section, .output-section {
    min-width: 0;
    flex: none;
    height: 50%;
  }
}
</style>
