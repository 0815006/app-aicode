<template>
  <el-dialog :title="'编辑 ' + sectionLabel + ' 字段定义'" :visible.sync="visible" width="98%" top="2vh" append-to-body @close="handleClose">
    <div class="dialog-layout">
      <div class="input-area">
        <div style="margin-bottom: 10px; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 6px;">
          <div style="display: flex; align-items: center; gap: 6px;">
            <el-button type="primary" size="small" icon="el-icon-plus" @click="addRow">添加一行</el-button>
            <el-tag size="small" type="info">{{ list.length }} 个字段</el-tag>
            <el-button type="success" size="small" icon="el-icon-download" @click="handleDownloadCurrentFields">下载本页定义</el-button>
            <el-button type="info" size="small" icon="el-icon-document" @click="handleDownloadGeneralTemplate">下载通用模板</el-button>
            <el-upload
              style="display: inline-block;"
              action=""
              :auto-upload="false"
              :show-file-list="false"
              :on-change="handleUploadExcel"
              accept=".xls,.xlsx">
              <el-button type="warning" size="small" icon="el-icon-upload2">上传解析</el-button>
            </el-upload>
          </div>
          <div class="table-tip">* 变量名和规则类型为必填项</div>
        </div>

        <el-table :data="list" border size="mini" height="58vh">
          <!-- 序号 -->
          <el-table-column label="序号" width="60" align="center">
            <template slot-scope="scope">
              <span style="font-family:monospace;color:#909399">{{ computeOrder(scope.$index) }}</span>
            </template>
          </el-table-column>

          <!-- 层级控制 -->
          <el-table-column label="层级" width="70" align="center">
            <template slot-scope="scope">
              <el-button
                v-if="scope.row.level === 1"
                type="text" size="mini" icon="el-icon-d-arrow-right"
                title="降级为子字段"
                @click="indentRow(scope.$index)"
              ></el-button>
              <el-button
                v-else
                type="text" size="mini" icon="el-icon-d-arrow-left"
                title="升级为一级字段"
                @click="outdentRow(scope.$index)"
              ></el-button>
              <span style="font-size:11px;color:#909399;margin-left:2px">{{ scope.row.level === 1 ? '一级' : '二级' }}</span>
            </template>
          </el-table-column>

          <!-- 变量名 -->
          <el-table-column label="变量名*" width="130">
            <template slot-scope="scope">
              <el-input v-model="scope.row.fieldKey" size="mini" placeholder="如 userName"></el-input>
            </template>
          </el-table-column>

          <!-- 字段描述 -->
          <el-table-column label="字段描述" width="110">
            <template slot-scope="scope">
              <el-input v-model="scope.row.fieldName" size="mini" placeholder="业务描述"></el-input>
            </template>
          </el-table-column>

          <!-- 长度(字节) -->
          <el-table-column label="长度(B)" width="80">
            <template slot-scope="scope">
              <el-input-number v-model="scope.row.length" :controls="false" size="mini" :min="1" :max="9999" style="width:100%"></el-input-number>
            </template>
          </el-table-column>

          <!-- 是否必填 -->
          <el-table-column label="必填" width="65" align="center">
            <template slot-scope="scope">
              <el-checkbox v-model="scope.row._isRequired" @change="onIsRequiredChange(scope.row)"></el-checkbox>
            </template>
          </el-table-column>

          <!-- 补齐方向 -->
          <el-table-column label="补齐" width="90">
            <template slot-scope="scope">
              <el-select v-model="scope.row.paddingDirection" size="mini" style="width:100%">
                <el-option label="不补" value="NONE" />
                <el-option label="左补" value="LEFT" />
                <el-option label="右补" value="RIGHT" />
              </el-select>
            </template>
          </el-table-column>

          <!-- 补齐字符 -->
          <el-table-column label="补齐字符" width="80">
            <template slot-scope="scope">
              <el-select v-model="scope.row.paddingChar" size="mini" style="width:100%" :disabled="scope.row.paddingDirection === 'NONE'">
                <el-option label="空格" value=" " />
                <el-option label="0" value="0" />
              </el-select>
            </template>
          </el-table-column>

          <!-- 规则类型 -->
          <el-table-column label="规则类型*" width="120">
            <template slot-scope="scope">
              <el-select v-model="scope.row.ruleType" size="mini" style="width:100%" @change="onRuleTypeChange(scope.row)">
                <el-option label="固定值" value="FIXED" />
                <el-option label="日期" value="DATE" />
                <el-option label="枚举" value="ENUM" />
                <el-option label="序列号" value="SEQUENCE" />
                <el-option label="随机汉字" value="RANDOM_CN" />
                <el-option label="随机数字" value="RANDOM_NUM" />
                <el-option label="随机UUID" value="RANDOM_UUID" />
                <el-option label="引用文件" value="REF_FILE" />
                <el-option label="引用字段" value="REF_FIELD" />
                <el-option label="汇总金额" value="SUM" />
                <el-option label="统计行数" value="COUNT" />
                <el-option label="批次号" value="BATCH_NO" />
                <el-option label="金额" value="AMOUNT" />
                <el-option label="表达式" value="EXPR" />
              </el-select>
            </template>
          </el-table-column>

          <!-- 引用/配置 -->
          <el-table-column label="引用/配置" min-width="180">
            <template slot-scope="scope">
              <!-- FIXED -->
              <el-input v-if="scope.row.ruleType === 'FIXED'" v-model="scope.row._fixedVal" size="mini" placeholder="固定值" />
              <!-- DATE -->
              <el-select v-else-if="scope.row.ruleType === 'DATE'" v-model="scope.row._dateFmt" size="mini" style="width:100%" allow-create filterable placeholder="日期格式">
                <el-option label="YYYYMMDD" value="YYYYMMDD" />
                <el-option label="yyyyMMdd" value="yyyyMMdd" />
                <el-option label="yyyyMMddHHmmss" value="yyyyMMddHHmmss" />
                <el-option label="yyMMdd" value="yyMMdd" />
                <el-option label="MMdd" value="MMdd" />
                <el-option label="HHmmss" value="HHmmss" />
              </el-select>
              <!-- ENUM -->
              <el-select v-else-if="scope.row.ruleType === 'ENUM'" v-model="scope.row.refEnumKey" size="mini" style="width:100%" placeholder="选择枚举Key">
                <el-option v-for="k in enumKeys" :key="k" :label="k" :value="k" />
              </el-select>
              <!-- REF_FIELD -->
              <el-select v-else-if="scope.row.ruleType === 'REF_FIELD'" v-model="scope.row.refFieldKey" size="mini" style="width:100%" placeholder="选择前置字段">
                <el-option v-for="key in getAvailableKeys(scope.$index)" :key="key" :label="key" :value="key" />
              </el-select>
              <!-- REF_FILE -->
              <template v-else-if="scope.row.ruleType === 'REF_FILE'">
                <el-select v-model="scope.row.refFileId" size="mini" style="width:100%" placeholder="选择素材文件">
                  <el-option v-for="rf in refFiles" :key="rf.id" :label="rf.refName" :value="rf.id" />
                </el-select>
              </template>
              <!-- SEQUENCE -->
              <template v-else-if="scope.row.ruleType === 'SEQUENCE'">
                <el-popover placement="bottom" width="320" trigger="click">
                  <div style="display:flex;flex-direction:column;gap:8px">
                    <div style="display:flex;align-items:center;gap:8px">
                      <span style="width:60px;font-size:12px">前缀:</span>
                      <el-input v-model="scope.row._seqPrefix" size="mini" placeholder="如 S" style="flex:1"></el-input>
                    </div>
                    <div style="display:flex;align-items:center;gap:8px">
                      <span style="width:60px;font-size:12px">起始值:</span>
                      <el-input-number v-model="scope.row._seqStart" :min="0" size="mini" style="flex:1"></el-input-number>
                    </div>
                    <div style="display:flex;align-items:center;gap:8px">
                      <span style="width:60px;font-size:12px">步长:</span>
                      <el-input-number v-model="scope.row._seqStep" :min="1" size="mini" style="flex:1"></el-input-number>
                    </div>
                    <div style="display:flex;align-items:center;gap:8px">
                      <span style="width:60px;font-size:12px">数字位数:</span>
                      <el-input-number v-model="scope.row._seqDigitLen" :min="1" :max="20" size="mini" style="flex:1"></el-input-number>
                    </div>
                    <div style="display:flex;align-items:center;gap:8px">
                      <span style="width:60px;font-size:12px">策略:</span>
                      <el-select v-model="scope.row._seqStrategy" size="mini" style="flex:1">
                        <el-option label="每行更新(PER_LINE)" value="PER_LINE" />
                        <el-option label="每文件更新(PER_FILE)" value="PER_FILE" />
                      </el-select>
                    </div>
                  </div>
                  <el-button slot="reference" size="mini" plain>
                    前缀:{{ scope.row._seqPrefix || '-' }} 起始:{{ scope.row._seqStart }} 位数:{{ scope.row._seqDigitLen }}
                  </el-button>
                </el-popover>
              </template>
              <!-- RANDOM_CN -->
              <template v-else-if="scope.row.ruleType === 'RANDOM_CN'">
                <span style="font-size:12px;margin-right:4px">汉字个数:</span>
                <el-input-number v-model="scope.row._randomCount" :min="1" :max="100" size="mini" style="width:80px"></el-input-number>
              </template>
              <!-- RANDOM_NUM -->
              <template v-else-if="scope.row.ruleType === 'RANDOM_NUM'">
                <span style="font-size:12px;margin-right:4px">数字位数:</span>
                <el-input-number v-model="scope.row._randomCount" :min="1" :max="30" size="mini" style="width:80px"></el-input-number>
              </template>
              <!-- RANDOM_UUID -->
              <template v-else-if="scope.row.ruleType === 'RANDOM_UUID'">
                <span style="font-size:12px;margin-right:4px">大写:</span>
                <el-switch v-model="scope.row._uuidUpperCase" size="mini"></el-switch>
              </template>
              <!-- SUM -->
              <span v-else-if="scope.row.ruleType === 'SUM'" style="color:#67c23a;font-size:12px">自动汇总Body金额</span>
              <!-- COUNT -->
              <span v-else-if="scope.row.ruleType === 'COUNT'" style="color:#67c23a;font-size:12px">自动统计Body行数</span>
              <!-- BATCH_NO -->
              <template v-else-if="scope.row.ruleType === 'BATCH_NO'">
                <el-popover placement="bottom" width="280" trigger="click">
                  <div style="display:flex;flex-direction:column;gap:8px">
                    <div style="display:flex;align-items:center;gap:8px">
                      <span style="width:60px;font-size:12px">前缀:</span>
                      <el-input v-model="scope.row._batchPrefix" size="mini" placeholder="如 B" style="flex:1"></el-input>
                    </div>
                    <div style="display:flex;align-items:center;gap:8px">
                      <span style="width:60px;font-size:12px">起始值:</span>
                      <el-input-number v-model="scope.row._batchStart" :min="0" size="mini" style="flex:1"></el-input-number>
                    </div>
                    <div style="display:flex;align-items:center;gap:8px">
                      <span style="width:60px;font-size:12px">策略:</span>
                      <el-select v-model="scope.row._batchStrategy" size="mini" style="flex:1">
                        <el-option label="每文件更新(PER_FILE)" value="PER_FILE" />
                      </el-select>
                    </div>
                  </div>
                  <el-button slot="reference" size="mini" plain>
                    前缀:{{ scope.row._batchPrefix || '-' }} 起始:{{ scope.row._batchStart }}
                  </el-button>
                </el-popover>
              </template>
              <!-- AMOUNT -->
              <template v-else-if="scope.row.ruleType === 'AMOUNT'">
                <el-popover placement="bottom" width="300" trigger="click">
                  <div style="display:flex;flex-direction:column;gap:8px">
                    <div style="display:flex;align-items:center;gap:8px">
                      <span style="width:60px;font-size:12px">精度:</span>
                      <el-input-number v-model="scope.row._amtPrecision" :min="0" :max="10" size="mini" style="flex:1"></el-input-number>
                    </div>
                    <div style="display:flex;align-items:center;gap:8px">
                      <span style="width:60px;font-size:12px">整数位:</span>
                      <el-input-number v-model="scope.row._amtScale" :min="1" :max="30" size="mini" style="flex:1"></el-input-number>
                    </div>
                    <div style="display:flex;align-items:center;gap:8px">
                      <span style="width:60px;font-size:12px">模式:</span>
                      <el-radio-group v-model="scope.row._amtDecimalMode" size="mini">
                        <el-radio label="IMPLICIT">隐式</el-radio>
                        <el-radio label="EXPLICIT">显式</el-radio>
                      </el-radio-group>
                    </div>
                    <div style="display:flex;align-items:center;gap:8px">
                      <span style="width:60px;font-size:12px">符号:</span>
                      <el-switch v-model="scope.row._amtSigned" size="mini"></el-switch>
                    </div>
                  </div>
                  <el-button slot="reference" size="mini" plain>
                    {{ scope.row._amtScale }}位/{{ scope.row._amtPrecision }}位小数 ({{ scope.row._amtDecimalMode }})
                  </el-button>
                </el-popover>
              </template>
              <!-- EXPR -->
              <el-input v-else-if="scope.row.ruleType === 'EXPR'" v-model="scope.row._exprParams" size="mini" placeholder='拼接字段如 ${dateField}-${batchNo}' />
              <span v-else style="color:#c0c4cc;font-size:12px">--</span>
            </template>
          </el-table-column>

          <!-- 操作 -->
          <el-table-column label="操作" width="100" fixed="right" align="center">
            <template slot-scope="scope">
              <el-button type="text" size="mini" icon="el-icon-top" :disabled="scope.$index === 0"
                @click="moveUp(scope.$index)"
                title="上移"></el-button>
              <el-button type="text" size="mini" icon="el-icon-bottom" :disabled="scope.$index === list.length - 1"
                @click="moveDown(scope.$index)"
                title="下移"></el-button>
              <el-button type="text" size="mini" icon="el-icon-delete" style="color:#F56C6C"
                @click="deleteRow(scope.$index)"
                title="删除"></el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="form-footer">
          <el-button type="primary" @click="handleSave" :loading="loading">保存字段定义</el-button>
        </div>
      </div>

      <div class="guide-area">
        <div class="guide-content">
          <h3>字段规则说明</h3>
          <div class="guide-item">
            <div class="guide-title">FIXED - 固定值</div>
            <div class="guide-desc">生成恒定不变的内容，在"引用/配置"列填写固定文本</div>
          </div>
          <div class="guide-item">
            <div class="guide-title">DATE - 日期时间</div>
            <div class="guide-desc">按指定格式生成当前日期时间，如 YYYYMMDD、yyyyMMddHHmmss</div>
          </div>
          <div class="guide-item">
            <div class="guide-title">ENUM - 枚举</div>
            <div class="guide-desc">从枚举库中随机选取一个值，需先在枚举库中定义 Key</div>
          </div>
          <div class="guide-item">
            <div class="guide-title">REF_FILE - 引用文件</div>
            <div class="guide-desc">从上传的素材文件中按行引用列数据</div>
          </div>
          <div class="guide-item">
            <div class="guide-title">REF_FIELD - 引用字段</div>
            <div class="guide-desc">引用当前报文中已定义的其它字段值（只能引用当前字段之前定义的字段，实现单向依赖）</div>
          </div>
          <div class="guide-item">
            <div class="guide-title">SEQUENCE - 序列号</div>
            <div class="guide-desc">带前缀的自动递增序列号；支持前缀、起始值、步长、数字位数、循环；可选每行(PER_LINE)或每文件(PER_FILE)更新策略</div>
          </div>
          <div class="guide-item">
            <div class="guide-title">RANDOM_CN - 随机汉字</div>
            <div class="guide-desc">随机生成指定个数的汉字，count参数控制汉字数量</div>
          </div>
          <div class="guide-item">
            <div class="guide-title">RANDOM_NUM - 随机数字</div>
            <div class="guide-desc">随机生成指定位数的数字，count参数控制数字位数</div>
          </div>
          <div class="guide-item">
            <div class="guide-title">RANDOM_UUID - 随机UUID</div>
            <div class="guide-desc">生成全局唯一标识符UUID，支持控制是否大写</div>
          </div>
          <div class="guide-item">
            <div class="guide-title">SUM - 汇总金额</div>
            <div class="guide-desc">引擎先生成Body并累加金额，最后回写Header/Footer缓冲区；需指定targetFieldKey和format如9(14)V99</div>
          </div>
          <div class="guide-item">
            <div class="guide-title">COUNT - 统计行数</div>
            <div class="guide-desc">自动统计Body中数据总行数，仅在文件头/文件尾中使用</div>
          </div>
          <div class="guide-item">
            <div class="guide-title">BATCH_NO - 批次号</div>
            <div class="guide-desc">全局批次号，每个文件只递增一次(PER_FILE)；与序列号(每行)不同，用于文件级标识</div>
          </div>
          <div class="guide-item">
            <div class="guide-title">AMOUNT - 金额格式</div>
            <div class="guide-desc">按金融金额格式生成，支持隐式/显式小数点、精度、整数位、符号位；format如9(14)V99</div>
          </div>
          <div class="guide-item">
            <div class="guide-title">EXPR - 复合表达式</div>
            <div class="guide-desc">使用${变量名}语法拼接已有字段生成复合值；常用于动态文件名、报文ID等场景</div>
          </div>
          <div class="guide-item" style="color:#F56C6C;font-weight:bold;margin-top:20px">
            ☆ 带*号为必填项。子字段(level 2)需先在其上方定义一个一级字段(level 1)。
            <br/>定长模式下，子字段长度之和须等于父字段长度。
            <br/>引用字段只能引用当前字段之前的字段。
          </div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script>
import { saveFields, getGeneralTemplateDownloadUrl, parseFieldExcel, exportFieldExcel } from '@/api/meta-gen'

export default {
  name: 'FieldEditDialog',
  props: {
    enumKeys: { type: Array, default: () => [] },
    refFiles: { type: Array, default: () => [] }
  },
  data() {
    return {
      visible: false,
      loading: false,
      modelId: null,
      modelName: '',
      section: 'BODY',
      list: []
    }
  },
  computed: {
    sectionLabel() {
      const map = { FILENAME: '文件名', HEADER: '文件头', BODY: '文件体', FOOTER: '文件尾' }
      return map[this.section] || this.section
    }
  },
  methods: {
    init(modelId, section, fields, modelName) {
      this.modelId = modelId
      this.section = section
      this.modelName = modelName || ''
      // 深拷贝并初始化临时编辑字段
      this.list = (fields || []).map(f => this.enrichField({ ...f }))
      this.visible = true
    },
    enrichField(f) {
      // 初始化临时编辑辅助字段
      if (!f._fixedVal && f.ruleType === 'FIXED' && f.ruleConfigJson) {
        try { f._fixedVal = JSON.parse(f.ruleConfigJson).value || '' } catch (e) { f._fixedVal = '' }
      }
      if (!f._dateFmt && f.ruleType === 'DATE' && f.ruleConfigJson) {
        try { f._dateFmt = JSON.parse(f.ruleConfigJson).format || 'yyyyMMdd' } catch (e) { f._dateFmt = 'yyyyMMdd' }
      }
      // SEQUENCE
      if (f.ruleType === 'SEQUENCE' && f.ruleConfigJson) {
        try {
          const cfg = JSON.parse(f.ruleConfigJson)
          f._seqPrefix = cfg.prefix || ''
          f._seqStart = cfg.start || 1
          f._seqStep = cfg.step || 1
          f._seqDigitLen = cfg.digitLength || 6
          f._seqStrategy = cfg.updateStrategy || 'PER_LINE'
        } catch (e) {
          f._seqPrefix = ''
          f._seqStart = 1
          f._seqStep = 1
          f._seqDigitLen = 6
          f._seqStrategy = 'PER_LINE'
        }
      }
      // RANDOM_CN / RANDOM_NUM
      if ((f.ruleType === 'RANDOM_CN' || f.ruleType === 'RANDOM_NUM') && f.ruleConfigJson) {
        try {
          const cfg = JSON.parse(f.ruleConfigJson)
          f._randomCount = cfg.count || 3
        } catch (e) { f._randomCount = 3 }
      }
      // RANDOM_UUID
      if (f.ruleType === 'RANDOM_UUID' && f.ruleConfigJson) {
        try {
          const cfg = JSON.parse(f.ruleConfigJson)
          f._uuidUpperCase = cfg.upperCase || false
        } catch (e) { f._uuidUpperCase = false }
      }
      // BATCH_NO
      if (f.ruleType === 'BATCH_NO' && f.ruleConfigJson) {
        try {
          const cfg = JSON.parse(f.ruleConfigJson)
          f._batchPrefix = cfg.prefix || ''
          f._batchStart = cfg.start || 1
          f._batchStrategy = cfg.updateStrategy || 'PER_FILE'
        } catch (e) {
          f._batchPrefix = ''
          f._batchStart = 1
          f._batchStrategy = 'PER_FILE'
        }
      }
      if (f.ruleType === 'AMOUNT' && f.ruleConfigJson) {
        try {
          const cfg = JSON.parse(f.ruleConfigJson)
          const config = cfg.config || cfg
          f._amtPrecision = config.precision || 2
          f._amtScale = config.scale || 14
          f._amtDecimalMode = config.decimalMode || 'IMPLICIT'
          f._amtSigned = config.signed || false
        } catch (e) {
          f._amtPrecision = 2
          f._amtScale = 14
          f._amtDecimalMode = 'IMPLICIT'
          f._amtSigned = false
        }
      }
      if (f.ruleType === 'EXPR' && f.ruleConfigJson) {
        try {
          const cfg = JSON.parse(f.ruleConfigJson)
          f._exprParams = cfg.pattern || (cfg.params || []).join(', ')
        } catch (e) { f._exprParams = '' }
      }
      // 兼容旧版：RANDOM → RANDOM_NUM，EXPRESSION → EXPR，SEQ → SEQUENCE
      if (f.ruleType === 'RANDOM') f.ruleType = 'RANDOM_NUM'
      if (f.ruleType === 'EXPRESSION') f.ruleType = 'EXPR'
      if (f.ruleType === 'SEQ') f.ruleType = 'SEQUENCE'
      // 初始化默认值
      if (!f.paddingDirection) f.paddingDirection = 'NONE'
      if (!f.paddingChar) f.paddingChar = ' '
      if (!f.level) f.level = 1
      if (!f.ruleType) f.ruleType = 'FIXED'
      // 初始化新字段默认值
      if (f._seqPrefix === undefined) f._seqPrefix = ''
      if (f._seqStart === undefined) f._seqStart = 1
      if (f._seqStep === undefined) f._seqStep = 1
      if (f._seqDigitLen === undefined) f._seqDigitLen = 6
      if (f._seqStrategy === undefined) f._seqStrategy = 'PER_LINE'
      if (f._randomCount === undefined) f._randomCount = 3
      if (f._uuidUpperCase === undefined) f._uuidUpperCase = false
      if (f._batchPrefix === undefined) f._batchPrefix = ''
      if (f._batchStart === undefined) f._batchStart = 1
      if (f._batchStrategy === undefined) f._batchStrategy = 'PER_FILE'
      // 初始化 isRequired 辅助字段
      if (f.isRequired === undefined || f.isRequired === null) f.isRequired = 0
      f._isRequired = f.isRequired === 1
      return f
    },
    computeOrder(idx) {
      const item = this.list[idx]
      if (!item) return ''
      if (item.level === 1) {
        let count = 0
        for (let i = 0; i <= idx; i++) {
          if (this.list[i].level === 1) count++
        }
        return count + '.0'
      }
      // level 2
      for (let i = idx - 1; i >= 0; i--) {
        if (this.list[i].level === 1) {
          let sub = 0
          for (let j = i + 1; j <= idx; j++) {
            if (this.list[j].level === 2) sub++
          }
          const parentIdx = i
          let parentCount = 0
          for (let k = 0; k <= parentIdx; k++) {
            if (this.list[k].level === 1) parentCount++
          }
          return parentCount + '.' + sub
        }
      }
      return '?.?'
    },
    getAvailableKeys(idx) {
      const keys = []
      for (let i = 0; i < idx; i++) {
        const f = this.list[i]
        if (f.fieldKey) keys.push(f.fieldKey)
      }
      return keys
    },
    addRow() {
      this.list.push(this.enrichField({
        section: this.section,
        level: 1,
        fieldKey: '',
        fieldName: '',
        length: null,
        isRequired: 0,
        paddingDirection: 'NONE',
        paddingChar: ' ',
        ruleType: 'FIXED',
        sortIndex: (this.list.length + 1) * 10
      }))
    },
    deleteRow(idx) {
      const item = this.list[idx]
      if (item.level === 1 && item.id) {
        // 检查子字段
        for (let j = this.list.length - 1; j >= 0; j--) {
          if (this.list[j].parentId === item.id) {
            this.list.splice(j, 1)
            if (j < idx) idx--
          }
        }
      }
      this.list.splice(idx, 1)
    },
    moveUp(idx) {
      if (idx <= 0) return
      const temp = this.list[idx]
      this.list.splice(idx, 1)
      this.list.splice(idx - 1, 0, temp)
    },
    moveDown(idx) {
      if (idx >= this.list.length - 1) return
      const temp = this.list[idx]
      this.list.splice(idx, 1)
      this.list.splice(idx + 1, 0, temp)
    },
    indentRow(idx) {
      const item = this.list[idx]
      if (item.level !== 1) return
      // 找上方最近的 level 1 作为父
      let parentId = null
      for (let i = idx - 1; i >= 0; i--) {
        if (this.list[i].level === 1) {
          parentId = this.list[i].id || ('_tmp_' + i)
          break
        }
      }
      item.level = 2
      item.parentId = parentId
      this.$set(this.list, idx, { ...item })
    },
    outdentRow(idx) {
      const item = this.list[idx]
      if (item.level !== 2) return
      item.level = 1
      item.parentId = null
      this.$set(this.list, idx, { ...item })
    },
    onRuleTypeChange(row) {
      // 清空之前的引用和配置
      row.refFieldKey = null
      row.refEnumKey = null
      row.refSequenceKey = null
      row.refFileId = null
      row.ruleConfigJson = null
    },
    buildRuleConfigJson(row) {
      if (row.ruleType === 'FIXED') {
        return JSON.stringify({ value: row._fixedVal || '' })
      } else if (row.ruleType === 'DATE') {
        return JSON.stringify({ format: row._dateFmt || 'yyyyMMdd', offset: 0 })
      } else if (row.ruleType === 'SEQUENCE') {
        return JSON.stringify({
          prefix: row._seqPrefix || '',
          start: row._seqStart || 1,
          step: row._seqStep || 1,
          max: 99999999999,
          cycle: true,
          digitLength: row._seqDigitLen || 6,
          updateStrategy: row._seqStrategy || 'PER_LINE'
        })
      } else if (row.ruleType === 'RANDOM_CN') {
        return JSON.stringify({ count: row._randomCount || 3 })
      } else if (row.ruleType === 'RANDOM_NUM') {
        return JSON.stringify({ count: row._randomCount || 5 })
      } else if (row.ruleType === 'RANDOM_UUID') {
        return JSON.stringify({ upperCase: row._uuidUpperCase || false })
      } else if (row.ruleType === 'BATCH_NO') {
        return JSON.stringify({
          prefix: row._batchPrefix || '',
          start: row._batchStart || 1,
          updateStrategy: row._batchStrategy || 'PER_FILE'
        })
      } else if (row.ruleType === 'AMOUNT') {
        return JSON.stringify({
          value: 0,
          format: '9(' + (row._amtScale || 14) + ')V' + (row._amtPrecision || 2),
          config: {
            precision: row._amtPrecision || 2,
            scale: row._amtScale || 14,
            decimalMode: row._amtDecimalMode || 'IMPLICIT',
            signed: row._amtSigned || false
          }
        })
      } else if (row.ruleType === 'EXPR') {
        return JSON.stringify({ pattern: row._exprParams || '' })
      }
      return row.ruleConfigJson || null
    },
    async handleSave() {
      // 重组 sortIndex
      this.list.forEach((f, i) => {
        f.section = this.section
        f.sortIndex = (i + 1) * 10
        f.ruleConfigJson = this.buildRuleConfigJson(f)
      })
      this.loading = true
      try {
        await saveFields(this.modelId, this.list)
        this.$message.success('字段保存成功')
        this.visible = false
        this.$emit('refresh')
      } finally {
        this.loading = false
      }
    },
    handleClose() {
      this.visible = false
    },
    onIsRequiredChange(row) {
      row.isRequired = row._isRequired ? 1 : 0
    },
    handleDownloadCurrentFields() {
      if (!this.list || this.list.length === 0) {
        this.$message.warning('当前没有字段数据可导出')
        return
      }
      // 构建导出数据，先将当前编辑态配置序列化为 ruleConfigJson
      const fieldsForExport = this.list.map(f => {
        const item = { ...f }
        item.ruleConfigJson = this.buildRuleConfigJson(item)
        return item
      })
      const payload = {
        modelName: this.modelName || '未命名模型',
        section: this.section,
        fields: fieldsForExport
      }
      exportFieldExcel(payload).then(res => {
        // 使用 blob 下载
        const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        // 从响应头提取文件名，如果不可用则使用默认名
        const disposition = res.headers && res.headers['content-disposition']
        let fileName = (this.modelName || '未命名模型') + '_字段定义.xlsx'
        if (disposition) {
          const match = disposition.match(/filename\*=UTF-8''(.+)/) || disposition.match(/filename="?(.+?)"?$/)
          if (match) fileName = decodeURIComponent(match[1])
        }
        link.download = fileName
        link.click()
        window.URL.revokeObjectURL(url)
        this.$message.success('字段定义导出成功')
      }).catch(e => {
        console.error('导出字段Excel失败', e)
        this.$message.error('导出失败: ' + (e.message || '未知错误'))
      })
    },
    handleDownloadGeneralTemplate() {
      window.open(getGeneralTemplateDownloadUrl(), '_blank')
    },
    handleUploadExcel(fileInfo) {
      const rawFile = fileInfo.raw
      if (!rawFile) {
        this.$message.warning('未获取到文件')
        return
      }
      const formData = new FormData()
      formData.append('file', rawFile)
      parseFieldExcel(formData).then(res => {
        const parsedFields = res.data || []
        if (parsedFields.length === 0) {
          this.$message.warning('Excel 中未解析到有效字段数据')
          return
        }
        const newFields = parsedFields.map(item => {
          const mappedRuleType = this.mapRuleType(item.ruleType)
          const f = {
            section: this.section,
            level: item.level || 1,
            fieldKey: item.fieldKey || '',
            fieldName: item.fieldName || '',
            length: item.length || null,
            isRequired: item.isRequired || 0,
            paddingDirection: 'NONE',
            paddingChar: ' ',
            ruleType: mappedRuleType,
            sortIndex: (this.list.length + 1) * 10
          }
          if (mappedRuleType === 'FIXED' && item.configValue) {
            f._fixedVal = item.configValue
          } else if (mappedRuleType === 'DATE' && item.configValue) {
            f._dateFmt = item.configValue
          } else if (mappedRuleType === 'ENUM' && item.configValue) {
            f.refEnumKey = item.configValue
          } else if (mappedRuleType === 'SEQUENCE' && item.configValue) {
            // 配置内容可能是简单Key或逗号分隔的参数: 前缀=S,位数=11,起始=1,步长=1,策略=PER_LINE
            const parts = item.configValue.split(',')
            parts.forEach(p => {
              const kv = p.split('=')
              if (kv.length === 2) {
                const k = kv[0].trim()
                const v = kv[1].trim()
                if (k === '前缀') f._seqPrefix = v
                else if (k === '起始') f._seqStart = parseInt(v) || 1
                else if (k === '步长') f._seqStep = parseInt(v) || 1
                else if (k === '位数') f._seqDigitLen = parseInt(v) || 6
                else if (k === '策略') f._seqStrategy = v
              }
            })
          } else if ((mappedRuleType === 'RANDOM_CN' || mappedRuleType === 'RANDOM_NUM') && item.configValue) {
            const match = item.configValue.match(/count=(\d+)/)
            if (match) f._randomCount = parseInt(match[1]) || 3
            else f._randomCount = parseInt(item.configValue) || 3
          } else if (mappedRuleType === 'RANDOM_UUID' && item.configValue) {
            f._uuidUpperCase = item.configValue.toLowerCase().includes('true')
          } else if (mappedRuleType === 'BATCH_NO' && item.configValue) {
            const parts = item.configValue.split(',')
            parts.forEach(p => {
              const kv = p.split('=')
              if (kv.length === 2) {
                const k = kv[0].trim()
                const v = kv[1].trim()
                if (k === '前缀') f._batchPrefix = v
                else if (k === '起始') f._batchStart = parseInt(v) || 1
                else if (k === '策略') f._batchStrategy = v
              }
            })
          } else if (mappedRuleType === 'EXPR' && item.configValue) {
            f._exprParams = item.configValue
          } else if (mappedRuleType === 'AMOUNT' && item.configValue) {
            const amtVal = parseFloat(item.configValue)
            if (!isNaN(amtVal)) {
              f._amtScale = String(Math.floor(amtVal)).length
              const parts = item.configValue.split('.')
              f._amtPrecision = parts.length > 1 ? parts[1].length : 2
            }
          }
          return this.enrichField(f)
        })
        this.list.push(...newFields)
        this.$message.success(`成功解析 ${parsedFields.length} 个字段定义`)
      }).catch(e => {
        console.error('Excel 解析失败', e)
        this.$message.error('Excel 解析失败: ' + (e.message || '未知错误'))
      })
    },
    mapRuleType(chineseType) {
      if (!chineseType) return 'FIXED'
      const t = chineseType.trim()
      const map = {
        '固定值': 'FIXED',
        '日期': 'DATE',
        '枚举': 'ENUM',
        '序列号': 'SEQUENCE',
        '随机汉字': 'RANDOM_CN',
        '随机数字': 'RANDOM_NUM',
        '随机UUID': 'RANDOM_UUID',
        '引用文件': 'REF_FILE',
        '引用字段': 'REF_FIELD',
        '汇总金额': 'SUM',
        '统计行数': 'COUNT',
        '批次号': 'BATCH_NO',
        '金额': 'AMOUNT',
        '表达式': 'EXPR',
        // 兼容旧版
        '随机值': 'RANDOM_NUM'
      }
      return map[t] || t.toUpperCase()
    }
  }
}
</script>

<style scoped>
.dialog-layout { display: flex; height: 75vh; }
.input-area { flex: 3.5; padding-right: 20px; border-right: 1px solid #ebeef5; overflow-y: auto; }
.guide-area { flex: 1; padding-left: 20px; overflow-y: auto; background-color: #fafafa; }
.form-footer { margin-top: 16px; text-align: right; }
.table-tip { font-size: 12px; color: #F56C6C; }
.guide-item { margin-bottom: 12px; line-height: 1.6; }
.guide-title { font-weight: bold; color: #303133; font-size: 13px; margin-bottom: 2px; }
.guide-desc { color: #606266; font-size: 12px; padding-left: 8px; }
</style>
