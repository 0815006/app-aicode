<template>
  <div class="xmind-converter-page">
    <div class="converter-container">
      <!-- 左侧：上传与预览区域 -->
      <div class="upload-section card">
        <div class="section-header">
          <h3><i class="el-icon-upload"></i> XMind 管理与预览</h3>
          <el-button 
            v-if="uploadedFiles.length > 0" 
            type="text" 
            icon="el-icon-delete" 
            @click="clearFiles"
            style="color: #f56c6c"
          >
            清空列表
          </el-button>
        </div>
        
        <div class="upload-content">
          <!-- 上方：上传按钮与文件列表 -->
          <div class="upload-top-bar">
            <div class="upload-btn-wrapper">
              <el-upload
                action=""
                :auto-upload="false"
                :on-change="handleFileChange"
                :show-file-list="false"
                accept=".xmind"
              >
                <el-button type="primary" icon="el-icon-plus">上传 XMind</el-button>
              </el-upload>
            </div>
            
            <div class="file-list-container">
              <div v-if="uploadedFiles.length === 0" class="no-file-tip">
                暂无上传文件
              </div>
              <div 
                v-for="(file, index) in uploadedFiles" 
                :key="index"
                :class="['file-item', { active: selectedFileIndex === index }]"
                @click="selectFile(index)"
              >
                <i class="el-icon-document"></i>
                <span class="file-name" :title="file.name">{{ file.name }}</span>
                <i v-if="selectedFileIndex === index" class="el-icon-check"></i>
              </div>
            </div>
          </div>

          <!-- 中间：操作区 -->
          <div class="action-bar">
            <el-button 
              type="info" 
              size="small" 
              icon="el-icon-view" 
              @click="previewWithSimpleMindMap"
              :disabled="!selectedFile"
            >
              simple-mind-map 预览
            </el-button>
            <el-button 
              type="info" 
              size="small" 
              icon="el-icon-data-analysis" 
              @click="previewWithXmindModel"
              :disabled="!selectedFile"
            >
              xmind-model 预览
            </el-button>
          </div>

          <!-- 下方：XMind 内容预览 -->
          <div class="preview-container">
            <div class="preview-header">
              <span><i class="el-icon-view"></i> 内容预览 ({{ previewModeName }})</span>
              <span v-if="selectedFile" class="current-file-tag">{{ selectedFile.name }}</span>
            </div>
            <div class="preview-body">
              <!-- Tree 预览 (默认) -->
              <el-tree
                v-if="previewMode === 'tree' && previewData"
                :data="[previewData]"
                :props="defaultProps"
                default-expand-all
                class="xmind-tree"
              >
                <span class="custom-tree-node" slot-scope="{ data }">
                  <i class="el-icon-set-up"></i>
                  <span>{{ data.data.text }}</span>
                  <el-tooltip v-if="data.data.note" :content="data.data.note" placement="top">
                    <i class="el-icon-notebook-2 note-icon"></i>
                  </el-tooltip>
                </span>
              </el-tree>

              <!-- Simple Mind Map 预览 -->
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
                    @click="saveMindMapData"
                  >
                    保存到原文件
                  </el-button>
                </div>
                <div 
                  id="mindMapContainer" 
                  ref="mindMapContainer"
                  class="mind-map-canvas"
                ></div>
              </div>

              <!-- XMind Model 预览 (展示模型信息) -->
              <div v-if="previewMode === 'model'" class="model-info">
                <el-alert
                  title="xmind-model 已加载工作簿模型"
                  type="success"
                  :closable="false"
                  show-icon
                  style="margin-bottom: 15px;"
                >
                  <div v-if="workbookInfo">
                    <p><strong>画布数量:</strong> {{ workbookInfo.sheetCount }}</p>
                    <p><strong>主画布标题:</strong> {{ workbookInfo.primarySheetTitle }}</p>
                    <p><strong>根节点标题:</strong> {{ workbookInfo.rootTopicTitle }}</p>
                  </div>
                </el-alert>

                <!-- 渲染模型树结构 -->
                <div v-if="workbookSheets.length > 0" class="model-tree-container">
                  <div class="tree-header">
                    <div class="tree-title"><i class="el-icon-s-operation"></i> 模型结构预览:</div>
                    <el-radio-group v-model="showRawModel" size="mini">
                      <el-radio-button :label="false">树形视图</el-radio-button>
                      <el-radio-button :label="true">原始 JSON</el-radio-button>
                    </el-radio-group>
                  </div>

                  <div v-if="!showRawModel">
                    <div class="model-view-mode">
                      <el-radio-group v-model="modelViewMode" size="mini">
                        <el-radio-button label="tree">树形视图</el-radio-button>
                        <el-radio-button label="mindmap">可编辑导图</el-radio-button>
                      </el-radio-group>
                      <el-select
                        v-model="activeSheetIndex"
                        size="mini"
                        placeholder="选择画布"
                        style="width: 220px; margin-left: 10px;"
                      >
                        <el-option
                          v-for="(sheet, index) in workbookSheets"
                          :key="`sheet-option-${index}`"
                          :label="sheet.title"
                          :value="index.toString()"
                        ></el-option>
                      </el-select>
                    </div>

                    <div v-if="modelViewMode === 'mindmap'" class="mind-map-wrapper model-mind-map-wrapper">
                      <div class="mind-map-toolbar">
                        <el-button-group>
                          <el-tooltip content="插入子节点 (Tab)">
                            <el-button size="mini" icon="el-icon-plus" @click="execModelCommand('INSERT_CHILD_NODE')"></el-button>
                          </el-tooltip>
                          <el-tooltip content="插入同级节点 (Enter)">
                            <el-button size="mini" icon="el-icon-video-play" @click="execModelCommand('INSERT_NODE')"></el-button>
                          </el-tooltip>
                          <el-tooltip content="删除节点 (Delete)">
                            <el-button size="mini" icon="el-icon-delete" @click="execModelCommand('REMOVE_NODE')"></el-button>
                          </el-tooltip>
                        </el-button-group>

                        <el-button-group style="margin-left: 10px;">
                          <el-tooltip content="撤销 (Ctrl+Z)">
                            <el-button size="mini" icon="el-icon-refresh-left" @click="execModelCommand('UNDO')"></el-button>
                          </el-tooltip>
                          <el-tooltip content="重做 (Ctrl+Y)">
                            <el-button size="mini" icon="el-icon-refresh-right" @click="execModelCommand('REDO')"></el-button>
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
                          @click="saveModelMindMapData"
                        >
                          保存到原文件
                        </el-button>
                      </div>
                      <div
                        id="modelMindMapContainer"
                        ref="modelMindMapContainer"
                        class="mind-map-canvas"
                      ></div>
                    </div>

                    <el-tabs v-else v-model="activeSheetIndex">
                      <el-tab-pane 
                        v-for="(sheet, index) in workbookSheets" 
                        :key="index" 
                        :label="sheet.title" 
                        :name="index.toString()"
                      >
                        <el-tree
                          v-if="sheet.treeData"
                          :data="[sheet.treeData]"
                          :props="defaultProps"
                          default-expand-all
                          class="xmind-tree"
                        >
                          <span class="custom-tree-node" slot-scope="{ data }">
                            <i class="el-icon-set-up"></i>
                            <span>{{ data.data.text }}</span>
                            <el-tag v-for="label in data.data.labels" :key="label" size="mini" type="info" style="margin-left: 5px;">{{ label }}</el-tag>
                            <el-tooltip v-if="data.data.notes" :content="data.data.notes" placement="top">
                              <i class="el-icon-notebook-2 note-icon"></i>
                            </el-tooltip>
                          </span>
                        </el-tree>
                        <div v-else class="empty-sheet">该画布无内容</div>
                      </el-tab-pane>
                    </el-tabs>
                  </div>
                  
                  <div v-else class="raw-model-json">
                    <pre><code>{{ JSON.stringify(rawModelData, null, 2) }}</code></pre>
                  </div>
                </div>
              </div>

              <div v-if="!previewMode" class="empty-preview">
                <i class="el-icon-monitor"></i>
                <p>选择文件并点击预览按钮</p>
              </div>
            </div>

            <!-- Simple Mind Map 导出功能区 -->
            <div v-if="showExportBar" class="export-bar">
              <div class="export-label">
                <i class="el-icon-download"></i> 导出:
              </div>
              <el-select v-model="exportFormat" size="mini" placeholder="选择格式" style="width: 120px;">
                <el-option
                  v-for="item in exportFormats"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                ></el-option>
              </el-select>
              <el-button 
                type="primary" 
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

      <!-- 右侧：转换控制与 JSON 输出区 -->
      <div class="output-section card">
        <div class="section-header">
          <h3><i class="el-icon-document"></i> 转换与输出</h3>
          <el-button 
            v-if="jsonResult" 
            type="success" 
            size="mini" 
            icon="el-icon-download"
            @click="downloadJson"
          >
            下载 JSON
          </el-button>
        </div>
        
        <div class="output-content">
          <!-- 转换控制区 (挪到右侧上方) -->
          <div class="convert-controls">
            <div class="method-select">
              <label>转换方式：</label>
              <el-select v-model="convertMethod" placeholder="请选择" size="small">
                <el-option label="纯前端转换 (JSZip)" value="frontend"></el-option>
                <el-option label="后端转换 (Java Native)" value="backend"></el-option>
              </el-select>
            </div>
            
            <el-button 
              type="primary" 
              icon="el-icon-refresh" 
              size="small"
              class="convert-btn"
              :loading="converting"
              @click="handleConvert"
              :disabled="!selectedFile"
            >
              开始转换
            </el-button>
          </div>

          <!-- JSON 展示区 -->
          <div class="json-wrapper">
            <div v-if="jsonResult" class="json-display">
              <pre><code>{{ jsonResult }}</code></pre>
            </div>
            <div v-else class="empty-tip">
              <i class="el-icon-info"></i>
              <p>转换后的内容将在此处展示</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { parseXmindToMap } from '@/utils/xmindHandler';
import { convertXmind } from '@/api/xmind';
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

import * as XMindModel from 'xmind-model';

export default {
  name: 'XmindConverter',
  data() {
    return {
      uploadedFiles: [],
      selectedFileIndex: -1,
      convertMethod: 'frontend',
      converting: false,
      jsonResult: null,
      previewMode: '', // 'tree', 'simple', 'model'
      mindMapInstance: null,
      workbookInfo: null,
      modelTreeData: null,
      workbookSheets: [],
      activeSheetIndex: '0',
      showRawModel: false,
      modelViewMode: 'tree',
      rawModelData: null,
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
      ],
      defaultProps: {
        children: 'children',
        label: (data) => data.data.text
      }
    };
  },
  computed: {
    selectedFile() {
      return this.selectedFileIndex >= 0 ? this.uploadedFiles[this.selectedFileIndex] : null;
    },
    previewData() {
      return this.selectedFile ? this.selectedFile.data : null;
    },
    previewModeName() {
      const modes = {
        'tree': '树形结构',
        'simple': 'Simple Mind Map',
        'model': 'XMind Model'
      };
      return modes[this.previewMode] || '未预览';
    },
    showExportBar() {
      return this.previewMode === 'simple' || (this.previewMode === 'model' && !this.showRawModel && this.modelViewMode === 'mindmap');
    }
  },
  watch: {
    modelViewMode(val) {
      if (this.previewMode !== 'model') return;
      if (val === 'mindmap') {
        this.initModelMindMap();
      } else {
        this.destroyMindMapInstance();
      }
    },
    showRawModel(val) {
      if (this.previewMode !== 'model') return;
      if (val) {
        this.destroyMindMapInstance();
      } else if (this.modelViewMode === 'mindmap') {
        this.initModelMindMap();
      }
    },
    activeSheetIndex() {
      if (this.previewMode === 'model' && this.modelViewMode === 'mindmap') {
        this.initModelMindMap();
      }
    }
  },
  methods: {
    async handleFileChange(file) {
      const rawFile = file.raw;
      try {
        const data = await parseXmindToMap(rawFile);
        const rawModelData = await this.loadRawModelData(rawFile);
        this.uploadedFiles.unshift({
          name: rawFile.name,
          size: rawFile.size,
          raw: rawFile,
          data: data,
          rawModelData
        });
        this.selectedFileIndex = 0;
        this.jsonResult = null;
        this.previewMode = ''; // 上传新文件后重置预览
        this.$message.success('文件上传成功，请点击预览按钮查看内容');
      } catch (error) {
        console.error('解析失败:', error);
        this.$message.error('解析失败: ' + error.message);
      }
    },
    selectFile(index) {
      this.selectedFileIndex = index;
      this.jsonResult = null;
      this.previewMode = ''; // 切换文件后重置预览
      this.modelViewMode = 'tree';
      this.destroyMindMapInstance();
    },
    clearFiles() {
      this.uploadedFiles = [];
      this.selectedFileIndex = -1;
      this.jsonResult = null;
      this.previewMode = '';
      this.modelViewMode = 'tree';
      this.destroyMindMapInstance();
    },
    async previewWithSimpleMindMap() {
      if (!this.selectedFile) return;
      
      this.previewMode = 'simple';
      this.modelViewMode = 'tree';
      
      try {
        this.$nextTick(() => {
          this.destroyMindMapInstance();
          
          this.mindMapInstance = new MindMap({
            el: this.$refs.mindMapContainer,
            data: this.selectedFile.data
          });
          this.updateZoomText();
        });
      } catch (e) {
        console.error('加载 simple-mind-map 失败:', e);
        this.$message.error('加载预览组件失败: ' + e.message);
        this.previewMode = 'tree'; // 回退到树形预览
      }
    },
    async previewWithXmindModel() {
      if (!this.selectedFile) return;
      
      this.previewMode = 'model';
      this.modelTreeData = null;
      this.workbookSheets = [];
      this.activeSheetIndex = '0';
      this.showRawModel = false;
      this.modelViewMode = 'tree';
      this.destroyMindMapInstance();
      
      try {
        const rawData = await this.ensureSelectedFileRawModelData();
        this.rawModelData = rawData;

        // 极其灵活的 Workbook 解析
        let Workbook = XMindModel.Workbook;
        if (!Workbook && XMindModel.default) {
          Workbook = XMindModel.default.Workbook || XMindModel.default;
        }
        if (!Workbook) {
          Workbook = XMindModel;
        }
        
        console.log('Workbook resolved as:', typeof Workbook);

        let workbook = new Workbook(rawData);
        const sheets = typeof workbook.getSheets === 'function' ? workbook.getSheets() : workbook.sheets;
        
        if (sheets && sheets.length > 0) {
          this.workbookSheets = sheets.map((sheet, index) => {
            const rootTopic = sheet.rootTopic || (sheet.getRootTopic && sheet.getRootTopic());
            return {
              title: sheet.title || (sheet.getTitle && sheet.getTitle()) || `画布 ${index + 1}`,
              treeData: rootTopic ? this.transformModelTopic(rootTopic) : null
            };
          });

          this.workbookInfo = {
            sheetCount: sheets.length,
            primarySheetTitle: this.workbookSheets[0].title,
            rootTopicTitle: (sheets[0].rootTopic || (sheets[0].getRootTopic && sheets[0].getRootTopic())) ? (sheets[0].rootTopic ? sheets[0].rootTopic.title : sheets[0].getRootTopic().getTitle()) : '无根节点'
          };

          this.modelTreeData = this.workbookSheets[0].treeData;
        }
      } catch (e) {
        console.error('加载 xmind-model 失败:', e);
        this.$message.error('加载模型组件失败: ' + e.message);
        this.previewMode = 'tree';
      }
    },
    transformModelTopic(topic) {
      if (!topic) return null;
      
      const title = topic.title || (topic.getTitle && topic.getTitle()) || '未命名主题';
      
      // 提取备注
      let notes = '';
      if (topic.getNotes) {
        const n = topic.getNotes();
        notes = typeof n === 'string' ? n : (n && n.plain ? n.plain.content : '');
      } else if (topic.notes) {
        notes = typeof topic.notes === 'string' ? topic.notes : (topic.notes.plain ? topic.notes.plain.content : '');
      }

      // 提取标签
      const labels = topic.labels || (topic.getLabels && topic.getLabels()) || [];

      let attachedChildren = [];

      // 兼容 xmind-model 的 Topic 实例：优先使用模型方法拿真实子节点
      if (typeof topic.getChildrenByType === 'function') {
        attachedChildren = topic.getChildrenByType('attached') || [];
      } else {
        const childrenSource = topic.children || (topic.getChildren && topic.getChildren()) || [];

        if (Array.isArray(childrenSource)) {
          attachedChildren = childrenSource;
        } else if (childrenSource && childrenSource.attached) {
          attachedChildren = childrenSource.attached;
        } else if (childrenSource && childrenSource.children && childrenSource.children.attached) {
          // getChildren() 在部分版本会返回完整 topicData，需要再下钻一层
          attachedChildren = childrenSource.children.attached;
        } else if (childrenSource && typeof childrenSource === 'object') {
          // 兜底：把 children 下所有数组类型子节点合并（避免结构差异导致丢节点）
          const merged = [];
          Object.keys(childrenSource).forEach((key) => {
            if (Array.isArray(childrenSource[key])) {
              merged.push(...childrenSource[key]);
            }
          });
          attachedChildren = merged;
        }
      }

      return {
        data: {
          text: title,
          expand: true,
          notes: notes,
          labels: labels
        },
        children: attachedChildren.map(child => this.transformModelTopic(child)).filter(c => c !== null)
      };
    },
    transformTreeDataToXmindTopic(node) {
      if (!node) return null;
      const data = node.data || {};
      const attachedChildren = Array.isArray(node.children)
        ? node.children.map(child => this.transformTreeDataToXmindTopic(child)).filter(Boolean)
        : [];

      return {
        title: data.text || '未命名主题',
        notes: data.notes ? { plain: { content: data.notes } } : undefined,
        labels: Array.isArray(data.labels) ? data.labels : [],
        children: attachedChildren.length > 0 ? { attached: attachedChildren } : undefined
      };
    },
    createTopicId() {
      return `topic-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
    },
    mergeTopicWithTreeData(existingTopic, node) {
      const baseTopic = existingTopic && typeof existingTopic === 'object' ? existingTopic : {};
      const data = (node && node.data) || {};
      const currentChildren = (baseTopic.children && baseTopic.children.attached) || [];
      const editedChildren = Array.isArray(node && node.children) ? node.children : [];
      const mergedChildren = editedChildren.map((child, index) => this.mergeTopicWithTreeData(currentChildren[index], child));

      const mergedTopic = {
        ...baseTopic,
        id: baseTopic.id || data.id || this.createTopicId(),
        title: data.text || baseTopic.title || '未命名主题',
        labels: Array.isArray(data.labels) ? data.labels : (baseTopic.labels || [])
      };

      if (data.notes) {
        mergedTopic.notes = { plain: { content: data.notes } };
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
    async loadRawModelData(rawFile) {
      const zip = new JSZip();
      const zipContent = await zip.loadAsync(rawFile);
      const jsonFile = zipContent.file('content.json');
      if (!jsonFile) {
        throw new Error('不支持旧版 Xmind (XML)，请使用新版文件');
      }
      const rawDataStr = await jsonFile.async('string');
      return JSON.parse(rawDataStr);
    },
    async ensureSelectedFileRawModelData() {
      if (!this.selectedFile) return null;
      if (!this.selectedFile.rawModelData) {
        const rawModelData = await this.loadRawModelData(this.selectedFile.raw);
        this.$set(this.selectedFile, 'rawModelData', rawModelData);
      }
      return this.selectedFile.rawModelData;
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
    destroyMindMapInstance() {
      if (this.mindMapInstance) {
        this.mindMapInstance.destroy();
        this.mindMapInstance = null;
      }
      this.updateZoomText();
    },
    initModelMindMap() {
      if (this.previewMode !== 'model' || this.modelViewMode !== 'mindmap') return;
      const sheet = this.workbookSheets[Number(this.activeSheetIndex)];
      if (!sheet || !sheet.treeData) return;

      this.$nextTick(() => {
        this.destroyMindMapInstance();
        this.mindMapInstance = new MindMap({
          el: this.$refs.modelMindMapContainer,
          data: sheet.treeData
        });
        this.updateZoomText();
      });
    },
    execModelCommand(command, ...args) {
      if (this.previewMode !== 'model' || this.modelViewMode !== 'mindmap') {
        this.$message.warning('请先切换到可编辑导图视图');
        return;
      }
      this.execCommand(command, ...args);
    },
    async saveModelMindMapData() {
      if (!this.mindMapInstance || this.previewMode !== 'model') {
        this.$message.warning('当前没有可保存的导图实例');
        return;
      }

      const currentSheetIndex = Number(this.activeSheetIndex);
      const data = this.mindMapInstance.getData();
      const currentSheet = this.workbookSheets[currentSheetIndex];
      if (!currentSheet) return;

      this.$set(this.workbookSheets, currentSheetIndex, {
        ...currentSheet,
        treeData: data
      });

      if (Array.isArray(this.rawModelData) && this.rawModelData[currentSheetIndex]) {
        const existingRootTopic = this.rawModelData[currentSheetIndex].rootTopic || {};
        this.rawModelData[currentSheetIndex] = {
          ...this.rawModelData[currentSheetIndex],
          rootTopic: this.mergeTopicWithTreeData(existingRootTopic, data)
        };
      }

      if (currentSheetIndex === 0 && this.workbookInfo) {
        this.workbookInfo = {
          ...this.workbookInfo,
          rootTopicTitle: data && data.data ? data.data.text : this.workbookInfo.rootTopicTitle
        };
      }

      await this.saveCurrentAsOriginalXmind(false);
    },
    async handleConvert() {
      if (!this.selectedFile) {
        this.$message.warning('请先选择 XMind 文件');
        return;
      }

      this.converting = true;
      try {
        if (this.convertMethod === 'frontend') {
          setTimeout(() => {
            this.jsonResult = JSON.stringify(this.selectedFile.data, null, 2);
            this.converting = false;
            this.$message.success('前端转换成功');
          }, 300);
        } else {
          const res = await convertXmind(this.selectedFile.raw);
          if (res.code === 200) {
            this.jsonResult = JSON.stringify(res.data, null, 2);
            this.$message.success('后端转换成功');
          } else {
            this.$message.error(res.message || '后端转换失败');
          }
          this.converting = false;
        }
      } catch (error) {
        console.error('转换失败:', error);
        this.$message.error('转换失败: ' + error.message);
        this.converting = false;
      }
    },
    downloadJson() {
      if (!this.selectedFile) return;
      const dataStr = JSON.stringify(this.selectedFile.data, null, 2);
      const blob = new Blob([dataStr], { type: "application/json" });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = this.selectedFile.name.replace('.xmind', '.json');
      link.click();
      URL.revokeObjectURL(url);
    },
    async handleExport() {
      if (!this.mindMapInstance) {
        this.$message.warning('思维导图实例未加载');
        return;
      }
      
      try {
        const fileName = this.selectedFile ? this.selectedFile.name.replace('.xmind', '') : 'mindmap';
        if (this.exportFormat === 'xmind') {
          await this.saveCurrentAsOriginalXmind(true);
          return;
        }
        
        // simple-mind-map 的导出插件通常将方法挂载在 doExport 属性上
        const exportMethod = this.mindMapInstance.export || (this.mindMapInstance.doExport && this.mindMapInstance.doExport.export);
        
        if (typeof exportMethod === 'function') {
          this.$message.info(`正在准备导出 ${this.exportFormat.toUpperCase()}...`);
          // 注意：如果是通过 doExport 调用，可能需要绑定上下文
          if (this.mindMapInstance.doExport && typeof this.mindMapInstance.doExport.export === 'function') {
            await this.mindMapInstance.doExport.export(this.exportFormat, true, fileName);
          } else {
            await this.mindMapInstance.export(this.exportFormat, true, fileName);
          }
          this.$message.success(`导出指令已发送`);
        } else {
          this.$message.error('当前思维导图组件未加载导出插件或不支持导出');
        }
      } catch (e) {
        console.error('导出失败:', e);
        this.$message.error('导出失败: ' + e.message);
      }
    },
    execCommand(command, ...args) {
      if (this.mindMapInstance) {
        this.mindMapInstance.execCommand(command, ...args);
      }
    },
    async saveMindMapData() {
      if (this.mindMapInstance) {
        const data = this.mindMapInstance.getData();
        if (this.selectedFile) {
          this.selectedFile.data = data;
          const rawModelData = await this.ensureSelectedFileRawModelData();
          if (Array.isArray(rawModelData) && rawModelData[0]) {
            const existingRootTopic = rawModelData[0].rootTopic || {};
            rawModelData[0] = {
              ...rawModelData[0],
              rootTopic: this.mergeTopicWithTreeData(existingRootTopic, data)
            };
          }
          await this.saveCurrentAsOriginalXmind(false);
        }
      }
    },
    async saveCurrentAsOriginalXmind(asNewFile = false) {
      if (!this.selectedFile) return;

      const rawModelData = await this.ensureSelectedFileRawModelData();
      if (!Array.isArray(rawModelData) || rawModelData.length === 0) {
        this.$message.warning('未找到可保存的模型数据');
        return;
      }

      const zip = new JSZip();
      const zipContent = await zip.loadAsync(this.selectedFile.raw);
      zipContent.file('content.json', JSON.stringify(rawModelData, null, 2));
      const xmindBlob = await zipContent.generateAsync({ type: 'blob' });

      const baseName = this.selectedFile.name.replace(/\.xmind$/i, '');
      const targetName = asNewFile ? `${baseName}_edited.xmind` : this.selectedFile.name;
      this.downloadBlob(xmindBlob, targetName);

      this.$message.success(asNewFile ? '已导出新的 xmind 文件' : '已保存本地原文件（同名下载）');
    },
    downloadBlob(blob, fileName) {
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = fileName;
      link.click();
      URL.revokeObjectURL(url);
    }
  },
  beforeDestroy() {
    this.destroyMindMapInstance();
  }
};
</script>

<style scoped>
.xmind-converter-page {
  padding: 20px;
  height: calc(100vh - 84px);
  background: linear-gradient(180deg, #f5f8ff 0%, #eef3fb 100%);
  box-sizing: border-box;
}

.converter-container {
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

.upload-section {
  flex: 1.2;
  min-width: 500px;
}

.output-section {
  flex: 1;
  min-width: 400px;
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

.upload-content {
  padding: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.upload-top-bar {
  display: flex;
  padding: 15px;
  gap: 15px;
  border-bottom: 1px solid #f0f4f8;
  background: #fafbfc;
}

.upload-btn-wrapper {
  flex-shrink: 0;
}

.file-list-container {
  flex: 1;
  height: 100px;
  overflow-y: auto;
  background: #ffffff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 5px;
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
  padding: 8px 12px;
  border-radius: 6px;
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
  font-weight: bold;
}

.file-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.action-bar {
  padding: 10px 15px;
  display: flex;
  gap: 10px;
  background: #ffffff;
  border-bottom: 1px solid #f0f4f8;
}

.preview-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.preview-header {
  padding: 10px 15px;
  background: #f5f7fa;
  font-size: 14px;
  color: #606266;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e4e7ed;
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

.preview-body {
  flex: 1;
  overflow: hidden;
  padding: 0;
  position: relative;
}

.xmind-tree {
  padding: 15px;
  background: transparent;
  height: 100%;
  overflow-y: auto;
}

.mind-map-canvas {
  width: 100%;
  height: 100%;
  background: #fff;
}

.mind-map-wrapper {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
}

.mind-map-toolbar {
  padding: 8px 15px;
  background: #f8f9fb;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  z-index: 10;
}

.zoom-text {
  margin-left: 10px;
  font-size: 12px;
  color: #606266;
  min-width: 44px;
}

.model-info {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
  box-sizing: border-box;
}

.model-tree-container {
  margin-top: 20px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 15px;
  background: #fff;
}

.model-view-mode {
  margin-bottom: 10px;
  display: flex;
  align-items: center;
}

.model-mind-map-wrapper {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  height: 520px;
}

.tree-title {
  font-size: 14px;
  font-weight: bold;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}

.tree-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.empty-sheet {
  padding: 30px;
  text-align: center;
  color: #909399;
  font-size: 14px;
}

.raw-model-json {
  background: #282c34;
  color: #abb2bf;
  padding: 15px;
  border-radius: 8px;
  font-family: 'Courier New', Courier, monospace;
  font-size: 12px;
  max-height: 500px;
  overflow: auto;
}

.raw-model-json pre {
  margin: 0;
}

.model-info p {
  margin: 10px 0;
  font-size: 14px;
  color: #606266;
}

.custom-tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}

.note-icon {
  color: #e6a23c;
  margin-left: 4px;
}

.export-bar {
  padding: 10px 20px;
  background: #ffffff;
  border-top: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);
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

.output-content {
  padding: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.convert-controls {
  padding: 15px;
  background: #fafbfc;
  border-bottom: 1px solid #f0f4f8;
  display: flex;
  align-items: flex-end;
  gap: 15px;
}

.method-select {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.method-select label {
  font-size: 12px;
  color: #909399;
}

.convert-btn {
  font-weight: bold;
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
  .converter-container {
    flex-direction: column;
  }
  .upload-section, .output-section {
    min-width: 0;
    flex: none;
    height: 50%;
  }
}
</style>
