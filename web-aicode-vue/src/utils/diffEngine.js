/**
 * diffEngine.js - 文本比对引擎
 * 支持行级比对、字符级比对、语义比对（JSON/YAML）
 * 纯前端实现，无后端依赖
 */

// ==================== 行级 Diff (Myers 算法简化版) ====================

/**
 * 计算两个数组的最长公共子序列 (LCS)
 * @param {string[]} a - 左侧行数组
 * @param {string[]} b - 右侧行数组
 * @param {Function} eq - 比较函数
 * @returns {Array} LCS 索引对
 */
function lcs(a, b, eq) {
  var m = a.length
  var n = b.length
  // 优化：过大数组使用简化算法
  if (m * n > 10000000) {
    return lcsGreedy(a, b, eq)
  }
  var dp = []
  var i, j
  for (i = 0; i <= m; i++) {
    dp[i] = []
    for (j = 0; j <= n; j++) {
      dp[i][j] = 0
    }
  }
  for (i = 1; i <= m; i++) {
    for (j = 1; j <= n; j++) {
      if (eq(a[i - 1], b[j - 1])) {
        dp[i][j] = dp[i - 1][j - 1] + 1
      } else {
        dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1])
      }
    }
  }
  // 回溯得到 LCS 索引对
  var result = []
  i = m
  j = n
  while (i > 0 && j > 0) {
    if (eq(a[i - 1], b[j - 1])) {
      result.unshift({ leftIdx: i - 1, rightIdx: j - 1 })
      i--
      j--
    } else if (dp[i - 1][j] > dp[i][j - 1]) {
      i--
    } else {
      j--
    }
  }
  return result
}

/**
 * 贪心 LCS（用于大文件）
 */
function lcsGreedy(a, b, eq) {
  var bMap = {}
  var j
  for (j = 0; j < b.length; j++) {
    var key = b[j]
    if (!bMap[key]) bMap[key] = []
    bMap[key].push(j)
  }
  var result = []
  var lastJ = -1
  for (var i = 0; i < a.length; i++) {
    var positions = bMap[a[i]]
    if (!positions) continue
    for (var k = 0; k < positions.length; k++) {
      if (positions[k] > lastJ && eq(a[i], b[positions[k]])) {
        result.push({ leftIdx: i, rightIdx: positions[k] })
        lastJ = positions[k]
        break
      }
    }
  }
  return result
}

/**
 * 行级 Diff：生成对齐行
 * @param {string[]} leftLines - 左侧文件行数组
 * @param {string[]} rightLines - 右侧文件行数组
 * @param {Object} options - 选项 { ignoreLineEnding, ignoreWhitespace, isYaml }
 * @returns {Array} 对齐行数组
 */
export function computeLineDiff(leftLines, rightLines, options) {
  options = options || {}
  var eq = function(a, b) {
    var la = a
    var lb = b
    if (options.ignoreLineEnding) {
      la = la.replace(/\r\n?/g, '\n')
      lb = lb.replace(/\r\n?/g, '\n')
    }
    if (options.ignoreWhitespace && !options.isYaml) {
      la = la.replace(/\s+/g, ' ').trim()
      lb = lb.replace(/\s+/g, ' ').trim()
    }
    return la === lb
  }
  var matches = lcs(leftLines, rightLines, eq)
  var aligned = []
  var li = 0
  var ri = 0
  var mi = 0
  while (mi < matches.length) {
    var match = matches[mi]
    // 处理匹配前的非匹配行
    while (li < match.leftIdx && ri < match.rightIdx) {
      aligned.push({
        type: 'modified',
        leftLine: { number: li + 1, content: leftLines[li] },
        rightLine: { number: ri + 1, content: rightLines[ri] },
        charDiffs: computeCharDiff(leftLines[li], rightLines[ri])
      })
      li++
      ri++
    }
    while (li < match.leftIdx) {
      aligned.push({
        type: 'added',
        leftLine: { number: li + 1, content: leftLines[li] },
        rightLine: null
      })
      li++
    }
    while (ri < match.rightIdx) {
      aligned.push({
        type: 'deleted',
        leftLine: null,
        rightLine: { number: ri + 1, content: rightLines[ri] }
      })
      ri++
    }
    // 匹配行
    aligned.push({
      type: 'equal',
      leftLine: { number: li + 1, content: leftLines[li] },
      rightLine: { number: ri + 1, content: rightLines[ri] }
    })
    li++
    ri++
    mi++
  }
  // 处理剩余行
  while (li < leftLines.length && ri < rightLines.length) {
    aligned.push({
      type: 'modified',
      leftLine: { number: li + 1, content: leftLines[li] },
      rightLine: { number: ri + 1, content: rightLines[ri] },
      charDiffs: computeCharDiff(leftLines[li], rightLines[ri])
    })
    li++
    ri++
  }
  while (li < leftLines.length) {
    aligned.push({
      type: 'added',
      leftLine: { number: li + 1, content: leftLines[li] },
      rightLine: null
    })
    li++
  }
  while (ri < rightLines.length) {
    aligned.push({
      type: 'deleted',
      leftLine: null,
      rightLine: { number: ri + 1, content: rightLines[ri] }
    })
    ri++
  }
  return aligned
}

// ==================== 字符级 Diff ====================

/**
 * 字符级比对，返回差异片段
 * @param {string} left - 左侧行内容
 * @param {string} right - 右侧行内容
 * @returns {Array} 字符差异片段数组 [{ type: 'equal'|'removed'|'added', value: string }]
 */
export function computeCharDiff(left, right) {
  if (left === right) {
    return [{ type: 'equal', value: left }]
  }
  if (!left) return [{ type: 'added', value: right }]
  if (!right) return [{ type: 'removed', value: left }]

  // 使用简化的字符级 LCS
  var la = left.split('')
  var ra = right.split('')
  var m = la.length
  var n = ra.length

  // 对于过长的行，使用简化算法
  if (m * n > 500000) {
    return computeCharDiffSimple(left, right)
  }

  var dp = []
  var i, j
  for (i = 0; i <= m; i++) {
    dp[i] = new Array(n + 1)
    dp[i][0] = 0
  }
  for (j = 0; j <= n; j++) {
    dp[0][j] = 0
  }
  for (i = 1; i <= m; i++) {
    for (j = 1; j <= n; j++) {
      if (la[i - 1] === ra[j - 1]) {
        dp[i][j] = dp[i - 1][j - 1] + 1
      } else {
        dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1])
      }
    }
  }

  // 回溯构建差异片段
  var result = []
  i = m
  j = n
  var ops = []
  while (i > 0 || j > 0) {
    if (i > 0 && j > 0 && la[i - 1] === ra[j - 1]) {
      ops.unshift({ type: 'equal', char: la[i - 1] })
      i--
      j--
    } else if (j > 0 && (i === 0 || dp[i][j - 1] >= dp[i - 1][j])) {
      ops.unshift({ type: 'added', char: ra[j - 1] })
      j--
    } else {
      ops.unshift({ type: 'removed', char: la[i - 1] })
      i--
    }
  }

  // 合并连续相同类型
  if (ops.length === 0) return result
  var current = { type: ops[0].type, value: ops[0].char }
  for (var k = 1; k < ops.length; k++) {
    if (ops[k].type === current.type) {
      current.value += ops[k].char
    } else {
      result.push(current)
      current = { type: ops[k].type, value: ops[k].char }
    }
  }
  result.push(current)
  return result
}

/**
 * 简化的字符级比对（用于长行）
 */
function computeCharDiffSimple(left, right) {
  // 找公共前缀
  var prefixLen = 0
  var minLen = Math.min(left.length, right.length)
  while (prefixLen < minLen && left[prefixLen] === right[prefixLen]) {
    prefixLen++
  }
  // 找公共后缀
  var suffixLen = 0
  while (suffixLen < minLen - prefixLen &&
    left[left.length - 1 - suffixLen] === right[right.length - 1 - suffixLen]) {
    suffixLen++
  }

  var result = []
  if (prefixLen > 0) {
    result.push({ type: 'equal', value: left.substring(0, prefixLen) })
  }
  var leftMid = left.substring(prefixLen, left.length - suffixLen)
  var rightMid = right.substring(prefixLen, right.length - suffixLen)
  if (leftMid) {
    result.push({ type: 'removed', value: leftMid })
  }
  if (rightMid) {
    result.push({ type: 'added', value: rightMid })
  }
  if (suffixLen > 0) {
    result.push({ type: 'equal', value: left.substring(left.length - suffixLen) })
  }
  return result
}

// ==================== 语义比对 (JSON) ====================

/**
 * 深度比较两个对象，返回差异
 * @param {*} left - 左侧对象
 * @param {*} right - 右侧对象
 * @param {string} path - 当前路径
 * @returns {Array} 差异列表
 */
export function deepCompare(left, right, path) {
  path = path || ''
  var diffs = []

  if (left === right) return diffs
  if (left === null || left === undefined || right === null || right === undefined) {
    diffs.push({ path: path, type: left == null ? 'added' : 'removed', left: left, right: right })
    return diffs
  }
  if (typeof left !== typeof right) {
    diffs.push({ path: path, type: 'type-changed', left: left, right: right })
    return diffs
  }
  if (typeof left !== 'object') {
    if (left !== right) {
      diffs.push({ path: path, type: 'value-changed', left: left, right: right })
    }
    return diffs
  }
  if (Array.isArray(left) && Array.isArray(right)) {
    var maxLen = Math.max(left.length, right.length)
    for (var i = 0; i < maxLen; i++) {
      var itemPath = path + '[' + i + ']'
      if (i >= left.length) {
        diffs.push({ path: itemPath, type: 'added', left: undefined, right: right[i] })
      } else if (i >= right.length) {
        diffs.push({ path: itemPath, type: 'removed', left: left[i], right: undefined })
      } else {
        diffs = diffs.concat(deepCompare(left[i], right[i], itemPath))
      }
    }
    return diffs
  }
  if (Array.isArray(left) !== Array.isArray(right)) {
    diffs.push({ path: path, type: 'type-changed', left: left, right: right })
    return diffs
  }

  // 对象比较
  var allKeys = {}
  Object.keys(left).forEach(function(k) { allKeys[k] = true })
  Object.keys(right).forEach(function(k) { allKeys[k] = true })
  Object.keys(allKeys).forEach(function(key) {
    var subPath = path ? path + '.' + key : key
    if (!(key in left)) {
      diffs.push({ path: subPath, type: 'added', left: undefined, right: right[key] })
    } else if (!(key in right)) {
      diffs.push({ path: subPath, type: 'removed', left: left[key], right: undefined })
    } else {
      diffs = diffs.concat(deepCompare(left[key], right[key], subPath))
    }
  })
  return diffs
}

/**
 * 检测 JSON 键的位置差异（语义相同但物理位置不同）
 * @param {*} leftObj
 * @param {*} rightObj
 * @returns {Array} 位置差异列表
 */
export function detectKeyOrderDiff(leftObj, rightObj) {
  if (typeof leftObj !== 'object' || typeof rightObj !== 'object') return []
  if (leftObj === null || rightObj === null) return []
  if (Array.isArray(leftObj) || Array.isArray(rightObj)) return []

  var leftKeys = Object.keys(leftObj)
  var rightKeys = Object.keys(rightObj)
  var commonKeys = leftKeys.filter(function(k) { return rightKeys.indexOf(k) !== -1 })

  var orderDiffs = []
  for (var i = 0; i < commonKeys.length; i++) {
    var key = commonKeys[i]
    var leftIndex = leftKeys.indexOf(key)
    var rightIndex = rightKeys.indexOf(key)
    if (leftIndex !== rightIndex) {
      orderDiffs.push({
        key: key,
        leftPosition: leftIndex + 1,
        rightPosition: rightIndex + 1,
        targetPosition: leftIndex + 1
      })
    }
  }
  return orderDiffs
}

/**
 * 按参照文件的键序重排对象
 * @param {*} reference - 参照对象
 * @param {*} target - 目标对象
 * @returns {*} 重排后的对象
 */
export function reorderByReference(reference, target) {
  if (typeof reference !== 'object' || typeof target !== 'object') return target
  if (reference === null || target === null) return target
  if (Array.isArray(reference) || Array.isArray(target)) return target

  var result = {}
  // 先按参照的键序排列共同的键
  Object.keys(reference).forEach(function(key) {
    if (key in target) {
      if (typeof reference[key] === 'object' && typeof target[key] === 'object' &&
        !Array.isArray(reference[key]) && !Array.isArray(target[key]) &&
        reference[key] !== null && target[key] !== null) {
        result[key] = reorderByReference(reference[key], target[key])
      } else {
        result[key] = target[key]
      }
    }
  })
  // 再加上目标独有的键
  Object.keys(target).forEach(function(key) {
    if (!(key in reference)) {
      result[key] = target[key]
    }
  })
  return result
}

// ==================== YAML 简易解析 / 验证 ====================

/**
 * 简单 YAML 语法验证
 * @param {string} content
 * @returns {{ valid: boolean, error: string }}
 */
export function validateYaml(content) {
  if (!content || !content.trim()) {
    return { valid: true, error: '' }
  }
  var lines = content.split('\n')
  var indentStack = [0]
  for (var i = 0; i < lines.length; i++) {
    var line = lines[i]
    // 跳过空行和注释
    if (!line.trim() || line.trim().startsWith('#')) continue
    var indent = line.search(/\S/)
    if (indent === -1) continue
    // 检查缩进是否使用 Tab
    if (line.indexOf('\t') !== -1 && line.indexOf('\t') < indent) {
      return { valid: false, error: '第 ' + (i + 1) + ' 行: YAML 不应使用 Tab 缩进' }
    }
    // 检查缩进是否合理（通常 2 或 4 空格）
    if (indent > indentStack[indentStack.length - 1]) {
      indentStack.push(indent)
    } else {
      while (indentStack.length > 1 && indentStack[indentStack.length - 1] > indent) {
        indentStack.pop()
      }
      if (indentStack[indentStack.length - 1] !== indent) {
        return { valid: false, error: '第 ' + (i + 1) + ' 行: 缩进层级不一致' }
      }
    }
  }
  return { valid: true, error: '' }
}

/**
 * JSON 语法验证
 * @param {string} content
 * @returns {{ valid: boolean, error: string }}
 */
export function validateJson(content) {
  if (!content || !content.trim()) {
    return { valid: true, error: '' }
  }
  try {
    JSON.parse(content)
    return { valid: true, error: '' }
  } catch (e) {
    return { valid: false, error: e.message }
  }
}

// ==================== 脱敏处理 ====================

var SENSITIVE_PATTERNS = [
  // password 相关
  { regex: /(password\s*[=:]\s*)(\S+)/gi, group: 2 },
  { regex: /(passwd\s*[=:]\s*)(\S+)/gi, group: 2 },
  { regex: /("password"\s*:\s*")([^"]+)(")/gi, group: 2 },
  { regex: /(password:\s+)(\S+)/gi, group: 2 },
  // token / secret / key
  { regex: /(token\s*[=:]\s*)(\S+)/gi, group: 2 },
  { regex: /(secret\s*[=:]\s*)(\S+)/gi, group: 2 },
  { regex: /(api[_-]?key\s*[=:]\s*)(\S+)/gi, group: 2 },
  { regex: /("token"\s*:\s*")([^"]+)(")/gi, group: 2 },
  { regex: /("secret"\s*:\s*")([^"]+)(")/gi, group: 2 },
  { regex: /("api[_-]?key"\s*:\s*")([^"]+)(")/gi, group: 2 },
  // 数据库连接串
  { regex: /(jdbc:[a-z]+:\/\/[^?\s]*\?[^&\s]*password=)([^&\s]+)/gi, group: 2 },
  { regex: /(url\s*[=:]\s*jdbc:[^\s]+password=)([^&\s]+)/gi, group: 2 },
  // 通用 connection string
  { regex: /(connection[_-]?string\s*[=:]\s*)(\S+)/gi, group: 2 }
]

/**
 * 对内容进行脱敏处理
 * @param {string} content - 原始内容
 * @returns {string} 脱敏后内容
 */
export function maskSensitiveContent(content) {
  var result = content
  SENSITIVE_PATTERNS.forEach(function(pattern) {
    result = result.replace(pattern.regex, function() {
      var args = Array.prototype.slice.call(arguments)
      var full = args[0]
      // 找到对应分组并替换为 *
      var groups = args.slice(1, -2) // 去掉 offset 和 full string
      if (groups.length >= pattern.group) {
        var sensitiveValue = groups[pattern.group - 1]
        var masked = sensitiveValue.replace(/./g, '*')
        return full.replace(sensitiveValue, masked)
      }
      return full
    })
  })
  return result
}

/**
 * 检测内容是否包含敏感信息
 * @param {string} line
 * @returns {boolean}
 */
export function hasSensitiveContent(line) {
  return SENSITIVE_PATTERNS.some(function(pattern) {
    pattern.regex.lastIndex = 0
    return pattern.regex.test(line)
  })
}

// ==================== 文件类型检测 ====================

/**
 * 根据文件名检测文件类型
 * @param {string} fileName
 * @returns {string} 文件类型 'json' | 'yaml' | 'properties' | 'markdown' | 'text'
 */
export function detectFileType(fileName) {
  if (!fileName) return 'text'
  var ext = fileName.toLowerCase().split('.').pop()
  switch (ext) {
    case 'json': return 'json'
    case 'yml':
    case 'yaml': return 'yaml'
    case 'properties': return 'properties'
    case 'md':
    case 'markdown': return 'markdown'
    case 'xml': return 'xml'
    case 'ini':
    case 'cfg':
    case 'conf': return 'properties'
    case 'env': return 'properties'
    case 'toml': return 'toml'
    default: return 'text'
  }
}

/**
 * 检测换行符类型
 * @param {string} content
 * @returns {string} 'LF' | 'CRLF' | 'mixed'
 */
export function detectLineEnding(content) {
  var hasCRLF = content.indexOf('\r\n') !== -1
  var hasLF = content.replace(/\r\n/g, '').indexOf('\n') !== -1
  if (hasCRLF && hasLF) return 'mixed'
  if (hasCRLF) return 'CRLF'
  return 'LF'
}

/**
 * 检测文件大小是否为大文件（>5MB）
 * @param {number} size - 文件大小（字节）
 * @returns {boolean}
 */
export function isLargeFile(size) {
  return size > 5 * 1024 * 1024
}

// ==================== 工具函数 ====================

/**
 * 将文件内容按行分割
 * @param {string} content
 * @returns {string[]}
 */
export function splitLines(content) {
  if (!content) return []
  return content.replace(/\r\n/g, '\n').replace(/\r/g, '\n').split('\n')
}

/**
 * 将行数组拼接为文件内容
 * @param {string[]} lines
 * @param {string} lineEnding - 'LF' | 'CRLF'
 * @returns {string}
 */
export function joinLines(lines, lineEnding) {
  var sep = lineEnding === 'CRLF' ? '\r\n' : '\n'
  return lines.join(sep)
}

/**
 * 生成保存文件名（带 -new 后缀和时间戳）
 * @param {string} originalName
 * @returns {string}
 */
export function generateNewFileName(originalName) {
  if (!originalName) return 'untitled-new.txt'
  var now = new Date()
  var ts = now.getFullYear() +
    padZero(now.getMonth() + 1) +
    padZero(now.getDate()) +
    padZero(now.getHours()) +
    padZero(now.getMinutes()) +
    padZero(now.getSeconds())
  var dotIndex = originalName.lastIndexOf('.')
  if (dotIndex === -1) {
    return originalName + '-new' + ts
  }
  return originalName.substring(0, dotIndex) + '-new' + ts + originalName.substring(dotIndex)
}

function padZero(n) {
  return n < 10 ? '0' + n : '' + n
}

/**
 * 统计差异数量
 * @param {Array} alignedLines
 * @returns {{ added: number, deleted: number, modified: number, equal: number }}
 */
export function countDiffs(alignedLines) {
  var stats = { added: 0, deleted: 0, modified: 0, equal: 0 }
  alignedLines.forEach(function(line) {
    stats[line.type] = (stats[line.type] || 0) + 1
  })
  return stats
}
