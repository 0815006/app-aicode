<template>
  <div class="diff-minimap" ref="minimap" @click="handleClick">
    <div class="minimap-track" ref="track">
      <div
        v-for="(block, idx) in colorBlocks"
        :key="idx"
        class="minimap-block"
        :style="block.style"
        :title="block.title"
      ></div>
      <!-- 视口指示器 -->
      <div
        class="minimap-viewport"
        :style="viewportStyle"
      ></div>
    </div>
    <!-- 差异计数 -->
    <div class="minimap-stats">
      <span class="stat-item stat-added" :title="'新增 ' + stats.added + ' 行'">+{{ stats.added }}</span>
      <span class="stat-item stat-deleted" :title="'删除 ' + stats.deleted + ' 行'">-{{ stats.deleted }}</span>
      <span class="stat-item stat-modified" :title="'修改 ' + stats.modified + ' 行'">~{{ stats.modified }}</span>
    </div>
    <!-- 快速导航按钮 -->
    <div class="minimap-nav">
      <el-tooltip content="上一处差异 (↑)" placement="left">
        <button class="nav-btn" @click.stop="$emit('prev-diff')" :disabled="!hasDiffs">
          <i class="el-icon-arrow-up"></i>
        </button>
      </el-tooltip>
      <el-tooltip content="下一处差异 (↓)" placement="left">
        <button class="nav-btn" @click.stop="$emit('next-diff')" :disabled="!hasDiffs">
          <i class="el-icon-arrow-down"></i>
        </button>
      </el-tooltip>
    </div>
  </div>
</template>

<script>
export default {
  name: 'DiffMinimap',
  props: {
    /** 对齐行数组 */
    alignedLines: {
      type: Array,
      default: function () { return [] }
    },
    /** 总行数 */
    totalLines: {
      type: Number,
      default: 0
    },
    /** 当前可视区域起始行 */
    visibleStart: {
      type: Number,
      default: 0
    },
    /** 当前可视区域行数 */
    visibleCount: {
      type: Number,
      default: 30
    },
    /** 差异统计 */
    stats: {
      type: Object,
      default: function () { return { added: 0, deleted: 0, modified: 0, equal: 0 } }
    }
  },
  computed: {
    hasDiffs: function () {
      return this.stats.added + this.stats.deleted + this.stats.modified > 0
    },
    colorBlocks: function () {
      if (!this.alignedLines.length) return []
      var total = this.alignedLines.length
      var trackHeight = this.trackHeight || 300
      var blocks = []
      var colorMap = {
        added: '#4caf50',
        deleted: '#f44336',
        modified: '#ff9800'
      }
      var titleMap = {
        added: '新增行',
        deleted: '待删除行',
        modified: '修改行'
      }
      // 合并连续相同类型块
      var i = 0
      while (i < total) {
        var type = this.alignedLines[i].type
        if (type === 'equal') { i++; continue }
        var start = i
        while (i < total && this.alignedLines[i].type === type) { i++ }
        var top = (start / total) * trackHeight
        var height = Math.max(2, ((i - start) / total) * trackHeight)
        blocks.push({
          style: {
            position: 'absolute',
            top: top + 'px',
            left: '0',
            right: '0',
            height: height + 'px',
            backgroundColor: colorMap[type] || '#999',
            borderRadius: '1px',
            cursor: 'pointer'
          },
          title: titleMap[type] + ' (行 ' + (start + 1) + '-' + i + ')',
          lineIndex: start
        })
      }
      return blocks
    },
    viewportStyle: function () {
      if (!this.alignedLines.length) return { display: 'none' }
      var total = this.alignedLines.length
      var trackHeight = this.trackHeight || 300
      var top = (this.visibleStart / total) * trackHeight
      var height = Math.max(20, (this.visibleCount / total) * trackHeight)
      return {
        position: 'absolute',
        top: top + 'px',
        left: '0',
        right: '0',
        height: height + 'px',
        border: '1px solid #409eff',
        backgroundColor: 'rgba(64, 158, 255, 0.1)',
        borderRadius: '2px',
        pointerEvents: 'none'
      }
    }
  },
  data: function () {
    return {
      trackHeight: 300
    }
  },
  mounted: function () {
    this.updateTrackHeight()
    window.addEventListener('resize', this.updateTrackHeight)
  },
  beforeDestroy: function () {
    window.removeEventListener('resize', this.updateTrackHeight)
  },
  methods: {
    updateTrackHeight: function () {
      if (this.$refs.track) {
        this.trackHeight = this.$refs.track.clientHeight || 300
      }
    },
    handleClick: function (e) {
      if (!this.$refs.track || !this.alignedLines.length) return
      var rect = this.$refs.track.getBoundingClientRect()
      var y = e.clientY - rect.top
      var ratio = y / rect.height
      var lineIndex = Math.floor(ratio * this.alignedLines.length)
      this.$emit('scroll-to', lineIndex)
    }
  }
}
</script>

<style scoped>
.diff-minimap {
  width: 40px;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
  border-left: 1px solid #dcdfe6;
  user-select: none;
  flex-shrink: 0;
}
.minimap-track {
  flex: 1;
  position: relative;
  margin: 4px 4px;
  min-height: 100px;
  cursor: pointer;
}
.minimap-stats {
  padding: 4px 2px;
  text-align: center;
  font-size: 10px;
  line-height: 1.6;
  border-top: 1px solid #dcdfe6;
}
.stat-item {
  display: block;
  font-family: monospace;
  font-weight: 600;
}
.stat-added { color: #4caf50; }
.stat-deleted { color: #f44336; }
.stat-modified { color: #ff9800; }
.minimap-nav {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4px 0;
  border-top: 1px solid #dcdfe6;
}
.nav-btn {
  width: 28px;
  height: 28px;
  border: 1px solid #dcdfe6;
  background: #fff;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 2px 0;
  font-size: 14px;
  color: #606266;
  transition: all 0.2s;
}
.nav-btn:hover:not(:disabled) {
  border-color: #409eff;
  color: #409eff;
  background: #ecf5ff;
}
.nav-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
</style>
