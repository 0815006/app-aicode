/**
 * src/utils/jsonToSheets.js
 * 通用 JSON -> 多 Sheet 数据模型引擎（不依赖 xlsx，纯函数，便于复用与验证）
 *
 * 主规则（叶子宽表）：
 *  1. 叶子路径 = 后代中不再包含「对象数组」的对象数组路径；每套叶子路径产出 1 张 Sheet，
 *     表的一行 = 一个叶子记录（最深一层数组的元素）。
 *  2. 祖先铺到根：从根对象到叶子途中所有层级的标量字段一律展开成列（不中途停），
 *     祖先的值在该祖先的每个叶子行上重复。
 *  3. 列名一律带「最近容器名」前缀（questionList.context=题干 / optionList.context=选项文本），
 *     前缀跨层撞名时自动升级为完整 JSON 路径；不在本叶子链上的数组整体序列化进单元格。
 *  4. 无叶子的父记录补一行（叶子层列留空），避免父记录在 Excel 中静默消失。
 *  5. 每个祖先层附带 <层名>._uniq 去重标记列（该层首次出现的行=1，重复行=0），
 *     SUM 即该层真实条数（宽表行数按子记录翻倍，直接数行数统计是错的）。
 *  6. Sheet 顺序 = 叶子路径发现顺序（深度优先）；_结构树（全量结构）与 _结构总览（列字典）
 *     追加在末尾，内容为空时不生成；_meta（单例键值表）默认不生成（已由结构树承载），
 *     但一张宽表都没拆出来时作为唯一兜底载体强制输出。
 *  7. describeShapeTree 产出 shape 结构树（一个字段名 = 一个节点，数组只标条数不列下标），
 *     供页面勾选要导出的字段；行粒度仍由本引擎自动判定，不随勾选变化。
 *  8. selection.excluded 为列过滤：被取消的 shape 路径不注册列也不写值；结构节点（对象、
 *     链上数组）由调用侧锁定，_uniq / _行路径 等内部列不受勾选影响。
 *  9. _结构总览 = 每张导出宽表的**全量列**字典（含标记列与回溯列，按列类别区分），
 *     非空率以该表总行为分母，另给空值行数与缺字段行数；值类型按全列聚合（number|string）；
 *     已铺开为宽表列的单例字段不在字典里重复，只补链外未拆表的结构。
 * 10. JSON 文本由 parseJsonSafe 解析：超出双精度安全范围的整数字面量先转字符串，防止 ID 失真。
 */

var SHEET_NAME_MAX = 31
var SHEET_NAME_ILLEGAL_RE = /[\\\/?:\[\]*]/g
var CELL_TEXT_MAX = 32767
var MAX_OBJ_DEPTH = 12
var MAX_LEAVES = 60
// 结构判定（某数组下是否还有对象数组）的采样上限，超出只按前 N 条裁定并告警
var MAX_SCAN_ITEMS = 2000
var MAX_CELL_ARRAY = 50
var COL_WARN_LIMIT = 1000
var ROW_WARN_LIMIT = 1048575
var ROW_SOFT_LIMIT = 50000
var ROW_HARD_LIMIT = 500000

var DEFAULTS = {
  fillEmptyLeafRows: true,
  dedupMarks: true,
  includeMeta: false,
  includeStructTree: true,
  includeOverview: true,
  booleanAs: 'text',
  excluded: null,
  leaves: null,
  shape: null
}

// ==================== JSON 解析（大整数保真） ====================

/**
 * 安全解析 JSON 文本：先把「超出安全整数范围的整数字面量」加引号，再 JSON.parse
 * （必须在 parse 之前处理：parse 之后 588899076176306200 已变成 ...176，原文不可恢复）
 * @returns {{ ok: boolean, data: any, error: Error|null, bigints: string[] }}
 */
export function parseJsonSafe(raw) {
  var res = { ok: false, data: null, error: null, bigints: [] }
  if (typeof raw !== 'string') {
    res.error = new Error('输入不是文本')
    return res
  }
  var scanned = protectUnsafeIntegers(raw)
  try {
    res.data = JSON.parse(scanned.text)
    res.ok = true
    res.bigints = scanned.bigints
  } catch (e) {
    res.error = e
  }
  return res
}

function protectUnsafeIntegers(text) {
  var out = []
  var bigints = []
  var i = 0
  var n = text.length
  var inStr = false
  var esc = false
  while (i < n) {
    var ch = text.charAt(i)
    if (inStr) {
      out.push(ch)
      if (esc) esc = false
      else if (ch === '\\') esc = true
      else if (ch === '"') inStr = false
      i++
      continue
    }
    if (ch === '"') { inStr = true; out.push(ch); i++; continue }
    if (ch === '-' || (ch >= '0' && ch <= '9')) {
      var j = i
      if (text.charAt(j) === '-') j++
      var dStart = j
      while (j < n && text.charAt(j) >= '0' && text.charAt(j) <= '9') j++
      var dEnd = j
      var isInt = true
      if (text.charAt(j) === '.') {
        isInt = false; j++
        while (j < n && text.charAt(j) >= '0' && text.charAt(j) <= '9') j++
      }
      if (text.charAt(j) === 'e' || text.charAt(j) === 'E') {
        isInt = false; j++
        if (text.charAt(j) === '+' || text.charAt(j) === '-') j++
        while (j < n && text.charAt(j) >= '0' && text.charAt(j) <= '9') j++
      }
      var token = text.slice(i, j)
      var digits = text.slice(dStart, dEnd)
      if (isInt && digits.length > 15 && !Number.isSafeInteger(Number(digits))) {
        out.push('"' + token + '"')
        bigints.push(token)
      } else {
        out.push(token)
      }
      i = j
      continue
    }
    out.push(ch)
    i++
  }
  return { text: out.join(''), bigints: bigints }
}

// ==================== 对外入口 ====================

/**
 * 将任意 JSON 拆成多张 Sheet 的数据模型
 * @param {any} data 已解析的 JSON 数据
 * @param {Object} options { fillEmptyLeafRows, dedupMarks, includeMeta, includeStructTree,
 *                           includeOverview, booleanAs, excluded, leaves }
 * @returns {{ sheets: Array, warnings: Array }}
 */
export function jsonToSheets(data, options) {
  var opts = {}
  Object.keys(DEFAULTS).forEach(function (k) {
    opts[k] = (options && options[k] !== undefined) ? options[k] : DEFAULTS[k]
  })

  var ctx = {
    sheets: [],
    warnings: [],
    warnSeen: {},
    nameUsed: {},
    metaRows: [],
    metaSeen: {},
    opts: opts,
    excluded: excludedMap(opts.excluded),
    leaves: []
  }

  if (data === null || data === undefined) {
    pushWarn(ctx, 'warn', '输入为空（null / undefined），无可导出结构')
    return { sheets: [], warnings: ctx.warnings }
  }

  ctx.leaves = opts.leaves && opts.leaves.length ? opts.leaves : collectLeafShapes(data, ctx)
  // 结构树先于宽表生成需要叶子集，此处复用已扫描结果，不重复遍历数据；
  // 页面已经算过结构树时直接复用（仅重标“是否导出”，勾选以本次 excluded 为准）
  var shape = null
  if (opts.shape && opts.shape.nodes && opts.shape.nodes.length) {
    shape = opts.shape
    shape.nodes.forEach(function (n) { if (!n.locked) n.exported = ctx.excluded[n.path] ? '否' : '是' })
  } else if (ctx.leaves.length) {
    shape = describeShapeTree(data, { leaves: ctx.leaves, excluded: opts.excluded })
  }

  if (!ctx.leaves.length) {
    collectSingletons(data, '', ctx)
    pushWarn(ctx, 'info', '未检测到对象数组，全部字段以单例键值表（_meta）输出')
  } else {
    ctx.leaves.slice(0, MAX_LEAVES).forEach(function (leaf) {
      buildLeafSheet(leaf, data, ctx)
    })
    if (ctx.leaves.length > MAX_LEAVES) {
      pushWarn(ctx, 'warn', '叶子记录路径共 ' + ctx.leaves.length + ' 套，仅输出前 ' + MAX_LEAVES + ' 张 Sheet')
    }
    if (opts.includeMeta) collectSingletons(data, '', ctx)
  }

  // 结构树只在拆出了宽表时生成（扁平 JSON 的结构已由 _meta 兜底承载，不重复一张表）
  if (shape && opts.includeStructTree && ctx.leaves.length) {
    var struct = buildStructSheet(shape, ctx)
    if (struct.rows.length) {
      struct.columns = buildColumns(struct, opts.booleanAs)
      ctx.sheets.push(struct)
    }
  }

  // _meta 默认不生成（结构已由结构树承载），但一张宽表都没生成时它是唯一载体，强制输出
  var hasWide = ctx.sheets.some(function (sh) { return sh.kind === 'data' })
  if (!hasWide && ctx.metaRows.length && !opts.includeMeta) {
    pushWarn(ctx, 'info', '未生成任何宽表，已强制输出 _meta 作为唯一兜底（结构过深或无数组）')
  }
  if ((opts.includeMeta || !hasWide) && ctx.metaRows.length) {
    var meta = {
      name: '_meta', path: '', shapePath: '', kind: 'meta', parentName: '', leafChain: [],
      columns: [], rows: ctx.metaRows.map(function (r) { return r }), fieldMeta: {}, fieldOrder: []
    }
    meta.fieldMeta = overviewFieldMeta([['字段', '字段'], ['值', '值'], ['类型', '类型']])
    meta.fieldOrder = Object.keys(meta.fieldMeta)
    meta.columns = buildColumns(meta, opts.booleanAs)
    ctx.sheets.push(meta)
  }

  finalizeSheets(ctx)

  if (opts.includeOverview) {
    var overview = buildOverview(ctx)
    if (overview.rows.length) {
      overview.columns = buildColumns(overview, opts.booleanAs)
      ctx.sheets.push(overview)
    }
  }
  checkLimits(ctx)
  return { sheets: ctx.sheets, warnings: ctx.warnings }
}

/**
 * Sheet 模型 -> 单元格矩阵（供 xlsx 写入），已完成文本强转与截断
 * @returns {{ header: Array, rows: Array }}
 */
export function sheetToMatrix(sheet, booleanAs) {
  var header = sheet.columns.map(function (c) {
    return coerceCell(c.name, booleanAs)
  })
  var rows = sheet.rows.map(function (row) {
    return sheet.columns.map(function (c) {
      return coerceCell(row[c.key], booleanAs)
    })
  })
  return { header: header, rows: rows }
}

/** 依据表头与前若干行内容估算列宽 */
export function columnWidths(sheet, booleanAs) {
  var sample = Math.min(sheet.rows.length, 50)
  return sheet.columns.map(function (c) {
    var w = String(c.name).length
    for (var i = 0; i < sample; i++) {
      var len = displayValue(sheet.rows[i][c.key], booleanAs).length
      if (len > w) w = len
    }
    var cn = /[\u4e00-\u9fa5]/.test(c.name) ? 1.7 : 1
    return { wch: Math.max(8, Math.min(48, Math.round(w * cn) + 2)) }
  })
}

// ==================== 叶子路径发现 ====================

/**
 * 收集全部叶子数组路径（自身含对象元素，且其下不再存在对象数组），按深度优先顺序
 * @returns {Array<{ shapePath: string, chain: string[], key: string }>}
 */
function collectLeafShapes(data, ctx) {
  var found = []
  var seen = {}
  if (Array.isArray(data)) {
    scanArray('$', 'root', data, [], found, seen, 0, ctx)
  } else if (isPlainObject(data)) {
    scanObject(data, '$', [], found, seen, 0, ctx)
  }
  return found
}

function scanObject(obj, path, chain, found, seen, depth, ctx) {
  if (depth > MAX_OBJ_DEPTH) return
  Object.keys(obj).forEach(function (key) {
    var val = obj[key]
    var p = path + '.' + key
    if (Array.isArray(val)) scanArray(p, key, val, chain, found, seen, depth, ctx)
    else if (isPlainObject(val)) scanObject(val, p, chain, found, seen, depth + 1, ctx)
  })
}

function scanArray(path, key, arr, chain, found, seen, depth, ctx) {
  if (depth > MAX_OBJ_DEPTH) return
  var nextChain = chain.concat([key])
  var scanned = Math.min(arr.length, MAX_SCAN_ITEMS)
  if (arr.length > MAX_SCAN_ITEMS && !seen['cap|' + stripIndices(path)]) {
    seen['cap|' + stripIndices(path)] = true
    pushWarn(ctx, 'warn', '数组「' + stripIndices(path) + '」共 ' + arr.length + ' 条，结构判定仅采样前 ' + MAX_SCAN_ITEMS + ' 条，更深层的记录路径可能未被识别')
  }
  if (hasObjectItem(arr)) {
    var nested = false
    for (var i = 0; i < scanned; i++) {
      if (isPlainObject(arr[i]) && hasNestedObjectArray(arr[i], 0)) { nested = true; break }
    }
    if (!nested) {
      var shapePath = stripIndices(path)
      if (!seen[shapePath]) {
        seen[shapePath] = true
        found.push({ shapePath: shapePath, chain: nextChain, key: key })
      }
      return
    }
  }
  for (var k = 0; k < scanned; k++) {
    if (isPlainObject(arr[k])) {
      scanObject(arr[k], path + '[' + k + ']', nextChain, found, seen, depth + 1, ctx)
    }
  }
}

function hasNestedObjectArray(obj, depth) {
  if (depth > MAX_OBJ_DEPTH) return false
  var names = Object.keys(obj)
  for (var i = 0; i < names.length; i++) {
    var val = obj[names[i]]
    if (Array.isArray(val) && hasObjectItem(val)) return true
    if (isPlainObject(val) && hasNestedObjectArray(val, depth + 1)) return true
  }
  return false
}

// ==================== 单张叶子宽表 ====================

function buildLeafSheet(leaf, data, ctx) {
  var sheet = newLeafSheet(ctx, leaf)
  var rows
  if (Array.isArray(data)) {
    // 根即数组：根数组就是链的第 0 段，容器名与发现阶段一致（'root'）
    rows = expandArray({ arr: data, key: leaf.chain[0] || 'root', path: '$' }, 0, [], leaf, 0, sheet, ctx)
  } else {
    rows = expandObject(data, '', 0, [], leaf, 0, sheet, ctx, 0)
  }
  if (rows.length > ROW_HARD_LIMIT) {
    pushWarn(ctx, 'warn', 'Sheet「' + sheet.name + '」展开后 ' + rows.length + ' 行，超过 ' + ROW_HARD_LIMIT + ' 行已截断（祖先 × 子记录笛卡尔展开导致）')
    rows = rows.slice(0, ROW_HARD_LIMIT)
  } else if (rows.length > ROW_SOFT_LIMIT) {
    pushWarn(ctx, 'warn', 'Sheet「' + sheet.name + '」展开后 ' + rows.length + ' 行，祖先逐层铺开会使行数按子记录数倍增')
  }
  buildUniqMarks(sheet, rows, ctx)
  attachPathColumns(sheet, rows, ctx)
  sheet.rows = rows.map(function (r) { return r.fields })
  return sheet
}

/**
 * 展开链上数组：逐元素下钻，父层字段随每个子行复制
 */
function expandArray(entry, slotBefore, ancestors, leaf, pos, sheet, ctx) {
  var out = []
  var arr = entry.arr
  for (var i = 0; i < arr.length; i++) {
    var item = arr[i]
    var instPath = entry.path + '[' + i + ']'
    if (!isPlainObject(item)) {
      // 混合结构中的标量元素：独立成行，值放「值」列
      var scalarFields = {}
      var valuePath = stripIndices(entry.path) + '.[]'
      regField(sheet, valuePath, entry.key, '值', slotBefore)
      scalarFields[valuePath] = item === null || item === undefined ? null : item
      out.push({ fields: scalarFields, ancestors: ancestors.concat([instPath]) })
      continue
    }
    var childRows = expandObject(item, entry.key, slotBefore + 1, ancestors.concat([instPath]), leaf, pos + 1, sheet, ctx, 0)
    for (var r = 0; r < childRows.length; r++) out.push(childRows[r])
  }
  return out
}

/**
 * 展开一个对象实例：本层标量/非链数组落成字段，链上下一段数组继续下钻
 */
function expandObject(obj, container, slot, ancestors, leaf, pos, sheet, ctx, depth) {
  if (depth > MAX_OBJ_DEPTH) {
    pushWarn(ctx, 'warn', '对象层级超过 ' + MAX_OBJ_DEPTH + ' 层，该分支已截断：' + (ancestors.join('') || '$'))
    return []
  }
  var local = {}
  var pending = []
  var wantKey = pos < leaf.chain.length ? leaf.chain[pos] : null
  var instPath = pos === 0 ? '$' : ancestors[ancestors.length - 1]
  fillFields(obj, container, slot, local, pending, instPath, wantKey, sheet, ctx, depth)

  if (pos >= leaf.chain.length) {
    return [{ fields: local, ancestors: ancestors }]
  }

  var rows = []
  for (var p = 0; p < pending.length; p++) {
    var childRows = expandArray(pending[p], slot, ancestors, leaf, pos, sheet, ctx)
    mergeInto(rows, local, childRows)
  }
  if (!rows.length && ctx.opts.fillEmptyLeafRows) {
    // 该实例的链上数组为空 / 缺失：补一行，叶子层列留空
    rows.push({ fields: copyFields(local), ancestors: ancestors, emptyLeaf: true })
  }
  return rows
}

function mergeInto(rows, local, childRows) {
  for (var i = 0; i < childRows.length; i++) {
    var merged = copyFields(local)
    var src = childRows[i].fields
    Object.keys(src).forEach(function (k) { merged[k] = src[k] })
    rows.push({ fields: merged, ancestors: childRows[i].ancestors, emptyLeaf: childRows[i].emptyLeaf })
  }
}

function copyFields(f) {
  var o = {}
  Object.keys(f).forEach(function (k) { o[k] = f[k] })
  return o
}

/**
 * 本层字段落地：非数组对象递归展列（容器名变为其 key），
 * 链上下一段数组收进 pending，其余数组内联为一格
 */
function fillFields(obj, container, slot, local, pending, instPath, wantKey, sheet, ctx, depth) {
  if (depth > MAX_OBJ_DEPTH) return
  Object.keys(obj).forEach(function (key) {
    var val = obj[key]
    var p = instPath + '.' + key
    if (wantKey && key === wantKey && Array.isArray(val)) {
      if (hasObjectItem(val)) {
        pending.push({ arr: val, key: key, path: p })
        return
      }
      // 空数组：不产生列，由上层补一行（否则多一个永久空列）；混合结构下的标量数组仍内联展示
      if (!val.length) return
      putLocalField(sheet, local, container, key, p, slot, inlineArray(val, ctx, p), ctx)
      return
    }
    if (isPlainObject(val)) {
      fillFields(val, key, slot, local, pending, p, wantKey, sheet, ctx, depth + 1)
      return
    }
    if (Array.isArray(val)) {
      putLocalField(sheet, local, container, key, p, slot, inlineArray(val, ctx, p), ctx)
      return
    }
    putLocalField(sheet, local, container, key, p, slot, val === undefined ? null : val, ctx)
  })
}

function putLocalField(sheet, local, container, key, sourcePath, slot, value, ctx) {
  // 行与列一律以「去下标的结构路径」为键，否则每个实例路径各成一列，列数会按行数膨胀
  var shapeKey = stripIndices(sourcePath)
  if (ctx && ctx.excluded[shapeKey]) return
  regField(sheet, shapeKey, container, key, slot)
  local[shapeKey] = value
}

function excludedMap(excluded) {
  var map = {}
  if (!excluded) return map
  if (Array.isArray(excluded)) {
    excluded.forEach(function (p) { if (p) map[p] = true })
  } else if (typeof excluded === 'object') {
    Object.keys(excluded).forEach(function (p) { if (excluded[p]) map[p] = true })
  }
  return map
}

function inlineArray(arr, ctx, path) {
  if (!arr.length) return ''
  if (!hasObjectItem(arr)) return joinScalars(arr, ctx.opts.booleanAs)
  var shown = arr.slice(0, MAX_CELL_ARRAY)
  pushWarn(ctx, 'info', '数组不在叶子记录链上，已整体序列化进单元格：' + stripIndices(path))
  return truncateText(safeStringify(shown)) + (arr.length > shown.length ? ' …共' + arr.length + '条' : '')
}

function joinScalars(arr, booleanAs) {
  var parts = []
  for (var i = 0; i < arr.length; i++) parts.push(displayValue(arr[i], booleanAs))
  return truncateText(parts.join(', '))
}

// ==================== 标记列 / 列定义 ====================

function newLeafSheet(ctx, leaf) {
  var sheet = {
    name: '', path: leaf.shapePath, shapePath: leaf.shapePath, kind: 'data',
    parentName: leaf.chain.slice(0, -1).join('.'), leafChain: leaf.chain,
    columns: [], rows: [], fieldMeta: {}, fieldOrder: []
  }
  sheet.name = leafSheetName(ctx, leaf)
  ctx.sheets.push(sheet)
  return sheet
}

/** 同名的两套叶子（如 questionList.optionList 与 childQuestionList.optionList）用链全名区分 */
function leafSheetName(ctx, leaf) {
  var sameName = ctx.leaves.filter(function (l) { return l.key === leaf.key }).length > 1
  var base = sameName ? leaf.chain.join('_') : leaf.key
  if (ctx.nameUsed[base]) return cleanSheetName(ctx, base)
  ctx.nameUsed[base] = leaf.shapePath
  return cleanSheetName(ctx, base)
}

function cleanSheetName(ctx, raw) {
  var name = String(raw || 'Sheet').replace(SHEET_NAME_ILLEGAL_RE, '.')
  if (name.length > SHEET_NAME_MAX) name = name.slice(0, SHEET_NAME_MAX)
  if (!name) name = 'Sheet'
  var final = name
  var idx = 2
  while (ctx.sheets.some(function (s) { return s.name === final })) {
    final = name.slice(0, SHEET_NAME_MAX - String(idx).length - 1) + '-' + idx
    idx++
  }
  return final
}

function regField(sheet, sourcePath, container, key, slot) {
  if (sheet.fieldMeta[sourcePath]) return sourcePath
  sheet.fieldMeta[sourcePath] = {
    sourcePath: sourcePath,
    name: container ? container + '.' + key : key,
    container: container,
    key: key,
    slot: slot,
    seq: sheet.fieldOrder.length
  }
  sheet.fieldOrder.push(sourcePath)
  return sourcePath
}

/** 为每个祖先层加 <层名>._uniq：该层首次出现的行=1，重复行=0（SUM 即该层去重条数） */
function buildUniqMarks(sheet, rows, ctx) {
  if (!ctx.opts.dedupMarks) return
  var chain = sheet.leafChain
  if (!chain || chain.length < 2) return
  for (var j = 0; j < chain.length - 1; j++) {
    var markerKey = regField(sheet, '#uniq.' + j, chain[j], '_uniq', j + 1)
    var lastPath = null
    for (var i = 0; i < rows.length; i++) {
      var row = rows[i]
      var layerPath = row.ancestors[j]
      if (layerPath === undefined) continue
      if (layerPath !== lastPath) {
        lastPath = layerPath
        row.fields[markerKey] = 1
      } else {
        row.fields[markerKey] = 0
      }
    }
  }
}

/** 内部回溯列：当前行实例路径 + 上一级实例路径（补行时两者相同） */
function attachPathColumns(sheet, rows, ctx) {
  var pathKey = regField(sheet, '_rowPath', '', '_行路径', 98)
  var parentKey = regField(sheet, '_parentPath', '', '_父记录路径', 99)
  rows.forEach(function (row) {
    var anc = row.ancestors || []
    var self = anc.length ? anc[anc.length - 1] : '$'
    if (row.emptyLeaf) self = self + ' ⟪无子记录⟫'
    row.fields[pathKey] = self
    row.fields[parentKey] = anc.length > 1 ? anc[anc.length - 2] : (anc.length === 1 ? '$' : '')
  })
}

function finalizeSheets(ctx) {
  ctx.sheets.forEach(function (sh) {
    if (sh.kind !== 'data') return
    sh.columns = buildColumns(sh, ctx.opts.booleanAs)
    assignNames(sh, ctx)
    if (!sh.columns.some(function (c) { return !c.internal })) {
      pushWarn(ctx, 'warn', 'Sheet「' + sh.name + '」业务字段已全部取消，仅保留标记列与回溯列（行数不变）')
    }
    if (!sh.columns.length) {
      sh.columns = [newColumn({ sourcePath: '_空行', name: '(无字段)', container: '', key: '_空行', slot: 99, seq: 0 })]
      pushWarn(ctx, 'info', 'Sheet「' + sh.name + '」内元素无字段，已输出占位列')
    }
  })
}

function buildColumns(sheet, booleanAs) {
  var stat = {}
  var cols = []
  var used = {}
  sheet.rows.forEach(function (row) {
    Object.keys(row).forEach(function (k) {
      if (!used[k]) {
        used[k] = true
        stat[k] = { present: 0, nonEmpty: 0, sample: undefined, types: {} }
        cols.push(newColumn(sheet.fieldMeta[k] || { sourcePath: k, name: k, container: '', key: k, slot: 90, seq: 0 }))
      }
      var s = stat[k]
      var v = row[k]
      s.present++
      if (v !== null && v !== undefined && v !== '') {
        s.nonEmpty++
        s.types[typeOf(v)] = true
        // 示例取首个有值样本（0 / false 算有效值，不再被“真值”判定跳过）
        if (s.sample === undefined) s.sample = v
      }
    })
  })
  // 该表存在过但所有行都无值的列（如补行导致叶子层整体为空）
  sheet.fieldOrder.forEach(function (k) {
    if (used[k]) return
    used[k] = true
    stat[k] = { present: 0, nonEmpty: 0, sample: undefined, types: {} }
    cols.push(newColumn(sheet.fieldMeta[k]))
  })
  var totalRows = sheet.rows.length
  cols.forEach(function (c) {
    var s = stat[c.key]
    // 非空率分母固定为该表总行数：否则补行与结构不齐的行会被排除在分母外，覆盖率被高估
    c.total = totalRows
    c.present = s.present
    c.nonEmpty = s.nonEmpty
    c.emptyCount = totalRows - s.nonEmpty
    c.missing = totalRows - s.present
    c.rate = totalRows ? Math.round((s.nonEmpty / totalRows) * 1000) / 10 : 0
    c.type = aggregateType(s.types)
    c.sample = displayValue(s.sample, booleanAs || 'text').slice(0, 60)
  })
  cols.sort(function (a, b) {
    if (a.slot !== b.slot) return a.slot - b.slot
    return a.seq - b.seq
  })
  return cols
}

var TYPE_ORDER = { number: 1, boolean: 2, string: 3, 'numeric-text(强转文本)': 4, array: 5, object: 6, null: 7 }

/** 混合类型全部标出（number|string），不再只按首个样本误标 */
function aggregateType(types) {
  var list = Object.keys(types)
  if (!list.length) return '全空'
  list.sort(function (a, b) {
    return (TYPE_ORDER[a] || 9) - (TYPE_ORDER[b] || 9)
  })
  if (list.length > 4) return list.slice(0, 4).join('|') + '|…共' + list.length + '类'
  return list.join('|')
}

/** 列类别：让总览成为真正的列全集字典（含标记列与回溯列） */
function columnClass(c) {
  if (c.key === '_空行') return '占位列'
  if (String(c.key).charAt(0) === '#') return '标记列'
  if (c.key === '_rowPath' || c.key === '_parentPath') return '回溯列'
  return c.internal ? '内部列' : '业务列'
}

/** 字典里的 JSON 路径列：合成列没有真实路径，写清用途而不是留内部编号 */
function dictPath(c) {
  if (c.sourcePath.charAt(0) === '#') return '(标记列，非 JSON 字段)'
  if (c.sourcePath === '_rowPath') return '(生成列：本行记录的 JSON 实例路径)'
  if (c.sourcePath === '_parentPath') return '(生成列：父记录的 JSON 实例路径)'
  if (c.sourcePath === '_空行') return '(生成列：无字段时的占位)'
  return c.sourcePath
}

function newColumn(m) {
  return {
    key: m.sourcePath,
    name: m.name,
    sourcePath: m.sourcePath,
    container: m.container,
    slot: m.slot,
    seq: m.seq,
    internal: String(m.key).charAt(0) === '_' || String(m.key).charAt(0) === '#',
    type: 'unknown',
    total: 0,
    present: 0,
    nonEmpty: 0,
    emptyCount: 0,
    missing: 0,
    rate: 0,
    sample: ''
  }
}

/** 列名跨层撞名时升级为完整 JSON 路径 */
function assignNames(sheet, ctx) {
  var byName = {}
  sheet.columns.forEach(function (c) {
    var list = byName[c.name]
    if (!list) byName[c.name] = [c]
    else list.push(c)
  })
  Object.keys(byName).forEach(function (name) {
    var list = byName[name]
    if (list.length < 2) return
    list.forEach(function (c) {
      var full = c.sourcePath.charAt(0) === '$' ? c.sourcePath : (c.container ? c.container + '.' + c.sourcePath : c.sourcePath)
      var guard = 0
      while (list.some(function (o) { return o !== c && o.name === full })) { full = c.sourcePath + '#' + (++guard) }
      c.name = full
    })
    pushWarn(ctx, 'info', 'Sheet「' + sheet.name + '」列名「' + name + '」跨层重复，已升级为完整路径')
  })
}

// ==================== shape 结构树 ====================

/**
 * 产出 JSON 的 shape 结构树：一个字段名 = 一个节点（不为下标建节点），数组节点只标元素条数，
 * 跳实例合并类型/计数。行粒度仍由引擎自动判定，本函数只负责把结构讲清楚并标出锁定项。
 * @param {any} data 已解析的 JSON
 * @param {Object} options { excluded, leaves, booleanAs }
 * @returns {{ tree: Array, nodes: Array, leaves: Array, lockedKeys: Array, selectableKeys: Array, warnings: Array }}
 */
export function describeShapeTree(data, options) {
  var opts = {}
  Object.keys(DEFAULTS).forEach(function (k) {
    opts[k] = (options && options[k] !== undefined) ? options[k] : DEFAULTS[k]
  })
  var sctx = {
    warnings: [], warnSeen: {}, opts: opts, nodes: {}, order: [],
    excluded: excludedMap(opts.excluded), leaves: null
  }
  if (opts.leaves && opts.leaves.length) {
    sctx.leaves = opts.leaves
  } else {
    var scanCtx = { warnings: sctx.warnings, warnSeen: sctx.warnSeen, leaves: [], opts: opts }
    sctx.leaves = collectLeafShapes(data, scanCtx)
  }
  if (data === null || data === undefined) {
    return { tree: [], nodes: [], leaves: [], lockedKeys: [], selectableKeys: [], warnings: sctx.warnings }
  }
  if (Array.isArray(data)) walkValue(data, '$', 'root', '', sctx, 0)
  else if (isPlainObject(data)) walkValue(data, '$', '$', '', sctx, 0)
  else walkValue(data, '$', '$', '', sctx, 0)

  var summary = finishShape(sctx)
  summary.warnings.push.apply(summary.warnings, sctx.warnings)
  return summary
}

function shapeNode(path, name, parent, depth) {
  return {
    key: path, path: path, name: name, parent: parent, depth: depth,
    kind: 'field', instances: 0, count: 0, nonEmpty: 0,
    items: 0, minItems: Infinity, maxItems: 0, truncated: false,
    sawMap: false, sawArrObj: false, sawArrScalar: false, sawEmptyArr: false,
    types: {}, sample: undefined, children: [], childKeys: {}
  }
}

/** 同一路径可能忽而是数组、忽而是 null / 对象，按“结构优先”合并判定，避免后到的空值把数组降级 */
function mergeKind(n) {
  if (n.sawArrObj) return 'array<object>'
  if (n.sawArrScalar) return 'array<scalar>'
  if (n.sawEmptyArr) return 'array<empty>'
  return n.sawMap ? 'object' : 'field'
}

function getShapeNode(ctx, path, name, parent) {
  var node = ctx.nodes[path]
  if (node) return node
  var parentDepth = parent && ctx.nodes[parent] ? ctx.nodes[parent].depth : -1
  node = shapeNode(path, name, parent, parentDepth + 1)
  ctx.nodes[path] = node
  ctx.order.push(path)
  var pn = parent ? ctx.nodes[parent] : null
  if (pn && !pn.childKeys[path]) {
    pn.childKeys[path] = true
    pn.children.push(node)
  }
  return node
}

function childPath(path, key) {
  return path === '$' ? '$.' + key : path + '.' + key
}

function walkValue(val, path, name, parent, ctx, depth) {
  var node = getShapeNode(ctx, path, name, parent)
  if (isPlainObject(val)) {
    node.sawMap = true
    node.instances++
    if (depth > MAX_OBJ_DEPTH) {
      node.truncated = true
      pushWarn(ctx, 'warn', '结构超出 ' + MAX_OBJ_DEPTH + ' 层，已截断展示：' + path)
      return node
    }
    Object.keys(val).forEach(function (k) {
      walkValue(val[k], childPath(path, k), k, path, ctx, depth + 1)
    })
    return node
  }
  if (Array.isArray(val)) {
    node.instances++
    node.items += val.length
    if (val.length < node.minItems) node.minItems = val.length
    if (val.length > node.maxItems) node.maxItems = val.length
    if (!val.length) node.sawEmptyArr = true
    else if (hasObjectItem(val)) node.sawArrObj = true
    else node.sawArrScalar = true
    if (hasObjectItem(val)) {
      var limit = Math.min(val.length, MAX_SCAN_ITEMS)
      for (var i = 0; i < limit; i++) {
        if (!isPlainObject(val[i])) continue
        Object.keys(val[i]).forEach(function (k) {
          walkValue(val[i][k], childPath(path, k), k, path, ctx, depth + 1)
        })
      }
      if (val.length > limit) node.truncated = true
      return node
    }
    var cap = Math.min(val.length, MAX_SCAN_ITEMS)
    for (var j = 0; j < cap; j++) {
      if (val[j] !== null && val[j] !== undefined && val[j] !== '') node.types[typeOf(val[j])] = true
    }
    node.count += val.length ? 1 : 0
    node.nonEmpty += val.length ? 1 : 0
    if (node.sample === undefined) node.sample = joinScalars(val, ctx.opts.booleanAs)
    return node
  }
  node.count++
  var has = val !== null && val !== undefined && val !== ''
  if (has) {
    node.nonEmpty++
    node.types[typeOf(val)] = true
    if (node.sample === undefined) node.sample = displayValue(val, ctx.opts.booleanAs)
  }
  return node
}

function finishShape(ctx) {
  var leafSet = {}
  var leafPaths = []
  ctx.leaves.forEach(function (l) {
    leafSet[l.shapePath] = true
    leafPaths.push(l.shapePath)
  })
  var lockedKeys = []
  var selectableKeys = []
  ctx.order.forEach(function (path) {
    var n = ctx.nodes[path]
    n.kind = mergeKind(n)
    n.parentOnChain = onChain(n.parent, leafPaths)
    n.selfOnChain = onChain(n.path, leafPaths)
    n.underLeaf = leafPaths.some(function (lp) { return n.path.indexOf(lp + '.') === 0 })
    n.sheet = sheetNameForNode(ctx, n, leafPaths)
    if (n.kind === 'object') {
      n.typeText = 'object'
      n.countText = n.instances > 1 ? n.instances + ' 个实例' : ''
      n.role = n.underLeaf ? '记录内对象（仅分组）' : (n.selfOnChain ? '父级对象（仅分组）' : '对象（仅分组）')
      n.locked = true
    } else if (n.kind === 'array<object>') {
      n.typeText = 'array<object>'
      n.countText = arrayCountText(n)
      if (leafSet[n.path]) { n.role = '行粒度数组（一个元素 = 一行）'; n.locked = true }
      else if (n.selfOnChain) { n.role = '祖先数组（逐层铺开）'; n.locked = true }
      else { n.role = '未拆表数组（整体序列化进单元格）'; n.locked = false }
      if (n.sample === undefined) n.sample = '{' + n.children.map(function (c) { return c.name }).slice(0, 8).join(', ') + '}' + (n.children.length > 8 ? '…共' + n.children.length + '个字段' : '')
    } else if (n.kind === 'array<scalar>' || n.kind === 'array<empty>') {
      var elType = aggregateType(n.types)
      n.typeText = 'array<' + (elType === '全空' ? 'empty' : elType) + '>'
      n.countText = arrayCountText(n)
      n.role = n.underLeaf ? '叶子层标量数组（内联成单元格）' : (n.parentOnChain ? '父级标量数组（内联）' : '标量数组（单例）')
      n.locked = false
    } else {
      n.typeText = aggregateType(n.types)
      n.countText = n.nonEmpty + '/' + n.count + ' 有值'
      if (leafSet[n.parent]) n.role = '叶子字段'
      else if (n.underLeaf) n.role = '记录内字段'
      else if (n.parentOnChain) n.role = '父级字段（祖先铺开）'
      else n.role = '单例字段'
      n.locked = false
    }
    n.exported = n.locked ? '结构节点' : (ctx.excluded[n.path] ? '否' : '是')
    if (n.sample === undefined || n.sample === null) n.sample = ''
    if (String(n.sample).length > 120) n.sample = String(n.sample).slice(0, 120) + '…'
    if (n.locked) lockedKeys.push(n.path)
    else selectableKeys.push(n.path)
  })
  var roots = ctx.order.length ? [ctx.nodes[ctx.order[0]]] : []
  return {
    tree: roots,
    nodes: ctx.order.map(function (p) { return ctx.nodes[p] }),
    leaves: ctx.leaves,
    lockedKeys: lockedKeys,
    selectableKeys: selectableKeys,
    warnings: []
  }
}

/** 该 shape 路径是否在某套叶子记录的链上（包含链上任意父层与叶子数组本身） */
function onChain(p, leafPaths) {
  if (!p) return false
  return leafPaths.some(function (lp) { return lp === p || lp.indexOf(p + '.') === 0 })
}

function arrayCountText(n) {
  if (!n.instances) return ''
  var range = n.minItems === n.maxItems ? n.maxItems + ' 条' : n.minItems + '~' + n.maxItems + ' 条'
  return range + ' ×' + n.instances + ' 组（共 ' + n.items + ' 条）' + (n.truncated ? ' ※仅采样前 ' + MAX_SCAN_ITEMS + ' 条判型' : '')
}

/** 节点归属哪些宽表：先看自身落在哪套叶子链上，落不到才看父容器（父级字段会同时出现在多张表里） */
function sheetNameForNode(ctx, n, leafPaths) {
  var own = []
  var viaParent = []
  leafPaths.forEach(function (lp) {
    var inNode = lp === n.path || lp.indexOf(n.path + '.') === 0 || n.path.indexOf(lp + '.') === 0
    var inParent = !!n.parent && (lp === n.parent || lp.indexOf(n.parent + '.') === 0)
    if (inNode) own.push(lp)
    else if (inParent) viaParent.push(lp)
  })
  var picks = own.length ? own : viaParent
  var names = {}
  var list = []
  picks.forEach(function (lp) {
    var leaf = null
    ctx.leaves.forEach(function (l) { if (l.shapePath === lp && !leaf) leaf = l })
    if (!leaf) return
    var sameKey = ctx.leaves.filter(function (l) { return l.key === leaf.key }).length > 1
    var nm = sameKey ? leaf.chain.join('_') : leaf.key
    if (!names[nm]) { names[nm] = true; list.push(nm) }
  })
  if (!list.length) return ''
  if (list.length > 3) return list.slice(0, 3).join('、') + '…共' + list.length + '张'
  return list.join('、')
}

/** 全量结构的静态呈现（Excel 里没有交互，用层级 + 角色列把树“压平”） */
function buildStructSheet(shape, ctx) {
  var built = {}
  ctx.sheets.forEach(function (sh) { if (sh.kind === 'data') built[sh.name] = true })
  var defs = [
    ['层级', 'depth'], ['节点', 'name'], ['JSON路径', 'path'], ['类型', 'typeText'],
    ['元素条数', 'countText'], ['结构角色', 'role'], ['归属Sheet', 'sheet'],
    ['是否导出', 'exported'], ['示例值', 'sample']
  ]
  var sheet = {
    name: '_结构树', path: '', shapePath: '', kind: 'struct', parentName: '', leafChain: [],
    columns: [], rows: [], fieldMeta: overviewFieldMeta(defs), fieldOrder: []
  }
  sheet.fieldOrder = Object.keys(sheet.fieldMeta)
  shape.nodes.forEach(function (n) {
    var belong = n.sheet || ''
    // 超过 Sheet 数上限时，结构树里的归属列会指向未生成的表，需标注免得误导
    if (belong && !belong.split('、').some(function (x) { return built[x.replace(/…共.*$/, '')] })) {
      belong += '（未生成）'
    }
    sheet.rows.push({
      depth: n.depth, name: n.name, path: n.path, typeText: n.typeText,
      countText: n.countText, role: n.role, sheet: belong, exported: n.exported, sample: n.sample
    })
  })
  return sheet
}

// ==================== _meta（单例键值表） ====================

/**
 * 只收录「不在任何对象数组元素内部」的字段：接口头部信息、根级标量、标量数组、null
 * （对象数组的内容已由宽表承载，故不重复收录）
 */
function collectSingletons(node, rel, ctx) {
  if (!isPlainObject(node)) {
    if (Array.isArray(node) && !metaExcluded(ctx, rel)) pushMeta(ctx, rel || '$', inlineArray(node, ctx, '$'), 'array')
    return
  }
  Object.keys(node).forEach(function (key) {
    var val = node[key]
    var rk = rel ? rel + '.' + key : key
    if (metaExcluded(ctx, rk)) return
    if (isPlainObject(val)) { collectSingletons(val, rk, ctx); return }
    if (Array.isArray(val)) {
      if (hasObjectItem(val)) {
        if (!isLeafShapePath(rk, ctx)) pushMeta(ctx, rk, inlineArray(val, ctx, rk), 'array<object>(未拆表)')
        return
      }
      pushMeta(ctx, rk, val.length ? joinScalars(val, ctx.opts.booleanAs) : '', val.length ? typeOf(val[0]) : 'array(空)')
      return
    }
    pushMeta(ctx, rk, displayValue(val, ctx.opts.booleanAs), typeOf(val))
  })
}

/** 结构树用 shape 路径（$.a.b），_meta 用去根相对路径，此处对齐后勾选才能同时生效 */
function metaExcluded(ctx, rk) {
  if (!ctx.excluded || !rk) return false
  return !!ctx.excluded['$.' + rk]
}

function isLeafShapePath(rel, ctx) {
  var full = '$.' + rel
  for (var i = 0; i < ctx.leaves.length; i++) {
    var sp = ctx.leaves[i].shapePath
    if (sp === full || sp.indexOf(full + '.') === 0 || full.indexOf(sp) === 0) return true
  }
  return false
}

function pushMeta(ctx, path, val, type) {
  if (ctx.metaSeen[path]) return
  ctx.metaSeen[path] = true
  ctx.metaRows.push({ 字段: path, 值: val, 类型: type })
}

// ==================== _结构总览 ====================

function overviewFieldMeta(pairs) {
  var m = {}
  pairs.forEach(function (p, i) {
    m[p[1]] = { sourcePath: p[1], name: p[0], container: '', key: p[1], slot: 0, seq: i }
  })
  return m
}

function buildOverview(ctx) {
  var defs = [
    ['Sheet名', 'sheetName'], ['表类型', 'kind'], ['列类别', 'colClass'], ['字段名', 'name'],
    ['JSON路径', 'sourcePath'], ['值类型', 'type'], ['总行数', 'total'], ['非空行数', 'nonEmpty'],
    ['空值行数', 'emptyCount'], ['缺字段行数', 'missing'], ['非空率', 'rate'], ['示例值', 'sample']
  ]
  var sheet = {
    name: '_结构总览', path: '', shapePath: '', kind: 'overview', parentName: '', leafChain: [],
    columns: [], rows: [], fieldMeta: overviewFieldMeta(defs), fieldOrder: []
  }
  Object.keys(sheet.fieldMeta).forEach(function (k) { sheet.fieldOrder.push(k) })
  // 先收集宽表已有的列路径：单例字段已作为宽表列进入字典，不再重复一条 _meta 条目
  var widePaths = {}
  var hasData = false
  ctx.sheets.forEach(function (sh) {
    if (sh.kind !== 'data') return
    hasData = true
    sh.columns.forEach(function (c) {
      // 去下标后的结构路径才是列的唯一身份，带下标的实例路径会随行数膨胀
      widePaths[c.sourcePath] = true
    })
  })
  // 无宽表时（纯扁平 JSON）全部字段都在 _meta，此时总览与 _meta 内容完全一致，不生成
  if (!hasData) return sheet
  ctx.sheets.forEach(function (sh) {
    if (sh.kind === 'data') {
      sh.columns.forEach(function (c) {
        sheet.rows.push({
          sheetName: sh.name,
          kind: sh.leafChain.length > 1 ? '叶子宽表(祖先已铺到根)' : '叶子宽表',
          colClass: columnClass(c),
          name: c.name,
          sourcePath: dictPath(c),
          type: c.type,
          total: c.total,
          nonEmpty: c.nonEmpty,
          emptyCount: c.emptyCount,
          missing: c.missing,
          rate: c.rate,
          sample: c.sample
        })
      })
      return
    }
    if (sh.kind === 'meta') {
      // 只补字典里没有的条目（未拆表数组、链外结构等），避免与宽表列重复
      sh.rows.forEach(function (row) {
        var sp = '$.' + row['字段']
        if (widePaths[sp]) return
        var val = row['值']
        var has = val !== '' && val !== null && val !== undefined
        sheet.rows.push({
          sheetName: '_meta',
          kind: '单例键值表',
          colClass: '单例字段',
          name: row['字段'],
          sourcePath: sp,
          type: row['类型'],
          total: 1,
          nonEmpty: has ? 1 : 0,
          emptyCount: has ? 0 : 1,
          missing: 0,
          rate: has ? 100 : 0,
          sample: String(val === null || val === undefined ? '' : val).slice(0, 60)
        })
      })
    }
  })
  return sheet
}

// ==================== 限制与告警 ====================

function checkLimits(ctx) {
  ctx.sheets.forEach(function (sh) {
    if (sh.columns.length > COL_WARN_LIMIT) {
      pushWarn(ctx, 'warn', 'Sheet「' + sh.name + '」列数 ' + sh.columns.length + ' 超过 ' + COL_WARN_LIMIT + '，请检查祖先铺开层级是否过深')
    }
    if (sh.rows.length > ROW_WARN_LIMIT) {
      pushWarn(ctx, 'warn', 'Sheet「' + sh.name + '」行数 ' + sh.rows.length + ' 超出 Excel 单表上限，导出将被截断')
    }
  })
  if (ctx.sheets.length > MAX_LEAVES) {
    pushWarn(ctx, 'warn', 'Sheet 数量超过 ' + MAX_LEAVES + '，超出部分未输出')
  }
}

function pushWarn(ctx, level, msg) {
  var key = level + '|' + msg
  if (ctx.warnSeen[key]) return
  ctx.warnSeen[key] = true
  ctx.warnings.push({ level: level, key: key, msg: msg })
}

// ==================== 通用工具 ====================

function isPlainObject(v) {
  return v !== null && typeof v === 'object' && !Array.isArray(v)
}

function hasObjectItem(arr) {
  for (var i = 0; i < arr.length; i++) if (isPlainObject(arr[i])) return true
  return false
}

function stripIndices(p) {
  return String(p || '').replace(/\[\d+\]/g, '')
}

function typeOf(v) {
  if (v === null || v === undefined) return 'null'
  if (Array.isArray(v)) return 'array'
  if (typeof v === 'number') return 'number'
  if (typeof v === 'boolean') return 'boolean'
  if (typeof v === 'string') {
    if (v !== '' && /^-?\d+(\.\d+)?$/.test(v)) return 'numeric-text(强转文本)'
    return 'string'
  }
  return 'object'
}

export function displayValue(v, booleanAs) {
  if (v === null || v === undefined) return ''
  if (typeof v === 'boolean') return boolText(v, booleanAs)
  if (Array.isArray(v)) return joinScalars(v, booleanAs)
  if (typeof v === 'object') return safeStringify(v)
  return truncateText(String(v))
}

function boolText(v, booleanAs) {
  if (booleanAs === 'cn') return v ? '是' : '否'
  if (booleanAs === 'number') return v ? '1' : '0'
  return v ? 'true' : 'false'
}

function safeStringify(v) {
  try {
    return JSON.stringify(v)
  } catch (e) {
    return String(v)
  }
}

function truncateText(s) {
  var str = String(s === null || s === undefined ? '' : s)
  if (str.length <= CELL_TEXT_MAX) return str
  return str.slice(0, CELL_TEXT_MAX - 8) + '…[截断]'
}

/**
 * 单元格类型强转：数字型字符串一律按文本写入，防科学计数法 / 前导零丢失 / 公式注入
 */
export function coerceCell(value, booleanAs) {
  if (value === null || value === undefined || value === '') return { t: 's', v: '' }
  if (typeof value === 'boolean') return { t: 's', v: boolText(value, booleanAs) }
  if (typeof value === 'number') {
    if (!isFinite(value)) return { t: 's', v: String(value), z: '@' }
    return { t: 'n', v: value }
  }
  var s = truncateText(String(value))
  if (s !== '' && /^-?\d+(\.\d+)?$/.test(s)) return { t: 's', v: s, z: '@' }
  return { t: 's', v: s }
}
