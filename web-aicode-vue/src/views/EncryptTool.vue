<!-- src/views/EncryptTool.vue -->
<template>
  <div class="encrypt-tool-page">
    <div class="encrypt-container">
      <!-- 左侧：输入区域 -->
      <div class="input-section card">
        <div class="section-header">
          <h3><i class="el-icon-edit"></i> 输入</h3>
          <div class="header-actions">
            <span class="type-label">类型</span>
            <el-select v-model="activeType" size="small" style="width: 130px" @change="onTypeChange">
              <el-option label="对称加密" value="symmetric" />
              <el-option label="非对称加密" value="asymmetric" />
              <el-option label="哈希摘要" value="hash" />
            </el-select>
          </div>
        </div>

        <!-- 对称加密参数面板 -->
        <div v-show="activeType === 'symmetric'" class="param-panel">
          <div class="param-row">
            <span class="param-label">加密算法</span>
            <el-select v-model="encryptAlgorithm" size="small" style="width: 100px" @change="onAlgorithmChange">
              <el-option label="AES" value="AES" />
              <el-option label="DES" value="DES" />
              <el-option label="3DES" value="TripleDES" />
              <el-option label="RC4" value="RC4" />
            </el-select>
            <span class="param-desc">{{ algorithmDesc }}</span>
          </div>
          <div v-show="encryptAlgorithm !== 'RC4'" class="param-row">
            <span class="param-label">加密模式</span>
            <el-select v-model="encryptMode" size="small" style="width: 100px" @change="onParamChange">
              <el-option v-for="m in availableModes" :key="m" :label="m" :value="m" />
            </el-select>
            <span class="param-desc">{{ modeDesc }}</span>
          </div>
          <div class="param-row">
            <span class="param-label">密钥</span>
            <el-input v-model="encryptKey" size="small" placeholder="加解密使用同一密钥" style="width: 180px" @input="onKeyChange" />
            <span class="param-desc">对称加密的凭证，加解密双方必须持有相同的密钥</span>
          </div>
          <div v-show="showIV" class="param-row">
            <span class="param-label">IV 偏移量</span>
            <el-input v-model="encryptIV" size="small" :placeholder="ivPlaceholder" style="width: 180px" @input="onParamChange" />
            <span class="param-desc">{{ ivDesc }}</span>
          </div>
          <div class="param-row">
            <span class="param-label">输出格式</span>
            <el-select v-model="encryptOutputFormat" size="small" style="width: 100px" @change="onParamChange">
              <el-option label="Base64" value="Base64" />
              <el-option label="Hex" value="Hex" />
            </el-select>
            <span class="param-desc">密文的编码方式，Base64 最通用，Hex 为十六进制字符串</span>
          </div>
        </div>

        <!-- 非对称加密参数面板 -->
        <div v-show="activeType === 'asymmetric'" class="param-panel">
          <div class="param-row">
            <span class="param-label">加密算法</span>
            <el-select v-model="rsaAlgorithm" size="small" style="width: 140px" @change="onRsaParamChange">
              <el-option label="RSA-OAEP" value="RSA-OAEP" />
            </el-select>
            <span class="param-desc">非对称加密标准，公钥加密 + 私钥解密</span>
          </div>
          <div class="param-row">
            <span class="param-label">密钥对</span>
            <el-button size="small" icon="el-icon-setting" @click="generateRSAKeyPair" :disabled="rsaGenerating">
              {{ rsaGenerating ? '正在生成...' : '生成 RSA 密钥对（2048位）' }}
            </el-button>
            <span class="param-desc">也可直接粘贴已有 PEM 格式密钥到下方输入框</span>
          </div>
          <div class="param-row param-row-vertical">
            <span class="param-label">公钥</span>
            <el-input v-model="rsaPublicKey" type="textarea" :rows="2" size="small" placeholder="粘贴 PEM 公钥 或 点击「生成密钥对」自动填充&#10;-----BEGIN PUBLIC KEY----- ..." @input="onRsaParamChange" />
          </div>
          <div class="param-row param-row-vertical">
            <span class="param-label">私钥</span>
            <el-input v-model="rsaPrivateKey" type="textarea" :rows="2" size="small" placeholder="粘贴 PEM 私钥 或 点击「生成密钥对」自动填充&#10;-----BEGIN PRIVATE KEY----- ..." @input="onRsaParamChange" />
          </div>
          <div class="param-row">
            <span class="param-label"></span>
            <span class="param-desc param-desc-warn">注意：RSA-2048 OAEP 单次最多加密 190 字节（约 63 个中文字）</span>
          </div>
        </div>

        <!-- 哈希摘要参数面板 -->
        <div v-show="activeType === 'hash'" class="param-panel">
          <div class="param-row">
            <span class="param-label">哈希算法</span>
            <el-select v-model="hashAlgorithm" size="small" style="width: 150px" @change="onHashParamChange">
              <el-option label="MD5" value="MD5" />
              <el-option label="SHA-1" value="SHA1" />
              <el-option label="SHA-256" value="SHA256" />
              <el-option label="SHA-512" value="SHA512" />
              <el-option label="HMAC-SHA256" value="HMAC-SHA256" />
              <el-option label="HMAC-SHA512" value="HMAC-SHA512" />
            </el-select>
            <span class="param-desc">{{ hashDesc }}</span>
          </div>
          <div v-show="isHMAC" class="param-row">
            <span class="param-label">HMAC 密钥</span>
            <el-input v-model="hashKey" size="small" placeholder="消息认证码密钥" style="width: 180px" @input="onHashKeyChange" />
            <span class="param-desc">带密钥的哈希，用于验证消息完整性和身份认证</span>
          </div>
        </div>

        <div class="input-content">
          <el-input v-model="inputText" type="textarea" :placeholder="inputPlaceholder" @input="onInputChange" />
          <div class="action-bar">
            <template v-if="activeType === 'symmetric'">
              <el-button type="primary" icon="el-icon-lock" :disabled="!canSymmetricEncrypt" @click="doSymmetricEncrypt">加密</el-button>
              <el-button type="warning" icon="el-icon-key" :disabled="!canSymmetricDecrypt" @click="doSymmetricDecrypt">解密</el-button>
            </template>
            <template v-else-if="activeType === 'asymmetric'">
              <el-button type="primary" icon="el-icon-lock" :disabled="!canRsaEncrypt" @click="doRsaEncrypt">公钥加密</el-button>
              <el-button type="warning" icon="el-icon-key" :disabled="!canRsaDecrypt" @click="doRsaDecrypt">私钥解密</el-button>
            </template>
            <template v-else>
              <el-button type="primary" icon="el-icon-files" :disabled="!inputText.trim()" @click="doHash">计算哈希</el-button>
            </template>
          </div>
        </div>
      </div>

      <!-- 右侧：输出区域 -->
      <div class="output-section card">
        <div class="section-header">
          <h3><i class="el-icon-lock"></i> {{ outputTitle }}</h3>
          <div class="header-actions">
            <el-button type="success" size="small" icon="el-icon-document-copy" :disabled="!outputText" @click="copyOutput">复制</el-button>
          </div>
        </div>
        <div class="output-content">
          <div v-if="outputText" class="output-display">
            <pre><code>{{ outputText }}</code></pre>
          </div>
          <div v-else class="empty-tip">
            <i class="el-icon-info"></i>
            <p>结果将显示在这里...</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import CryptoJS from 'crypto-js'
import JSEncrypt from 'jsencrypt'
import copyToClipboard from '@/utils/clipboard'

export default {
  name: 'EncryptTool',
  data() {
    return {
      activeType: 'symmetric',

      // 通用
      inputText: '',
      outputText: '',

      // === 对称加密 ===
      encryptKey: 'ihaierForTodoKey',
      encryptIV: 'ihaierForTodo_Iv',
      encryptAlgorithm: 'AES',
      encryptMode: 'CBC',
      encryptOutputFormat: 'Base64',

      // === 非对称加密 RSA ===
      rsaAlgorithm: 'RSA-OAEP',
      rsaPublicKey: '',
      rsaPrivateKey: '',
      rsaGenerating: false,
      _rsaKeyPair: null,

      // === 哈希摘要 ===
      hashAlgorithm: 'SHA256',
      hashKey: 'my-hmac-secret-key'
    }
  },
  computed: {
    inputPlaceholder() {
      if (this.activeType === 'symmetric') return '输入待加密的明文 或 待解密的密文...'
      if (this.activeType === 'asymmetric') return '输入待加密的明文 或 待解密的 Base64 密文（不超过 190 字节）...'
      return '输入待计算哈希的文本内容...'
    },
    outputTitle() {
      if (this.activeType === 'hash') return this.hashAlgorithm.replace('HMAC-', '') + ' 结果'
      return '结果'
    },
    isHMAC() { return this.hashAlgorithm.startsWith('HMAC-') },
    rsaHasKeys() { return this.rsaPublicKey.trim() || this.rsaPrivateKey.trim() },
    availableModes() {
      return this.encryptAlgorithm === 'AES' ? ['CBC', 'ECB', 'CTR', 'CFB'] : ['CBC', 'ECB']
    },
    showIV() {
      if (this.encryptAlgorithm === 'RC4') return false
      return this.encryptMode !== 'ECB'
    },
    ivPlaceholder() {
      if (this.encryptMode === 'CTR') return 'Nonce 计数器初始值 (16字节)'
      return '初始向量 (16字节)'
    },
    algorithmDesc() {
      const m = {
        AES: '最常用的对称加密标准，安全性高、性能好',
        DES: '老旧标准，56 位密钥已不安全，仅用于兼容旧系统',
        TripleDES: 'DES 的增强版，三次加密提高安全性',
        RC4: '流加密算法，速度快但安全性已不足'
      }
      return m[this.encryptAlgorithm] || ''
    },
    modeDesc() {
      const m = {
        CBC: '密文分组链接模式（推荐），前后块相关，需要 IV',
        ECB: '电子密码本模式（不推荐），相同明文产生相同密文',
        CTR: '计数器模式，可并行计算，需要 Nonce',
        CFB: '密文反馈模式，流式加密，需要 IV'
      }
      return m[this.encryptMode] || ''
    },
    ivDesc() {
      if (this.encryptMode === 'CTR') return '计数器初始值，解密需相同 Nonce（不要求保密）'
      return '初始向量，解密需相同 IV（不要求保密，但建议每次加密更换）'
    },
    hashDesc() {
      const m = {
        MD5: '128 位哈希，已不安全，仅用于校验',
        SHA1: '160 位哈希，已被淘汰',
        SHA256: '256 位哈希（推荐），目前最常用的安全哈希',
        SHA512: '512 位哈希，更高的安全强度',
        'HMAC-SHA256': '带密钥的 SHA-256，用于消息认证',
        'HMAC-SHA512': '带密钥的 SHA-512，用于消息认证'
      }
      return m[this.hashAlgorithm] || ''
    },
    canSymmetricEncrypt() { return this.encryptKey.trim() && this.inputText.trim() },
    canSymmetricDecrypt() { return this.encryptKey.trim() && this.inputText.trim() },
    canRsaEncrypt() { return this.rsaPublicKey.trim() && this.inputText.trim() },
    canRsaDecrypt() { return this.rsaPrivateKey.trim() && this.inputText.trim() }
  },
  watch: {
    encryptAlgorithm() {
      if (this.encryptAlgorithm === 'RC4') {
        this.encryptMode = 'CBC'
      } else if (!this.availableModes.includes(this.encryptMode)) {
        this.encryptMode = 'CBC'
      }
    }
  },
  methods: {
    // ============ 通用 ============
    onTypeChange() { this.inputText = ''; this.outputText = '' },
    onInputChange() { this.outputText = '' },
    copyOutput() {
      if (!this.outputText) return this.$message.warning('没有可复制的内容')
      var self = this
      copyToClipboard(this.outputText)
        .then(function () { self.$message.success('复制成功') })
        .catch(function () { self.$message.error('复制失败') })
    },

    // ============ 对称加密 ============
    onKeyChange() { this.outputText = '' },
    onAlgorithmChange() {
      this.encryptMode = 'CBC'
      this.outputText = ''
      // 切换算法时自动填入对应长度的默认密钥/IV
      const defaults = {
        AES: { key: 'ihaierForTodoKey', iv: 'ihaierForTodo_Iv' },
        DES: { key: 'DesKey01', iv: 'DesIv_01' },
        TripleDES: { key: '3DesKey24Bytes!TestKey!', iv: 'DesIv_01' },
        RC4: { key: 'RC4StreamKey!@#8', iv: '' }
      }
      const d = defaults[this.encryptAlgorithm]
      if (d) {
        this.encryptKey = d.key
        this.encryptIV = d.iv
      }
    },
    onParamChange() { this.outputText = '' },
    getCryptoConfig() {
      // WordArray 原始密钥模式：Utf8.parse 直接作为加密密钥，不经过 EvpKDF 派生
      const key = CryptoJS.enc.Utf8.parse(this.encryptKey)
      const cfg = { mode: CryptoJS.mode[this.encryptMode], padding: CryptoJS.pad.Pkcs7 }
      if (this.showIV && this.encryptIV.trim()) {
        cfg.iv = CryptoJS.enc.Utf8.parse(this.encryptIV)
      }
      return { key, cfg }
    },
    doSymmetricEncrypt() {
      if (!this.encryptKey.trim()) return this.$message.warning('请输入密钥')
      if (!this.inputText.trim()) return this.$message.warning('请输入明文')
      try {
        const { key, cfg } = this.getCryptoConfig()
        let result
        switch (this.encryptAlgorithm) {
          case 'AES': result = CryptoJS.AES.encrypt(this.inputText, key, cfg); break
          case 'DES': result = CryptoJS.DES.encrypt(this.inputText, key, cfg); break
          case 'TripleDES': result = CryptoJS.TripleDES.encrypt(this.inputText, key, cfg); break
          case 'RC4': result = CryptoJS.RC4.encrypt(this.inputText, key); break
          default: return this.$message.error('不支持的算法')
        }
        // 输出原始密文（不用 OpenSSL Salted__ 包装），key 和 IV 由用户精确控制
        // 这样才能与参考代码 encryptPassword() 的加解密行为一致
        this.outputText = this.encryptOutputFormat === 'Base64'
          ? result.ciphertext.toString(CryptoJS.enc.Base64)
          : result.ciphertext.toString()
        this.$message.success('加密成功')
      } catch (e) {
        this.$message.error('加密失败: ' + (e.message || '请检查参数'))
        this.outputText = ''
      }
    },
    doSymmetricDecrypt() {
      if (!this.encryptKey.trim()) return this.$message.warning('请输入密钥')
      if (!this.inputText.trim()) return this.$message.warning('请输入密文')
      try {
        const { key, cfg } = this.getCryptoConfig()
        // 将原始 Base64/Hex 密文转为 WordArray，构建不带 salt 的 CipherParams
        // 不加 OpenSSL 格式 → CryptoJS 不触发 EvpKDF，直接使用用户 key + IV 解密
        const trimmed = this.inputText.trim()
        let ciphertextWA
        if (/^[0-9a-fA-F]+$/.test(trimmed)) {
          ciphertextWA = CryptoJS.enc.Hex.parse(trimmed)
        } else {
          ciphertextWA = CryptoJS.enc.Base64.parse(trimmed)
        }
        const cipherParams = CryptoJS.lib.CipherParams.create({ ciphertext: ciphertextWA })
        let result
        switch (this.encryptAlgorithm) {
          case 'AES': result = CryptoJS.AES.decrypt(cipherParams, key, cfg); break
          case 'DES': result = CryptoJS.DES.decrypt(cipherParams, key, cfg); break
          case 'TripleDES': result = CryptoJS.TripleDES.decrypt(cipherParams, key, cfg); break
          case 'RC4': result = CryptoJS.RC4.decrypt(cipherParams, key); break
          default: return this.$message.error('不支持的算法')
        }
        const decrypted = result.toString(CryptoJS.enc.Utf8)
        if (!decrypted) {
          this.$message.error('解密失败：密钥不正确或密文已损坏')
          this.outputText = ''
        } else {
          this.outputText = decrypted
          this.$message.success('解密成功')
        }
      } catch (e) {
        this.$message.error('解密失败: ' + (e.message || '请检查参数'))
        this.outputText = ''
      }
    },

    // ============ 非对称加密 RSA（Web Crypto 优先，jsencrypt 降级） ============
    onRsaParamChange() { this.outputText = '' },

    generateRSAKeyPair() {
      this.rsaGenerating = true
      // 优先使用 Web Crypto API（HTTPS/localhost，毫秒级）
      if (window.crypto && window.crypto.subtle) {
        this._generateWithWebCrypto()
      } else {
        // 降级：HTTP 环境使用 jsencrypt（纯 JS，需 2~15 秒）
        this._generateWithJsEncrypt()
      }
    },

    async _generateWithWebCrypto() {
      try {
        var self = this
        var keyPair = await window.crypto.subtle.generateKey(
          { name: 'RSA-OAEP', modulusLength: 2048, publicExponent: new Uint8Array([1, 0, 1]), hash: 'SHA-256' },
          true, ['encrypt', 'decrypt']
        )
        var pubBuf = await window.crypto.subtle.exportKey('spki', keyPair.publicKey)
        var privBuf = await window.crypto.subtle.exportKey('pkcs8', keyPair.privateKey)
        self.rsaPublicKey = self._arrayBufferToPem(pubBuf, 'PUBLIC KEY')
        self.rsaPrivateKey = self._arrayBufferToPem(privBuf, 'PRIVATE KEY')
        self._rsaKeyPair = keyPair
        self.outputText = ''
        self.$message.success('密钥对生成成功（RSA 2048 位，Web Crypto）')
      } catch (e) {
        // Web Crypto 失败时降级到 jsencrypt
        this._generateWithJsEncrypt()
      } finally {
        this.rsaGenerating = false
      }
    },

    _generateWithJsEncrypt() {
      var self = this
      this.$message.info('正在生成 RSA 密钥对（纯 JS 计算，约需 2-15 秒）...')
      var crypt = new JSEncrypt({ default_key_size: 2048 })
      crypt.getKey(function () {
        try {
          self.rsaPublicKey = crypt.getPublicKey()
          self.rsaPrivateKey = crypt.getPrivateKey()
          self._rsaKeyPair = crypt
          self.outputText = ''
          self.$message.success('密钥对生成成功（RSA 2048 位）')
        } catch (e) {
          self.$message.error('密钥生成失败: ' + e.message)
        } finally {
          self.rsaGenerating = false
        }
      })
    },

    _arrayBufferToPem(buffer, label) {
      var bytes = new Uint8Array(buffer)
      var binary = ''
      for (var i = 0; i < bytes.length; i++) binary += String.fromCharCode(bytes[i])
      var b64 = btoa(binary)
      var lines = b64.match(/.{1,64}/g) || []
      return '-----BEGIN ' + label + '-----\n' + lines.join('\n') + '\n-----END ' + label + '-----'
    },

    doRsaEncrypt() {
      if (!this.rsaPublicKey.trim()) return this.$message.warning('请填写或生成公钥')
      if (!this.inputText.trim()) return this.$message.warning('请输入明文')
      try {
        var encrypt = new JSEncrypt()
        encrypt.setPublicKey(this.rsaPublicKey.trim())
        var result = encrypt.encrypt(this.inputText)
        if (result === false) {
          this.$message.error('加密失败：公钥无效或明文过长（RSA-2048 最多 190 字节）')
          this.outputText = ''
        } else {
          this.outputText = result
          this.$message.success('公钥加密成功')
        }
      } catch (e) {
        this.$message.error('加密失败: ' + (e.message || '请检查密钥和输入'))
        this.outputText = ''
      }
    },

    doRsaDecrypt() {
      if (!this.rsaPrivateKey.trim()) return this.$message.warning('请填写或生成私钥')
      if (!this.inputText.trim()) return this.$message.warning('请输入密文')
      try {
        var decrypt = new JSEncrypt()
        decrypt.setPrivateKey(this.rsaPrivateKey.trim())
        var result = decrypt.decrypt(this.inputText.trim())
        if (result === false || result === null) {
          this.$message.error('解密失败：私钥不正确或密文已损坏')
          this.outputText = ''
        } else {
          this.outputText = result
          this.$message.success('私钥解密成功')
        }
      } catch (e) {
        this.$message.error('解密失败: ' + (e.message || '请检查密钥和密文'))
        this.outputText = ''
      }
    },

    // ============ 哈希摘要 ============
    onHashKeyChange() { this.outputText = '' },
    onHashParamChange() {
      if (this.isHMAC && !this.hashKey.trim()) {
        this.hashKey = 'my-hmac-secret-key'
      }
      this.outputText = ''
    },
    doHash() {
      if (!this.inputText.trim()) return this.$message.warning('请输入文本')
      if (this.isHMAC && !this.hashKey.trim()) return this.$message.warning('请输入 HMAC 密钥')
      try {
        let result
        switch (this.hashAlgorithm) {
          case 'MD5': result = CryptoJS.MD5(this.inputText); break
          case 'SHA1': result = CryptoJS.SHA1(this.inputText); break
          case 'SHA256': result = CryptoJS.SHA256(this.inputText); break
          case 'SHA512': result = CryptoJS.SHA512(this.inputText); break
          case 'HMAC-SHA256': result = CryptoJS.HmacSHA256(this.inputText, this.hashKey); break
          case 'HMAC-SHA512': result = CryptoJS.HmacSHA512(this.inputText, this.hashKey); break
          default: return this.$message.error('不支持的算法')
        }
        this.outputText = result.toString()
        this.$message.success('计算成功')
      } catch (e) {
        this.$message.error('计算失败: ' + (e.message || '未知错误'))
        this.outputText = ''
      }
    }
  }
}
</script>

<style scoped>
.encrypt-tool-page {
  padding: 20px;
  height: 100%;
  background: linear-gradient(180deg, #f5f8ff 0%, #eef3fb 100%);
  box-sizing: border-box;
  overflow: hidden;
}

.encrypt-container {
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

.input-section { flex: 1; min-width: 0; }
.output-section { flex: 1; min-width: 0; }

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
  white-space: nowrap;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.type-label {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}

/* ============ 参数面板 ============ */
.param-panel {
  padding: 12px 15px;
  border-bottom: 1px solid #f0f4f8;
  background: #fafbfc;
  flex-shrink: 0;
  overflow-y: auto;
  max-height: 55%;
}

.param-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
}

.param-row + .param-row {
  margin-top: 2px;
}

.param-row-vertical {
  align-items: flex-start;
}

.param-row-vertical .param-label {
  padding-top: 4px;
}

.param-label {
  font-size: 13px;
  color: #303133;
  font-weight: 500;
  white-space: nowrap;
  min-width: 65px;
  text-align: right;
}

.param-desc {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.param-desc-warn {
  color: #e6a23c;
}

/* ============ 输入内容区 ============ */
.input-content {
  padding: 15px;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  gap: 12px;
}

.input-content ::v-deep .el-textarea {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.input-content ::v-deep .el-textarea__inner {
  flex: 1;
  resize: vertical;
  font-family: 'Courier New', Courier, monospace;
  font-size: 14px;
}

.output-content {
  padding: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.output-display {
  background: #282c34;
  color: #abb2bf;
  margin: 15px;
  padding: 15px;
  border-radius: 8px;
  font-family: 'Courier New', Courier, monospace;
  font-size: 13px;
  line-height: 1.5;
  flex: 1;
  overflow-y: auto;
  box-sizing: border-box;
}

.output-display pre {
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

.action-bar {
  display: flex;
  gap: 10px;
  justify-content: center;
  flex-shrink: 0;
}
</style>
