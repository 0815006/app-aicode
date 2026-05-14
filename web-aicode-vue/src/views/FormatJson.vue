<!-- src/views/FormatJson.vue -->
<template>
  <div class="format-json-page">
    <div class="json-container">
      <!-- 左侧：输入区域 -->
      <div class="input-section card">
        <div class="section-header">
          <h3><i class="el-icon-edit"></i> 输入 JSON</h3>
        </div>
        <div class="input-content">
          <el-input
            v-model="inputJson"
            type="textarea"
            placeholder="请输入JSON字符串..."
            :rows="20"
            @input="handleInputChange"
          />
        </div>
      </div>

      <!-- 右侧：输出区域 -->
      <div class="output-section card">
        <div class="section-header">
          <h3><i class="el-icon-document"></i> 格式化结果</h3>
          <div class="header-actions">
            <el-button
              type="primary"
              icon="el-icon-refresh"
              @click="formatJson"
              :disabled="!inputJson.trim()"
            >
              刷新
            </el-button>
            <el-button
              type="success"
              icon="el-icon-document-copy"
              @click="copyFormattedJson"
              :disabled="!formattedJson"
            >
              复制
            </el-button>
          </div>
        </div>
        <div class="output-content">
          <div class="json-wrapper">
            <div v-if="formattedJson" class="json-display">
              <pre><code>{{ formattedJson }}</code></pre>
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
  name: 'FormatJson',
  data() {
    return {
      inputJson: '',
      formattedJson: ''
    }
  },
  methods: {
    handleInputChange() {
      // 输入变化时清空格式化结果
      this.formattedJson = ''
    },
    formatJson() {
      if (!this.inputJson.trim()) {
        this.$message.warning('请输入JSON字符串')
        return
      }

      try {
        const parsed = JSON.parse(this.inputJson)
        this.formattedJson = JSON.stringify(parsed, null, 2)
      } catch (error) {
        this.$message.error('JSON解析失败，请检查输入格式')
        this.formattedJson = ''
      }
    },
    copyFormattedJson() {
      if (!this.formattedJson) {
        this.$message.warning('没有可复制的内容')
        return
      }

      navigator.clipboard.writeText(this.formattedJson)
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
.format-json-page {
  padding: 20px;
  height: 100%;
  background: linear-gradient(180deg, #f5f8ff 0%, #eef3fb 100%);
  box-sizing: border-box;
  overflow: hidden;
}

.json-container {
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
  padding: 16px 20px;
  border-bottom: 1px solid #f0f4f8;
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.json-wrapper {
  flex: 1;
  padding: 15px;
  overflow-y: auto;
}

.json-display {
  background: #282c34;
  color: #abb2bf;
  padding: 15px;
  border-radius: 8px;
  font-family: 'Courier New', Courier, monospace;
  font-size: 13px;
  line-height: 1.5;
  box-sizing: border-box;
}

.json-display pre {
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
  .json-container {
    flex-direction: column;
  }
  .input-section, .output-section {
    min-width: 0;
    flex: none;
    height: 50%;
  }
}
</style>