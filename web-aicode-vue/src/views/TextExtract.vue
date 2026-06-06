<!-- src/views/TextExtract.vue -->
<template>
  <div class="text-extract-page">
    <!-- 左侧：文件列表 -->
    <aside class="left-panel card">
      <div class="panel-header">
        <h3><i class="el-icon-files"></i> 文件列表</h3>
        <el-button type="primary" icon="el-icon-plus" circle size="small" @click="triggerUpload" />
      </div>
      <!-- 隐藏的上传input -->
      <input
        ref="fileInput"
        type="file"
        accept=".docx,.xlsx,.xls"
        style="display: none"
        @change="handleFileSelect"
      />
      <div class="file-list" v-loading="listLoading">
        <div
          v-for="file in fileList"
          :key="file.fileName"
          :class="['file-item', { active: selectedFile && selectedFile.fileName === file.fileName }]"
          @click="selectFile(file)"
        >
          <div class="file-icon">
            <i v-if="file.fileType === 'excel'" class="el-icon-s-grid" style="color: #67c23a;"></i>
            <i v-else class="el-icon-document" style="color: #409eff;"></i>
          </div>
          <div class="file-info">
            <div class="file-name" :title="file.fileName">{{ file.fileName }}</div>
            <div class="file-meta">
              <span class="file-size">{{ formatFileSize(file.fileSize) }}</span>
              <span class="file-type">{{ file.fileType === 'excel' ? 'Excel' : 'Word' }}</span>
            </div>
          </div>
          <el-button
            type="text"
            icon="el-icon-delete"
            class="file-delete"
            @click.stop="handleDelete(file)"
            title="删除文件"
          />
        </div>
        <el-empty v-if="!listLoading && fileList.length === 0" description="暂无文件，点击 + 上传" :image-size="72" />
      </div>
      <div class="upload-hint">
        <span>支持 .docx / .xlsx / .xls</span>
      </div>
    </aside>

    <!-- 右侧：内容提取展示区 -->
    <section class="right-panel card">
      <div v-if="!selectedFile" class="empty-wrap">
        <el-empty description="请在左侧选择一个文件" :image-size="120" />
      </div>
      <div v-else class="content-wrap">
        <!-- 顶部操作栏 -->
        <div class="toolbar">
          <div class="toolbar-title" :title="selectedFile.fileName">
            <i v-if="selectedFile.fileType === 'excel'" class="el-icon-s-grid" style="color: #67c23a;"></i>
            <i v-else class="el-icon-document" style="color: #409eff;"></i>
            <span>{{ selectedFile.fileName }}</span>
          </div>
          <div class="toolbar-actions">
            <el-button
              type="primary"
              icon="el-icon-s-data"
              size="small"
              :disabled="converting"
              :loading="converting && activeMode === 'json'"
              @click="convertToJson"
            >
              转成 JSON
            </el-button>
            <el-button
              type="success"
              icon="el-icon-s-order"
              size="small"
              :disabled="converting"
              :loading="converting && activeMode === 'markdown'"
              @click="convertToMarkdown"
            >
              转成 Markdown
            </el-button>
            <el-button
              type="warning"
              icon="el-icon-document-copy"
              size="small"
              :disabled="!extractedContent"
              @click="copyContent"
            >
              复制
            </el-button>
          </div>
        </div>

        <!-- 内容展示区 -->
        <div class="content-area">
          <div v-if="!extractedContent" class="empty-content">
            <i class="el-icon-info"></i>
            <p>点击上方「转成 JSON」或「转成 Markdown」开始提取内容</p>
          </div>
          <div v-else class="result-display">
            <div class="result-header">
              <el-tag size="mini" :type="activeMode === 'json' ? 'primary' : 'success'">
                {{ activeMode === 'json' ? 'JSON' : 'Markdown' }}
              </el-tag>
              <span class="result-lines">{{ lineCount }} 行</span>
            </div>
            <div class="code-wrapper">
              <pre><code>{{ extractedContent }}</code></pre>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script>
import { getFileList, uploadFile, deleteFile } from '@/api/text-extract';
import axios from 'axios';

// 按需加载的第三方库
let XLSX = null;
let mammoth = null;
let TurndownService = null;

export default {
  name: 'TextExtract',
  data() {
    return {
      fileList: [],
      selectedFile: null,
      extractedContent: '',
      activeMode: '',        // 'json' | 'markdown'
      converting: false,
      listLoading: false,
      rawBuffer: null        // 当前选中文件的 ArrayBuffer 缓存
    };
  },
  computed: {
    lineCount() {
      if (!this.extractedContent) return 0;
      return this.extractedContent.split('\n').length;
    }
  },
  async mounted() {
    await this.fetchFileList();
  },
  methods: {
    // ==================== 文件列表操作 ====================
    async fetchFileList() {
      this.listLoading = true;
      try {
        const res = await getFileList();
        this.fileList = res.data || [];
      } catch (error) {
        this.$message.error('获取文件列表失败: ' + error.message);
      } finally {
        this.listLoading = false;
      }
    },

    triggerUpload() {
      this.$refs.fileInput.click();
    },

    async handleFileSelect(e) {
      const file = e.target.files[0];
      if (!file) return;

      const loading = this.$loading({ text: '正在上传...', target: '.left-panel' });
      try {
        await uploadFile(file);
        this.$message.success('上传成功');
        await this.fetchFileList();
      } catch (error) {
        this.$message.error('上传失败: ' + error.message);
      } finally {
        loading.close();
        // 重置 input 以便重复选择同一文件
        this.$refs.fileInput.value = '';
      }
    },

    selectFile(file) {
      if (this.selectedFile && this.selectedFile.fileName === file.fileName) return;
      this.selectedFile = file;
      this.extractedContent = '';
      this.activeMode = '';
      this.rawBuffer = null;
    },

    async handleDelete(file) {
      try {
        await this.$confirm('确定删除该文件吗？', '提示', { type: 'warning' });
        await deleteFile(file.fileName);
        this.$message.success('删除成功');
        if (this.selectedFile && this.selectedFile.fileName === file.fileName) {
          this.selectedFile = null;
          this.extractedContent = '';
          this.activeMode = '';
          this.rawBuffer = null;
        }
        await this.fetchFileList();
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error('删除失败: ' + error.message);
        }
      }
    },

    // ==================== 文件加载 ====================
    async loadFileBuffer() {
      if (this.rawBuffer) return this.rawBuffer;
      if (!this.selectedFile) return null;

      try {
        const baseURL = axios.defaults.baseURL || '';
        // 文件名可能包含 []、中文等特殊字符，必须 encodeURIComponent，否则后端 400
        const fileName = encodeURIComponent(this.selectedFile.fileName);
        const url = baseURL + '/api/uploads/text-extract/' + fileName;
        const response = await axios.get(url, { responseType: 'arraybuffer' });
        this.rawBuffer = response.data;
        return this.rawBuffer;
      } catch (error) {
        this.$message.error('加载文件失败: ' + error.message);
        return null;
      }
    },

    // ==================== 第三方库动态加载 ====================
    async ensureXLSX() {
      if (XLSX) return;
      XLSX = await import('xlsx');
    },

    async ensureMammothTurndown() {
      if (mammoth && TurndownService) return;
      mammoth = await import('mammoth');
      const td = await import('turndown');
      TurndownService = td.default || td;
    },

    // ==================== 顶层转换入口 ====================
    async convertToJson() {
      if (!this.selectedFile) {
        this.$message.warning('请先选择文件');
        return;
      }

      this.converting = true;
      this.activeMode = 'json';

      try {
        const buffer = await this.loadFileBuffer();
        if (!buffer) return;

        let result = '';
        if (this.selectedFile.fileType === 'excel') {
          await this.ensureXLSX();
          result = this.convertExcelToJson(buffer);
        } else {
          await this.ensureMammothTurndown();
          result = await this.convertWordToJson(buffer);
        }
        this.extractedContent = result;
      } catch (error) {
        this.$message.error('转换失败: ' + error.message);
        this.extractedContent = '';
      } finally {
        this.converting = false;
      }
    },

    async convertToMarkdown() {
      if (!this.selectedFile) {
        this.$message.warning('请先选择文件');
        return;
      }

      this.converting = true;
      this.activeMode = 'markdown';

      try {
        const buffer = await this.loadFileBuffer();
        if (!buffer) return;

        let result = '';
        if (this.selectedFile.fileType === 'excel') {
          await this.ensureXLSX();
          result = this.convertExcelToMarkdown(buffer);
        } else {
          await this.ensureMammothTurndown();
          result = await this.convertWordToMarkdown(buffer);
        }
        this.extractedContent = result;
      } catch (error) {
        this.$message.error('转换失败: ' + error.message);
        this.extractedContent = '';
      } finally {
        this.converting = false;
      }
    },

    // ==================== Excel → JSON ====================
    convertExcelToJson(buffer) {
      const workbook = XLSX.read(buffer, { type: 'array', raw: false });
      const sheetNames = workbook.SheetNames;
      if (sheetNames.length === 0) return '{}';

      // 多 Sheet 时外层套 Sheet 名，单 Sheet 直接返回数组
      if (sheetNames.length === 1) {
        const sheet = workbook.Sheets[sheetNames[0]];
        const json = XLSX.utils.sheet_to_json(sheet, { defval: '' });
        return JSON.stringify(json, null, 2);
      }

      const result = {};
      for (const name of sheetNames) {
        const sheet = workbook.Sheets[name];
        result[name] = XLSX.utils.sheet_to_json(sheet, { defval: '' });
      }
      return JSON.stringify(result, null, 2);
    },

    // ==================== Excel → Markdown ====================
    convertExcelToMarkdown(buffer) {
      const workbook = XLSX.read(buffer, { type: 'array', raw: false });
      const parts = [];

      for (const name of workbook.SheetNames) {
        const sheet = workbook.Sheets[name];
        const data = XLSX.utils.sheet_to_json(sheet, { header: 1, defval: '', raw: false });
        if (data.length === 0) continue;

        // 多 Sheet 时加二级标题
        if (workbook.SheetNames.length > 1) {
          parts.push(`## ${name}\n`);
        }

        const headers = data[0] || [];
        if (headers.length === 0) continue;

        // 表头行
        parts.push('| ' + headers.map(h => String(h || '')).join(' | ') + ' |');
        // 分隔行
        parts.push('| ' + headers.map(() => '---').join(' | ') + ' |');
        // 数据行 — 单元格内换行替换为 <br>
        for (let i = 1; i < data.length; i++) {
          const row = data[i] || [];
          const cells = headers.map((_, ci) => {
            const val = row[ci] !== undefined ? row[ci] : '';
            return String(val).replace(/\n/g, '<br>');
          });
          parts.push('| ' + cells.join(' | ') + ' |');
        }
        parts.push('');
      }

      return parts.join('\n');
    },

    // ==================== Word → JSON（按段落提取） ====================
    async convertWordToJson(buffer) {
      const result = await mammoth.extractRawText({ arrayBuffer: buffer });
      const text = result.value || '';
      const paragraphs = text
        .split('\n')
        .map(line => line.trim())
        .filter(Boolean);
      return JSON.stringify({
        fileName: this.selectedFile.fileName,
        paragraphs: paragraphs
      }, null, 2);
    },

    // ==================== Word → Markdown ====================
    async convertWordToMarkdown(buffer) {
      // mammoth: docx → HTML，忽略图片
      const htmlResult = await mammoth.convertToHtml(
        { arrayBuffer: buffer },
        { convertImage: null }
      );
      const html = htmlResult.value || '';

      // turndown: HTML → Markdown
      const turndownService = new TurndownService({
        headingStyle: 'atx',
        codeBlockStyle: 'fenced'
      });
      turndownService.remove('img');

      return turndownService.turndown(html);
    },

    // ==================== 复制 ====================
    copyContent() {
      if (!this.extractedContent) {
        this.$message.warning('没有可复制的内容');
        return;
      }
      navigator.clipboard.writeText(this.extractedContent)
        .then(() => {
          this.$message.success('复制成功');
        })
        .catch(() => {
          this.$message.error('复制失败，请手动选择复制');
        });
    },

    // ==================== 工具函数 ====================
    formatFileSize(bytes) {
      if (bytes == null) return '-';
      if (bytes < 1024) return bytes + ' B';
      if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
      return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
    }
  }
};
</script>

<style scoped>
.text-extract-page {
  display: flex;
  gap: 16px;
  height: 100%;
  padding: 14px;
  box-sizing: border-box;
  overflow: hidden;
  background: linear-gradient(180deg, #f7f9ff 0%, #eef3ff 100%);
}

.card {
  background: #fff;
  border-radius: 14px;
  border: 1px solid #e8eefb;
  box-shadow: 0 8px 18px rgba(39, 66, 144, 0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ===== 左侧面板 ===== */
.left-panel {
  width: 320px;
  flex-shrink: 0;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid #f0f4ff;
}

.panel-header h3 {
  margin: 0;
  font-size: 16px;
  color: #23324a;
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid #edf2ff;
  border-radius: 10px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.file-item:hover {
  border-color: #cfe0ff;
  background: #fafcff;
}

.file-item.active {
  border-color: #77afff;
  background: #edf5ff;
}

.file-icon {
  font-size: 22px;
  flex-shrink: 0;
}

.file-info {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 13px;
  font-weight: 600;
  color: #263248;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-meta {
  display: flex;
  gap: 10px;
  font-size: 11px;
  color: #8a94a6;
  margin-top: 2px;
}

.file-type {
  color: #409eff;
}

.file-delete {
  font-size: 16px;
  color: #c0c4cc;
  flex-shrink: 0;
}

.file-delete:hover {
  color: #f56c6c;
}

.upload-hint {
  padding: 8px 16px;
  font-size: 11px;
  color: #b0b9c6;
  border-top: 1px solid #f0f4ff;
  text-align: center;
}

/* ===== 右侧面板 ===== */
.right-panel {
  flex: 1;
  min-width: 0;
}

.empty-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.content-wrap {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

/* ===== 工具栏 ===== */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f4ff;
  gap: 12px;
  flex-shrink: 0;
}

.toolbar-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #1f2d4d;
  min-width: 0;
  max-width: 320px;
}

.toolbar-title span {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

/* ===== 内容展示区 ===== */
.content-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.empty-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.empty-content i {
  font-size: 48px;
  margin-bottom: 12px;
}

.result-display {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.result-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px;
  border-bottom: 1px solid #f0f4ff;
  flex-shrink: 0;
}

.result-lines {
  font-size: 12px;
  color: #909399;
}

.code-wrapper {
  flex: 1;
  overflow: auto;
  padding: 16px;
  background: #1e1e1e;
}

.code-wrapper pre {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: 'Consolas', 'Courier New', 'Source Code Pro', monospace;
  font-size: 13px;
  line-height: 1.6;
  color: #d4d4d4;
}

.code-wrapper code {
  font-family: inherit;
}

@media (max-width: 1000px) {
  .text-extract-page {
    flex-direction: column;
    height: auto;
  }
  .left-panel {
    width: 100%;
    max-height: 280px;
  }
}
</style>
