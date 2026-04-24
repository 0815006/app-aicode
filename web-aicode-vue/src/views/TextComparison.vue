<template>
  <div class="text-comparison-container">
    <!-- 核心比对区域 -->
    <div class="comparison-section">
      <div class="file-comparison-area">
        <!-- 参照文件预览 -->
        <div class="file-preview-wrapper">
          <div class="file-header">
            <span class="file-label">参照文件</span>
          </div>
          <div class="file-content" 
               @dragover="handleDragOver($event, 'reference')"
               @dragleave="handleDragLeave($event, 'reference')"
               @drop="handleDrop($event, 'reference')"
               @click="handleFileClick('reference')">
            <div v-if="referenceContent" class="code-content">
              <div v-for="(line, index) in referenceLines" :key="index" 
                   :class="['code-line', getReferenceLineClass(index)]" 
                   @click="handleLineClick('reference', index)">
                <span class="line-number">{{ index + 1 }}</span>
                <span class="line-text">{{ line }}</span>
              </div>
            </div>
            <div v-else class="drop-zone">
              <i class="el-icon-upload"></i>
              <p>拖拽文件到此处或点击选择文件</p>
            </div>
            <input type="file" ref="referenceFileInput" @change="handleReferenceFileChange" accept=".json,.md,.properties,.yml,.txt" style="display: none" />
          </div>
        </div>

        <!-- 目标文件预览 -->
        <div class="file-preview-wrapper">
          <div class="file-header">
            <span class="file-label">目标文件</span>
          </div>
          <div class="file-content" 
               @dragover="handleDragOver($event, 'target')"
               @dragleave="handleDragLeave($event, 'target')"
               @drop="handleDrop($event, 'target')"
               @click="handleFileClick('target')">
            <div v-if="targetContent" class="code-content">
              <div v-for="(line, index) in targetLines" :key="index" 
                   :class="['code-line', getTargetLineClass(index)]" 
                   @click="handleLineClick('target', index)">
                <span class="line-number">{{ index + 1 }}</span>
                <span class="line-text">{{ line }}</span>
              </div>
            </div>
            <div v-else class="drop-zone">
              <i class="el-icon-upload"></i>
              <p>拖拽文件到此处或点击选择文件</p>
            </div>
            <input type="file" ref="targetFileInput" @change="handleTargetFileChange" accept=".json,.md,.properties,.yml,.txt" style="display: none" />
          </div>
        </div>
      </div>
    </div>

    <!-- 比对结果区域 -->
    <div class="result-section" v-if="compareResults.length > 0">
      <div class="section-header">
        <span class="title">比对结果</span>
        <span class="result-summary">
          共 {{ compareResults.length }} 行差异
        </span>
      </div>
      <el-table :data="compareResults" border style="width: 100%" size="small">
        <el-table-column prop="lineNumber" label="行号" width="80" align="center" />
        <el-table-column prop="referenceLine" label="参照内容" show-overflow-tooltip />
        <el-table-column prop="targetLine" label="目标内容" show-overflow-tooltip />
        <el-table-column prop="difference" label="差异类型" width="120">
          <template slot-scope="scope">
            <el-tag :type="getDifferenceType(scope.row.difference)" size="mini">
              {{ scope.row.difference }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template slot-scope="scope">
            <el-button type="primary" size="mini" @click="handleOverrideLine(scope.row)">
              覆盖
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 弹窗组件 -->
    <el-dialog title="选择参照文件" :visible.sync="showReferenceDialog" width="600px">
      <div class="file-list">
        <el-table :data="availableFiles" border style="width: 100%" size="small" @row-click="selectAvailableFile">
          <el-table-column prop="fileName" label="文件名" />
          <el-table-column prop="fileSize" label="大小" width="100" />
          <el-table-column prop="lastModified" label="修改时间" />
        </el-table>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button size="small" @click="showReferenceDialog = false">取消</el-button>
        <el-button type="primary" size="small" @click="confirmReferenceFile">确定</el-button>
      </span>
    </el-dialog>

    <el-dialog title="选择目标文件" :visible.sync="showTargetDialog" width="600px">
      <div class="file-list">
        <el-table :data="availableFiles" border style="width: 100%" size="small" @row-click="selectAvailableFile">
          <el-table-column prop="fileName" label="文件名" />
          <el-table-column prop="fileSize" label="大小" width="100" />
          <el-table-column prop="lastModified" label="修改时间" />
        </el-table>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button size="small" @click="showTargetDialog = false">取消</el-button>
        <el-button type="primary" size="small" @click="confirmTargetFile">确定</el-button>
      </span>
    </el-dialog>

    <el-dialog title="文件内容" :visible.sync="showContentDialog" width="800px">
      <div class="code-content" style="max-height: 400px; overflow-y: auto;">
        <div v-for="(line, index) in currentFileContent" :key="index" 
             :class="['code-line', currentFileType === 'reference' ? 'reference-line' : 'target-line']">
          <span class="line-number">{{ index + 1 }}</span>
          <span class="line-text">{{ line }}</span>
        </div>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button size="small" @click="showContentDialog = false">关闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
// 简化版 - 模拟数据
const loadReferenceFile = async (configId) => ({ code: 200, data: { content: 'JSON数据\n{"key": "value"}\n数组数据\n[1, 2, 3]' } })
const loadTargetFile = async (configId) => ({ code: 200, data: { content: 'JSON数据\n{"key": "value"}\n对象数据\n{"a": 1}' } })
const compareTextFiles = async ({ referenceContent, targetContent }) => ({ code: 200, data: [
  { lineNumber: 3, referenceLine: '数组数据', targetLine: '对象数据', difference: '修改' },
  { lineNumber: 4, referenceLine: '[1, 2, 3]', targetLine: '{"a": 1}', difference: '修改' }
] })
const saveComparisonResult = async ({ referenceContent, targetContent, differences }) => ({ code: 200 })
const getAvailableFiles = async () => ({ code: 200, data: [{ fileName: 'config.json', fileSize: '2KB', lastModified: '2024-01-01' }] })

export default {
  name: 'TextComparison',
  data() {
    return {
      fileConfigs: [],
      referenceConfigId: null,
      targetConfigId: null,
      referenceContent: null,
      targetContent: null,
      referenceLines: [],
      targetLines: [],
      referenceLoading: false,
      targetLoading: false,
      compareLoading: false,
      referenceProgress: 0,
      targetProgress: 0,
      compareResults: [],
      showReferenceDialog: false,
      showTargetDialog: false,
      showContentDialog: false,
      availableFiles: [],
      currentFileType: '',
      currentFileContent: [],
      selectedReferenceLine: -1,
      selectedTargetLine: -1
    }
  },
  created() {
    this.loadFileConfigs()
    this.loadAvailableFiles()
  },
  methods: {
    async loadFileConfigs() {
      try {
        this.fileConfigs = [
          { id: 1, configName: '配置文件1' },
          { id: 2, configName: '配置文件2' }
        ]
        if (this.fileConfigs.length > 0) {
          this.referenceConfigId = this.fileConfigs[0].id
          this.targetConfigId = this.fileConfigs[1].id
        }
      } catch (error) {
        this.$message.error('获取文件配置失败')
      }
    },
    async loadAvailableFiles() {
      try {
        const res = await getAvailableFiles()
        if (res.code === 200) {
          this.availableFiles = res.data || []
        }
      } catch (error) {
        this.$message.error('获取文件列表失败')
      }
    },
    async handleReferenceConfigChange(id) {
      this.referenceContent = null
      this.referenceLines = []
      this.referenceLoading = false
      this.referenceProgress = 0
    },
    async handleTargetConfigChange(id) {
      this.targetContent = null
      this.targetLines = []
      this.targetLoading = false
      this.targetProgress = 0
    },
    async handleReferenceFileChange(event) {
      const file = event.target.files[0]
      if (file) {
        this.referenceLoading = true
        this.referenceProgress = 0
        try {
          const reader = new FileReader()
          reader.onload = (e) => {
            this.referenceContent = e.target.result
            this.referenceLines = this.referenceContent.split('\n')
            this.referenceLoading = false
            this.referenceProgress = 100
            this.$message.success('参照文件加载成功')
          }
          reader.readAsText(file)
        } catch (error) {
          this.$message.error('加载参照文件失败')
          this.referenceLoading = false
        }
      }
    },
    async handleTargetFileChange(event) {
      const file = event.target.files[0]
      if (file) {
        this.targetLoading = true
        this.targetProgress = 0
        try {
          const reader = new FileReader()
          reader.onload = (e) => {
            this.targetContent = e.target.result
            this.targetLines = this.targetContent.split('\n')
            this.targetLoading = false
            this.targetProgress = 100
            this.$message.success('目标文件加载成功')
          }
          reader.readAsText(file)
        } catch (error) {
          this.$message.error('加载目标文件失败')
          this.targetLoading = false
        }
      }
    },
    async handleDragOver(event, type) {
      event.preventDefault()
      event.currentTarget.classList.add('drag-over')
    },
    async handleDragLeave(event, type) {
      event.currentTarget.classList.remove('drag-over')
    },
    async handleDrop(event, type) {
      event.preventDefault()
      event.currentTarget.classList.remove('drag-over')
      const files = event.dataTransfer.files
      if (files.length > 0) {
        if (type === 'reference') {
          this.$refs.referenceFileInput.files = files
          this.handleReferenceFileChange({ target: { files } })
        } else {
          this.$refs.targetFileInput.files = files
          this.handleTargetFileChange({ target: { files } })
        }
      }
    },
    handleFileClick(type) {
      if (type === 'reference') {
        this.$refs.referenceFileInput.click()
      } else {
        this.$refs.targetFileInput.click()
      }
    },
    async handleLoadReferenceFile() {
      this.showReferenceDialog = true
    },
    async handleLoadTargetFile() {
      this.showTargetDialog = true
    },
    async selectAvailableFile(row) {
      this.currentFileType = row.type
      this.currentFileContent = row.content.split('\n')
      this.showContentDialog = true
    },
    async confirmReferenceFile() {
      this.referenceContent = '文件内容行1\n文件内容行2\n文件内容行3'
      this.referenceLines = this.referenceContent.split('\n')
      this.showReferenceDialog = false
      this.$message.success('参照文件选择成功')
    },
    async confirmTargetFile() {
      this.targetContent = '目标内容行1\n目标内容行2\n目标内容行4'
      this.targetLines = this.targetContent.split('\n')
      this.showTargetDialog = false
      this.$message.success('目标文件选择成功')
    },
    async compareText() {
      if (!this.referenceContent || !this.targetContent) {
        this.$message.warning('请先加载两个文件')
        return
      }
      
      this.compareLoading = true
      try {
        const res = await compareTextFiles({
          referenceContent: this.referenceContent,
          targetContent: this.targetContent
        })
        if (res.code === 200) {
          this.compareResults = res.data || []
          this.$message.success('比对完成，发现 ' + this.compareResults.length + ' 处差异')
        }
      } catch (error) {
        this.$message.error('比对失败')
      } finally {
        this.compareLoading = false
      }
    },
    getReferenceLineClass(index) {
      const result = this.compareResults.find(r => r.lineNumber === index + 1)
      if (result) return 'diff-line'
      return ''
    },
    getTargetLineClass(index) {
      const result = this.compareResults.find(r => r.lineNumber === index + 1)
      if (result) return 'diff-line'
      return ''
    },
    handleLineClick(type, index) {
      if (type === 'reference') {
        this.selectedReferenceLine = index
      } else {
        this.selectedTargetLine = index
      }
    },
    async handleOverride() {
      if (this.selectedTargetLine < 0) {
        this.$message.warning('请选择目标文件中的行')
        return
      }
      
      const refLine = this.referenceLines[this.selectedReferenceLine] || ''
      this.targetLines[this.selectedTargetLine] = refLine
      this.targetContent = this.targetLines.join('\n')
      
      this.compareResults = this.compareResults.filter(r => r.lineNumber !== this.selectedTargetLine + 1)
      
      this.selectedTargetLine = -1
      this.$message.success('覆盖成功')
    },
    async handleDeleteLine() {
      if (this.selectedTargetLine < 0) {
        this.$message.warning('请选择目标文件中的行')
        return
      }
      
      this.targetLines.splice(this.selectedTargetLine, 1)
      this.targetContent = this.targetLines.join('\n')
      
      this.compareResults = this.compareResults.filter(r => r.lineNumber !== this.selectedTargetLine + 1)
      
      this.selectedTargetLine = -1
      this.$message.success('删除成功')
    },
    async handleOverrideLine(row) {
      const refLine = this.referenceLines[row.lineNumber - 1] || ''
      const targetIndex = this.targetLines.findIndex((_, i) => i === row.lineNumber - 1)
      if (targetIndex >= 0) {
        this.targetLines[targetIndex] = refLine
        this.targetContent = this.targetLines.join('\n')
        
        this.compareResults = this.compareResults.filter(r => r.lineNumber !== row.lineNumber)
        
        this.$message.success('覆盖成功')
      }
    },
    getDifferenceType(difference) {
      if (difference.includes('新增')) return 'success'
      if (difference.includes('删除')) return 'danger'
      if (difference.includes('修改')) return 'warning'
      return 'info'
    },
    async handleSave() {
      if (!this.compareResults.length) {
        this.$message.info('没有比对结果需要保存')
        return
      }
      
      try {
        const res = await saveComparisonResult({
          referenceContent: this.referenceContent,
          targetContent: this.targetContent,
          differences: this.compareResults
        })
        if (res.code === 200) {
          this.$message.success('比对结果保存成功')
        }
      } catch (error) {
        this.$message.error('保存失败')
      }
    },
    refreshAll() {
      this.referenceContent = null
      this.targetContent = null
      this.referenceLines = []
      this.targetLines = []
      this.compareResults = []
      this.selectedReferenceLine = -1
      this.selectedTargetLine = -1
    }
  }
}
</script>

<style scoped>
.text-comparison-container {
  height: 100vh;
  box-sizing: border-box;
  overflow: auto;
  padding: 16px;
  background: linear-gradient(180deg, #f7f9ff 0%, #eef3ff 100%);
}

.comparison-section {
  margin-bottom: 15px;
}

.file-comparison-area {
  display: flex;
  gap: 20px;
  margin-top: 15px;
}

.file-preview-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.file-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  padding: 8px 12px;
  background: #f3f8ff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.file-label {
  font-weight: 700;
  color: #42506a;
}

.file-content {
  flex: 1;
  border: 1px solid #e8eefb;
  border-radius: 8px;
  overflow: hidden;
  max-height: calc(100vh - 200px);
  display: flex;
  flex-direction: column;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.drop-zone {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  border: 2px dashed #dcdfe6;
  transition: all 0.3s;
  cursor: pointer;
}

.drop-zone:hover {
  border-color: #2f8ff0;
  background: #f0f7ff;
}

.drop-zone i {
  font-size: 48px;
  color: #c0c4cc;
  margin-bottom: 10px;
}

.drop-zone p {
  color: #909399;
  font-size: 14px;
}

.code-content {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.code-line {
  display: flex;
  align-items: center;
  padding: 4px 8px;
  cursor: pointer;
  transition: background-color 0.2s;
  position: relative;
}

.code-line:hover {
  background-color: #f0f7ff;
}

.code-line.diff-line {
  background-color: #fff7e6;
}

.code-line.diff-line.reference-line {
  background-color: #e6f7ff;
}

.code-line.diff-line.target-line {
  background-color: #fff2e8;
}

.line-number {
  display: inline-block;
  width: 40px;
  text-align: right;
  margin-right: 12px;
  color: #909399;
  font-size: 12px;
  user-select: none;
}

.line-text {
  flex: 1;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  color: #303133;
  word-break: break-all;
}

.result-section {
  margin-top: 15px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  border-bottom: 1px solid #edf2ff;
  padding-bottom: 10px;
}

.section-header .title {
  font-size: 16px;
  font-weight: 700;
  color: #23324a;
}

.result-summary {
  font-size: 12px;
  color: #909399;
}

.operation-buttons {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #e8eefb;
}

.operation-buttons .el-button {
  width: 100%;
}

.file-list {
  max-height: 400px;
  overflow-y: auto;
}

.dialog-footer {
  text-align: center;
}

.code-line.reference-line {
  background-color: #e6f7ff;
}

.code-line.target-line {
  background-color: #f0f7ff;
}

::v-deep .el-table {
  border-radius: 10px;
  overflow: hidden;
}

::v-deep .el-table th {
  background: #f3f8ff;
  color: #42506a;
}

::v-deep .el-button {
  border-radius: 8px;
}
</style>