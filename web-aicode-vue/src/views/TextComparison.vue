<template>
  <div class="text-comparison" @keydown="handleKeydown" tabindex="0">
    <!-- ==================== 顶部工具栏 ==================== -->
    <div class="toolbar">
      <div class="toolbar-left">
        <span class="toolbar-title">
          <i class="el-icon-document-copy"></i> 文本比对工具
        </span>
        <el-tag v-if="leftFileType" size="small" effect="plain">{{ leftFileType.toUpperCase() }}</el-tag>
        <el-tag v-if="hasLineEndingDiff" size="small" type="warning" effect="plain">
          换行符差异: {{ leftLineEnding }} ↔ {{ rightLineEnding }}
        </el-tag>
        <el-tag v-if="isLargeFileMode" size="small" type="danger" effect="plain">大文件模式</el-tag>
      </div>
      <div class="toolbar-center">
        <!-- 比对模式切换 -->
        <el-radio-group v-model="diffMode" size="mini" @change="handleDiffModeChange">
          <el-radio-button label="line">行级比对</el-radio-button>
          <el-radio-button label="semantic" :disabled="!canSemanticDiff">
            语义比对
          </el-radio-button>
        </el-radio-group>
      </div>
      <div class="toolbar-right">
        <!-- 开关组 -->
        <el-tooltip content="忽略换行符差异 (LF / CRLF)" placement="bottom">
          <el-switch
            v-model="ignoreLineEnding"
            active-text="忽略换行"
            inactive-color="#dcdfe6"
            size="mini"
            class="toolbar-switch"
            @change="recompute"
          ></el-switch>
        </el-tooltip>
        <el-tooltip content="忽略空白字符差异（YAML缩进除外）" placement="bottom">
          <el-switch
            v-model="ignoreWhitespace"
            active-text="忽略空白"
            inactive-color="#dcdfe6"
            size="mini"
            class="toolbar-switch"
            @change="recompute"
          ></el-switch>
        </el-tooltip>
        <el-tooltip content="脱敏显示（仅预览，保存仍为明文）" placement="bottom">
          <el-switch
            v-model="maskEnabled"
            active-text="脱敏"
            inactive-color="#dcdfe6"
            size="mini"
            class="toolbar-switch"
          ></el-switch>
        </el-tooltip>
        <el-divider direction="vertical"></el-divider>
        <!-- 语法状态 -->
        <el-tooltip v-if="syntaxStatus.left || syntaxStatus.right" placement="bottom">
          <template slot="content">
            <div v-if="syntaxStatus.left">左侧: {{ syntaxStatus.left }}</div>
            <div v-if="syntaxStatus.right">右侧: {{ syntaxStatus.right }}</div>
          </template>
          <el-tag size="small" :type="syntaxOk ? 'success' : 'danger'" effect="dark">
            <i :class="syntaxOk ? 'el-icon-circle-check' : 'el-icon-warning'"></i>
            语法{{ syntaxOk ? '正常' : '错误' }}
          </el-tag>
        </el-tooltip>
        <el-divider direction="vertical"></el-divider>
        <!-- 操作按钮 -->
        <el-tooltip content="撤销 (Ctrl+Z)" placement="bottom">
          <el-button
            size="mini"
            icon="el-icon-refresh-left"
            :disabled="undoStack.length === 0"
            @click="undo"
          >回退</el-button>
        </el-tooltip>
        <el-tooltip content="放弃所有修改，重新加载" placement="bottom">
          <el-button
            size="mini"
            icon="el-icon-refresh"
            :disabled="!rightOriginalContent"
            @click="reloadRight"
          >重新加载</el-button>
        </el-tooltip>
        <el-tooltip content="保存右侧文件 (Ctrl+S)" placement="bottom">
          <el-button
            type="primary"
            size="mini"
            icon="el-icon-download"
            :disabled="!rightContent"
            @click="showSaveDialog"
          >保存</el-button>
        </el-tooltip>
      </div>
    </div>

    <!-- ==================== 主内容区 ==================== -->
    <div class="main-content">
      <!-- 比对视图区域 -->
      <div class="diff-body">
        <!-- 单侧已加载 / 拖拽区（两个文件未全部加载时，保持双栏布局） -->
        <template v-if="!leftContent || !rightContent">
          <!-- 左侧：未加载时显示拖拽区，已加载时显示占位卡片 -->
          <div
            v-if="!leftContent"
            class="drop-zone drop-zone-left"
            :class="{ 'drop-active': leftDropActive }"
            @dragenter.prevent="leftDropActive = true"
            @dragover.prevent="leftDropActive = true"
            @dragleave.prevent="leftDropActive = false"
            @drop.prevent="handleDropLeft"
            @click="triggerFileInput('left')"
          >
            <div class="drop-hint">
              <i class="el-icon-upload" style="font-size: 48px; color: #c0c4cc;"></i>
              <p>拖拽参照文件到此处</p>
              <p class="drop-sub">或点击选择文件</p>
              <p class="drop-sub">支持 .properties, .yml, .json, .md 等</p>
            </div>
          </div>
          <div
            v-else
            class="loaded-zone loaded-zone-left"
            @dragenter.prevent="leftDropActive = true"
            @dragover.prevent="leftDropActive = true"
            @dragleave.prevent="leftDropActive = false"
            @drop.prevent="handleDropLeft"
            @click="triggerFileInput('left')"
          >
            <div class="loaded-card">
              <i class="el-icon-document-checked loaded-icon"></i>
              <p class="loaded-title">参照文件已加载</p>
              <p class="loaded-filename" :title="leftFileName">{{ leftFileName }}</p>
              <p class="loaded-meta">
                <el-tag v-if="leftFileType" size="mini" effect="plain">{{ leftFileType.toUpperCase() }}</el-tag>
                <span v-if="leftFileSize" class="loaded-size">{{ formatFileSize(leftFileSize) }}</span>
              </p>
              <p class="loaded-hint">拖拽或点击可替换文件</p>
            </div>
          </div>

          <!-- 右侧：未加载时显示拖拽区，已加载时显示占位卡片 -->
          <div
            v-if="!rightContent"
            class="drop-zone drop-zone-right"
            :class="{ 'drop-active': rightDropActive }"
            @dragenter.prevent="rightDropActive = true"
            @dragover.prevent="rightDropActive = true"
            @dragleave.prevent="rightDropActive = false"
            @drop.prevent="handleDropRight"
            @click="triggerFileInput('right')"
          >
            <div class="drop-hint">
              <i class="el-icon-upload" style="font-size: 48px; color: #c0c4cc;"></i>
              <p>拖拽目标文件到此处</p>
              <p class="drop-sub">或点击选择文件</p>
              <p class="drop-sub">支持 .properties, .yml, .json, .md 等</p>
            </div>
          </div>
          <div
            v-else
            class="loaded-zone loaded-zone-right"
            @dragenter.prevent="rightDropActive = true"
            @dragover.prevent="rightDropActive = true"
            @dragleave.prevent="rightDropActive = false"
            @drop.prevent="handleDropRight"
            @click="triggerFileInput('right')"
          >
            <div class="loaded-card">
              <i class="el-icon-document-checked loaded-icon"></i>
              <p class="loaded-title">目标文件已加载</p>
              <p class="loaded-filename" :title="rightFileName">{{ rightFileName }}</p>
              <p class="loaded-meta">
                <el-tag v-if="rightFileType" size="mini" effect="plain">{{ rightFileType.toUpperCase() }}</el-tag>
                <span v-if="rightFileSize" class="loaded-size">{{ formatFileSize(rightFileSize) }}</span>
              </p>
              <p class="loaded-hint">拖拽或点击可替换文件</p>
            </div>
          </div>
        </template>

        <!-- 比对内容区（两个文件都加载后显示） -->
        <template v-if="leftContent && rightContent">
          <!-- 竖向滚动视口（统一纵向滚动） -->
          <div
            class="diff-viewport"
            ref="viewport"
            @scroll="onViewportScroll"
            @dragenter.prevent="handleViewportDragEnter"
            @dragover.prevent="handleViewportDragOver"
            @dragleave.prevent="handleViewportDragLeave"
            @drop.prevent="handleViewportDrop"
          >
            <!-- 固定表头行（复制上方 file-headers，sticky 在视口顶部） -->
            <div class="sticky-header-row">
              <div class="sticky-header-left">
                <span class="file-label">📄 参照文件（只读）</span>
                <span v-if="leftFileName" class="file-name" :title="leftFileName">{{ leftFileName }}</span>
                <span v-else class="file-placeholder">—</span>
                <el-tag v-if="leftLineEnding" size="mini" effect="plain">{{ leftLineEnding }}</el-tag>
              </div>
              <div class="sticky-header-gutter"></div>
              <div class="sticky-header-right">
                <span class="file-label">📝 目标文件（可编辑）</span>
                <span v-if="rightFileName" class="file-name" :title="rightFileName">{{ rightFileName }}</span>
                <span v-else class="file-placeholder">—</span>
                <el-tag v-if="rightLineEnding" size="mini" effect="plain">{{ rightLineEnding }}</el-tag>
                <el-tag v-if="isModified" size="mini" type="warning" effect="dark">已修改</el-tag>
              </div>
            </div>

            <div class="diff-panels-wrapper">
              <!-- ========== 左侧面板（独立横向滚动） ========== -->
              <div
                class="diff-panel-scroll diff-panel-scroll-left"
                ref="leftScrollPanel"
                @scroll="onLeftPanelScroll"
              >
                <div class="diff-panel-inner">
                  <div
                    v-for="(row, idx) in displayLines"
                    :key="'lp-' + idx"
                    class="diff-panel-row"
                    :class="[
                      'diff-row-' + row.type,
                      { 'diff-row-highlight': highlightRowIdx === idx },
                      { 'diff-row-semantic-order': row.semanticOrderDiff && diffMode === 'semantic' }
                    ]"
                    :data-index="idx"
                  >
                    <div class="line-num line-num-left" :class="{ 'line-num-diff': row.type !== 'equal' }">
                      <span v-if="row.leftLine">{{ row.leftLine.number }}</span>
                    </div>
                    <div
                      class="line-content line-content-left"
                      :class="lineContentClass(row, 'left')"
                    >
                      <template v-if="row.leftLine">
                        <template v-if="row.type === 'modified' && row.charDiffs">
                          <span
                            v-for="(seg, sIdx) in leftCharSegments(row)"
                            :key="'lc-' + sIdx"
                            :class="charSegClass(seg, 'left')"
                          >{{ maskLine(seg.value) }}</span>
                        </template>
                        <template v-else>
                          <span>{{ maskLine(row.leftLine.content) }}</span>
                        </template>
                      </template>
                      <template v-else>
                        <span class="placeholder-line">&nbsp;</span>
                      </template>
                    </div>
                  </div>
                </div>
              </div>

              <!-- ========== 中间操作栏（固定不滚动） ========== -->
              <div class="diff-gutter-fixed">
                <div
                  v-for="(row, idx) in displayLines"
                  :key="'gtr-' + idx"
                  class="gutter-row"
                  :class="[
                    { 'diff-row-highlight': highlightRowIdx === idx }
                  ]"
                >
                  <span v-if="row.type !== 'equal'" class="diff-indicator" :class="'indicator-' + row.type"></span>
                  <el-tooltip v-if="row.type === 'added'" content="复制此行到右侧" placement="top">
                    <button class="gutter-btn gutter-btn-copy" @click="copyLineToRight(idx)">
                      <i class="el-icon-arrow-right"></i>
                    </button>
                  </el-tooltip>
                  <el-tooltip v-if="row.type === 'modified'" content="用左侧整行覆盖右侧" placement="top">
                    <button class="gutter-btn gutter-btn-replace" @click="replaceRightLine(idx)">
                      <i class="el-icon-arrow-right"></i>
                    </button>
                  </el-tooltip>
                  <el-tooltip
                    v-if="row.semanticOrderDiff && diffMode === 'semantic'"
                    placement="top"
                  >
                    <template slot="content">
                      位置不同：右侧第{{ row.semanticOrderDiff.rightPosition }}行 → 参照第{{ row.semanticOrderDiff.targetPosition }}行<br>
                      双击可调整位置
                    </template>
                    <button
                      class="gutter-btn gutter-btn-reorder"
                      @dblclick="reorderSemanticLine(idx, row.semanticOrderDiff)"
                    >
                      <i class="el-icon-sort"></i>
                    </button>
                  </el-tooltip>
                </div>
              </div>

              <!-- ========== 右侧面板（独立横向滚动） ========== -->
              <div
                class="diff-panel-scroll diff-panel-scroll-right"
                ref="rightScrollPanel"
                @scroll="onRightPanelScroll"
              >
                <div class="diff-panel-inner">
                  <div
                    v-for="(row, idx) in displayLines"
                    :key="'rp-' + idx"
                    class="diff-panel-row"
                    :class="[
                      'diff-row-' + row.type,
                      { 'diff-row-highlight': highlightRowIdx === idx },
                      { 'diff-row-semantic-order': row.semanticOrderDiff && diffMode === 'semantic' }
                    ]"
                    :data-index="idx"
                  >
                    <div class="line-num line-num-right" :class="{ 'line-num-diff': row.type !== 'equal' }">
                      <span v-if="row.rightLine">{{ row.rightLine.number }}</span>
                    </div>
                    <div
                      class="line-content line-content-right"
                      :class="lineContentClass(row, 'right')"
                      @dblclick="startEditLine(idx)"
                    >
                      <button
                        v-if="row.type === 'deleted'"
                        class="delete-line-btn"
                        title="删除此行"
                        @click.stop="deleteRightLine(idx)"
                      >
                        <i class="el-icon-close"></i>
                      </button>
                      <template v-if="editingLineIdx === idx">
                        <input
                          ref="lineEditor"
                          class="line-editor"
                          :value="getEditableContent(row)"
                          @input="onLineEdit(idx, $event)"
                          @blur="finishEditLine"
                          @keydown.enter="finishEditLine"
                          @keydown.esc="cancelEditLine"
                        />
                      </template>
                      <template v-else>
                        <template v-if="row.rightLine">
                          <template v-if="row.type === 'modified' && row.charDiffs">
                            <span
                              v-for="(seg, sIdx) in rightCharSegments(row)"
                              :key="'rc-' + sIdx"
                              :class="charSegClass(seg, 'right')"
                              :title="seg.type === 'added' ? '双击可片段替换' : ''"
                              @dblclick.stop="fragmentReplace(idx, row, sIdx)"
                            >{{ maskLine(seg.value) }}</span>
                          </template>
                          <template v-else>
                            <span>{{ maskLine(row.rightLine.content) }}</span>
                          </template>
                        </template>
                        <template v-else>
                          <span
                            class="placeholder-line placeholder-editable"
                            @dblclick.stop="startAddLine(idx)"
                            title="双击此处可添加新行"
                          >&nbsp;</span>
                        </template>
                      </template>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 固定在底部的横向滚动条（不随上下滚动移动） -->
            <div class="hscroll-sticky" ref="hscrollSticky" @scroll="onHScrollSticky">
              <div class="hscroll-spacer" ref="hscrollSpacer"></div>
            </div>
          </div>

          <!-- 缩略图导轨 -->
          <DiffMinimap
            :aligned-lines="displayLines"
            :total-lines="displayLines.length"
            :visible-start="visibleStart"
            :visible-count="visibleCount"
            :stats="diffStats"
            @scroll-to="scrollToLine"
            @prev-diff="navigateDiff(-1)"
            @next-diff="navigateDiff(1)"
          />
        </template>

        <!-- 差异清单面板 -->
        <DiffSummaryPanel
          v-if="leftContent && rightContent"
          :original-diffs="originalDiffList"
          :change-log="changeLog"
          :remaining-diffs="remainingDiffList"
          @locate-diff="scrollToLine"
          @clear-changes="clearChangeLog"
          @toggle="onSummaryToggle"
        />
      </div>
    </div>

    <!-- ==================== 保存对话框 ==================== -->
    <el-dialog
      title="保存文件"
      :visible.sync="saveDialogVisible"
      width="500px"
      :close-on-click-modal="false"
    >
      <div v-if="!syntaxOk" class="save-warning">
        <el-alert
          title="语法错误警告"
          type="error"
          :closable="false"
          show-icon
        >
          <template slot="title">
            <strong>当前文件存在语法错误，强制保存可能导致应用无法启动！</strong>
          </template>
          <div v-if="syntaxStatus.right" style="margin-top: 4px;">{{ syntaxStatus.right }}</div>
        </el-alert>
      </div>
      <el-form label-width="100px" style="margin-top: 16px;">
        <el-form-item label="保存方式">
          <el-radio-group v-model="saveMode">
            <el-radio label="new">
              <span>另存为新文件</span>
              <span class="save-recommend">（推荐）</span>
            </el-radio>
            <el-radio label="overwrite">
              <span>覆盖原文件</span>
              <el-tag size="mini" type="danger" effect="plain" style="margin-left: 4px;">谨慎</el-tag>
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="saveMode === 'new'" label="文件名">
          <el-input v-model="saveNewName" size="small" placeholder="自动生成带时间戳的文件名"></el-input>
        </el-form-item>
        <el-form-item v-if="saveMode === 'overwrite'" label="">
          <el-alert
            title="覆盖文件将丢失原始记录，建议先保存到新文件！"
            type="warning"
            :closable="false"
            show-icon
          ></el-alert>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button size="small" @click="saveDialogVisible = false">取消</el-button>
        <el-button type="primary" size="small" @click="doSave">
          {{ !syntaxOk ? '强制保存' : '确认保存' }}
        </el-button>
      </span>
    </el-dialog>

    <!-- 隐藏文件输入 -->
    <input
      ref="fileInputLeft"
      type="file"
      style="display: none;"
      accept=".properties,.yml,.yaml,.json,.md,.xml,.ini,.cfg,.conf,.env,.toml,.txt"
      @change="handleFileInputLeft"
    />
    <input
      ref="fileInputRight"
      type="file"
      style="display: none;"
      accept=".properties,.yml,.yaml,.json,.md,.xml,.ini,.cfg,.conf,.env,.toml,.txt"
      @change="handleFileInputRight"
    />

    <!-- 拖拽覆盖提示 -->
    <div v-if="viewportDropSide" class="viewport-drop-overlay" :class="'drop-overlay-' + viewportDropSide">
      <div class="drop-overlay-hint">
        <i class="el-icon-upload"></i>
        <span>释放以替换{{ viewportDropSide === 'left' ? '参照' : '目标' }}文件</span>
      </div>
    </div>
  </div>
</template>

<script>
import {
  computeLineDiff,
  computeCharDiff,
  detectFileType,
  detectLineEnding,
  isLargeFile,
  splitLines,
  joinLines,
  generateNewFileName,
  countDiffs,
  maskSensitiveContent,
  validateJson,
  validateYaml,
  deepCompare,
  detectKeyOrderDiff,
  reorderByReference
} from '@/utils/diffEngine'
import DiffMinimap from '@/components/text-comparison/DiffMinimap.vue'
import DiffSummaryPanel from '@/components/text-comparison/DiffSummaryPanel.vue'

export default {
  name: 'TextComparison',
  components: {
    DiffMinimap,
    DiffSummaryPanel
  },
  data: function () {
    return {
      // ---- 文件数据 ----
      leftContent: '',
      rightContent: '',
      leftOriginalContent: '',
      rightOriginalContent: '',
      leftFileName: '',
      rightFileName: '',
      leftFileHandle: null,
      rightFileHandle: null,
      leftFileSize: 0,
      rightFileSize: 0,

      // ---- 比对结果 ----
      alignedLines: [],
      displayLines: [],
      diffStats: { added: 0, deleted: 0, modified: 0, equal: 0 },

      // ---- 差异清单 ----
      originalDiffList: [],
      changeLog: [],

      // ---- 编辑状态 ----
      editingLineIdx: -1,
      undoStack: [],
      maxUndoSteps: 100,

      // ---- UI 状态 ----
      leftDropActive: false,
      rightDropActive: false,
      viewportDropSide: '',
      highlightRowIdx: -1,
      visibleStart: 0,
      visibleCount: 30,
      currentDiffIdx: -1,
      summaryCollapsed: false,

      // ---- 选项 ----
      diffMode: 'line',
      ignoreLineEnding: false,
      ignoreWhitespace: false,
      maskEnabled: false,

      // ---- 保存 ----
      saveDialogVisible: false,
      saveMode: 'new',
      saveNewName: '',

      // ---- 语法状态 ----
      syntaxStatus: { left: '', right: '' },

      // ---- 语义比对 ----
      semanticOrderDiffs: [],
      semanticFlashIdx: -1,

      // ---- 防抖 ----
      recomputeTimer: null,

      // ---- 大文件 ----
      isLargeFileMode: false,

      // ---- 行编辑缓冲 ----
      editBuffer: ''
    }
  },
  computed: {
    leftFileType: function () {
      return detectFileType(this.leftFileName)
    },
    rightFileType: function () {
      return detectFileType(this.rightFileName)
    },
    leftLineEnding: function () {
      return this.leftOriginalContent ? detectLineEnding(this.leftOriginalContent) : ''
    },
    rightLineEnding: function () {
      return this.rightOriginalContent ? detectLineEnding(this.rightOriginalContent) : ''
    },
    hasLineEndingDiff: function () {
      return this.leftLineEnding && this.rightLineEnding && this.leftLineEnding !== this.rightLineEnding
    },
    canSemanticDiff: function () {
      if (this.isLargeFileMode) return false
      var lt = this.leftFileType
      var rt = this.rightFileType
      return (lt === 'json' || lt === 'yaml') && (rt === 'json' || rt === 'yaml')
    },
    isModified: function () {
      return this.rightContent !== this.rightOriginalContent
    },
    syntaxOk: function () {
      return !this.syntaxStatus.left && !this.syntaxStatus.right
    },
    remainingDiffList: function () {
      var list = []
      this.displayLines.forEach(function (row, idx) {
        if (row.type !== 'equal') {
          var preview = ''
          if (row.leftLine) preview = row.leftLine.content
          else if (row.rightLine) preview = row.rightLine.content
          list.push({
            type: row.type,
            lineIndex: idx,
            lineDisplay: row.leftLine ? row.leftLine.number : (row.rightLine ? row.rightLine.number : '?'),
            preview: (preview || '').substring(0, 60)
          })
        }
      })
      return list
    },
    diffIndices: function () {
      var indices = []
      this.displayLines.forEach(function (row, idx) {
        if (row.type !== 'equal') {
          indices.push(idx)
        }
      })
      return indices
    }
  },
  watch: {
    leftContent: function () {
      this.debouncedRecompute()
    },
    rightContent: function () {
      this.debouncedRecompute()
    }
  },
  mounted: function () {
    // 绑定全局快捷键
    document.addEventListener('keydown', this.handleGlobalKeydown)
  },
  beforeDestroy: function () {
    document.removeEventListener('keydown', this.handleGlobalKeydown)
    if (this.recomputeTimer) clearTimeout(this.recomputeTimer)
  },
  methods: {
    // ==================== 工具方法 ====================
    formatFileSize: function (bytes) {
      if (!bytes) return ''
      if (bytes < 1024) return bytes + ' B'
      if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
      return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
    },

    // ==================== 文件加载 ====================
    triggerFileInput: function (side) {
      if (side === 'left') {
        this.$refs.fileInputLeft.click()
      } else {
        this.$refs.fileInputRight.click()
      }
    },
    handleFileInputLeft: function (e) {
      var file = e.target.files[0]
      if (file) this.loadFile(file, 'left')
      e.target.value = ''
    },
    handleFileInputRight: function (e) {
      var file = e.target.files[0]
      if (file) this.loadFile(file, 'right')
      e.target.value = ''
    },
    handleDropLeft: function (e) {
      this.leftDropActive = false
      var file = e.dataTransfer.files[0]
      if (file) this.loadFile(file, 'left')
    },
    handleDropRight: function (e) {
      this.rightDropActive = false
      var file = e.dataTransfer.files[0]
      if (file) this.loadFile(file, 'right')
    },
    handleViewportDragEnter: function (e) {
      // 判断拖拽位置在左半还是右半
      var rect = this.$refs.viewport.getBoundingClientRect()
      var x = e.clientX - rect.left
      this.viewportDropSide = x < rect.width / 2 ? 'left' : 'right'
    },
    handleViewportDragOver: function (e) {
      var rect = this.$refs.viewport.getBoundingClientRect()
      var x = e.clientX - rect.left
      this.viewportDropSide = x < rect.width / 2 ? 'left' : 'right'
    },
    handleViewportDragLeave: function () {
      this.viewportDropSide = ''
    },
    handleViewportDrop: function (e) {
      var side = this.viewportDropSide
      this.viewportDropSide = ''
      var file = e.dataTransfer.files[0]
      if (file && side) {
        if (side === 'left' || (!this.leftContent && this.rightContent)) {
          this.loadFile(file, 'left')
        } else {
          // 如果右侧已修改，提醒用户
          if (this.isModified) {
            var self = this
            this.$confirm('右侧文件已修改，加载新文件将丢失当前修改。是否继续？', '提示', {
              confirmButtonText: '确定',
              cancelButtonText: '取消',
              type: 'warning'
            }).then(function () {
              self.loadFile(file, 'right')
            }).catch(function () {})
          } else {
            this.loadFile(file, 'right')
          }
        }
      }
    },
    loadFile: function (file, side) {
      var self = this
      var reader = new FileReader()
      // 大文件检测
      var largeFile = isLargeFile(file.size)

      reader.onload = function (e) {
        var content = e.target.result
        if (side === 'left') {
          self.leftContent = content
          self.leftOriginalContent = content
          self.leftFileName = file.name
          self.leftFileSize = file.size
        } else {
          self.rightContent = content
          self.rightOriginalContent = content
          self.rightFileName = file.name
          self.rightFileSize = file.size
          self.undoStack = []
          self.changeLog = []
        }
        // 大文件模式
        if (largeFile || (side === 'left' && isLargeFile(self.rightFileSize)) || (side === 'right' && isLargeFile(self.leftFileSize))) {
          self.isLargeFileMode = true
          if (self.diffMode === 'semantic') {
            self.diffMode = 'line'
          }
          self.$message.warning('文件较大（>5MB），已切换到大文件模式，仅支持行级比对')
        } else {
          self.isLargeFileMode = false
        }
        self.recompute()
      }
      reader.readAsText(file)
    },

    // ==================== 比对计算 ====================
    debouncedRecompute: function () {
      var self = this
      if (this.recomputeTimer) clearTimeout(this.recomputeTimer)
      this.recomputeTimer = setTimeout(function () {
        self.recompute()
      }, 300)
    },
    recompute: function () {
      if (!this.leftContent || !this.rightContent) {
        this.alignedLines = []
        this.displayLines = []
        this.diffStats = { added: 0, deleted: 0, modified: 0, equal: 0 }
        return
      }

      var leftLines = splitLines(this.leftContent)
      var rightLines = splitLines(this.rightContent)

      var fileType = this.leftFileType || this.rightFileType
      var isYaml = fileType === 'yaml'

      // 行级比对
      var aligned = computeLineDiff(leftLines, rightLines, {
        ignoreLineEnding: this.ignoreLineEnding,
        ignoreWhitespace: this.ignoreWhitespace,
        isYaml: isYaml
      })

      this.alignedLines = aligned

      // 语义比对模式（JSON/YAML）
      if (this.diffMode === 'semantic' && this.canSemanticDiff) {
        this.applySemanticDiff(aligned, leftLines, rightLines)
      } else {
        this.semanticOrderDiffs = []
        this.displayLines = aligned
      }

      // 统计
      this.diffStats = countDiffs(this.displayLines)

      // 生成原始差异清单（首次加载时记录）
      if (!this.originalDiffList.length || this.originalDiffList._snapshot !== this.leftOriginalContent + '|' + this.rightOriginalContent) {
        this.originalDiffList = this.buildDiffList(aligned)
        this.originalDiffList._snapshot = this.leftOriginalContent + '|' + this.rightOriginalContent
      }

      // 语法检查
      this.checkSyntax()

      // 更新底部 sticky 横向滚动条 spacer 宽度
      this.updateHScrollSpacer()
    },
    buildDiffList: function (aligned) {
      var list = []
      aligned.forEach(function (row, idx) {
        if (row.type !== 'equal') {
          var preview = ''
          if (row.leftLine) preview = row.leftLine.content
          else if (row.rightLine) preview = row.rightLine.content
          list.push({
            type: row.type,
            lineIndex: idx,
            lineDisplay: row.leftLine ? row.leftLine.number : (row.rightLine ? row.rightLine.number : '?'),
            preview: (preview || '').substring(0, 60)
          })
        }
      })
      return list
    },
    applySemanticDiff: function (aligned, leftLines, rightLines) {
      var fileType = this.leftFileType
      var leftObj = null
      var rightObj = null

      try {
        if (fileType === 'json') {
          leftObj = JSON.parse(this.leftContent)
          rightObj = JSON.parse(this.rightContent)
        }
      } catch (e) {
        // JSON 解析失败，回退到行级比对
        this.semanticOrderDiffs = []
        this.displayLines = aligned
        return
      }

      if (leftObj && rightObj) {
        // 检测语义差异
        var semanticDiffs = deepCompare(leftObj, rightObj)
        // 检测键序差异
        var orderDiffs = detectKeyOrderDiff(leftObj, rightObj)
        this.semanticOrderDiffs = orderDiffs

        // 对行标记语义顺序差异
        var display = aligned.map(function (row) {
          var newRow = Object.assign({}, row)
          // 检查此行是否仅为位置不同
          if (row.type !== 'equal' && row.leftLine && row.rightLine) {
            var leftTrimmed = row.leftLine.content.trim()
            var rightTrimmed = row.rightLine.content.trim()
            // 简化：如果内容在语义上可能只是位置不同
            for (var i = 0; i < orderDiffs.length; i++) {
              var od = orderDiffs[i]
              var keyPattern = '"' + od.key + '"'
              if (leftTrimmed.indexOf(keyPattern) !== -1 || rightTrimmed.indexOf(keyPattern) !== -1) {
                newRow.semanticOrderDiff = od
                // 语义相同但位置不同，调整类型
                if (semanticDiffs.length === 0 ||
                  !semanticDiffs.some(function (d) { return d.path.indexOf(od.key) !== -1 })) {
                  newRow.type = 'equal'
                  newRow.semanticOrderDiff = od
                }
                break
              }
            }
          }
          return newRow
        })
        this.displayLines = display
      } else {
        this.semanticOrderDiffs = []
        this.displayLines = aligned
      }
    },

    // ==================== 语法检查 ====================
    checkSyntax: function () {
      var leftType = this.leftFileType
      var rightType = this.rightFileType
      var status = { left: '', right: '' }

      if (leftType === 'json' && this.leftContent) {
        var lr = validateJson(this.leftContent)
        if (!lr.valid) status.left = lr.error
      }
      if (leftType === 'yaml' && this.leftContent) {
        var lyr = validateYaml(this.leftContent)
        if (!lyr.valid) status.left = lyr.error
      }
      if (rightType === 'json' && this.rightContent) {
        var rr = validateJson(this.rightContent)
        if (!rr.valid) status.right = rr.error
      }
      if (rightType === 'yaml' && this.rightContent) {
        var ryr = validateYaml(this.rightContent)
        if (!ryr.valid) status.right = ryr.error
      }
      this.syntaxStatus = status
    },

    // ==================== 字符级片段 ====================
    leftCharSegments: function (row) {
      if (!row.charDiffs) return []
      return row.charDiffs.filter(function (seg) {
        return seg.type === 'equal' || seg.type === 'removed'
      })
    },
    rightCharSegments: function (row) {
      if (!row.charDiffs) return []
      return row.charDiffs.filter(function (seg) {
        return seg.type === 'equal' || seg.type === 'added'
      })
    },
    charSegClass: function (seg, side) {
      if (seg.type === 'equal') return 'char-equal'
      if (side === 'left' && seg.type === 'removed') return 'char-removed'
      if (side === 'right' && seg.type === 'added') return 'char-added'
      return ''
    },
    lineContentClass: function (row, side) {
      var cls = {}
      if (row.type === 'added') {
        cls['line-added'] = side === 'left'
        cls['line-placeholder'] = side === 'right'
      } else if (row.type === 'deleted') {
        cls['line-placeholder'] = side === 'left'
        cls['line-deleted'] = side === 'right'
      } else if (row.type === 'modified') {
        cls['line-modified'] = true
      }
      if (row.semanticOrderDiff && this.diffMode === 'semantic') {
        cls['line-semantic-order'] = true
      }
      return cls
    },

    // ==================== 脱敏 ====================
    maskLine: function (text) {
      if (!text) return text
      if (this.maskEnabled) {
        return maskSensitiveContent(text)
      }
      return text
    },

    // ==================== 操作：复制行到右侧 ====================
    copyLineToRight: function (idx) {
      var row = this.displayLines[idx]
      if (!row || !row.leftLine) return
      this.pushUndo()

      var rightLines = splitLines(this.rightContent)
      // 计算插入位置：找到当前行在右侧文件中的正确位置
      var insertPos = this.findRightInsertPosition(idx)
      rightLines.splice(insertPos, 0, row.leftLine.content)
      this.rightContent = joinLines(rightLines, this.rightLineEnding || 'LF')

      this.addChangeLog('copy-line', idx, '← ' + row.leftLine.content.substring(0, 40))
      this.flashRow(idx)
    },

    // ==================== 操作：整行替换 ====================
    replaceRightLine: function (idx) {
      var row = this.displayLines[idx]
      if (!row || !row.leftLine || !row.rightLine) return
      this.pushUndo()

      var rightLines = splitLines(this.rightContent)
      var rightLineNum = row.rightLine.number - 1
      if (rightLineNum >= 0 && rightLineNum < rightLines.length) {
        rightLines[rightLineNum] = row.leftLine.content
        this.rightContent = joinLines(rightLines, this.rightLineEnding || 'LF')
      }

      this.addChangeLog('copy-line', idx, row.leftLine.content.substring(0, 40) + ' → 覆盖右侧第' + row.rightLine.number + '行')
      this.flashRow(idx)
    },

    // ==================== 操作：删除右侧行 ====================
    deleteRightLine: function (idx) {
      var row = this.displayLines[idx]
      if (!row || !row.rightLine) return
      this.pushUndo()

      var rightLines = splitLines(this.rightContent)
      var rightLineNum = row.rightLine.number - 1
      if (rightLineNum >= 0 && rightLineNum < rightLines.length) {
        var deleted = rightLines.splice(rightLineNum, 1)
        this.rightContent = joinLines(rightLines, this.rightLineEnding || 'LF')
        this.addChangeLog('delete-line', idx, '删除: ' + (deleted[0] || '').substring(0, 40))
      }
      this.flashRow(idx)
    },

    // ==================== 操作：片段替换 ====================
    fragmentReplace: function (idx, row, segIdx) {
      if (!row.charDiffs) return
      // 找到右侧片段
      var rightSegs = this.rightCharSegments(row)
      var targetSeg = rightSegs[segIdx]
      if (!targetSeg || targetSeg.type !== 'added') return

      // 找到对应的左侧片段（removed）
      var charDiffs = row.charDiffs
      var addedCount = 0
      var corresponding = null
      for (var i = 0; i < charDiffs.length; i++) {
        if (charDiffs[i].type === 'added') {
          // 计算这是右侧第几个差异片段
          var rightOnlyIdx = 0
          for (var j = 0; j < i; j++) {
            if (charDiffs[j].type === 'equal' || charDiffs[j].type === 'added') rightOnlyIdx++
          }
          if (rightOnlyIdx === segIdx) {
            // 查找前面紧邻的 removed
            if (i > 0 && charDiffs[i - 1].type === 'removed') {
              corresponding = charDiffs[i - 1]
            }
            break
          }
        }
      }

      this.pushUndo()
      var rightLines = splitLines(this.rightContent)
      var lineNum = row.rightLine.number - 1
      if (lineNum >= 0 && lineNum < rightLines.length) {
        var currentLine = rightLines[lineNum]
        var replacement = corresponding ? corresponding.value : ''
        // 替换片段
        var pos = currentLine.indexOf(targetSeg.value)
        if (pos !== -1) {
          rightLines[lineNum] = currentLine.substring(0, pos) + replacement + currentLine.substring(pos + targetSeg.value.length)
          this.rightContent = joinLines(rightLines, this.rightLineEnding || 'LF')
          this.addChangeLog('copy-fragment', idx, '"' + targetSeg.value + '" → "' + replacement + '"')
        }
      }
      this.flashRow(idx)
    },

    // ==================== 操作：语义重排 ====================
    reorderSemanticLine: function (idx, orderDiff) {
      if (!orderDiff || this.leftFileType !== 'json') return
      this.pushUndo()

      try {
        var rightObj = JSON.parse(this.rightContent)
        var leftObj = JSON.parse(this.leftContent)
        var reordered = reorderByReference(leftObj, rightObj)
        this.rightContent = JSON.stringify(reordered, null, 2)
        this.addChangeLog('reorder', idx, '按参照文件键序重排')

        // 闪烁动画
        this.semanticFlashIdx = idx
        var self = this
        setTimeout(function () {
          self.semanticFlashIdx = -1
        }, 1500)
      } catch (e) {
        this.$message.error('重排失败: ' + e.message)
      }
    },

    // ==================== 行编辑 ====================
    startEditLine: function (idx) {
      var row = this.displayLines[idx]
      if (!row) return
      // 只能编辑有内容的行/或不存在行（新增行）
      this.editingLineIdx = idx
      this.editBuffer = this.getEditableContent(row)
      this.$nextTick(function () {
        if (this.$refs.lineEditor && this.$refs.lineEditor[0]) {
          this.$refs.lineEditor[0].focus()
        }
      }.bind(this))
    },
    startAddLine: function (idx) {
      // 在不存在行处新增
      this.editingLineIdx = idx
      this.editBuffer = ''
      this.$nextTick(function () {
        if (this.$refs.lineEditor && this.$refs.lineEditor[0]) {
          this.$refs.lineEditor[0].focus()
        }
      }.bind(this))
    },
    getEditableContent: function (row) {
      if (row && row.rightLine) return row.rightLine.content
      return ''
    },
    onLineEdit: function (idx, event) {
      this.editBuffer = event.target.value
    },
    finishEditLine: function () {
      if (this.editingLineIdx < 0) return
      var idx = this.editingLineIdx
      var row = this.displayLines[idx]
      var newValue = this.editBuffer

      if (row && row.rightLine) {
        // 修改已有行
        var oldValue = row.rightLine.content
        if (newValue !== oldValue) {
          this.pushUndo()
          var rightLines = splitLines(this.rightContent)
          var lineNum = row.rightLine.number - 1
          if (lineNum >= 0 && lineNum < rightLines.length) {
            rightLines[lineNum] = newValue
            this.rightContent = joinLines(rightLines, this.rightLineEnding || 'LF')
            this.addChangeLog('manual-edit', idx, '行' + row.rightLine.number + ': "' + oldValue.substring(0, 20) + '" → "' + newValue.substring(0, 20) + '"')
          }
        }
      } else if (newValue) {
        // 新增行（在不存在行处输入）
        this.pushUndo()
        var rightLines2 = splitLines(this.rightContent)
        var insertPos = this.findRightInsertPosition(idx)
        rightLines2.splice(insertPos, 0, newValue)
        this.rightContent = joinLines(rightLines2, this.rightLineEnding || 'LF')
        this.addChangeLog('add-line', idx, '新增: "' + newValue.substring(0, 40) + '"')
      }

      this.editingLineIdx = -1
      this.editBuffer = ''
    },
    cancelEditLine: function () {
      this.editingLineIdx = -1
      this.editBuffer = ''
    },

    // ==================== 撤销 / 重新加载 ====================
    pushUndo: function () {
      this.undoStack.push(this.rightContent)
      if (this.undoStack.length > this.maxUndoSteps) {
        this.undoStack.shift()
      }
    },
    undo: function () {
      if (this.undoStack.length === 0) return
      this.rightContent = this.undoStack.pop()
      this.$message.info('已撤销')
    },
    reloadRight: function () {
      var self = this
      this.$confirm('是否放弃所有修改，重新加载右侧原始文件？', '确认重新加载', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(function () {
        self.rightContent = self.rightOriginalContent
        self.undoStack = []
        self.changeLog = []
        self.$message.success('已重新加载')
      }).catch(function () {})
    },

    // ==================== 保存 ====================
    showSaveDialog: function () {
      this.saveNewName = generateNewFileName(this.rightFileName)
      this.saveMode = 'new'
      this.saveDialogVisible = true
    },
    doSave: function () {
      var content = this.rightContent
      var fileName = this.saveMode === 'new' ? this.saveNewName : this.rightFileName

      if (!fileName) {
        this.$message.error('请输入文件名')
        return
      }

      // 创建 Blob 并下载（纯前端保存）
      var blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
      var url = URL.createObjectURL(blob)
      var link = document.createElement('a')
      link.href = url
      link.download = fileName
      link.style.display = 'none'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(url)

      this.saveDialogVisible = false
      this.$message.success('文件已保存: ' + fileName)

      // 如果是覆盖保存，更新原始内容
      if (this.saveMode === 'overwrite') {
        this.rightOriginalContent = content
        this.undoStack = []
      }
    },

    // ==================== 滚动 ====================
    onViewportScroll: function () {
      if (!this.$refs.viewport) return
      var scrollTop = this.$refs.viewport.scrollTop
      var rowHeight = 24
      this.visibleStart = Math.floor(scrollTop / rowHeight)
      this.visibleCount = Math.ceil(this.$refs.viewport.clientHeight / rowHeight)
    },
    scrollToLine: function (lineIndex) {
      if (!this.$refs.viewport) return
      var rowHeight = 24
      this.$refs.viewport.scrollTop = lineIndex * rowHeight
      this.flashRow(lineIndex)
    },
    onLeftPanelScroll: function () {
      if (this._syncScrolling) return
      this._syncScrolling = true
      if (this.$refs.rightScrollPanel) {
        this.$refs.rightScrollPanel.scrollLeft = this.$refs.leftScrollPanel.scrollLeft
      }
      // 同步 sticky 滚动条
      if (this.$refs.hscrollSticky) {
        this.$refs.hscrollSticky.scrollLeft = this.$refs.leftScrollPanel.scrollLeft
      }
      var self = this
      this.$nextTick(function () {
        self._syncScrolling = false
      })
    },
    onRightPanelScroll: function () {
      if (this._syncScrolling) return
      this._syncScrolling = true
      if (this.$refs.leftScrollPanel) {
        this.$refs.leftScrollPanel.scrollLeft = this.$refs.rightScrollPanel.scrollLeft
      }
      // 同步 sticky 滚动条
      if (this.$refs.hscrollSticky) {
        this.$refs.hscrollSticky.scrollLeft = this.$refs.rightScrollPanel.scrollLeft
      }
      var self = this
      this.$nextTick(function () {
        self._syncScrolling = false
      })
    },
    onHScrollSticky: function () {
      // 底部 sticky 滚动条驱动左右面板同步
      if (this._syncScrolling) return
      this._syncScrolling = true
      var sl = this.$refs.hscrollSticky.scrollLeft
      if (this.$refs.leftScrollPanel) {
        this.$refs.leftScrollPanel.scrollLeft = sl
      }
      if (this.$refs.rightScrollPanel) {
        this.$refs.rightScrollPanel.scrollLeft = sl
      }
      var self = this
      this.$nextTick(function () {
        self._syncScrolling = false
      })
    },
    updateHScrollSpacer: function () {
      var self = this
      this.$nextTick(function () {
        var leftPanel = self.$refs.leftScrollPanel
        if (leftPanel) {
          var inner = leftPanel.querySelector('.diff-panel-inner')
          if (inner) {
            self.$refs.hscrollSpacer.style.width = inner.scrollWidth + 'px'
          }
        }
      })
    },
    navigateDiff: function (direction) {
      if (this.diffIndices.length === 0) return
      if (direction > 0) {
        // 下一处差异
        var nextIdx = this.diffIndices.find(function (i) { return i > this.currentDiffIdx }.bind(this))
        if (nextIdx === undefined) nextIdx = this.diffIndices[0]
        this.currentDiffIdx = nextIdx
      } else {
        // 上一处差异
        var prevIdx = -1
        for (var i = this.diffIndices.length - 1; i >= 0; i--) {
          if (this.diffIndices[i] < this.currentDiffIdx) {
            prevIdx = this.diffIndices[i]
            break
          }
        }
        if (prevIdx === -1) prevIdx = this.diffIndices[this.diffIndices.length - 1]
        this.currentDiffIdx = prevIdx
      }
      this.scrollToLine(this.currentDiffIdx)
    },

    // ==================== 辅助方法 ====================
    findRightInsertPosition: function (displayIdx) {
      // 从 displayIdx 往前找到最近的右侧行号，计算插入位置
      for (var i = displayIdx - 1; i >= 0; i--) {
        var r = this.displayLines[i]
        if (r && r.rightLine) {
          return r.rightLine.number // 行号 = 下标+1，正好是下一个插入点
        }
      }
      return 0
    },
    flashRow: function (idx) {
      var self = this
      this.highlightRowIdx = idx
      setTimeout(function () {
        self.highlightRowIdx = -1
      }, 800)
    },
    addChangeLog: function (action, lineIndex, detail) {
      var row = this.displayLines[lineIndex]
      this.changeLog.push({
        action: action,
        lineIndex: lineIndex,
        lineDisplay: row && row.leftLine ? row.leftLine.number : (row && row.rightLine ? row.rightLine.number : '?'),
        detail: detail,
        timestamp: new Date().toLocaleTimeString()
      })
    },
    clearChangeLog: function () {
      this.changeLog = []
    },
    onSummaryToggle: function (collapsed) {
      this.summaryCollapsed = collapsed
    },
    handleDiffModeChange: function () {
      this.recompute()
      if (this.diffMode === 'semantic') {
        this.$message.info('已切换到语义比对模式')
      }
    },

    // ==================== 快捷键 ====================
    handleGlobalKeydown: function (e) {
      // Ctrl + S
      if (e.ctrlKey && e.key === 's') {
        e.preventDefault()
        if (this.rightContent) {
          this.showSaveDialog()
        }
      }
      // Ctrl + Z
      if (e.ctrlKey && e.key === 'z') {
        e.preventDefault()
        this.undo()
      }
    },
    handleKeydown: function () {
      // 组件级按键（预留）
    }
  }
}
</script>

<style scoped>
/* ==================== 全局布局 ==================== */
.text-comparison {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  outline: none;
  font-family: 'Microsoft YaHei', 'PingFang SC', -apple-system, sans-serif;
}

/* ==================== 工具栏 ==================== */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: #fff;
  border-bottom: 1px solid #dcdfe6;
  flex-shrink: 0;
  flex-wrap: wrap;
  gap: 8px;
}
.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.toolbar-center {
  display: flex;
  align-items: center;
}
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.toolbar-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.toolbar-switch {
  margin: 0 2px;
}
.toolbar-switch >>> .el-switch__label {
  font-size: 12px;
}

/* ==================== 主内容 ==================== */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ==================== 文件头 ==================== */
.file-headers {
  display: flex;
  border-bottom: 1px solid #dcdfe6;
  background: #fafafa;
  flex-shrink: 0;
}
.file-header {
  flex: 1;
  padding: 8px 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.file-header-left {
  border-right: 1px solid #ebeef5;
}
.file-header-right {
  border-left: 1px solid #ebeef5;
}
.file-header-gutter {
  width: 48px;
  flex-shrink: 0;
  background: #f0f2f5;
}
.file-header-minimap {
  width: 40px;
  flex-shrink: 0;
  background: #f5f5f5;
}
.file-header-summary {
  width: 280px;
  flex-shrink: 0;
  background: #fafafa;
}
.file-label {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  flex-shrink: 0;
}
.file-name {
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: 'Consolas', 'Monaco', monospace;
}
.file-placeholder {
  font-size: 12px;
  color: #c0c4cc;
  font-style: italic;
}

/* ==================== Sticky 表头行（diff-viewport 内，固定不随滚动移动） ==================== */
.sticky-header-row {
  position: sticky;
  top: 0;
  display: flex;
  background: #fafafa;
  border-bottom: 1px solid #dcdfe6;
  z-index: 6;
  flex-shrink: 0;
}
.sticky-header-left {
  flex: 1;
  padding: 8px 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.sticky-header-right {
  flex: 1;
  padding: 8px 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  border-left: 1px solid #ebeef5;
}
.sticky-header-gutter {
  width: 48px;
  flex-shrink: 0;
  background: #f0f2f5;
}
.sticky-header-minimap {
  width: 40px;
  flex-shrink: 0;
  background: #f5f5f5;
}

/* ==================== Diff 主体 ==================== */
.diff-body {
  flex: 1;
  display: flex;
  overflow: hidden;
  position: relative;
}

/* ==================== 拖拽区 ==================== */
.drop-zone {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px dashed #dcdfe6;
  margin: 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  background: #fff;
}
.drop-zone:hover,
.drop-zone.drop-active {
  border-color: #409eff;
  background: #ecf5ff;
}
.drop-zone-left {
  margin-right: 8px;
}
.drop-zone-right {
  margin-left: 8px;
}
.drop-hint {
  text-align: center;
  color: #909399;
}
.drop-hint p {
  margin: 8px 0;
  font-size: 14px;
}
.drop-sub {
  font-size: 12px !important;
  color: #c0c4cc !important;
}

/* ==================== 已加载占位区 ==================== */
.loaded-zone {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px dashed #e1f3d8;
  margin: 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  background: #f0f9eb;
}
.loaded-zone:hover {
  border-color: #67c23a;
  background: #ecf8e5;
}
.loaded-zone-left {
  margin-right: 8px;
}
.loaded-zone-right {
  margin-left: 8px;
}
.loaded-card {
  text-align: center;
}
.loaded-icon {
  font-size: 40px;
  color: #67c23a;
}
.loaded-title {
  margin: 8px 0 4px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.loaded-filename {
  margin: 4px 0;
  font-size: 13px;
  color: #409eff;
  font-family: 'Consolas', 'Monaco', monospace;
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.loaded-meta {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin: 6px 0;
}
.loaded-size {
  font-size: 12px;
  color: #909399;
}
.loaded-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #c0c4cc;
  font-style: italic;
}

/* ==================== 视口（仅纵向滚动） ==================== */
.diff-viewport {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  background: #fff;
  position: relative;
}

/* ==================== 三栏面板布局 ==================== */
.diff-panels-wrapper {
  display: flex;
  min-height: 100%;
}

/* 左/右面板（独立横向滚动） */
.diff-panel-scroll {
  flex: 1;
  overflow-x: auto;
  overflow-y: hidden;
  min-width: 0;
  /* 隐藏原生滚动条，改用底部 sticky 滚动条 */
  scrollbar-width: none;          /* Firefox */
  -ms-overflow-style: none;       /* IE/Edge */
}
.diff-panel-scroll::-webkit-scrollbar {
  display: none;                  /* Chrome/Safari */
}
.diff-panel-scroll-left {
  margin-right: 0;
}
.diff-panel-scroll-right {
  margin-left: 0;
}
.diff-panel-inner {
  display: inline-block;
  min-width: 100%;
}

/* ==================== 中间操作栏（固定不滚动） ==================== */
.diff-gutter-fixed {
  width: 48px;
  min-width: 48px;
  max-width: 48px;
  flex-shrink: 0;
  background: #f0f2f5;
  position: sticky;
  top: 0;
  z-index: 2;
}
.gutter-row {
  min-height: 24px;
  line-height: 24px;
  border-bottom: 1px solid #f2f6fc;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
}

/* ==================== 底部 sticky 横向滚动条 ==================== */
.hscroll-sticky {
  position: sticky;
  bottom: 0;
  left: 0;
  right: 0;
  height: 12px;
  overflow-x: auto;
  overflow-y: hidden;
  background: #f5f5f5;
  border-top: 1px solid #e0e0e0;
  z-index: 3;
}
.hscroll-sticky::-webkit-scrollbar {
  height: 8px;
}
.hscroll-sticky::-webkit-scrollbar-thumb {
  background: #c0c4cc;
  border-radius: 4px;
}
.hscroll-sticky::-webkit-scrollbar-thumb:hover {
  background: #909399;
}
.hscroll-sticky::-webkit-scrollbar-track {
  background: transparent;
}
.hscroll-spacer {
  height: 1px;          /* 不可见，仅撑开 scrollWidth */
  pointer-events: none;
}

/* ==================== 面板行 ==================== */
.diff-panel-row {
  display: flex;
  min-height: 24px;
  line-height: 24px;
  border-bottom: 1px solid #f2f6fc;
  transition: background 0.15s;
}
.diff-panel-row:hover {
  background: rgba(64, 158, 255, 0.03);
}
.diff-row-highlight {
  animation: rowFlash 0.8s ease;
}
@keyframes rowFlash {
  0%, 100% { background: transparent; }
  30% { background: rgba(64, 158, 255, 0.2); }
  60% { background: rgba(64, 158, 255, 0.1); }
}
.diff-row-semantic-order {
  animation: semanticPulse 1.5s ease;
}
@keyframes semanticPulse {
  0%, 100% { background: transparent; }
  25% { background: rgba(230, 162, 60, 0.15); }
  50% { background: transparent; }
  75% { background: rgba(230, 162, 60, 0.1); }
}

/* ==================== 行号 ==================== */
.line-num {
  width: 48px;
  min-width: 48px;
  text-align: right;
  padding: 0 8px 0 4px;
  font-size: 12px;
  font-family: 'Consolas', 'Monaco', monospace;
  color: #c0c4cc;
  background: #fafafa;
  flex-shrink: 0;
  user-select: none;
  border-right: 1px solid #ebeef5;
}
.line-num-left {
  border-right: 1px solid #ebeef5;
}
.line-num-right {
  border-left: 1px solid #ebeef5;
  border-right: 1px solid #ebeef5;
}
.line-num-diff {
  color: #909399;
  font-weight: 600;
}

/* ==================== 行内容 ==================== */
.line-content {
  flex: 1;
  padding: 0 8px;
  font-size: 13px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  white-space: pre;
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: 0;
  position: relative;
}
.line-content-left {
  border-right: none;
}
.line-content-right {
  border-left: none;
  cursor: text;
}

/* 行类型样式 */
.line-added {
  background: #e6ffed;
  border-left: 3px solid #4caf50;
}
.line-deleted {
  background: #ffeef0;
  border-left: 3px solid #f44336;
}
.line-modified {
  background: #fff8e1;
}
.line-placeholder {
  background: repeating-linear-gradient(
    -45deg,
    #f5f5f5,
    #f5f5f5 4px,
    #e8e8e8 4px,
    #e8e8e8 8px
  );
}
.line-semantic-order {
  background: #fff3e0 !important;
  border-left: 3px solid #ff9800;
}

.placeholder-line {
  display: block;
  width: 100%;
  height: 24px;
  font-style: italic;
  color: #c0c4cc;
}
.placeholder-editable {
  cursor: text;
}
.placeholder-editable:hover {
  background: rgba(64, 158, 255, 0.05);
}

/* 字符级高亮 */
.char-equal {
  /* 默认样式 */
}
.char-removed {
  background: #fdb8c0;
  border-radius: 2px;
  text-decoration: line-through;
  text-decoration-color: rgba(244, 67, 54, 0.5);
}
.char-added {
  background: #acf2bd;
  border-radius: 2px;
  cursor: pointer;
  position: relative;
}
.char-added:hover {
  background: #85e89d;
  outline: 1px solid #34d058;
}
.line-content-right .char-added:hover::after {
  content: '双击替换';
  position: absolute;
  top: -28px;
  left: 50%;
  transform: translateX(-50%);
  background: #303133;
  color: #fff;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  white-space: nowrap;
  z-index: 10;
  pointer-events: none;
}

/* ==================== 中间操作栏按钮 ==================== */
.diff-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.indicator-added { background: #4caf50; }
.indicator-deleted { background: #f44336; }
.indicator-modified { background: #ff9800; }

.gutter-btn {
  width: 24px;
  height: 18px;
  border: 1px solid #dcdfe6;
  border-radius: 3px;
  background: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: #606266;
  transition: all 0.2s;
  padding: 0;
}
.gutter-btn:hover {
  border-color: #409eff;
  color: #409eff;
  background: #ecf5ff;
}
.gutter-btn-copy { color: #4caf50; }
.gutter-btn-copy:hover { border-color: #4caf50; background: #e8f5e9; }
.gutter-btn-replace { color: #ff9800; }
.gutter-btn-replace:hover { border-color: #ff9800; background: #fff3e0; }
.gutter-btn-reorder { color: #9c27b0; }
.gutter-btn-reorder:hover { border-color: #9c27b0; background: #f3e5f5; }

/* ==================== 删除按钮 ==================== */
.delete-line-btn {
  position: absolute;
  left: -2px;
  top: 50%;
  transform: translateY(-50%);
  width: 18px;
  height: 18px;
  border: 1px solid #f44336;
  border-radius: 50%;
  background: #fff;
  color: #f44336;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  z-index: 5;
  transition: all 0.2s;
  padding: 0;
}
.delete-line-btn:hover {
  background: #f44336;
  color: #fff;
}

/* ==================== 行编辑器 ==================== */
.line-editor {
  width: 100%;
  height: 22px;
  border: 1px solid #409eff;
  border-radius: 2px;
  outline: none;
  font-size: 13px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  padding: 0 4px;
  background: #fff;
  box-sizing: border-box;
}
.line-editor:focus {
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

/* ==================== 视口拖拽覆盖 ==================== */
.viewport-drop-overlay {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(64, 158, 255, 0.1);
  border: 2px dashed #409eff;
  z-index: 100;
  pointer-events: none;
}
.drop-overlay-left {
  left: 0;
}
.drop-overlay-right {
  right: 0;
  left: 50%;
}
.drop-overlay-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #409eff;
  font-size: 16px;
  font-weight: 600;
}
.drop-overlay-hint i {
  font-size: 32px;
}

/* ==================== 保存对话框 ==================== */
.save-warning {
  margin-bottom: 12px;
}
.save-recommend {
  color: #67c23a;
  font-size: 12px;
  margin-left: 4px;
}

/* ==================== 响应式 ==================== */
@media (max-width: 1200px) {
  .file-header-summary {
    width: 200px;
  }
}
@media (max-width: 900px) {
  .toolbar {
    flex-direction: column;
    align-items: flex-start;
  }
  .file-header-summary {
    display: none;
  }
}
</style>