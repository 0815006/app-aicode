<!-- src/views/EncodeTool.vue -->
<template>
  <div class="encode-tool-page">
    <div class="encode-container">
      <!-- 左侧：输入区域 -->
      <div class="input-section card">
        <div class="section-header">
          <h3><i class="el-icon-edit"></i> 输入</h3>
          <div class="header-actions">
            <input
              ref="imageInput"
              type="file"
              accept="image/*"
              style="display: none"
              @change="handleImageSelect"
            />
            <el-button
              v-show="encodeType === 'base64'"
              size="small"
              icon="el-icon-upload2"
              @click="triggerFileInput"
            >
              上传图片
            </el-button>
          </div>
        </div>
        <div class="input-content">
          <el-input
            v-model="inputText"
            type="textarea"
            :placeholder="inputPlaceholder"
            :rows="16"
            :disabled="!!imageFile"
            @input="handleInputChange"
          />
          <div v-if="encodeType === 'base64' && imageFile" class="image-preview-input">
            <img :src="imagePreviewUrl" alt="图片预览" />
            <span class="image-name">{{ imageFileName }}</span>
            <i class="el-icon-circle-close remove-btn" @click="clearImage"></i>
          </div>
        </div>
      </div>

      <!-- 右侧：输出区域 -->
      <div class="output-section card">
        <div class="section-header">
          <h3><i class="el-icon-refresh"></i> 结果</h3>
          <div class="header-actions">
            <el-select
              v-model="encodeType"
              placeholder="选择编码类型"
              size="small"
              @change="handleTypeChange"
            >
              <el-option label="Base64" value="base64"></el-option>
              <el-option label="URL" value="url"></el-option>
              <el-option label="Unicode" value="unicode"></el-option>
              <el-option label="Hex" value="hex"></el-option>
            </el-select>
          </div>
        </div>
        <div class="output-content">
          <div class="output-wrapper">
            <!-- 图片预览模式 -->
            <div v-if="isImageOutput" class="image-preview">
              <img :src="outputText" alt="Base64 图片预览" />
              <p class="image-tip">Base64 图片编码结果</p>
            </div>
            <!-- 文本输出模式 -->
            <div v-else-if="outputText" class="output-display">
              <pre><code>{{ outputText }}</code></pre>
            </div>
            <!-- 空状态 -->
            <div v-else class="empty-tip">
              <i class="el-icon-info"></i>
              <p>点击编码或解码按钮查看结果</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部操作栏 -->
    <div class="action-bar">
      <el-button
        type="primary"
        icon="el-icon-lock"
        @click="doEncode"
        :disabled="!canEncode"
      >
        编码
      </el-button>
      <el-button
        type="warning"
        icon="el-icon-key"
        @click="doDecode"
        :disabled="!canDecode"
      >
        解码
      </el-button>
      <el-button
        type="success"
        icon="el-icon-document-copy"
        @click="copyOutput"
        :disabled="!outputText"
      >
        复制结果
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

export default {
  name: 'EncodeTool',
  data() {
    return {
      inputText: '',
      outputText: '',
      encodeType: 'base64',
      imageFile: null,
      imageFileName: '',
      imagePreviewUrl: '',
      lastOperation: ''
    }
  },
  computed: {
    inputPlaceholder() {
      var map = {
        base64: '输入待编码的文本，或点击上方"上传图片"选择图片文件...',
        url: '输入待编码的文本，或粘贴 URL 编码字符串进行解码...',
        unicode: '输入待编码的文本，或粘贴 \\uXXXX 字符串进行解码...',
        hex: '输入待编码的文本，或粘贴十六进制字符串进行解码...'
      }
      return map[this.encodeType] || '请输入文本...'
    },
    canEncode() {
      if (this.encodeType === 'base64' && this.imageFile) {
        return true
      }
      return !!this.inputText.trim()
    },
    canDecode() {
      return !!this.inputText.trim()
    },
    isImageOutput() {
      return this.outputText
        && this.encodeType === 'base64'
        && this.lastOperation === 'decode'
        && this.outputText.indexOf('data:image/') === 0
    }
  },
  methods: {
    handleInputChange() {
      this.outputText = ''
    },
    handleTypeChange() {
      this.outputText = ''
      this.clearImage()
    },
    triggerFileInput() {
      this.$refs.imageInput.click()
    },
    handleImageSelect(e) {
      var file = e.target.files[0]
      if (!file) return
      if (file.type.indexOf('image/') !== 0) {
        this.$message.warning('请选择图片文件')
        return
      }
      this.imageFile = file
      this.imageFileName = file.name
      this.inputText = ''
      this.outputText = ''
      var self = this
      var reader = new FileReader()
      reader.onload = function (e) {
        self.imagePreviewUrl = e.target.result
      }
      reader.readAsDataURL(file)
      // 清空 input 以便重复选择同一文件
      this.$refs.imageInput.value = ''
    },
    clearImage() {
      this.imageFile = null
      this.imageFileName = ''
      this.imagePreviewUrl = ''
    },
    doEncode() {
      try {
        // Base64 图片编码：使用 FileReader 读取
        if (this.encodeType === 'base64' && this.imageFile) {
          this.encodeImageToBase64()
          return
        }

        if (!this.inputText.trim()) return

        switch (this.encodeType) {
          case 'base64':
            this.outputText = this.base64Encode(this.inputText)
            break
          case 'url':
            this.outputText = encodeURIComponent(this.inputText)
            break
          case 'unicode':
            this.outputText = this.unicodeEncode(this.inputText)
            break
          case 'hex':
            this.outputText = this.hexEncode(this.inputText)
            break
        }
        this.lastOperation = 'encode'
        this.$message.success('编码成功')
      } catch (error) {
        this.$message.error('编码失败: ' + (error.message || '未知错误'))
      }
    },
    encodeImageToBase64() {
      var self = this
      var reader = new FileReader()
      reader.onload = function (e) {
        self.outputText = e.target.result
        self.$message.success('图片编码成功')
      }
      reader.onerror = function () {
        self.$message.error('图片读取失败')
      }
      reader.readAsDataURL(this.imageFile)
    },
    doDecode() {
      if (!this.inputText.trim()) return

      try {
        switch (this.encodeType) {
          case 'base64':
            // 如果输入是 data:image/... 的 data URI，去掉前缀后解码为文本
            var raw = this.inputText.trim()
            if (raw.indexOf('data:image/') === 0 && raw.indexOf(';base64,') !== -1) {
              // 直接展示图片预览
              this.outputText = raw
            } else {
              // 纯净 Base64 解码为文本
              // 如果输入本身是 data URI 但用户想解码为文本，按纯 Base64 处理
              if (raw.indexOf(';base64,') !== -1) {
                raw = raw.split(';base64,')[1]
              }
              this.outputText = this.base64Decode(raw)
            }
            break
          case 'url':
            this.outputText = decodeURIComponent(this.inputText)
            break
          case 'unicode':
            this.outputText = this.unicodeDecode(this.inputText)
            break
          case 'hex':
            this.outputText = this.hexDecode(this.inputText)
            break
        }
        this.lastOperation = 'decode'
        this.$message.success('解码成功')
      } catch (error) {
        this.$message.error('解码失败: ' + (error.message || '输入格式不正确'))
      }
    },
    // ========== Base64（支持 Unicode） ==========
    base64Encode(str) {
      var utf8Bytes = this.stringToUtf8Bytes(str)
      var binary = ''
      for (var i = 0; i < utf8Bytes.length; i++) {
        binary += String.fromCharCode(utf8Bytes[i])
      }
      return btoa(binary)
    },
    base64Decode(str) {
      var cleaned = str.replace(/\s/g, '')
      var binary = atob(cleaned)
      var bytes = []
      for (var i = 0; i < binary.length; i++) {
        bytes.push(binary.charCodeAt(i))
      }
      return this.utf8BytesToString(bytes)
    },
    // ========== Unicode 编解码 ==========
    unicodeEncode(str) {
      var result = ''
      for (var i = 0; i < str.length; i++) {
        var code = str.charCodeAt(i)
        if (code > 127) {
          result += '\\u' + code.toString(16).padStart(4, '0')
        } else {
          result += str.charAt(i)
        }
      }
      return result
    },
    unicodeDecode(str) {
      return str.replace(/\\u([0-9a-fA-F]{4})/g, function (match, hex) {
        return String.fromCharCode(parseInt(hex, 16))
      })
    },
    // ========== Hex 编解码 ==========
    hexEncode(str) {
      var bytes = this.stringToUtf8Bytes(str)
      var result = ''
      for (var i = 0; i < bytes.length; i++) {
        result += bytes[i].toString(16).padStart(2, '0')
      }
      return result
    },
    hexDecode(str) {
      var cleaned = str.replace(/\s/g, '')
      if (cleaned.length % 2 !== 0) {
        throw new Error('十六进制字符串长度必须为偶数')
      }
      if (!/^[0-9a-fA-F]+$/.test(cleaned)) {
        throw new Error('输入包含非十六进制字符')
      }
      var bytes = []
      for (var i = 0; i < cleaned.length; i += 2) {
        bytes.push(parseInt(cleaned.substr(i, 2), 16))
      }
      return this.utf8BytesToString(bytes)
    },
    // ========== UTF-8 工具方法 ==========
    stringToUtf8Bytes(str) {
      var bytes = []
      for (var i = 0; i < str.length; i++) {
        var code = str.charCodeAt(i)
        if (code < 0x80) {
          bytes.push(code)
        } else if (code < 0x800) {
          bytes.push(0xc0 | (code >> 6))
          bytes.push(0x80 | (code & 0x3f))
        } else if (code < 0xd800 || code >= 0xe000) {
          bytes.push(0xe0 | (code >> 12))
          bytes.push(0x80 | ((code >> 6) & 0x3f))
          bytes.push(0x80 | (code & 0x3f))
        } else {
          if (i + 1 < str.length) {
            var next = str.charCodeAt(i + 1)
            var cp = 0x10000 + ((code - 0xd800) << 10) + (next - 0xdc00)
            bytes.push(0xf0 | (cp >> 18))
            bytes.push(0x80 | ((cp >> 12) & 0x3f))
            bytes.push(0x80 | ((cp >> 6) & 0x3f))
            bytes.push(0x80 | (cp & 0x3f))
            i++
          }
        }
      }
      return bytes
    },
    utf8BytesToString(bytes) {
      var result = ''
      var i = 0
      while (i < bytes.length) {
        var b = bytes[i]
        var code
        if (b < 0x80) {
          code = b
          i += 1
        } else if ((b & 0xe0) === 0xc0) {
          code = ((b & 0x1f) << 6) | (bytes[i + 1] & 0x3f)
          i += 2
        } else if ((b & 0xf0) === 0xe0) {
          code = ((b & 0x0f) << 12) | ((bytes[i + 1] & 0x3f) << 6) | (bytes[i + 2] & 0x3f)
          i += 3
        } else if ((b & 0xf8) === 0xf0) {
          code = ((b & 0x07) << 18) | ((bytes[i + 1] & 0x3f) << 12) | ((bytes[i + 2] & 0x3f) << 6) | (bytes[i + 3] & 0x3f)
          i += 4
        } else {
          code = 0xfffd
          i += 1
        }
        if (code <= 0xffff) {
          result += String.fromCharCode(code)
        } else {
          code -= 0x10000
          result += String.fromCharCode(0xd800 | (code >> 10))
          result += String.fromCharCode(0xdc00 | (code & 0x3ff))
        }
      }
      return result
    },
    // ========== 公共操作 ==========
    copyOutput() {
      if (!this.outputText) {
        this.$message.warning('没有可复制的内容')
        return
      }
      var self = this
      copyToClipboard(this.outputText)
        .then(function () { self.$message.success('复制成功') })
        .catch(function () { self.$message.error('复制失败') })
    },
    clearAll() {
      this.inputText = ''
      this.outputText = ''
      this.lastOperation = ''
      this.clearImage()
      this.$message.info('已清空')
    }
  }
}
</script>

<style scoped>
.encode-tool-page {
  padding: 20px;
  height: 100%;
  background: linear-gradient(180deg, #f5f8ff 0%, #eef3fb 100%);
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.encode-container {
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

/* 输入区图片预览 */
.image-preview-input {
  margin-top: 12px;
  position: relative;
  display: inline-block;
}

.image-preview-input img {
  max-width: 100%;
  max-height: 200px;
  border-radius: 8px;
  border: 1px solid #d9e3f2;
  display: block;
}

.image-preview-input .image-name {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: #606266;
  text-align: center;
}

.image-preview-input .remove-btn {
  position: absolute;
  top: -8px;
  right: -8px;
  font-size: 20px;
  color: #909399;
  cursor: pointer;
  background: #fff;
  border-radius: 50%;
  transition: color 0.2s;
}

.image-preview-input .remove-btn:hover {
  color: #f56c6c;
}

.output-content {
  padding: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.output-wrapper {
  flex: 1;
  padding: 15px;
  overflow-y: auto;
}

.output-display {
  background: #282c34;
  color: #abb2bf;
  padding: 15px;
  border-radius: 8px;
  font-family: 'Courier New', Courier, monospace;
  font-size: 13px;
  line-height: 1.5;
  box-sizing: border-box;
  min-height: 100%;
}

.output-display pre {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
}

/* 图片预览 */
.image-preview {
  text-align: center;
}

.image-preview img {
  max-width: 100%;
  max-height: 400px;
  border-radius: 8px;
  border: 1px solid #e0e6ed;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.image-tip {
  margin: 12px 0 0;
  font-size: 12px;
  color: #909399;
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
  .encode-container {
    flex-direction: column;
  }
  .input-section, .output-section {
    min-width: 0;
    flex: none;
    height: 50%;
  }
}
</style>
