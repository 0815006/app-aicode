<template>
  <div class="diff-summary-panel" :class="{ collapsed: isCollapsed }">
    <!-- 折叠/展开按钮 -->
    <div class="panel-toggle" @click="isCollapsed = !isCollapsed">
      <i :class="isCollapsed ? 'el-icon-arrow-left' : 'el-icon-arrow-right'"></i>
    </div>
    <div v-show="!isCollapsed" class="panel-content">
      <div class="panel-header">
        <span class="panel-title">差异清单</span>
      </div>

      <!-- Tab 切换 -->
      <el-tabs v-model="activeTab" class="summary-tabs" stretch>
        <el-tab-pane label="原始差异" name="original">
          <div class="diff-list-section">
            <div class="section-header">
              <span>原始文件差异 ({{ originalDiffs.length }})</span>
            </div>
            <div class="diff-list-body" v-if="originalDiffs.length">
              <div
                v-for="(item, idx) in originalDiffs"
                :key="'orig-' + idx"
                class="diff-list-item"
                :class="'diff-type-' + item.type"
                @click="$emit('locate-diff', item.lineIndex)"
              >
                <span class="diff-line-num">L{{ item.lineDisplay }}</span>
                <span class="diff-type-badge" :class="'badge-' + item.type">
                  {{ typeLabel(item.type) }}
                </span>
                <span class="diff-preview" :title="item.preview">{{ item.preview }}</span>
              </div>
            </div>
            <div v-else class="empty-tip">
              <i class="el-icon-check" style="color: #67c23a;"></i>
              <span>无差异</span>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="本次修改" name="changes">
          <div class="diff-list-section">
            <div class="section-header">
              <span>本次修改记录 ({{ changeLog.length }})</span>
              <el-button
                v-if="changeLog.length"
                type="text"
                size="mini"
                @click="$emit('clear-changes')"
              >清空</el-button>
            </div>
            <div class="diff-list-body" v-if="changeLog.length">
              <div
                v-for="(item, idx) in changeLog"
                :key="'chg-' + idx"
                class="diff-list-item diff-type-change"
                @click="$emit('locate-diff', item.lineIndex)"
              >
                <span class="diff-line-num">L{{ item.lineDisplay }}</span>
                <span class="diff-type-badge badge-change">
                  {{ changeTypeLabel(item.action) }}
                </span>
                <span class="diff-preview" :title="item.detail">{{ item.detail }}</span>
              </div>
            </div>
            <div v-else class="empty-tip">
              <i class="el-icon-edit" style="color: #909399;"></i>
              <span>暂无修改</span>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="残余差异" name="remaining">
          <div class="diff-list-section">
            <div class="section-header">
              <span>当前残余差异 ({{ remainingDiffs.length }})</span>
              <el-tag
                v-if="remainingDiffs.length === 0"
                type="success"
                size="mini"
                effect="dark"
              >已全部处理</el-tag>
            </div>
            <div class="diff-list-body" v-if="remainingDiffs.length">
              <div
                v-for="(item, idx) in remainingDiffs"
                :key="'rem-' + idx"
                class="diff-list-item"
                :class="'diff-type-' + item.type"
                @click="$emit('locate-diff', item.lineIndex)"
              >
                <span class="diff-line-num">L{{ item.lineDisplay }}</span>
                <span class="diff-type-badge" :class="'badge-' + item.type">
                  {{ typeLabel(item.type) }}
                </span>
                <span class="diff-preview" :title="item.preview">{{ item.preview }}</span>
              </div>
            </div>
            <div v-else class="empty-tip">
              <i class="el-icon-circle-check" style="color: #67c23a; font-size: 24px;"></i>
              <span style="color: #67c23a; font-weight: 600;">所有差异已处理完毕！</span>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script>
export default {
  name: 'DiffSummaryPanel',
  props: {
    /** 原始差异清单 */
    originalDiffs: {
      type: Array,
      default: function () { return [] }
    },
    /** 修改操作记录 */
    changeLog: {
      type: Array,
      default: function () { return [] }
    },
    /** 残余差异清单 */
    remainingDiffs: {
      type: Array,
      default: function () { return [] }
    }
  },
  data: function () {
    return {
      isCollapsed: false,
      activeTab: 'original'
    }
  },
  methods: {
    typeLabel: function (type) {
      var map = {
        added: '新增',
        deleted: '多余',
        modified: '修改'
      }
      return map[type] || type
    },
    changeTypeLabel: function (action) {
      var map = {
        'copy-line': '整行复制',
        'copy-fragment': '片段替换',
        'delete-line': '删除行',
        'manual-edit': '手工编辑',
        'reorder': '重排位置',
        'add-line': '新增行'
      }
      return map[action] || action
    }
  }
}
</script>

<style scoped>
.diff-summary-panel {
  display: flex;
  flex-direction: row;
  background: #fff;
  border-left: 1px solid #dcdfe6;
  transition: width 0.3s ease;
  width: 280px;
  flex-shrink: 0;
  overflow: hidden;
}
.diff-summary-panel.collapsed {
  width: 24px;
}
.panel-toggle {
  width: 24px;
  min-width: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: #f5f7fa;
  border-right: 1px solid #ebeef5;
  color: #909399;
  font-size: 14px;
  transition: background 0.2s;
  flex-shrink: 0;
}
.panel-toggle:hover {
  background: #e4e7ed;
  color: #409eff;
}
.panel-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}
.panel-header {
  padding: 10px 12px;
  border-bottom: 1px solid #ebeef5;
  background: #f5f7fa;
}
.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.summary-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.summary-tabs >>> .el-tabs__header {
  margin: 0;
}
.summary-tabs >>> .el-tabs__content {
  flex: 1;
  overflow: hidden;
}
.summary-tabs >>> .el-tab-pane {
  height: 100%;
  overflow: hidden;
}
.diff-list-section {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.section-header {
  padding: 8px 12px;
  font-size: 12px;
  color: #909399;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #ebeef5;
  flex-shrink: 0;
}
.diff-list-body {
  flex: 1;
  overflow-y: auto;
  padding: 4px 0;
}
.diff-list-item {
  display: flex;
  align-items: center;
  padding: 6px 12px;
  cursor: pointer;
  font-size: 12px;
  border-bottom: 1px solid #f2f6fc;
  transition: background 0.15s;
}
.diff-list-item:hover {
  background: #f5f7fa;
}
.diff-line-num {
  width: 36px;
  flex-shrink: 0;
  color: #909399;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 11px;
}
.diff-type-badge {
  display: inline-block;
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 10px;
  margin-right: 6px;
  flex-shrink: 0;
  font-weight: 600;
}
.badge-added {
  background: #e8f5e9;
  color: #4caf50;
}
.badge-deleted {
  background: #ffebee;
  color: #f44336;
}
.badge-modified {
  background: #fff3e0;
  color: #ff9800;
}
.badge-change {
  background: #e3f2fd;
  color: #1976d2;
}
.diff-preview {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #606266;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 11px;
}
.empty-tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #909399;
  font-size: 13px;
  gap: 8px;
}
</style>
