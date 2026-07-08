<!-- src/views/FormatXml.vue -->
<template>
  <div class="format-xml-page">
    <div class="xml-container">
      <!-- 左侧：输入区域 -->
      <div class="input-section card">
        <div class="section-header">
          <h3><i class="el-icon-edit"></i> 输入 XML</h3>
        </div>
        <div class="input-content">
          <el-input
            v-model="inputXml"
            type="textarea"
            placeholder="请输入XML字符串..."
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
              @click="formatXml"
              :disabled="!inputXml.trim()"
            >
              刷新
            </el-button>
            <el-button
              type="success"
              icon="el-icon-document-copy"
              @click="copyFormattedXml"
              :disabled="!formattedXml"
            >
              复制
            </el-button>
          </div>
        </div>
        <div class="output-content">
          <div class="xml-wrapper">
            <div v-if="formattedXml" class="xml-display">
              <pre><code>{{ formattedXml }}</code></pre>
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
export default {
  name: 'FormatXml',
  data() {
    return {
      inputXml: '',
      formattedXml: ''
    }
  },
  methods: {
    handleInputChange() {
      // 输入变化时清空格式化结果
      this.formattedXml = ''
    },
    formatXml() {
      if (!this.inputXml.trim()) {
        this.$message.warning('请输入XML字符串')
        return
      }

      try {
        // 使用 DOMParser 校验 XML 格式合法性
        const parser = new DOMParser()
        const doc = parser.parseFromString(this.inputXml, 'application/xml')
        const errorNode = doc.querySelector('parsererror')
        if (errorNode) {
          throw new Error(errorNode.textContent || 'XML格式不正确')
        }

        this.formattedXml = this.formatXmlString(this.inputXml)
        this.$message.success('格式化成功')
      } catch (error) {
        this.$message.error('XML格式化失败: ' + (error.message || '请检查输入格式'))
        this.formattedXml = ''
      }
    },
    formatXmlString(xmlStr) {
      const tab = '  '
      let result = ''
      let indent = 0

      // 移除标签间的空白字符，保留文本内容
      const normalized = xmlStr.replace(/>\s+</g, '><')

      // 将XML拆分为标签和文本内容的token数组
      const tokens = normalized.split(/(<[^>]+>)/g).filter(function (t) {
        return t && t.trim()
      })

      for (var i = 0; i < tokens.length; i++) {
        var token = tokens[i]

        if (/^<\/\w/.test(token)) {
          // 闭合标签：减少缩进
          indent = Math.max(0, indent - 1)
          result += tab.repeat(indent) + token + '\n'
        } else if (/^<[?!]/.test(token)) {
          // XML声明、DOCTYPE、注释、CDATA
          result += tab.repeat(indent) + token + '\n'
        } else if (/\/>$/.test(token)) {
          // 自闭合标签
          result += tab.repeat(indent) + token + '\n'
        } else if (/^<\w/.test(token)) {
          // 开始标签
          result += tab.repeat(indent) + token + '\n'

          // 窥探下一个token：如果不是闭合标签，则增加缩进
          var next = i + 1 < tokens.length ? tokens[i + 1] : ''
          if (next && !/^<\//.test(next)) {
            indent++
          }
        } else {
          // 文本内容
          result += tab.repeat(indent) + token + '\n'
        }
      }

      return result.trim()
    },
    copyFormattedXml() {
      if (!this.formattedXml) {
        this.$message.warning('没有可复制的内容')
        return
      }

      navigator.clipboard.writeText(this.formattedXml)
        .then(() => {
          this.$message.success('复制成功')
        })
        .catch(() => {
          this.$message.error('复制失败')
        })
    }
  }
}
</script>

<style scoped>
.format-xml-page {
  padding: 20px;
  height: 100%;
  background: linear-gradient(180deg, #f5f8ff 0%, #eef3fb 100%);
  box-sizing: border-box;
  overflow: hidden;
}

.xml-container {
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

.xml-wrapper {
  flex: 1;
  padding: 15px;
  overflow-y: auto;
}

.xml-display {
  background: #282c34;
  color: #abb2bf;
  padding: 15px;
  border-radius: 8px;
  font-family: 'Courier New', Courier, monospace;
  font-size: 13px;
  line-height: 1.5;
  box-sizing: border-box;
}

.xml-display pre {
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
  .xml-container {
    flex-direction: column;
  }
  .input-section, .output-section {
    min-width: 0;
    flex: none;
    height: 50%;
  }
}
</style>
