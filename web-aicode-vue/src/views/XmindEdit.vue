<template>
  <div class="xmind-edit-page">
    <div class="edit-container">
      <!-- 左侧：文件列表 -->
      <div v-show="!isFullscreen" class="file-panel card">
        <div class="panel-header">
          <h3><i class="el-icon-folder-opened"></i> 我的导图</h3>
          <div class="panel-header-actions">
            <el-upload
              action=""
              :auto-upload="false"
              :show-file-list="false"
              :on-change="handleFileChange"
              accept=".xmind"
            >
              <el-button
                icon="el-icon-upload2"
                size="mini"
                circle
                title="上传 XMind"
              ></el-button>
            </el-upload>
            <el-button
              icon="el-icon-plus"
              size="mini"
              circle
              type="primary"
              title="新建 XMind"
              @click="createNewXmind"
            ></el-button>
            <el-button
              icon="el-icon-refresh"
              size="mini"
              circle
              title="刷新列表"
              @click="loadFileList"
            ></el-button>
          </div>
        </div>

        <div v-loading="listLoading" class="file-list">
          <div v-if="!listLoading && fileList.length === 0" class="no-file-tip">
            暂无文件，请上传或新建
          </div>
          <div
            v-for="(file, index) in fileList"
            :key="file.name"
            :class="['file-item', { active: selectedFileIndex === index }]"
            @click="selectFile(index)"
          >
            <i class="el-icon-document"></i>
            <div class="file-meta">
              <span class="file-name" :title="file.name">{{ file.name }}</span>
              <span class="file-size">{{ formatSize(file.size) }}</span>
            </div>
            <el-dropdown
              trigger="click"
              class="more-wrapper"
              @command="handleFileCommand($event, file)"
              @click.native.stop
            >
              <i class="el-icon-more more-btn"></i>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="download" icon="el-icon-download">下载</el-dropdown-item>
                <el-dropdown-item command="delete" icon="el-icon-delete">
                  <span style="color: #f56c6c;">删除</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </div>
        </div>
      </div>

      <!-- 右侧：预览区 -->
      <div :class="['preview-panel', 'card', { 'fullscreen-preview': isFullscreen }]">
        <div class="panel-header">
          <h3><i class="el-icon-view"></i> 预览</h3>
          <div class="panel-header-actions">
            <span v-if="selectedFile" class="current-file-tag">{{ selectedFile.name }}</span>
            <el-button
              type="primary"
              size="mini"
              icon="el-icon-view"
              :loading="previewLoading"
              @click="previewWithSimpleMindMap"
            >
              预览加载
            </el-button>
            <el-button
              v-if="!isFullscreen"
              icon="el-icon-full-screen"
              size="mini"
              circle
              type="primary"
              plain
              title="全屏预览"
              @click="enterFullscreen"
            ></el-button>
            <el-button
              v-if="isFullscreen"
              icon="el-icon-close"
              size="mini"
              circle
              type="text"
              title="退出全屏"
              style="color: #e6a23c;"
              @click="exitFullscreen"
            ></el-button>
          </div>
        </div>

        <div class="preview-body">
          <div v-show="previewMode === 'simple'" class="mind-map-wrapper">
            <div class="mind-map-toolbar">
              <el-button-group>
                <el-tooltip content="插入子节点 (Tab)">
                  <el-button size="mini" icon="el-icon-plus" @click="execCommand('INSERT_CHILD_NODE')"></el-button>
                </el-tooltip>
                <el-tooltip content="插入同级节点 (Enter)">
                  <el-button size="mini" icon="el-icon-video-play" @click="execCommand('INSERT_NODE')"></el-button>
                </el-tooltip>
                <el-tooltip content="删除节点 (Delete)">
                  <el-button size="mini" icon="el-icon-delete" @click="execCommand('REMOVE_NODE')"></el-button>
                </el-tooltip>
              </el-button-group>

              <el-button-group style="margin-left: 10px;">
                <el-tooltip content="撤销 (Ctrl+Z)">
                  <el-button size="mini" icon="el-icon-refresh-left" @click="execCommand('UNDO')"></el-button>
                </el-tooltip>
                <el-tooltip content="重做 (Ctrl+Y)">
                  <el-button size="mini" icon="el-icon-refresh-right" @click="execCommand('REDO')"></el-button>
                </el-tooltip>
              </el-button-group>

              <el-button-group style="margin-left: 10px;">
                <el-tooltip content="缩小">
                  <el-button size="mini" icon="el-icon-zoom-out" @click="changeViewScale('narrow')"></el-button>
                </el-tooltip>
                <el-tooltip content="放大">
                  <el-button size="mini" icon="el-icon-zoom-in" @click="changeViewScale('enlarge')"></el-button>
                </el-tooltip>
                <el-tooltip content="适配画布">
                  <el-button size="mini" icon="el-icon-full-screen" @click="fitMindMapView"></el-button>
                </el-tooltip>
                <el-tooltip content="重置视图">
                  <el-button size="mini" icon="el-icon-refresh" @click="resetMindMapView"></el-button>
                </el-tooltip>
              </el-button-group>

              <span class="zoom-text">{{ currentZoomText }}</span>

              <el-button
                type="success"
                size="mini"
                icon="el-icon-check"
                style="margin-left: auto;"
                :loading="saving"
                @click="saveMindMapData"
              >
                保存到原文件
              </el-button>
            </div>
            <div ref="mindMapContainer" class="mind-map-canvas"></div>
          </div>

          <div v-show="previewMode !== 'simple'" class="empty-preview">
            <i class="el-icon-monitor"></i>
            <p v-if="selectedFile">已选择「{{ selectedFile.name }}」，点击右上角「预览加载」按钮</p>
            <p v-else>请先在左侧选择或上传一个 XMind 文件</p>
          </div>
        </div>

        <!-- 底部：导出功能区 -->
        <div class="export-bar">
          <div class="export-label">
            <i class="el-icon-download"></i> 导出:
          </div>
          <el-select v-model="exportFormat" size="mini" placeholder="选择格式" style="width: 140px;">
            <el-option
              v-for="item in exportFormats"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            ></el-option>
          </el-select>
          <el-button
            type="success"
            size="mini"
            icon="el-icon-document-checked"
            @click="handleExport"
          >
            导出文件
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {
  listXmindFiles,
  createXmind,
  uploadXmind,
  downloadXmind,
  saveXmind,
  deleteXmind
} from '@/api/xmind-edit';
import { parseXmindToMap } from '@/utils/xmindHandler';
import JSZip from 'jszip';
import MindMap from 'simple-mind-map';
import Export from 'simple-mind-map/src/plugins/Export.js';
import Select from 'simple-mind-map/src/plugins/Select.js';
import Drag from 'simple-mind-map/src/plugins/Drag.js';
import KeyboardNavigation from 'simple-mind-map/src/plugins/KeyboardNavigation.js';

// 注册插件
MindMap.usePlugin(Export)
  .usePlugin(Select)
  .usePlugin(Drag)
  .usePlugin(KeyboardNavigation);

export default {
  name: 'XmindEdit',
  data() {
    return {
      fileList: [],
      listLoading: false,
      selectedFileIndex: -1,
      previewMode: '', // '' | 'simple'
      previewLoading: false,
      saving: false,
      isFullscreen: false,
      mindMapInstance: null,
      rawXmindBlob: null, // 当前预览文件的原始压缩包，保存时复用
      currentZoomText: '100%',
      exportFormat: 'png',
      exportFormats: [
        { label: 'PNG 图片', value: 'png' },
        { label: 'SVG 矢量图', value: 'svg' },
        { label: 'PDF 文档', value: 'pdf' },
        { label: 'XMind 文件', value: 'xmind' },
        { label: 'JSON 数据', value: 'json' },
        { label: 'Markdown', value: 'md' },
        { label: 'Text 文本', value: 'txt' }
      ]
    };
  },
  computed: {
    selectedFile() {
      return this.selectedFileIndex >= 0 ? this.fileList[this.selectedFileIndex] : null;
    }
  },
  mounted() {
    this.loadFileList();
    document.addEventListener('keydown', this.handleFullscreenKeydown);
  },
  beforeDestroy() {
    this.destroyMindMapInstance();
    document.removeEventListener('keydown', this.handleFullscreenKeydown);
  },
  methods: {
    // ---------------- 文件列表 ----------------
    async loadFileList() {
      this.listLoading = true;
      try {
        const res = await listXmindFiles();
        this.fileList = res.data || [];
        // 列表刷新后保持选中项的有效性
        if (this.selectedFileIndex >= this.fileList.length) {
          this.selectedFileIndex = this.fileList.length > 0 ? 0 : -1;
        }
      } catch (e) {
        this.$message.error('获取文件列表失败: ' + e.message);
      } finally {
        this.listLoading = false;
      }
    },
    formatSize(size) {
      if (!size) return '0 B';
      if (size < 1024) return size + ' B';
      if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB';
      return (size / 1024 / 1024).toFixed(2) + ' MB';
    },
    selectFile(index) {
      if (this.selectedFileIndex === index) return;
      this.selectedFileIndex = index;
      this.resetPreview();
    },
    handleFileCommand(command, file) {
      if (command === 'download') {
        this.downloadFile(file);
      } else if (command === 'delete') {
        this.deleteFile(file);
      }
    },

    // ---------------- 新建 / 上传 ----------------
    createNewXmind() {
      this.$prompt('请输入文件名', '新建 XMind', {
        confirmButtonText: '创建',
        cancelButtonText: '取消',
        inputPlaceholder: '例如：需求梳理',
        inputValidator: (value) => {
          if (!value || !value.trim()) return '文件名不能为空';
          if (/[\\/:*?"<>|]/.test(value)) return '文件名不能包含 \\ / : * ? " < > |';
          return true;
        }
      }).then(async ({ value }) => {
        try {
          const res = await createXmind(value.trim());
          this.$message.success('创建成功：' + res.data);
          await this.loadFileList();
          const idx = this.fileList.findIndex((f) => f.name === res.data);
          if (idx >= 0) this.selectFile(idx);
        } catch (e) {
          this.$message.error('创建失败: ' + e.message);
        }
      }).catch(() => {});
    },
    async handleFileChange(file) {
      const rawFile = file.raw;
      if (!rawFile) return;
      if (!/\.xmind$/i.test(rawFile.name)) {
        this.$message.error('仅支持 .xmind 文件');
        return;
      }
      try {
        const res = await uploadXmind(rawFile);
        this.$message.success('上传成功：' + res.data);
        await this.loadFileList();
        const idx = this.fileList.findIndex((f) => f.name === res.data);
        if (idx >= 0) this.selectFile(idx);
      } catch (e) {
        this.$message.error('上传失败: ' + e.message);
      }
    },

    // ---------------- 下载 / 删除 ----------------
    async fetchXmindBlob(fileName) {
      const res = await downloadXmind(fileName);
      const blob = res.data;
      // 后端错误会以 JSON 形式返回，需要识别
      if (blob && blob.type && blob.type.indexOf('application/json') > -1) {
        const text = await blob.text();
        let msg = '下载失败';
        try {
          msg = JSON.parse(text).message || msg;
        } catch (err) {
          // ignore
        }
        throw new Error(msg);
      }
      return blob;
    },
    async downloadFile(file) {
      try {
        const blob = await this.fetchXmindBlob(file.name);
        this.downloadBlob(blob, file.name);
        this.$message.success('已开始下载：' + file.name);
      } catch (e) {
        this.$message.error('下载失败: ' + e.message);
      }
    },
    deleteFile(file) {
      this.$confirm(`确定删除「${file.name}」吗？删除后不可恢复。`, '删除确认', {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deleteXmind(file.name);
          this.$message.success('已删除：' + file.name);
          if (this.selectedFile && this.selectedFile.name === file.name) {
            this.resetPreview();
            this.selectedFileIndex = -1;
          }
          await this.loadFileList();
        } catch (e) {
          this.$message.error('删除失败: ' + e.message);
        }
      }).catch(() => {});
    },

    // ---------------- 预览 ----------------
    resetPreview() {
      this.previewMode = '';
      this.rawXmindBlob = null;
      this.destroyMindMapInstance();
    },
    async previewWithSimpleMindMap() {
      if (!this.selectedFile) {
        this.$message.warning('请先在左侧选择或上传一个 XMind 文件');
        return;
      }
      const targetName = this.selectedFile.name;
      this.previewLoading = true;
      try {
        const blob = await this.fetchXmindBlob(targetName);
        const data = await parseXmindToMap(blob);
        this.rawXmindBlob = blob;
        this.previewMode = 'simple';

        this.$nextTick(() => {
          this.destroyMindMapInstance();
          this.mindMapInstance = new MindMap({
            el: this.$refs.mindMapContainer,
            data
          });
          this.updateZoomText();
        });
      } catch (e) {
        console.error('加载 simple-mind-map 失败:', e);
        this.$message.error('加载失败: ' + e.message);
        this.previewMode = '';
        this.rawXmindBlob = null;
      } finally {
        this.previewLoading = false;
      }
    },
    destroyMindMapInstance() {
      if (this.mindMapInstance) {
        this.mindMapInstance.destroy();
        this.mindMapInstance = null;
      }
      this.currentZoomText = '100%';
    },
    execCommand(command, ...args) {
      if (this.mindMapInstance) {
        this.mindMapInstance.execCommand(command, ...args);
      }
    },
    updateZoomText() {
      if (this.mindMapInstance && this.mindMapInstance.view) {
        const scale = this.mindMapInstance.view.scale || 1;
        this.currentZoomText = `${Math.round(scale * 100)}%`;
      } else {
        this.currentZoomText = '100%';
      }
    },
    changeViewScale(type) {
      if (!this.mindMapInstance || !this.mindMapInstance.view) return;
      if (type === 'enlarge') {
        this.mindMapInstance.view.enlarge();
      } else {
        this.mindMapInstance.view.narrow();
      }
      this.updateZoomText();
    },
    fitMindMapView() {
      if (!this.mindMapInstance || !this.mindMapInstance.view) return;
      this.mindMapInstance.view.fit();
      this.updateZoomText();
    },
    resetMindMapView() {
      if (!this.mindMapInstance || !this.mindMapInstance.view) return;
      this.mindMapInstance.view.reset();
      this.updateZoomText();
    },

    // ---------------- 保存到后台 ----------------
    createTopicId() {
      return `topic-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
    },
    mergeTopicWithTreeData(existingTopic, node) {
      const baseTopic = existingTopic && typeof existingTopic === 'object' ? existingTopic : {};
      const data = (node && node.data) || {};
      const currentChildren = (baseTopic.children && baseTopic.children.attached) || [];
      const editedChildren = Array.isArray(node && node.children) ? node.children : [];
      const mergedChildren = editedChildren.map((child, index) =>
        this.mergeTopicWithTreeData(currentChildren[index], child)
      );

      const mergedTopic = {
        ...baseTopic,
        id: baseTopic.id || this.createTopicId(),
        title: data.text || baseTopic.title || '未命名主题'
      };

      if (data.note) {
        mergedTopic.notes = { plain: { content: data.note } };
      } else {
        delete mergedTopic.notes;
      }

      const childrenObj = { ...(baseTopic.children || {}) };
      if (mergedChildren.length > 0) {
        childrenObj.attached = mergedChildren;
        mergedTopic.children = childrenObj;
      } else if (Object.keys(childrenObj).length > 0) {
        delete childrenObj.attached;
        mergedTopic.children = Object.keys(childrenObj).length > 0 ? childrenObj : undefined;
      } else {
        delete mergedTopic.children;
      }

      return mergedTopic;
    },
    async buildEditedXmindBlob() {
      if (!this.rawXmindBlob) {
        throw new Error('缺少原始文件数据，请重新加载预览');
      }
      const zip = new JSZip();
      const zipContent = await zip.loadAsync(this.rawXmindBlob);
      const contentFile = zipContent.file('content.json');
      if (!contentFile) {
        throw new Error('原文件中未找到 content.json');
      }
      const rawModelData = JSON.parse(await contentFile.async('string'));
      if (!Array.isArray(rawModelData) || rawModelData.length === 0) {
        throw new Error('原文件内容结构不合法');
      }
      const mapData = this.mindMapInstance.getData();
      const existingRootTopic = rawModelData[0].rootTopic || {};
      rawModelData[0].rootTopic = this.mergeTopicWithTreeData(existingRootTopic, mapData);

      zipContent.file('content.json', JSON.stringify(rawModelData, null, 2));
      return zipContent.generateAsync({ type: 'blob' });
    },
    async saveMindMapData() {
      if (!this.mindMapInstance || !this.selectedFile) {
        this.$message.warning('当前没有可保存的导图');
        return;
      }
      this.saving = true;
      const fileName = this.selectedFile.name;
      try {
        const blob = await this.buildEditedXmindBlob();
        await saveXmind(fileName, blob);
        this.rawXmindBlob = blob;
        this.$message.success('已保存到后台：' + fileName);
        await this.loadFileList();
      } catch (e) {
        console.error('保存失败:', e);
        this.$message.error('保存失败: ' + e.message);
      } finally {
        this.saving = false;
      }
    },

    // ---------------- 导出画布 ----------------
    async handleExport() {
      if (!this.mindMapInstance) {
        this.$message.warning('思维导图实例未加载');
        return;
      }
      const baseName = this.selectedFile
        ? this.selectedFile.name.replace(/\.xmind$/i, '')
        : 'mindmap';
      try {
        if (this.exportFormat === 'xmind') {
          // 导出 xmind 时基于当前画布数据重新打包（不落后台）
          const blob = await this.buildEditedXmindBlob();
          this.downloadBlob(blob, baseName + '.xmind');
          this.$message.success('已导出 XMind 文件');
          return;
        }

        const exportMethod = this.mindMapInstance.export
          || (this.mindMapInstance.doExport && this.mindMapInstance.doExport.export);
        if (typeof exportMethod !== 'function') {
          this.$message.error('当前思维导图组件未加载导出插件或不支持导出');
          return;
        }
        this.$message.info(`正在准备导出 ${this.exportFormat.toUpperCase()}...`);
        if (this.mindMapInstance.doExport && typeof this.mindMapInstance.doExport.export === 'function') {
          await this.mindMapInstance.doExport.export(this.exportFormat, true, baseName);
        } else {
          await this.mindMapInstance.export(this.exportFormat, true, baseName);
        }
        this.$message.success('导出指令已发送');
      } catch (e) {
        console.error('导出失败:', e);
        this.$message.error('导出失败: ' + e.message);
      }
    },
    downloadBlob(blob, fileName) {
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = fileName;
      link.click();
      URL.revokeObjectURL(url);
    },

    // ---------------- 全屏预览 ----------------
    enterFullscreen() {
      if (this.previewMode !== 'simple') {
        this.$message.warning('请先加载预览后再全屏');
        return;
      }
      this.isFullscreen = true;
      this.$nextTick(() => {
        setTimeout(() => {
          if (this.mindMapInstance) {
            if (typeof this.mindMapInstance.resize === 'function') {
              this.mindMapInstance.resize();
            }
            if (this.mindMapInstance.view && typeof this.mindMapInstance.view.fit === 'function') {
              this.mindMapInstance.view.fit();
            }
          }
          this.updateZoomText();
        }, 200);
      });
    },
    exitFullscreen() {
      this.isFullscreen = false;
      this.$nextTick(() => {
        setTimeout(() => {
          if (this.mindMapInstance) {
            if (typeof this.mindMapInstance.resize === 'function') {
              this.mindMapInstance.resize();
            }
            if (this.mindMapInstance.view && typeof this.mindMapInstance.view.fit === 'function') {
              this.mindMapInstance.view.fit();
            }
          }
          this.updateZoomText();
        }, 200);
      });
    },
    handleFullscreenKeydown(e) {
      if (e.key === 'Escape' && this.isFullscreen) {
        this.exitFullscreen();
      }
    }
  }
};
</script>

<style scoped>
.xmind-edit-page {
  padding: 20px;
  height: 100%;
  background: linear-gradient(180deg, #f5f8ff 0%, #eef3fb 100%);
  box-sizing: border-box;
  overflow: hidden;
}

.edit-container {
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

.file-panel {
  width: 300px;
  flex-shrink: 0;
}

.preview-panel {
  flex: 1;
  min-width: 0;
}

/* 全屏预览：预览卡片铺满视口 */
.preview-panel.fullscreen-preview {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 2000;
  border-radius: 0;
  margin: 0;
  border: none;
}

.panel-header {
  padding: 14px 16px;
  border-bottom: 1px solid #f0f4f8;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}

.panel-header h3 {
  margin: 0;
  font-size: 16px;
  color: #1f2d3d;
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-header-actions .el-button + .el-button {
  margin-left: 0;
}

.current-file-tag {
  font-size: 12px;
  background: #409eff;
  color: #fff;
  padding: 2px 8px;
  border-radius: 10px;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 8px;
}

.no-file-tip {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 13px;
}

.file-item {
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #606266;
  transition: all 0.2s;
  margin-bottom: 2px;
}

.file-item:hover {
  background: #f5f7fa;
}

.file-item.active {
  background: #ecf5ff;
  color: #409eff;
}

.file-item.active .file-name {
  font-weight: bold;
}

.file-meta {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.file-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  font-size: 11px;
  color: #a8abb2;
}

.more-wrapper {
  flex-shrink: 0;
}

.more-btn {
  visibility: hidden;
  color: #909399;
  font-size: 16px;
  padding: 2px 4px;
  border-radius: 4px;
  transition: all 0.2s;
}

.file-item:hover .more-btn {
  visibility: visible;
}

.more-btn:hover {
  color: #409eff;
  background: #e6f0ff;
}

.preview-body {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  position: relative;
}

.mind-map-wrapper {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  overflow: hidden;
}

.mind-map-toolbar {
  padding: 8px 15px;
  background: #f8f9fb;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  flex-shrink: 0;
  z-index: 10;
}

.zoom-text {
  margin-left: 10px;
  font-size: 12px;
  color: #606266;
  min-width: 44px;
}

.mind-map-canvas {
  flex: 1;
  min-height: 0;
  width: 100%;
  background: #fff;
}

.empty-preview {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #c0c4cc;
}

.empty-preview i {
  font-size: 40px;
  margin-bottom: 10px;
}

.empty-preview p {
  margin: 4px 0;
  font-size: 14px;
}

.export-bar {
  padding: 10px 20px;
  background: #ffffff;
  border-top: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);
  flex-shrink: 0;
  z-index: 10;
}

.export-label {
  font-size: 13px;
  color: #606266;
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 5px;
}

@media (max-width: 1000px) {
  .edit-container {
    flex-direction: column;
  }
  .file-panel {
    width: 100%;
    height: 40%;
  }
}
</style>
