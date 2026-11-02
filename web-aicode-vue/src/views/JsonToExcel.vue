<!-- src/views/JsonToExcel.vue -->
<template>
  <div class="json-excel-page">
    <div class="json-excel-container">
      <!-- 左侧：输入区域 -->
      <div class="input-section card">
        <div class="section-header">
          <h3><i class="el-icon-edit"></i> 输入 JSON</h3>
          <div class="header-actions">
            <el-button size="mini" type="text" icon="el-icon-magic-stick" @click="fillExample">示例</el-button>
            <el-button size="mini" type="text" icon="el-icon-delete" @click="clearAll">清空</el-button>
          </div>
        </div>
        <div class="input-content">
          <el-input
            v-model="inputJson"
            type="textarea"
            placeholder="粘贴任意结构的 JSON 串（以最深层记录为一行，父级字段逐层铺开，几套叶子记录就几张 Sheet）..."
            :rows="20"
            @input="handleInputChange"
          />
          <div class="parse-status">
            <span v-if="!analyzed" class="status-idle">
              <i class="el-icon-info"></i> 尚未解析
            </span>
            <span v-else-if="parseError" class="status-error">
              <i class="el-icon-circle-close"></i> {{ parseError }}
            </span>
            <span v-else class="status-ok">
              <i class="el-icon-circle-check"></i>
              合法 JSON · 顶层{{ rootType }} · {{ leafCount }} 套叶子记录 · {{ sheetCount }} 个 Sheet · {{ totalRows }} 行数据{{ fieldTip }}
            </span>
            <span v-if="bigintCount" class="status-bigint">
              <i class="el-icon-alarm-clock"></i> {{ bigintCount }} 处超出安全范围的整数已按文本保真（避免 ID 失真 / 科学计数法）
            </span>
            <span v-if="stale" class="status-stale">
              <i class="el-icon-warning-outline"></i> 输入已变更，预览与导出仍为上次解析结果
            </span>
          </div>
        </div>

        <!-- 字段勾选：shape 结构树（勾选只影响列，不影响行粒度） -->
        <div v-if="shapeRoots.length" class="tree-panel">
          <div class="tree-header">
            <span class="tree-title">
              <i class="el-icon-set-up"></i> 字段勾选
              <em>{{ pickedCount }}/{{ selectableCount }}</em>
              <el-tag v-if="treeDirty" size="mini" type="warning">未应用</el-tag>
            </span>
            <div class="header-actions">
              <el-button size="mini" type="text" @click="checkAll">全选</el-button>
              <el-button size="mini" type="text" @click="checkNone">全不选</el-button>
              <el-button size="mini" type="text" @click="toggleExpand">{{ expandAll ? '折叠全部' : '展开全部' }}</el-button>
              <el-button size="mini" type="primary" :disabled="!treeDirty" :loading="analyzing" @click="applySelection">应用勾选</el-button>
            </div>
          </div>
          <el-tree
            ref="shapeTree"
            class="shape-tree"
            :data="shapeRoots"
            :props="treeProps"
            node-key="key"
            show-checkbox
            check-strictly
            :default-expand-all="true"
            :expand-on-click-node="false"
            @check="onTreeCheck"
          >
            <template slot-scope="{ data }">
              <span class="tnode" :class="{ 'is-locked': data.locked, 'is-off': !data.locked && !isChecked(data.key) }">
                <span class="tname">{{ data.name }}</span>
                <span class="tmeta">{{ data.typeText }}<template v-if="data.countText"> · {{ data.countText }}</template></span>
                <el-tag v-if="data.locked" size="mini" type="info" class="tlock">{{ data.role }}</el-tag>
                <span v-else class="trole">{{ data.role }}</span>
              </span>
            </template>
          </el-tree>
          <div class="tree-tip">
            锁定项是结构节点（对象 / 行粒度数组 / 祖先数组），取消会破坏「一行 = 一个叶子记录」，已自动还原；
            勾选只删列、不减行，Sheet 数与行数保持不变。
          </div>
        </div>
      </div>

      <!-- 右侧：Sheet 预览 -->
      <div class="output-section card">
        <div class="section-header">
          <h3><i class="el-icon-s-data"></i> Sheet 拆分预览</h3>
          <div class="header-actions">
            <el-button
              size="mini"
              type="primary"
              icon="el-icon-view"
              :disabled="!inputJson.trim()"
              :loading="analyzing"
              @click="analyze"
            >
              解析预览
            </el-button>
          </div>
        </div>

        <div class="option-bar">
          <el-checkbox v-model="opts.fillEmptyLeafRows" @change="analyze">无子记录时补行</el-checkbox>
          <el-checkbox v-model="opts.dedupMarks" @change="analyze">父层 _uniq 标记列</el-checkbox>
          <el-checkbox v-model="opts.includeStructTree" @change="analyze">生成 _结构树</el-checkbox>
          <el-checkbox v-model="opts.includeOverview" @change="analyze">生成 _结构总览</el-checkbox>
          <el-checkbox v-model="opts.includeMeta" @change="analyze">附带 _meta</el-checkbox>
          <el-select v-model="opts.booleanAs" size="mini" style="width: 148px" @change="analyze">
            <el-option label="布尔 true/false" value="text"></el-option>
            <el-option label="布尔 是/否" value="cn"></el-option>
            <el-option label="布尔 1/0" value="number"></el-option>
          </el-select>
        </div>

        <div class="output-content">
          <div v-if="!sheetCount" class="empty-tip">
            <i class="el-icon-document"></i>
            <p>粘贴 JSON 后点击「解析预览」，规则为：叶子记录成行、祖先字段铺到根、几套叶子几张表</p>
          </div>
          <div v-else class="preview-wrap">
            <el-alert
              v-for="(w, i) in warnings"
              :key="'w' + i"
              :title="w.msg"
              :type="w.level === 'warn' ? 'warning' : 'info'"
              :closable="false"
              show-icon
              class="warn-item"
            />
            <el-tabs v-model="activeSheet" type="card" class="sheet-tabs">
              <el-tab-pane
                v-for="sheet in sheets"
                :key="sheet.name"
                :name="sheet.name"
                :label="tabLabel(sheet)"
              >
                <div class="sheet-meta">
                  <el-tag size="mini" :type="kindTagType(sheet)">
                    {{ kindLabel(sheet) }}
                  </el-tag>
                  <span class="meta-text">JSON 路径：{{ sheet.path || '(工具表)' }}</span>
                  <span class="meta-text">共 {{ sheet.rows.length }} 行 × {{ sheet.columns.length }} 列</span>
                  <span v-if="sheet.rows.length > previewLimit" class="meta-text">
                    预览仅显示前 {{ previewLimit }} 行，导出为全量
                  </span>
                  <span v-if="uniqHint(sheet)" class="meta-text meta-hint">
                    按父级计数请用 SUM({{ uniqHint(sheet) }})，直接数行数会按子记录翻倍
                  </span>
                </div>
                <el-table
                  :data="previewRows(sheet)"
                  border
                  size="mini"
                  max-height="380"
                  class="preview-table"
                >
                  <el-table-column
                    v-for="(col, idx) in sheet.columns"
                    :key="col.key"
                    :prop="'c' + idx"
                    :label="col.name"
                    :min-width="130"
                    show-overflow-tooltip
                  >
                    <template slot="header">
                      <span :class="{ 'col-internal': col.internal }" :title="colTip(col)">
                        {{ col.name }}
                      </span>
                    </template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>
            </el-tabs>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部操作栏 -->
    <div class="action-bar">
      <el-input
        v-model="fileName"
        size="small"
        placeholder="导出文件名"
        prefix-icon="el-icon-document"
        style="width: 240px"
      />
      <el-button
        type="primary"
        icon="el-icon-download"
        :disabled="!sheetCount"
        :loading="exporting"
        @click="exportExcel"
      >
        导出 Excel
      </el-button>
      <el-button
        type="success"
        icon="el-icon-copy-document"
        :disabled="!sheetCount"
        @click="copyCurrentSheet"
      >
        复制当前 Sheet
      </el-button>
      <el-button
        type="info"
        icon="el-icon-delete"
        @click="clearAll"
      >
        清空
      </el-button>
    </div>
  </div>
</template>

<script>
import { jsonToSheets, sheetToMatrix, columnWidths, displayValue, parseJsonSafe, describeShapeTree } from '@/utils/jsonToSheets'
import copyToClipboard from '@/utils/clipboard'

var PREVIEW_LIMIT = 20

export default {
  name: 'JsonToExcel',
  data() {
    return {
      inputJson: '',
      analyzed: false,
      stale: false,
      analyzing: false,
      exporting: false,
      parseError: '',
      sheets: [],
      warnings: [],
      activeSheet: '',
      fileName: '',
      previewLimit: PREVIEW_LIMIT,
      bigintCount: 0,
      // 结构树与勾选状态（shape 粒度：一个字段名 = 一个节点）
      shapeResult: null,
      shapeRoots: [],
      lockedKeys: [],
      selectableKeys: [],
      checkedKeys: [],
      appliedExcluded: [],
      lastParsed: null,
      expandAll: true,
      treeProps: { children: 'children', label: 'name' },
      opts: {
        fillEmptyLeafRows: true,
        dedupMarks: true,
        includeStructTree: true,
        includeOverview: true,
        includeMeta: false,
        booleanAs: 'text'
      }
    }
  },
  computed: {
    sheetCount() {
      return this.sheets.length
    },
    totalRows() {
      return this.sheets.reduce(function (sum, s) {
        return sum + (s.kind === 'data' ? s.rows.length : 0)
      }, 0)
    },
    leafCount() {
      return this.sheets.filter(function (s) { return s.kind === 'data' }).length
    },
    rootType() {
      var text = this.inputJson.trim()
      if (!text) return '-'
      if (text.charAt(0) === '[') return '数组'
      if (text.charAt(0) === '{') return '对象'
      return '标量'
    },
    selectableCount() {
      return this.selectableKeys.length
    },
    pickedCount() {
      var checked = this.keySet(this.checkedKeys)
      return this.selectableKeys.filter(function (k) { return !!checked[k] }).length
    },
    // 当前勾选对应的排除列（锁定节点不参与勾选，永远不会出现在这里）
    pendingExcluded() {
      var checked = this.keySet(this.checkedKeys)
      return this.selectableKeys.filter(function (k) { return !checked[k] })
    },
    treeDirty() {
      if (this.pendingExcluded.length !== this.appliedExcluded.length) return true
      var applied = this.keySet(this.appliedExcluded)
      return this.pendingExcluded.some(function (p) { return !applied[p] })
    },
    fieldTip() {
      if (!this.selectableKeys.length) return ''
      var off = this.appliedExcluded.length
      if (!off) return ''
      return ' · 字段 ' + (this.selectableKeys.length - off) + '/' + this.selectableKeys.length + '（已勾掉 ' + off + '）'
    }
  },
  methods: {
    handleInputChange() {
      this.analyzed = false
      // 输入改动后旧预览仍在，需明确提示“未重新解析”，否则会误以为预览就是新数据
      this.stale = this.sheets.length > 0
    },

    // ==================== 解析 ====================
    analyze() {
      this.analyzed = false
      this.stale = false
      this.parseError = ''
      this.sheets = []
      this.warnings = []
      this.activeSheet = ''
      this.bigintCount = 0
      this.clearShapeState()
      var text = this.inputJson.trim()
      if (!text) return

      this.analyzing = true
      var self = this
      // 让 loading 先渲染，避免大 JSON 同步解析时界面假死
      this.$nextTick(function () {
        try {
          // 不用裸 JSON.parse：超出 2^53-1 的整数字面量在 parse 阶段已被丢精度且不可恢复
          var parsed = parseJsonSafe(text)
          if (!parsed.ok) {
            self.parseError = self.describeParseError(parsed.error.message, text)
            self.analyzed = true
            return
          }
          self.bigintCount = parsed.bigints.length
          self.lastParsed = parsed
          // 结构树在页面算一次，引擎复用同一份叶子集，避免大 JSON 走两遍
          self.applyShape(describeShapeTree(parsed.data))
          self.renderSheets([])
          self.analyzed = true
        } catch (e) {
          self.parseError = e.message
          self.analyzed = true
          self.$message.error('解析失败: ' + e.message)
        } finally {
          self.analyzing = false
        }
      })
    },

    // 用当前勾选（excluded）重新拆分并刷新预览，不重新解析文本
    renderSheets(excluded) {
      var data = this.lastParsed ? this.lastParsed.data : null
      var res = jsonToSheets(data, Object.assign({}, this.opts, {
        excluded: excluded,
        leaves: this.shapeResult ? this.shapeResult.leaves : null,
        shape: this.shapeResult
      }))
      this.sheets = res.sheets
      this.warnings = res.warnings
      this.activeSheet = res.sheets.length ? res.sheets[0].name : ''
      this.appliedExcluded = (excluded || []).slice()
      if (!this.fileName) this.fileName = this.buildFileName()
      if (!res.sheets.length) this.$message.warning('未识别到可导出的数据')
      return res
    },

    applyShape(shape) {
      this.shapeResult = shape && shape.nodes && shape.nodes.length ? shape : null
      if (!this.shapeResult) return
      this.shapeRoots = this.shapeResult.tree || []
      this.lockedKeys = (this.shapeResult.lockedKeys || []).slice()
      this.selectableKeys = (this.shapeResult.selectableKeys || []).slice()
      // 默认全选；锁定项也置为勾选，使其不能通过取消来逃避（el-tree 的 disabled 会
      // 连程序化 setChecked 一起忽略，所以上层用自身逻辑拦截，不靠 disabled）
      this.checkedKeys = this.lockedKeys.concat(this.selectableKeys)
      this.expandAll = true
      var self = this
      this.$nextTick(function () {
        if (self.$refs.shapeTree) self.$refs.shapeTree.setCheckedKeys(self.checkedKeys)
      })
    },

    clearShapeState() {
      this.shapeResult = null
      this.shapeRoots = []
      this.lockedKeys = []
      this.selectableKeys = []
      this.checkedKeys = []
      this.appliedExcluded = []
      this.lastParsed = null
    },

    keySet(list) {
      var map = {}
      for (var i = 0; i < list.length; i++) map[list[i]] = true
      return map
    },

    isChecked(key) {
      return this.checkedKeys.indexOf(key) >= 0
    },

    onTreeCheck(data, info) {
      var keys = (info && info.checkedKeys) || []
      if (data && this.keySet(this.lockedKeys)[data.key]) {
        // 结构节点被取消：还原到上次勾选状态，否则行粒度/父级身份会丢
        var self = this
        this.$nextTick(function () {
          if (self.$refs.shapeTree) self.$refs.shapeTree.setCheckedKeys(self.checkedKeys)
        })
        this.$message.info('「' + data.name + '」是' + data.role + '，属结构节点，不可取消')
        return
      }
      this.checkedKeys = keys.slice()
    },

    setChecked(keys) {
      this.checkedKeys = keys.slice()
      if (this.$refs.shapeTree) this.$refs.shapeTree.setCheckedKeys(keys.slice())
    },

    checkAll() {
      this.setChecked(this.lockedKeys.concat(this.selectableKeys))
    },

    checkNone() {
      // “全不选”只清空可选字段，结构节点保留（行粒度不变）
      this.setChecked(this.lockedKeys.slice())
    },

    toggleExpand() {
      var tree = this.$refs.shapeTree
      if (!tree) return
      var nodesMap = tree.store.nodesMap
      var target = !this.expandAll
      Object.keys(nodesMap).forEach(function (k) {
        if (nodesMap[k] && nodesMap[k].childNodes && nodesMap[k].childNodes.length) nodesMap[k].expanded = target
      })
      this.expandAll = target
    },

    applySelection() {
      if (!this.lastParsed) return
      var excluded = this.pendingExcluded
      this.analyzing = true
      var self = this
      this.$nextTick(function () {
        try {
          self.renderSheets(excluded)
          self.$message.success(excluded.length
            ? '已按勾选过滤 ' + excluded.length + ' 个字段（行数与 Sheet 数不变）'
            : '已恢复全部字段')
        } catch (e) {
          self.$message.error('应用勾选失败: ' + e.message)
        } finally {
          self.analyzing = false
        }
      })
    },

    describeParseError(message, text) {
      var matched = /position (\d+)/.exec(message)
      if (!matched) return 'JSON 解析失败：' + message
      var pos = Number(matched[1])
      var before = text.slice(0, pos)
      var line = before.split('\n').length
      var col = pos - before.lastIndexOf('\n')
      return 'JSON 解析失败：第 ' + line + ' 行第 ' + col + ' 列附近（' + message + '）'
    },

    buildFileName() {
      var now = new Date()
      var pad = function (n) { return n < 10 ? '0' + n : '' + n }
      return 'json-export-' + now.getFullYear() + pad(now.getMonth() + 1) + pad(now.getDate()) +
        '-' + pad(now.getHours()) + pad(now.getMinutes())
    },

    // ==================== 预览 ====================
    tabLabel(sheet) {
      if (sheet.kind !== 'data') return sheet.name
      return sheet.name + ' ' + sheet.rows.length + '×' + sheet.columns.length
    },
    kindLabel(sheet) {
      if (sheet.kind === 'meta') return '元数据表'
      if (sheet.kind === 'overview') return '结构总览'
      if (sheet.kind === 'struct') return '结构树（全量 JSON 结构）'
      var chain = (sheet.leafChain || []).join(' → ')
      return '叶子宽表（一行 = 一个 ' + (chain.split(' → ').pop() || '记录') + '）'
    },
    kindTagType(sheet) {
      if (sheet.kind === 'meta') return 'warning'
      if (sheet.kind === 'overview') return 'info'
      if (sheet.kind === 'struct') return 'success'
      return 'primary'
    },
    // 列提示：真实覆盖率以全表行数为分母，缺字段行数单列出来
    colTip(col) {
      var src = col.sourcePath.charAt(0) === '#' ? '(标记列，非 JSON 字段)' : col.sourcePath
      var tail = col.missing ? '，缺字段 ' + col.missing + ' 行' : ''
      return src + '（' + col.type + '，非空率 ' + col.rate + '%' + tail + '）'
    },
    // 提醒：宽表的父级计数必须用 _uniq 列，否则行数被子记录翻倍
    uniqHint(sheet) {
      if (sheet.kind !== 'data') return ''
      var marks = sheet.columns.filter(function (c) { return c.name && String(c.name).indexOf('._uniq') > 0 })
      return marks.map(function (c) { return c.name }).join('、')
    },
    previewRows(sheet) {
      var booleanAs = this.opts.booleanAs
      var limited = sheet.rows.slice(0, PREVIEW_LIMIT)
      // el-table 的 prop 会把「.」当层级路径，而嵌套列的列名本身含点（如 meta.creator.name），
      // 因此预览数据统一用 c0/c1 这类安全键，列名只用于表头展示
      return limited.map(function (row) {
        var item = {}
        sheet.columns.forEach(function (col, idx) {
          item['c' + idx] = displayValue(row[col.key], booleanAs)
        })
        return item
      })
    },

    // ==================== 导出 ====================
    async exportExcel() {
      if (!this.sheets.length) {
        this.$message.warning('请先解析 JSON')
        return
      }
      this.exporting = true
      try {
        var lib = await import('xlsx')
        // xlsx 为 CJS 包，不同打包环境下命名空间可能多一层 default
        var XLSX = lib.utils ? lib : (lib.default || lib)
        var wb = XLSX.utils.book_new()
        var booleanAs = this.opts.booleanAs
        var skipped = []

        this.sheets.forEach(function (sheet) {
          var matrix = sheetToMatrix(sheet, booleanAs)
          if (!matrix.header.length) {
            skipped.push(sheet.name)
            return
          }
          var ws = {}
          var all = [matrix.header].concat(matrix.rows)
          all.forEach(function (cells, r) {
            cells.forEach(function (cell, c) {
              if (cell) ws[XLSX.utils.encode_cell({ r: r, c: c })] = cell
            })
          })
          ws['!ref'] = XLSX.utils.encode_range({
            s: { r: 0, c: 0 },
            e: { r: all.length - 1, c: matrix.header.length - 1 }
          })
          ws['!cols'] = columnWidths(sheet, booleanAs)
          XLSX.utils.book_append_sheet(wb, ws, sheet.name)
        })

        if (!wb.SheetNames.length) {
          this.$message.error('没有可导出的 Sheet')
          return
        }
        var name = (this.fileName || 'json-export').replace(/[\\\/?:\[\]*]/g, '')
        XLSX.writeFile(wb, name + '.xlsx')
        var tip = '已导出 ' + wb.SheetNames.length + ' 个 Sheet'
        if (skipped.length) tip += '，跳过空表：' + skipped.join('、')
        this.$message.success(tip)
      } catch (error) {
        this.$message.error('导出失败: ' + error.message)
      } finally {
        this.exporting = false
      }
    },

    copyCurrentSheet() {
      var sheet = this.sheets.find(item => item.name === this.activeSheet)
      if (!sheet) {
        this.$message.warning('没有可复制的内容')
        return
      }
      var booleanAs = this.opts.booleanAs
      var lines = [sheet.columns.map(function (c) { return c.name }).join('\t')]
      sheet.rows.slice(0, 500).forEach(function (row) {
        lines.push(sheet.columns.map(function (col) {
          return displayValue(row[col.key], booleanAs).replace(/\t/g, ' ')
        }).join('\t'))
      })
      var self = this
      copyToClipboard(lines.join('\n'))
        .then(function () { self.$message.success('已复制前 ' + Math.min(sheet.rows.length, 500) + ' 行（TSV，可直接粘进 Excel）') })
        .catch(function () { self.$message.error('复制失败') })
    },

    // ==================== 其他 ====================
    clearAll() {
      this.inputJson = ''
      this.analyzed = false
      this.stale = false
      this.parseError = ''
      this.sheets = []
      this.warnings = []
      this.activeSheet = ''
      this.clearShapeState()
    },
    fillExample() {
      this.inputJson = JSON.stringify({
        code: 1,
        msg: 'MSG_SUCCESS',
        data: {
          examTitle: '2026年测试经理（性能非功能）认证测评',
          score: 81.5,
          passMark: 60,
          questionList: [
            {
              questionId: 23336909,
              context: '性能测试中（ ）问题原则上不允许遗留。',
              sort: 1,
              questionType: '单选题',
              correct: true,
              userAnswerId: 588899076176306200,
              optionList: [
                { optionId: 43998353, context: '涉及监管', sort: 1, correct: false, select: false },
                { optionId: 43998355, context: '一般问题', sort: 2, correct: false, select: true }
              ]
            },
            {
              questionId: 23337001,
              context: '稳定性测试中内存缓慢增长就一定存在问题。',
              sort: 2,
              questionType: '判断题',
              correct: false,
              userAnswerId: 588899076176306200,
              optionList: []
            }
          ]
        }
      }, null, 2)
      this.analyze()
    }
  }
}
</script>

<style scoped>
.json-excel-page {
  padding: 20px;
  height: 100%;
  background: linear-gradient(180deg, #f5f8ff 0%, #eef3fb 100%);
  box-sizing: border-box;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.json-excel-container {
  display: flex;
  gap: 20px;
  flex: 1;
  min-height: 0;
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
  flex: 1.2;
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
  align-items: center;
}

.input-content {
  padding: 15px;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.input-content ::v-deep .el-textarea {
  flex: 1;
  display: flex;
  min-height: 0;
}

.input-content ::v-deep .el-textarea__inner {
  flex: 1;
  min-height: 0;
  resize: none;
  font-family: 'Courier New', Courier, monospace;
  font-size: 14px;
}

.parse-status {
  flex-shrink: 0;
  padding-top: 10px;
  font-size: 13px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.status-idle {
  color: #909399;
}

.status-ok {
  color: #67c23a;
}

.status-error {
  color: #f56c6c;
  word-break: break-all;
}

.status-stale {
  display: block;
  margin-top: 4px;
  color: #e6a23c;
}

.status-bigint {
  display: block;
  margin-top: 4px;
  color: #409eff;
}

.option-bar {
  flex-shrink: 0;
  padding: 10px 15px;
  border-bottom: 1px solid #f0f4f8;
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  background: #f9fbff;
}

.output-content {
  padding: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.preview-wrap {
  flex: 1;
  padding: 12px 15px 15px;
  overflow: auto;
}

.warn-item {
  margin-bottom: 8px;
}

.warn-item ::v-deep .el-alert__title {
  font-size: 12px;
}

.sheet-tabs ::v-deep .el-tabs__header {
  margin-bottom: 10px;
}

.sheet-tabs ::v-deep .el-tabs__item {
  font-family: 'Courier New', Courier, monospace;
  font-size: 12px;
}

.sheet-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.meta-text {
  font-size: 12px;
  color: #606266;
}

.meta-hint {
  color: #e6a23c;
}

.preview-table ::v-deep th {
  background: #f5f8ff !important;
  color: #1f2d3d;
  font-family: 'Courier New', Courier, monospace;
  font-size: 12px;
}

.col-internal {
  color: #b07000;
}

.empty-tip {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100%;
  color: #909399;
  padding: 0 30px;
  text-align: center;
}

.empty-tip i {
  font-size: 48px;
  margin-bottom: 16px;
}

/* 字段勾选结构树 */
.tree-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border-top: 1px solid #f0f4f8;
  background: #fbfdff;
}

.tree-header {
  flex-shrink: 0;
  padding: 8px 15px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #f0f4f8;
}

.tree-title {
  font-size: 14px;
  color: #1f2d3d;
  display: flex;
  align-items: center;
  gap: 8px;
}

.tree-title em {
  font-style: normal;
  font-size: 12px;
  color: #409eff;
}

.shape-tree {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 6px 10px;
  background: transparent;
  font-size: 12px;
}

.shape-tree ::v-deep .el-tree-node__content {
  height: 26px;
}

.tnode {
  display: flex;
  align-items: center;
  gap: 8px;
  overflow: hidden;
}

.tname {
  font-family: 'Courier New', Courier, monospace;
  font-size: 12px;
  color: #1f2d3d;
}

.tmeta {
  font-size: 11px;
  color: #909399;
  white-space: nowrap;
}

.trole {
  font-size: 11px;
  color: #b07000;
  white-space: nowrap;
}

.tnode.is-locked .tname {
  color: #909399;
}

.tnode.is-off .tname,
.tnode.is-off .tmeta {
  color: #c0c4cc;
  text-decoration: line-through;
}

.tree-tip {
  flex-shrink: 0;
  padding: 8px 15px;
  font-size: 11px;
  line-height: 1.6;
  color: #909399;
  border-top: 1px dashed #e4ecf7;
}

/* 底部操作栏 */
.action-bar {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  padding: 16px 0 0;
  flex-shrink: 0;
}

@media (max-width: 1000px) {
  .json-excel-container {
    flex-direction: column;
  }
  .input-section, .output-section {
    min-width: 0;
    flex: none;
    height: 50%;
  }
}
</style>
