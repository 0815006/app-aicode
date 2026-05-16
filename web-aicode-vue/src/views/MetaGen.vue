<template>
  <div class="meta-gen-page">
    <div class="meta-main-content">
      <!-- 左侧模型列表 -->
      <div class="sidebar-container">
        <div class="model-list-sidebar">
          <div class="sidebar-header">
            <h3>📋 造数模型</h3>
            <div class="sidebar-header-btns">
              <el-button
                type="primary"
                size="mini"
                icon="el-icon-collection-tag"
                circle
                title="枚举库管理"
                @click="showEnumDialog = true"
              ></el-button>
              <el-button
                type="primary"
                size="mini"
                icon="el-icon-folder-opened"
                circle
                title="引用文件管理"
                @click="showRefFileDialog = true"
              ></el-button>
              <el-button
                type="primary"
                size="mini"
                icon="el-icon-setting"
                circle
                title="模板维护"
                @click="handleManageTemplates"
              ></el-button>
              <el-button
                type="primary"
                size="mini"
                icon="el-icon-plus"
                circle
                title="新建模型"
                @click="handleCreateModel"
              ></el-button>
            </div>
          </div>
          <div class="model-list-content" v-loading="modelLoading">
            <div v-if="models.length === 0" class="no-models-tip">暂无模型</div>
            <ul class="model-items-list">
              <li
                v-for="item in models"
                :key="item.id"
                class="model-item"
                :class="{ active: activeModelId === item.id }"
                @click="handleSelectModel(item.id)"
              >
                <div class="model-info">
                  <div class="model-name" :title="item.modelName">{{ item.modelName || '未命名模型' }}</div>
                  <div class="model-meta">
                    <el-tag :type="item.status === 'PUBLISHED' ? 'success' : item.status === 'DRAFT' ? 'info' : 'danger'" size="mini">{{ item.status }}</el-tag>
                    <span style="margin-left:4px">{{ item.encoding }} | {{ item.splitType === 'FIXED' ? '定长' : '分隔符' }}</span>
                  </div>
                </div>
                <i class="el-icon-arrow-right"></i>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <!-- 右侧详情展示区 -->
      <div class="detail-content-area">
        <div v-if="!activeModelId" class="empty-state">
          <el-empty description="请选择或新建一个造数模型"></el-empty>
        </div>
        <div v-else class="detail-scroll-container" v-loading="detailLoading">
          <!-- 模型属性区块 -->
          <el-card class="info-block" shadow="hover">
            <div slot="header" class="card-header">
              <span class="block-title"><i class="el-icon-info"></i> 模型属性</span>
              <div class="header-actions">
                <el-button type="primary" size="mini" icon="el-icon-edit" @click="openModelEdit">编辑</el-button>
                <el-button v-if="modelInfo.status === 'DRAFT'" type="success" size="mini" icon="el-icon-upload2" @click="handlePublishConfirm">发布</el-button>
                <el-button v-if="modelInfo.status === 'PUBLISHED'" type="warning" size="mini" icon="el-icon-back" @click="handleUnpublish">退回草稿</el-button>
                <el-button type="primary" size="mini" icon="el-icon-s-promotion" @click="showGenDialog = true">批量生成</el-button>
                <el-button type="danger" size="mini" icon="el-icon-delete" @click="handleDeleteConfirm">删除</el-button>
              </div>
            </div>
            <el-descriptions :column="4" border size="small">
              <el-descriptions-item label="模型名称">{{ modelInfo.modelName }}</el-descriptions-item>
              <el-descriptions-item label="编码">{{ modelInfo.encoding }}</el-descriptions-item>
              <el-descriptions-item label="格式">{{ modelInfo.splitType === 'FIXED' ? '定长 (FIXED)' : '分隔符 (DELIMITER)' }}</el-descriptions-item>
              <el-descriptions-item v-if="modelInfo.splitType === 'DELIMITER'" label="分隔符">{{ modelInfo.delimiter }}</el-descriptions-item>
              <el-descriptions-item label="换行符">{{ modelInfo.lineEndingChar || '\\r\\n' }}</el-descriptions-item>
              <el-descriptions-item label="最大行数">{{ modelInfo.maxRowsLimit }}</el-descriptions-item>
              <el-descriptions-item label="文件头">{{ modelInfo.hasHeader ? '有' : '无' }}</el-descriptions-item>
              <el-descriptions-item label="文件尾">{{ modelInfo.hasFooter ? '有' : '无' }}</el-descriptions-item>
              <el-descriptions-item label="共享给">{{ modelInfo.sharedWith || '-' }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="modelInfo.status === 'PUBLISHED' ? 'success' : modelInfo.status === 'DRAFT' ? 'info' : 'danger'" size="small">{{ modelInfo.status }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="版本">{{ modelInfo.modelVersion }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- 实时预览区块 -->
          <el-card class="info-block preview-block" shadow="hover">
            <div slot="header" class="card-header">
              <span class="block-title"><i class="el-icon-view"></i> 实时预览</span>
              <el-button type="text" size="mini" icon="el-icon-refresh" @click="handlePreview" :loading="previewLoading">刷新预览</el-button>
            </div>
            <div class="byte-ruler" v-if="previewText" ref="rulerRef">
              <span class="ruler-text" ref="rulerTextRef">{{ rulerContent }}</span>
            </div>
            <pre class="preview-box" :class="{ empty: !previewText }" ref="previewBoxRef" @scroll="syncRulerScroll">{{ previewText || '点击"刷新预览"查看生成效果（3行Body）' }}</pre>
          </el-card>

          <!-- 区块字段定义卡片：文件名 / 文件头 / 文件体 / 文件尾 -->
          <el-card v-for="sec in visibleSections" :key="sec.key" class="info-block" shadow="hover">
            <div slot="header" class="card-header">
              <span class="block-title"><i :class="sec.icon"></i> {{ sec.label }}</span>
              <el-button type="primary" size="mini" icon="el-icon-edit" @click="openFieldEdit(sec.key)">编辑</el-button>
            </div>
            <el-table :data="getSectionFields(sec.key)" border stripe size="small" v-if="getSectionFields(sec.key).length > 0">
              <el-table-column label="序号" width="60" align="center">
                <template slot-scope="scope">
                  <span style="font-family:monospace;color:#909399">{{ computeSortOrder(scope.$index, sec.key) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="fieldKey" label="变量名" min-width="50" show-overflow-tooltip></el-table-column>
              <el-table-column prop="fieldName" label="字段描述" min-width="100" show-overflow-tooltip></el-table-column>
              <el-table-column label="长度(B)" width="70" align="center">
                <template slot-scope="scope">{{ scope.row.length || '-' }}</template>
              </el-table-column>
              <el-table-column label="必填" width="55" align="center">
                <template slot-scope="scope">
                  <span :style="{ color: scope.row.isRequired ? '#F56C6C' : '#C0C4CC' }">{{ scope.row.isRequired ? '是' : '否' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="补齐" width="60" align="center">
                <template slot-scope="scope">
                  <span>{{ scope.row.paddingDirection !== 'NONE' ? (scope.row.paddingDirection === 'LEFT' ? '左补' : '右补') : '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="补齐字符" width="75" align="center">
                <template slot-scope="scope">
                  <span>{{ scope.row.paddingDirection !== 'NONE' ? (scope.row.paddingChar === ' ' ? '空格' : scope.row.paddingChar) : '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="ruleType" label="规则类型" width="100">
                <template slot-scope="scope">
                  <el-tag size="mini" type="info">{{ ruleTypeToChinese(scope.row.ruleType) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="规则配置JSON" min-width="200" show-overflow-tooltip>
                <template slot-scope="scope">
                  <span style="font-family:monospace;font-size:11px;color:#606266">{{ scope.row.ruleConfigJson || '-' }}</span>
                </template>
              </el-table-column>
            </el-table>
            <div v-else style="color:#909399;font-size:12px;text-align:center;padding:16px">暂无字段定义，点击"编辑"添加</div>
          </el-card>

          <!-- 生成历史 -->
          <el-card class="info-block" shadow="hover">
            <div slot="header" class="card-header">
              <span class="block-title"><i class="el-icon-time"></i> 生成历史</span>
            </div>
            <el-table :data="historyList" border stripe size="small" v-loading="historyLoading">
              <el-table-column prop="fileName" label="文件名" min-width="180" show-overflow-tooltip></el-table-column>
              <el-table-column label="类型" width="80">
                <template slot-scope="scope">{{ scope.row.fileType === 'PREVIEW' ? '预览' : '正式' }}</template>
              </el-table-column>
              <el-table-column prop="rowCount" label="行数" width="80"></el-table-column>
              <el-table-column label="状态" width="100">
                <template slot-scope="scope">
                  <el-tag v-if="scope.row.status === 'SUCCESS'" type="success" size="mini">成功</el-tag>
                  <el-tag v-else-if="scope.row.status === 'RUNNING'" type="warning" size="mini">生成中</el-tag>
                  <el-tag v-else type="danger" size="mini">失败</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="durationMs" label="耗时(ms)" width="90"></el-table-column>
              <el-table-column prop="createTime" label="创建时间" width="160"></el-table-column>
              <el-table-column label="操作" width="120">
                <template slot-scope="scope">
                  <el-button v-if="scope.row.status === 'SUCCESS'" type="text" size="small" @click="handleDownload(scope.row)">下载</el-button>
                  <el-button type="text" size="small" style="color:#f56c6c" @click="handleDeleteFile(scope.row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </div>
      </div>
    </div>

    <!-- ========== 弹窗组件 ========== -->
    <ModelEditDialog ref="modelEditDialog" @refresh="refreshDetail" @listRefresh="loadModels" />
    <FieldEditDialog ref="fieldEditDialog" @refresh="refreshDetail" :enumKeys="enumKeys" :refFiles="refFiles" />
    <EnumManageDialog :dialog-visible.sync="showEnumDialog" @changed="loadResources" />
    <RefFileDialog :dialog-visible.sync="showRefFileDialog" @changed="loadResources" />
    <TemplateManageDialog ref="templateManageDialog" />

    <!-- 批量生成弹窗 -->
    <el-dialog title="批量生成" :visible.sync="showGenDialog" width="420px" append-to-body>
      <el-form label-width="80px" size="small">
        <el-form-item label="生成行数">
          <el-input-number v-model="genRowCount" :min="1" :max="9999999" style="width:100%"></el-input-number>
          <div v-if="genRowCount > (modelInfo.maxRowsLimit || 100000)" style="color:#f56c6c;font-size:12px;margin-top:4px">
            ⚠ 超过最大行数限制 {{ modelInfo.maxRowsLimit }}，无法生成！
          </div>
        </el-form-item>
        <el-form-item label="批次名称">
          <el-input v-model="genBatchName" placeholder="可选"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button size="small" @click="showGenDialog = false">取消</el-button>
        <el-button size="small" type="primary" @click="doGenerate" :loading="genLoading" :disabled="genRowCount > (modelInfo.maxRowsLimit || 100000)">开始生成</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  listModels, getModelDetail, deleteModel, publishModel, updateModel,
  preview, generate, getHistory, getTaskStatus, deleteEntityFile
} from '@/api/meta-gen'
import { getEnumKeys, listRefFiles } from '@/api/meta-gen'
import ModelEditDialog from '@/components/meta-gen/ModelEditDialog.vue'
import FieldEditDialog from '@/components/meta-gen/FieldEditDialog.vue'
import EnumManageDialog from '@/components/meta-gen/EnumManageDialog.vue'
import RefFileDialog from '@/components/meta-gen/RefFileDialog.vue'
import TemplateManageDialog from '@/components/meta-gen/TemplateManageDialog.vue'

export default {
  name: 'MetaGen',
  components: { ModelEditDialog, FieldEditDialog, EnumManageDialog, RefFileDialog, TemplateManageDialog },
  data() {
    return {
      models: [],
      modelLoading: false,
      activeModelId: null,
      modelInfo: {},
      allFields: [],
      detailLoading: false,
      // 预览
      previewText: '',
      previewLoading: false,
      // 历史
      historyList: [],
      historyLoading: false,
      // 生成
      showGenDialog: false,
      genRowCount: 100,
      genBatchName: '',
      genLoading: false,
      // 资源
      enumKeys: [],
      refFiles: [],
      showEnumDialog: false,
      showRefFileDialog: false
    }
  },
  computed: {
    visibleSections() {
      const secs = [{ key: 'FILENAME', label: '文件名 (FILENAME)', icon: 'el-icon-document' }]
      if (this.modelInfo.hasHeader) {
        secs.push({ key: 'HEADER', label: '文件头 (HEADER)', icon: 'el-icon-s-grid' })
      }
      secs.push({ key: 'BODY', label: '文件体 (BODY)', icon: 'el-icon-menu' })
      if (this.modelInfo.hasFooter) {
        secs.push({ key: 'FOOTER', label: '文件尾 (FOOTER)', icon: 'el-icon-tickets' })
      }
      return secs
    },
    rulerContent() {
      const base = '1234567890'
      if (!this.previewText) {
        let result = ''
        for (let i = 0; i < 10; i++) result += base
        return result
      }
      // 按显示宽度（中文=2，ASCII=1）计算最长行的宽度
      let maxWidth = 0
      const lines = this.previewText.split('\n')
      for (const line of lines) {
        let width = 0
        for (const ch of line) {
          width += this.isFullWidthChar(ch) ? 2 : 1
        }
        if (width > maxWidth) maxWidth = width
      }
      const total = Math.max(maxWidth + 10, 100) // 留一些余量
      let result = ''
      for (let i = 0; i < Math.ceil(total / 10); i++) {
        result += base
      }
      return result
    }
  },
  mounted() {
    this.loadModels()
    this.loadResources()
  },
  methods: {
    // ========== 模型列表 ==========
    async loadModels() {
      this.modelLoading = true
      try {
        const res = await listModels({ page: 1, size: 200 })
        this.models = (res.data && res.data.records) || []
      } finally {
        this.modelLoading = false
      }
    },
    async handleSelectModel(modelId) {
      this.activeModelId = modelId
      this.detailLoading = true
      try {
        const res = await getModelDetail(modelId)
        const detail = res.data
        this.modelInfo = detail.model || detail
        const fields = detail.fields || []
        fields.sort((a, b) => (a.sortIndex || 0) - (b.sortIndex || 0))
        this.allFields = fields
        this.previewText = ''
        this.loadHistory()
      } finally {
        this.detailLoading = false
      }
    },
    async refreshDetail() {
      if (this.activeModelId) {
        await this.handleSelectModel(this.activeModelId)
      }
    },
    handleCreateModel() {
      this.$refs.modelEditDialog.init(null)
    },
    handleManageTemplates() {
      this.$refs.templateManageDialog.init()
    },
    openModelEdit() {
      this.$refs.modelEditDialog.init({ ...this.modelInfo })
    },

    // ========== 字段区块 ==========
    getSectionFields(section) {
      return this.allFields.filter(f => f.section === section)
    },
    computeSortOrder(idx, section) {
      const fields = this.getSectionFields(section)
      if (idx >= fields.length) return ''
      const f = fields[idx]
      if (f.level === 1) {
        // 找到在当前section level-1中的序号
        let count = 0
        for (let i = 0; i <= idx; i++) {
          if (fields[i].level === 1) count++
        }
        return count + '.0'
      }
      // level 2: 找到父字段
      for (let i = idx - 1; i >= 0; i--) {
        if (fields[i].level === 1) {
          let sub = 0
          for (let j = i + 1; j <= idx; j++) {
            if (fields[j].level === 2) sub++
          }
          return (i + 1) + '.' + sub
        }
      }
      return '?.?'
    },
    openFieldEdit(section) {
      this.$refs.fieldEditDialog.init(this.activeModelId, section, this.getSectionFields(section), this.modelInfo.modelName)
    },

    // ========== 发布/删除/退回草稿 ==========
    handlePublishConfirm() {
      this.$confirm('确定要发布该模型吗？发布后字段将不可编辑，需退回草稿才能修改。', '发布确认', {
        confirmButtonText: '确定发布',
        type: 'warning'
      }).then(() => {
        publishModel(this.activeModelId).then(() => {
          this.$message.success('发布成功')
          this.refreshDetail()
          this.loadModels()
        })
      }).catch(() => {})
    },
    handleUnpublish() {
      this.$confirm('确定退回草稿吗？退回后可以重新编辑字段定义。', '退回确认', {
        confirmButtonText: '确定退回',
        type: 'info'
      }).then(() => {
        const data = { ...this.modelInfo, status: 'DRAFT' }
        updateModel(this.activeModelId, data).then(() => {
          this.$message.success('已退回草稿')
          this.refreshDetail()
          this.loadModels()
        }).catch(err => {
          this.$message.error('退回草稿失败: ' + (err.message || '未知错误'))
        })
      }).catch(() => {})
    },
    handleDeleteConfirm() {
      this.$confirm('确定要删除该模型及其所有字段定义吗？此操作不可恢复。', '删除确认', {
        confirmButtonText: '确定删除',
        type: 'warning'
      }).then(() => {
        deleteModel(this.activeModelId).then(() => {
          this.$message.success('删除成功')
          this.activeModelId = null
          this.modelInfo = {}
          this.allFields = []
          this.loadModels()
        })
      }).catch(() => {})
    },

    // ========== 预览 ==========
    handlePreview() {
      this.previewLoading = true
      preview(this.activeModelId).then(res => {
        if (res.code === 200) {
          this.previewText = res.data || ''
          this.$nextTick(() => this.syncRulerWidth())
        } else {
          this.$alert(res.message || '预览失败', '错误', { type: 'error' })
          this.previewText = '预览失败: ' + (res.message || '未知错误')
        }
      }).catch(err => {
        this.$alert(err.message || '预览请求失败', '错误', { type: 'error' })
        this.previewText = '预览请求失败: ' + (err.message || '未知错误')
      }).finally(() => { this.previewLoading = false })
    },

    /** 预览区横向滚动时，标尺同步跟随 */
    syncRulerScroll() {
      if (this.$refs.rulerRef && this.$refs.previewBoxRef) {
        this.$refs.rulerRef.scrollLeft = this.$refs.previewBoxRef.scrollLeft
      }
    },

    /** 让标尺内容宽度匹配预览区滚动宽度 */
    syncRulerWidth() {
      const preview = this.$refs.previewBoxRef
      const rulerText = this.$refs.rulerTextRef
      if (preview && rulerText) {
        rulerText.style.display = 'inline-block'
        rulerText.style.minWidth = (preview.scrollWidth || preview.offsetWidth || 0) + 'px'
      }
    },

    /** 判断字符是否为全角（中文/日韩文等，显示宽度计 2） */
    isFullWidthChar(ch) {
      const cp = ch.codePointAt(0)
      if (cp >= 0x4E00 && cp <= 0x9FFF) return true
      if (cp >= 0x3400 && cp <= 0x4DBF) return true
      if (cp >= 0xF900 && cp <= 0xFAFF) return true
      if (cp >= 0x20000 && cp <= 0x2FFFF) return true
      if (cp >= 0xFF01 && cp <= 0xFF60) return true
      if (cp >= 0xFFE0 && cp <= 0xFFE6) return true
      if (cp >= 0x3000 && cp <= 0x303F) return true
      if (cp >= 0x3040 && cp <= 0x30FF) return true
      if (cp >= 0xAC00 && cp <= 0xD7AF) return true
      if (cp >= 0x1100 && cp <= 0x11FF) return true
      return false
    },

    // ========== 批量生成 ==========
    doGenerate() {
      if (this.genRowCount > (this.modelInfo.maxRowsLimit || 100000)) {
        this.$message.error('生成行数超过模型最大行数限制，请调整')
        return
      }
      this.genLoading = true
      generate({ modelId: this.activeModelId, rowCount: this.genRowCount, batchName: this.genBatchName }).then(res => {
        if (res.code === 200) {
          this.$message.success('生成任务已提交')
          this.showGenDialog = false
          this.loadHistory()
          const taskId = res.data && res.data.id
          if (taskId) {
            const timer = setInterval(() => {
              getTaskStatus(taskId).then(r => {
                const record = r.data
                if (record && record.status !== 'RUNNING') {
                  clearInterval(timer)
                  this.loadHistory()
                  if (record.status === 'SUCCESS') this.$message.success('文件生成完成')
                  else this.$message.error('文件生成失败: ' + (record.errorMsg || ''))
                }
              }).catch(() => { clearInterval(timer) })
            }, 2000)
          }
        } else {
          this.$alert(res.message || '提交生成任务失败', '错误', { type: 'error' })
        }
      }).catch(err => {
        this.$alert(err.message || '请求生成失败', '错误', { type: 'error' })
      }).finally(() => { this.genLoading = false })
    },

    // ========== 历史 ==========
    loadHistory() {
      if (!this.activeModelId) return
      this.historyLoading = true
      getHistory(this.activeModelId).then(res => {
        this.historyList = res.data || []
      }).finally(() => { this.historyLoading = false })
    },
    handleDownload(row) {
      window.open('/api/meta/execute/download/' + row.id, '_blank')
    },
    handleDeleteFile(row) {
      this.$confirm('确定删除该文件记录？', '提示', { type: 'warning' }).then(() => {
        deleteEntityFile(row.id).then(() => {
          this.$message.success('删除成功')
          this.loadHistory()
        })
      }).catch(() => {})
    },

    // ========== 资源 ==========
    loadResources() {
      getEnumKeys().then(res => { this.enumKeys = res.data || [] })
      listRefFiles().then(res => { this.refFiles = res.data || [] })
    },

    // ========== 工具方法 ==========
    ruleTypeToChinese(ruleType) {
      if (!ruleType) return '-'
      const map = {
        'FIXED': '固定值',
        'DATE': '日期',
        'ENUM': '枚举',
        'SEQUENCE': '序列号',
        'RANDOM_CN': '随机汉字',
        'RANDOM_NUM': '随机数字',
        'RANDOM_UUID': '随机UUID',
        'REF_FILE': '引用文件',
        'REF_FIELD': '引用字段',
        'SUM': '汇总金额',
        'COUNT': '统计行数',
        'BATCH_NO': '批次号',
        'AMOUNT': '金额',
        'EXPR': '表达式'
      }
      return map[ruleType] || ruleType
    }
  }
}
</script>

<style scoped>
.meta-gen-page {
  padding: 20px;
  height: 100%;
  background: linear-gradient(180deg, #f5f8ff 0%, #eef3fb 100%);
  box-sizing: border-box;
  overflow: hidden;
}
.meta-main-content {
  display: flex;
  height: 100%;
  gap: 18px;
}

/* 左侧列表 */
.sidebar-container {
  width: 280px;
  display: flex;
  flex-direction: column;
  height: 100%;
}
.model-list-sidebar {
  padding: 16px 14px;
  border: 1px solid #d9e3f2;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 8px 20px rgba(16, 43, 98, 0.08);
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}
.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  flex-shrink: 0;
}
.sidebar-header h3 {
  margin: 0;
  font-size: 14px;
  color: #1f2d3d;
  flex-shrink: 0;
}
.sidebar-header-btns {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
}
.model-list-content {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}
.model-list-content::-webkit-scrollbar { width: 6px; }
.model-list-content::-webkit-scrollbar-thumb {
  background: #c8d8f0;
  border-radius: 8px;
}
.no-models-tip {
  color: #92a1b7;
  text-align: center;
  padding: 20px;
  font-size: 13px;
}
.model-items-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.model-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-radius: 10px;
  background: #f8faff;
  margin-bottom: 10px;
  transition: all 0.2s;
  cursor: pointer;
  border: 1px solid transparent;
}
.model-item:hover {
  background: #f0f5ff;
  transform: translateY(-1px);
}
.model-item.active {
  background: #eaf4ff;
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
}
.model-info { flex: 1; min-width: 0; margin-right: 10px; }
.model-name { font-size: 13px; color: #2c3e50; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; font-weight: 600; }
.model-meta { font-size: 11px; color: #909399; margin-top: 4px; }
.model-item i { color: #c0c4cc; font-size: 12px; }
.model-item.active i { color: #409eff; }

/* 右侧详情 */
.detail-content-area {
  flex: 1;
  min-width: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  border: 1px solid #d9e3f2;
  border-radius: 16px;
  padding: 20px;
  background: #ffffff;
  box-shadow: 0 12px 24px rgba(16, 43, 98, 0.08);
  overflow: hidden;
}
.detail-content-area > .empty-state {
  background: transparent;
}
.empty-state {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.detail-scroll-container {
  flex: 1;
  overflow-y: auto;
  padding: 0 5px 12px 0;
}
.detail-scroll-container::-webkit-scrollbar { width: 6px; }
.detail-scroll-container::-webkit-scrollbar-thumb {
  background: #c8d8f0;
  border-radius: 8px;
}

.info-block {
  margin-bottom: 20px;
  border-radius: 12px;
  border: 1px solid #ebeef5;
  overflow: hidden;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.block-title {
  font-weight: bold;
  color: #303133;
  font-size: 15px;
}
.block-title i {
  margin-right: 6px;
  color: #409eff;
}
.header-actions {
  display: flex;
  gap: 8px;
}

/* 预览 */
.preview-block { background: #1e1e1e; border-color: #333; }
.preview-block >>> .el-card__header { border-bottom-color: #333; }
.preview-block >>> .card-header { color: #ccc; }
.preview-block >>> .block-title i { color: #67c23a; }
.byte-ruler {
  height: 20px;
  background: #2d2d2d;
  position: relative;
  border-bottom: 1px solid #444;
  overflow: hidden;
  margin: 0 -16px;
  padding: 0 10px 0 26px;
}
.ruler-text {
  font-family: 'Consolas', 'Courier New', monospace;
  font-size: 13px;
  color: #888;
  white-space: pre;
}
.preview-box {
  background: #1e1e1e;
  color: #d4d4d4;
  font-family: 'Consolas', 'Courier New', monospace;
  font-size: 13px;
  padding: 10px;
  min-height: 60px;
  white-space: pre;
  overflow-x: auto;
  margin: 0;
  border-radius: 0 0 4px 4px;
}
.preview-box.empty { color: #666; }
</style>
