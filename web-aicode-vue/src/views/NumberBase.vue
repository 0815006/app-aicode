<!-- src/views/NumberBase.vue -->
<template>
  <div class="number-base-page">
    <div class="number-container">
      <!-- 左侧：输入区域 -->
      <div class="input-section card">
        <div class="section-header">
          <h3><i class="el-icon-edit"></i> 输入</h3>
          <div class="header-actions">
            <el-select
              v-model="sourceBase"
              placeholder="源进制"
              size="small"
              style="width: 130px"
            >
              <el-option label="自动识别" value="auto"></el-option>
              <el-option label="二进制 (2)" :value="2"></el-option>
              <el-option label="八进制 (8)" :value="8"></el-option>
              <el-option label="十进制 (10)" :value="10"></el-option>
              <el-option label="十六进制 (16)" :value="16"></el-option>
              <el-option label="三十六进制 (36)" :value="36"></el-option>
            </el-select>
          </div>
        </div>
        <div class="input-content">
          <el-input
            v-model="inputText"
            type="textarea"
            :placeholder="inputPlaceholder"
            :rows="10"
            @input="handleInputChange"
          />
          <div class="example-bar">
            <span class="example-label">示例：</span>
            <el-button size="mini" type="text" @click="fillExample('23567')">十进制 23567</el-button>
            <el-button size="mini" type="text" @click="fillExample('0x5C0F')">十六进制 0x5C0F</el-button>
            <el-button size="mini" type="text" @click="fillExample('101110000001111')">二进制长数</el-button>
            <el-button size="mini" type="text" @click="fillExample('755')">权限八进制 755</el-button>
          </div>
        </div>
      </div>

      <!-- 右侧：结果区域 -->
      <div class="output-section card">
        <div class="section-header">
          <h3><i class="el-icon-refresh"></i> 结果</h3>
          <div class="header-actions">
            <el-button
              size="small"
              icon="el-icon-document-copy"
              :disabled="!result"
              @click="copyAll"
            >
              复制全部
            </el-button>
          </div>
        </div>
        <div class="output-content">
          <div v-if="result" class="result-list">
            <div v-for="item in resultItems" :key="item.base" class="result-item">
              <div class="result-label">
                <span class="base-name">{{ item.name }}</span>
                <el-tag size="mini" type="info">基数 {{ item.base }}</el-tag>
              </div>
              <div class="result-value-row">
                <div class="result-value">{{ item.value }}</div>
                <el-button
                  size="mini"
                  icon="el-icon-copy-document"
                  class="copy-btn"
                  @click="copyOne(item)"
                />
              </div>
            </div>
          </div>
          <!-- 空状态 -->
          <div v-else class="empty-tip">
            <i class="el-icon-info"></i>
            <p>输入数值后点击转换，同时展示 2 / 8 / 10 / 16 进制结果</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部操作栏 -->
    <div class="action-bar">
      <el-button
        type="primary"
        icon="el-icon-sort"
        @click="doConvert"
        :disabled="!inputText.trim()"
      >
        转换
      </el-button>
      <el-button
        type="success"
        icon="el-icon-document-copy"
        @click="copyAll"
        :disabled="!result"
      >
        复制全部
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
import copyToClipboard from '@/utils/clipboard'

var DIGITS = '0123456789abcdefghijklmnopqrstuvwxyz'
var MAX_DIGITS = 5000 // 超大数保护，防止转换卡死页面

export default {
  name: 'NumberBase',
  data() {
    return {
      inputText: '',
      sourceBase: 'auto',
      result: null // { bin, oct, dec, hex }
    }
  },
  computed: {
    inputPlaceholder() {
      if (this.sourceBase === 'auto') {
        return '输入数值，自动识别前缀：0x 十六进制 / 0b 二进制 / 0o 八进制，无前缀按十进制...'
      }
      var map = {
        2: '输入二进制数，仅包含 0 和 1，如 101110000001111',
        8: '输入八进制数，仅包含 0-7，如 755',
        10: '输入十进制数，如 23567',
        16: '输入十六进制数，仅包含 0-9 和 a-f，如 5C0F',
        36: '输入三十六进制数，仅包含 0-9 和 a-z，如 zzz'
      }
      return map[this.sourceBase] || '请输入数值...'
    },
    resultItems() {
      if (!this.result) return []
      return [
        { key: 'bin', base: 2, name: '二进制', value: this.result.bin },
        { key: 'oct', base: 8, name: '八进制', value: this.result.oct },
        { key: 'dec', base: 10, name: '十进制', value: this.result.dec },
        { key: 'hex', base: 16, name: '十六进制', value: this.result.hex }
      ]
    }
  },
  methods: {
    handleInputChange() {
      this.result = null
    },
    fillExample(text) {
      this.inputText = text
      this.result = null
      this.doConvert()
    },
    doConvert() {
      try {
        var raw = this.inputText.trim()
        if (!raw) return

        var negative = false
        if (raw.charAt(0) === '-') {
          negative = true
          raw = raw.substring(1).trim()
        } else if (raw.charAt(0) === '+') {
          raw = raw.substring(1).trim()
        }
        if (!raw) {
          throw new Error('输入为空，无法解析')
        }

        var parsed = this.parseNumber(raw)
        var value = this.evalToBigInt(parsed.digits, parsed.base)
        var sign = negative && !this.isZeroBig(value) ? '-' : ''

        this.result = {
          bin: sign + this.bigToBase(value, 2),
          oct: sign + this.bigToBase(value, 8),
          dec: sign + this.bigToBase(value, 10),
          hex: sign + this.bigToBase(value, 16)
        }
        this.$message.success('转换成功')
      } catch (error) {
        this.result = null
        this.$message.error('转换失败: ' + (error.message || '输入格式不正确'))
      }
    },
    // 解析输入，返回 { digits: 纯数字串(小写), base: 源进制 }
    parseNumber(raw) {
      var base = this.sourceBase
      var body = raw

      if (base === 'auto') {
        var lower = raw.toLowerCase()
        if (lower.indexOf('0x') === 0) {
          base = 16
          body = raw.substring(2)
        } else if (lower.indexOf('0b') === 0) {
          base = 2
          body = raw.substring(2)
        } else if (lower.indexOf('0o') === 0) {
          base = 8
          body = raw.substring(2)
        } else {
          base = 10
        }
      } else {
        // 手动指定进制时，允许带对应前缀，一并去掉
        var prefixMap = { 2: '0b', 8: '0o', 16: '0x' }
        var prefix = prefixMap[base]
        if (prefix && body.toLowerCase().indexOf(prefix) === 0) {
          body = body.substring(2)
        }
      }

      body = body.toLowerCase()
      if (!body) {
        throw new Error('去掉前缀后没有有效数字')
      }
      if (body.length > MAX_DIGITS) {
        throw new Error('数字过长，最多支持 ' + MAX_DIGITS + ' 位')
      }

      var validChars = DIGITS.slice(0, base)
      for (var i = 0; i < body.length; i++) {
        if (validChars.indexOf(body.charAt(i)) === -1) {
          throw new Error('第 ' + (i + 1) + ' 位 "' + body.charAt(i) + '" 不是 ' + base + ' 进制的合法字符')
        }
      }

      // 去掉前导零，保留至少一位
      var trimmed = body.replace(/^0+/, '') || '0'
      return { digits: trimmed, base: base }
    },
    // 按源进制求值：digitString(base) → BigInt 数值（Horner 法，任意位数精确）
    evalToBigInt(digits, fromBase) {
      var base = BigInt(fromBase)
      var zero = BigInt(0)
      var n = zero
      for (var i = 0; i < digits.length; i++) {
        var d = BigInt(DIGITS.indexOf(digits.charAt(i)))
        n = n * base + d
      }
      return n
    },
    isZeroBig(value) {
      return value === BigInt(0)
    },
    // 通用进制转换：BigInt 数值 → 目标进制字符串（短除法）
    bigToBase(value, toBaseVal) {
      if (this.isZeroBig(value)) return '0'
      var to = BigInt(toBaseVal)
      var zero = BigInt(0)
      var out = ''
      var n = value
      while (n > zero) {
        var rem = Number(n % to)
        out = DIGITS.charAt(rem) + out
        n = n / to
      }
      return out
    },
    copyOne(item) {
      var self = this
      copyToClipboard(item.value)
        .then(function () { self.$message.success(item.name + '已复制') })
        .catch(function () { self.$message.error('复制失败') })
    },
    copyAll() {
      if (!this.result) {
        this.$message.warning('没有可复制的内容')
        return
      }
      var text = [
        '二进制: ' + this.result.bin,
        '八进制: ' + this.result.oct,
        '十进制: ' + this.result.dec,
        '十六进制: ' + this.result.hex
      ].join('\n')
      var self = this
      copyToClipboard(text)
        .then(function () { self.$message.success('复制成功') })
        .catch(function () { self.$message.error('复制失败') })
    },
    clearAll() {
      this.inputText = ''
      this.result = null
      this.$message.info('已清空')
    }
  }
}
</script>

<style scoped>
.number-base-page {
  padding: 20px;
  height: 100%;
  background: linear-gradient(180deg, #f5f8ff 0%, #eef3fb 100%);
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.number-container {
  display: flex;
  gap: 20px;
  flex: 1;
  align-items: stretch;
  min-height: 0;
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
}

.input-content ::v-deep .el-textarea__inner {
  flex: 1;
  resize: none;
  font-family: 'Courier New', Courier, monospace;
  font-size: 14px;
}

.example-bar {
  flex-shrink: 0;
  padding-top: 8px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.example-label {
  font-size: 13px;
  color: #909399;
}

.output-content {
  padding: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.result-list {
  flex: 1;
  padding: 15px;
  overflow-y: auto;
}

.result-item {
  margin-bottom: 14px;
  border: 1px solid #e4ebf5;
  border-radius: 10px;
  overflow: hidden;
}

.result-label {
  padding: 8px 12px;
  background: #f5f8ff;
  display: flex;
  align-items: center;
  gap: 8px;
}

.base-name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2d3d;
}

.result-value-row {
  display: flex;
  align-items: flex-start;
}

.result-value {
  flex: 1;
  background: #282c34;
  color: #abb2bf;
  padding: 12px 14px;
  font-family: 'Courier New', Courier, monospace;
  font-size: 13px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
  box-sizing: border-box;
  min-height: 44px;
}

.copy-btn {
  flex-shrink: 0;
  margin: 6px 8px;
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

/* 底部操作栏 */
.action-bar {
  display: flex;
  justify-content: center;
  gap: 12px;
  padding: 16px 0 0;
  flex-shrink: 0;
}

@media (max-width: 1000px) {
  .number-container {
    flex-direction: column;
  }
  .input-section, .output-section {
    min-width: 0;
    flex: none;
    height: 50%;
  }
}
</style>
