<template>
  <el-dialog :title="'编辑 ' + sectionLabel + ' 字段定义'" :visible.sync="visible" width="98%" top="2vh" append-to-body @close="handleClose">
    <div class="dialog-layout">
      <!-- 左侧：编辑区 -->
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
          <div class="table-tip">* 变量名和规则类型为必填项 &nbsp;|&nbsp; 选择规则类型后自动填充模板JSON，可直接修改或点击"可视编辑"</div>
        </div>

        <el-table :data="list" border size="mini" height="58vh">
          <!-- 序号 -->
          <el-table-column label="序号" width="55" align="center">
            <template slot-scope="scope">
              <span style="font-family:monospace;color:#909399;font-size:11px">{{ computeOrder(scope.$index) }}</span>
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
          <el-table-column label="变量名*" width="120">
            <template slot-scope="scope">
              <el-input v-model="scope.row.fieldKey" size="mini" placeholder="如 userName"></el-input>
            </template>
          </el-table-column>

          <!-- 字段描述 -->
          <el-table-column label="字段描述" width="100">
            <template slot-scope="scope">
              <el-input v-model="scope.row.fieldName" size="mini" placeholder="业务描述"></el-input>
            </template>
          </el-table-column>

          <!-- 长度(字节) -->
          <el-table-column label="长度(B)" width="76">
            <template slot-scope="scope">
              <el-input-number v-model="scope.row.length" :controls="false" size="mini" :min="1" :max="9999" style="width:100%"></el-input-number>
            </template>
          </el-table-column>

          <!-- 必填 -->
          <el-table-column label="必填" width="50" align="center">
            <template slot-scope="scope">
              <el-checkbox v-model="scope.row._isRequired" @change="onIsRequiredChange(scope.row)"></el-checkbox>
            </template>
          </el-table-column>

          <!-- 补齐方向 -->
          <el-table-column label="补齐" width="85">
            <template slot-scope="scope">
              <el-select v-model="scope.row.paddingDirection" size="mini" style="width:100%">
                <el-option label="不补" value="NONE" />
                <el-option label="左补" value="LEFT" />
                <el-option label="右补" value="RIGHT" />
              </el-select>
            </template>
          </el-table-column>

          <!-- 补齐字符 -->
          <el-table-column label="补齐字符" width="75">
            <template slot-scope="scope">
              <el-select v-model="scope.row.paddingChar" size="mini" style="width:100%" :disabled="scope.row.paddingDirection === 'NONE'">
                <el-option label="空格" value=" " />
                <el-option label="0" value="0" />
              </el-select>
            </template>
          </el-table-column>

          <!-- 规则类型 -->
          <el-table-column label="规则类型*" width="115">
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

          <!-- 规则配置JSON -->
          <el-table-column label="规则配置JSON" min-width="260">
            <template slot-scope="scope">
              <div style="display:flex;align-items:flex-start;gap:4px">
                <el-input
                  v-model="scope.row.ruleConfigJson"
                  type="textarea"
                  :rows="2"
                  size="mini"
                  placeholder="选择规则类型后自动填充，可直接修改此JSON串"
                  style="font-family:monospace;font-size:11px;flex:1"
                ></el-input>
                <el-button
                  type="primary"
                  size="mini"
                  plain
                  title="可视化编辑JSON字段"
                  style="padding:3px 6px;flex-shrink:0;margin-top:1px"
                  @click="openVisualEdit(scope.row, scope.$index)"
                ><i class="el-icon-setting"></i></el-button>
              </div>
            </template>
          </el-table-column>

          <!-- 操作 -->
          <el-table-column label="操作" width="90" fixed="right" align="center">
            <template slot-scope="scope">
              <el-button type="text" size="mini" icon="el-icon-top" :disabled="scope.$index === 0"
                @click="moveUp(scope.$index)" title="上移"></el-button>
              <el-button type="text" size="mini" icon="el-icon-bottom" :disabled="scope.$index === list.length - 1"
                @click="moveDown(scope.$index)" title="下移"></el-button>
              <el-button type="text" size="mini" icon="el-icon-delete" style="color:#F56C6C"
                @click="deleteRow(scope.$index)" title="删除"></el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="form-footer">
          <el-button type="primary" @click="handleSave" :loading="loading">保存字段定义</el-button>
        </div>
      </div>

      <!-- 右侧：说明区 -->
      <div class="guide-area">
        <div class="guide-content">
          <h3 style="color:#303133;margin:0 0 12px;font-size:14px;border-bottom:2px solid #409EFF;padding-bottom:6px">📖 字段规则说明手册</h3>

          <div class="guide-section">
            <div class="guide-section-title">🔧 基础填写说明</div>
            <ul class="guide-ul">
              <li><b>变量名*</b>：英文标识符，如 <code>tran_amt</code>，在EXPR引用时用 <code>${tran_amt}</code></li>
              <li><b>长度(B)</b>：字节长度，定长模式下子字段长度之和须等于父字段长度</li>
              <li><b>补齐</b>：长度不足时向左/右填充字符（空格或0），如金额左补零</li>
              <li><b>规则类型*</b>：决定该字段值的生成规则，选择后自动填入模板JSON</li>
              <li><b>规则配置JSON</b>：存储规则参数，可直接编辑JSON串，也可点击 ⚙ 按钮可视化编辑后写回</li>
            </ul>
          </div>

          <div class="guide-section">
            <div class="guide-section-title">📋 各规则类型详解 & JSON样例</div>

            <div class="guide-item">
              <div class="guide-title">FIXED — 固定值</div>
              <div class="guide-desc">生成恒定不变的字符串，适用于报文头中的固定标识、分隔符、版本号等。</div>
              <div class="guide-json">{"value": "中划线"}</div>
              <div class="guide-note">▸ <code>value</code>：固定输出的字符串，长度不得超过字段定义的 length</div>
            </div>

            <div class="guide-item">
              <div class="guide-title">DATE — 当前日期/时间</div>
              <div class="guide-desc">按指定格式输出当前系统日期或时间，常用于文件头日期字段。</div>
              <div class="guide-json">{"format": "yyyyMMdd", "offset": 0}</div>
              <div class="guide-note">▸ <code>format</code>：Java日期格式，如 <code>yyyyMMdd</code> / <code>yyyyMMddHHmmss</code> / <code>HHmmss</code><br/>▸ <code>offset</code>：天数偏移，0=今天，-1=昨天，1=明天</div>
            </div>

            <div class="guide-item">
              <div class="guide-title">ENUM — 枚举随机值</div>
              <div class="guide-desc">从枚举库中随机抽取一个枚举值，需先在枚举库管理中定义枚举Key。</div>
              <div class="guide-json">{"enumKey": "sex", "useDefault": "0"}</div>
              <div class="guide-note">▸ <code>enumKey</code>：枚举库中已定义的Key，如 sex / tranType<br/>▸ <code>useDefault</code>：当枚举库为空时的默认值</div>
            </div>

            <div class="guide-item">
              <div class="guide-title">SEQUENCE — 序列号/计数器</div>
              <div class="guide-desc">带前缀的自动递增序列号，支持步长、循环、定长数字位数和更新策略。</div>
              <div class="guide-json">{"prefix": "S", "start": 1, "max": 99999999999, "cycle": true, "step": 1, "updateStrategy": "PER_LINE", "digitLength": 11}</div>
              <div class="guide-note">▸ <code>prefix</code>：字符串前缀，如 "S"，可为空<br/>▸ <code>start</code>：起始计数值<br/>▸ <code>step</code>：每次递增步长，默认1<br/>▸ <code>max</code>：最大值，超过后根据 cycle 决定是否重置<br/>▸ <code>cycle</code>：true=达到max后循环重置为start，false=停止<br/>▸ <code>digitLength</code>：数字部分位数（左补零），最终长度 = 前缀长度 + digitLength<br/>▸ <code>updateStrategy</code>：PER_LINE=每行递增，PER_FILE=每个文件才递增一次</div>
            </div>

            <div class="guide-item">
              <div class="guide-title">RANDOM_CN — 随机汉字</div>
              <div class="guide-desc">随机生成指定个数的汉字，适用于姓名、备注等测试数据字段。</div>
              <div class="guide-json">{"count": 3}</div>
              <div class="guide-note">▸ <code>count</code>：生成汉字个数</div>
            </div>

            <div class="guide-item">
              <div class="guide-title">RANDOM_NUM — 随机数字</div>
              <div class="guide-desc">随机生成指定位数的数字字符串，适用于卡号后缀、随机码等。</div>
              <div class="guide-json">{"count": 5}</div>
              <div class="guide-note">▸ <code>count</code>：生成数字位数</div>
            </div>

            <div class="guide-item">
              <div class="guide-title">RANDOM_UUID — 随机UUID</div>
              <div class="guide-desc">生成全局唯一UUID（不含横杠的32位字符串），可控制大小写。</div>
              <div class="guide-json">{"upperCase": true}</div>
              <div class="guide-note">▸ <code>upperCase</code>：true=大写，false=小写</div>
            </div>

            <div class="guide-item">
              <div class="guide-title">REF_FILE — 引用素材文件</div>
              <div class="guide-desc">从预先上传的素材文件中按行读取原始数据，如真实姓名库、账号库等。</div>
              <div class="guide-json">{"refFileId": 10, "columnKey": "userName", "updateStrategy": "PER_LINE", "atEnd": "LOOP"}</div>
              <div class="guide-note">▸ <code>refFileId</code>：素材文件的ID（在素材管理中查看）<br/>▸ <code>columnKey</code>：读取素材文件的哪一列（表头名称）<br/>▸ <code>updateStrategy</code>：PER_LINE=每行取下一条，PER_FILE=整文件用同一条<br/>▸ <code>atEnd</code>：LOOP=循环复用，STOP=耗尽后中止任务</div>
            </div>

            <div class="guide-item">
              <div class="guide-title">REF_FIELD — 引用同报文字段</div>
              <div class="guide-desc">引用当前报文中已定义的其他字段的值，实现字段间数据联动（单向依赖，只能引用前面的字段）。</div>
              <div class="guide-json">{"targetFieldKey": "fixField"}</div>
              <div class="guide-note">▸ <code>targetFieldKey</code>：被引用字段的变量名（必须在当前字段的前面定义）</div>
            </div>

            <div class="guide-item">
              <div class="guide-title">SUM — 汇总金额</div>
              <div class="guide-desc">自动累加Body段中指定字段的所有金额值，回写到Header/Footer缓冲区。引擎先流式处理Body再填写此字段。</div>
              <div class="guide-json">{"targetFieldKey": "tran_amt", "format": "9(14)V99"}</div>
              <div class="guide-note">▸ <code>targetFieldKey</code>：Body中金额字段的变量名<br/>▸ <code>format</code>：输出格式，9(14)V99 表示14位整数+2位隐式小数（V表示隐含小数点）</div>
            </div>

            <div class="guide-item">
              <div class="guide-title">COUNT — 统计行数</div>
              <div class="guide-desc">自动统计Body段的总数据行数，仅在文件头/文件尾中使用。</div>
              <div class="guide-json">{"targetFieldKey": "body_row"}</div>
              <div class="guide-note">▸ <code>targetFieldKey</code>：Body中任意字段变量名（用于定位Body段）</div>
            </div>

            <div class="guide-item">
              <div class="guide-title">BATCH_NO — 批次号（全局）</div>
              <div class="guide-desc">全局批次号，每生成一个文件才递增一次（PER_FILE策略），用于文件级唯一标识，与序列号（每行递增）不同。</div>
              <div class="guide-json">{"prefix": "B", "start": 1, "updateStrategy": "PER_FILE"}</div>
              <div class="guide-note">▸ <code>prefix</code>：批次号前缀，如 "B"<br/>▸ <code>start</code>：批次号起始值<br/>▸ <code>updateStrategy</code>：固定为 PER_FILE（每文件递增一次）</div>
            </div>

            <div class="guide-item">
              <div class="guide-title">AMOUNT — 金额格式化</div>
              <div class="guide-desc">按金融报文标准格式生成随机金额，支持隐式/显式小数点（COBOL写法）。</div>
              <div class="guide-json">{"value": 123.45, "format": "9(14)V99"}</div>
              <div class="guide-note">▸ <code>value</code>：金额示例值（生成时使用随机值，此为模板参考）<br/>▸ <code>format</code>：9(N)V99 = N位整数+隐式2位小数；9(N).99 = 显式小数点</div>
            </div>

            <div class="guide-item">
              <div class="guide-title">EXPR — 复合表达式</div>
              <div class="guide-desc">使用 ${变量名} 语法将多个已有字段拼接生成复合值，常用于动态文件名、报文流水号等场景。</div>
              <div class="guide-json">{"pattern": "PROJ_${currentDate}_${batchNo}", "desc": "动态文件名"}</div>
              <div class="guide-note">▸ <code>pattern</code>：拼接模板，${xxx} 中的 xxx 对应其他字段的变量名<br/>▸ <code>desc</code>：可选描述说明（不影响生成）</div>
            </div>
          </div>

          <div class="guide-section" style="margin-top:16px">
            <div class="guide-section-title">⚠️ 校验规则（保存时强制检查）</div>
            <ul class="guide-ul" style="color:#E6A23C">
              <li>FIXED 值的长度不得超过字段定义的 length</li>
              <li>SEQUENCE: 前缀长度 + digitLength 须等于 length</li>
              <li>子字段 (level 2) 长度之和须等于父字段 (level 1) 的 length</li>
              <li>REF_FIELD 只能引用当前字段前面的字段</li>
              <li>HEADER/FOOTER 中的 SUM/COUNT 引用的 targetFieldKey 必须存在于 BODY 中</li>
              <li>ruleConfigJson 必须是合法的 JSON 格式</li>
            </ul>
          </div>

          <div class="guide-section" style="margin-top:12px;padding:8px;background:#fff3cd;border-radius:4px;border-left:3px solid #ffc107">
            <div style="font-size:12px;color:#856404;line-height:1.8">
              💡 <b>使用技巧：</b><br/>
              1. 选类型后先用模板JSON，再按需修改参数<br/>
              2. 点击 ⚙ 按钮可打开可视化编辑面板（自动解析当前JSON并提供表单填写，填完后点"写入JSON"）<br/>
              3. 保存时系统自动校验每行JSON格式，格式错误会列出行号提示<br/>
              4. 下载模板含完整JSON示例，上传导入会覆盖当前所有字段（会有二次确认）
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 可视化规则编辑弹窗 -->
    <el-dialog
      title="可视化规则配置编辑器"
      :visible.sync="visualEditVisible"
      width="500px"
      append-to-body
      :close-on-click-modal="false"
    >
      <div v-if="visualEditRow" class="visual-edit-body">
        <div class="ve-row" style="margin-bottom:12px;padding:6px 8px;background:#f0f9ff;border-radius:4px;">
          <span style="font-size:12px;color:#606266">规则类型：</span>
          <el-tag size="small" type="primary">{{ visualEditRow.ruleType }}</el-tag>
          <span style="font-size:12px;color:#909399;margin-left:8px">字段：{{ visualEditRow.fieldKey || '(未命名)' }}</span>
        </div>

        <!-- FIXED -->
        <template v-if="visualEditRow.ruleType === 'FIXED'">
          <div class="ve-row"><span class="ve-label">固定值：</span>
            <el-input v-model="veForm.value" size="small" placeholder="固定输出的字符串" style="flex:1"></el-input>
          </div>
        </template>

        <!-- DATE -->
        <template v-else-if="visualEditRow.ruleType === 'DATE'">
          <div class="ve-row"><span class="ve-label">日期格式：</span>
            <el-select v-model="veForm.format" size="small" allow-create filterable style="flex:1">
              <el-option label="yyyyMMdd（8位日期）" value="yyyyMMdd" />
              <el-option label="yyyyMMddHHmmss（14位日期时间）" value="yyyyMMddHHmmss" />
              <el-option label="yyMMdd（6位短日期）" value="yyMMdd" />
              <el-option label="MMdd（月日）" value="MMdd" />
              <el-option label="HHmmss（时分秒）" value="HHmmss" />
              <el-option label="yyyy-MM-dd HH:mm:ss" value="yyyy-MM-dd HH:mm:ss" />
            </el-select>
          </div>
          <div class="ve-row"><span class="ve-label">天数偏移：</span>
            <el-input-number v-model="veForm.offset" :min="-365" :max="365" size="small" style="flex:1"></el-input-number>
            <span class="ve-hint">0=今天，-1=昨天，1=明天</span>
          </div>
        </template>

        <!-- ENUM -->
        <template v-else-if="visualEditRow.ruleType === 'ENUM'">
          <div class="ve-row"><span class="ve-label">枚举Key：</span>
            <el-select v-model="veForm.enumKey" size="small" style="flex:1" allow-create filterable placeholder="选择或输入枚举Key">
              <el-option v-for="k in enumKeys" :key="k" :label="k" :value="k" />
            </el-select>
          </div>
          <div class="ve-row"><span class="ve-label">默认值：</span>
            <el-input v-model="veForm.useDefault" size="small" placeholder="枚举库为空时的默认值" style="flex:1"></el-input>
          </div>
        </template>

        <!-- SEQUENCE -->
        <template v-else-if="visualEditRow.ruleType === 'SEQUENCE'">
          <div class="ve-row"><span class="ve-label">前缀：</span>
            <el-input v-model="veForm.prefix" size="small" placeholder="如 S，可为空" style="flex:1"></el-input>
          </div>
          <div class="ve-row"><span class="ve-label">起始值：</span>
            <el-input-number v-model="veForm.start" :min="0" size="small" style="flex:1"></el-input-number>
          </div>
          <div class="ve-row"><span class="ve-label">步长：</span>
            <el-input-number v-model="veForm.step" :min="1" size="small" style="flex:1"></el-input-number>
          </div>
          <div class="ve-row"><span class="ve-label">最大值：</span>
            <el-input-number v-model="veForm.max" :min="1" size="small" style="flex:1"></el-input-number>
          </div>
          <div class="ve-row"><span class="ve-label">循环：</span>
            <el-switch v-model="veForm.cycle" size="small"></el-switch>
            <span class="ve-hint">达到最大值后是否重置为起始值</span>
          </div>
          <div class="ve-row"><span class="ve-label">数字位数：</span>
            <el-input-number v-model="veForm.digitLength" :min="1" :max="20" size="small" style="flex:1"></el-input-number>
            <span class="ve-hint">左补零，最终长度=前缀+数字位数</span>
          </div>
          <div class="ve-row"><span class="ve-label">更新策略：</span>
            <el-select v-model="veForm.updateStrategy" size="small" style="flex:1">
              <el-option label="每行递增 (PER_LINE)" value="PER_LINE" />
              <el-option label="每文件递增 (PER_FILE)" value="PER_FILE" />
            </el-select>
          </div>
        </template>

        <!-- RANDOM_CN / RANDOM_NUM -->
        <template v-else-if="visualEditRow.ruleType === 'RANDOM_CN' || visualEditRow.ruleType === 'RANDOM_NUM'">
          <div class="ve-row">
            <span class="ve-label">{{ visualEditRow.ruleType === 'RANDOM_CN' ? '汉字个数' : '数字位数' }}：</span>
            <el-input-number v-model="veForm.count" :min="1" :max="100" size="small" style="flex:1"></el-input-number>
          </div>
        </template>

        <!-- RANDOM_UUID -->
        <template v-else-if="visualEditRow.ruleType === 'RANDOM_UUID'">
          <div class="ve-row"><span class="ve-label">大写：</span>
            <el-switch v-model="veForm.upperCase" size="small"></el-switch>
            <span class="ve-hint">true=大写UUID，false=小写</span>
          </div>
        </template>

        <!-- REF_FILE -->
        <template v-else-if="visualEditRow.ruleType === 'REF_FILE'">
          <div class="ve-row"><span class="ve-label">素材文件：</span>
            <el-select v-model="veForm.refFileId" size="small" style="flex:1" placeholder="选择素材文件">
              <el-option v-for="rf in refFiles" :key="rf.id" :label="rf.refName" :value="rf.id" />
            </el-select>
          </div>
          <div class="ve-row"><span class="ve-label">列名(Key)：</span>
            <el-input v-model="veForm.columnKey" size="small" placeholder="素材文件的表头列名" style="flex:1"></el-input>
          </div>
          <div class="ve-row"><span class="ve-label">更新策略：</span>
            <el-select v-model="veForm.updateStrategy" size="small" style="flex:1">
              <el-option label="每行取下一条 (PER_LINE)" value="PER_LINE" />
              <el-option label="整文件同一条 (PER_FILE)" value="PER_FILE" />
            </el-select>
          </div>
          <div class="ve-row"><span class="ve-label">耗尽策略：</span>
            <el-select v-model="veForm.atEnd" size="small" style="flex:1">
              <el-option label="循环复用 (LOOP)" value="LOOP" />
              <el-option label="中止任务 (STOP)" value="STOP" />
            </el-select>
          </div>
        </template>

        <!-- REF_FIELD -->
        <template v-else-if="visualEditRow.ruleType === 'REF_FIELD'">
          <div class="ve-row"><span class="ve-label">引用字段：</span>
            <el-select v-model="veForm.targetFieldKey" size="small" style="flex:1" allow-create filterable placeholder="选择当前字段前面的字段">
              <el-option v-for="key in getAvailableKeys(visualEditIdx)" :key="key" :label="key" :value="key" />
            </el-select>
          </div>
        </template>

        <!-- SUM -->
        <template v-else-if="visualEditRow.ruleType === 'SUM'">
          <div class="ve-row"><span class="ve-label">目标字段：</span>
            <el-input v-model="veForm.targetFieldKey" size="small" placeholder="Body中金额字段的变量名" style="flex:1"></el-input>
          </div>
          <div class="ve-row"><span class="ve-label">输出格式：</span>
            <el-select v-model="veForm.format" size="small" style="flex:1" allow-create filterable>
              <el-option label="9(14)V99（隐式，16字节）" value="9(14)V99" />
              <el-option label="9(12)V99（隐式，14字节）" value="9(12)V99" />
              <el-option label="9(14).99（显式，17字节）" value="9(14).99" />
            </el-select>
          </div>
        </template>

        <!-- COUNT -->
        <template v-else-if="visualEditRow.ruleType === 'COUNT'">
          <div class="ve-row"><span class="ve-label">目标字段：</span>
            <el-input v-model="veForm.targetFieldKey" size="small" placeholder="Body中任意字段的变量名（用于定位）" style="flex:1"></el-input>
          </div>
        </template>

        <!-- BATCH_NO -->
        <template v-else-if="visualEditRow.ruleType === 'BATCH_NO'">
          <div class="ve-row"><span class="ve-label">前缀：</span>
            <el-input v-model="veForm.prefix" size="small" placeholder="如 B，可为空" style="flex:1"></el-input>
          </div>
          <div class="ve-row"><span class="ve-label">起始值：</span>
            <el-input-number v-model="veForm.start" :min="0" size="small" style="flex:1"></el-input-number>
          </div>
          <div class="ve-row"><span class="ve-label">更新策略：</span>
            <el-select v-model="veForm.updateStrategy" size="small" style="flex:1">
              <el-option label="每文件递增 (PER_FILE)" value="PER_FILE" />
            </el-select>
          </div>
        </template>

        <!-- AMOUNT -->
        <template v-else-if="visualEditRow.ruleType === 'AMOUNT'">
          <div class="ve-row"><span class="ve-label">示例金额：</span>
            <el-input-number v-model="veForm.value" :precision="4" size="small" style="flex:1"></el-input-number>
            <span class="ve-hint">用于生成格式预览</span>
          </div>
          <div class="ve-row"><span class="ve-label">整数位长：</span>
            <el-input-number v-model="veForm._amtScale" :min="1" :max="30" size="small" style="flex:1"></el-input-number>
          </div>
          <div class="ve-row"><span class="ve-label">小数位长：</span>
            <el-input-number v-model="veForm._amtPrecision" :min="0" :max="10" size="small" style="flex:1"></el-input-number>
          </div>
          <div class="ve-row"><span class="ve-label">小数点模式：</span>
            <el-radio-group v-model="veForm._amtDecimalMode" size="small">
              <el-radio label="IMPLICIT">隐式 (V)</el-radio>
              <el-radio label="EXPLICIT">显式 (.)</el-radio>
            </el-radio-group>
          </div>
          <div class="ve-row" style="margin-top:6px;padding:6px;background:#f5f7fa;border-radius:4px">
            <span style="font-size:12px;color:#606266">预览格式：</span>
            <code style="font-size:12px;color:#409EFF">{{ buildAmtFormat() }}</code>
          </div>
        </template>

        <!-- EXPR -->
        <template v-else-if="visualEditRow.ruleType === 'EXPR'">
          <div class="ve-row"><span class="ve-label">拼接模板：</span>
            <el-input v-model="veForm.pattern" size="small" placeholder="如 ${dateField}-${batchNo}-${seqField}" style="flex:1"></el-input>
          </div>
          <div class="ve-row"><span class="ve-label">说明描述：</span>
            <el-input v-model="veForm.desc" size="small" placeholder="可选说明（不影响生成）" style="flex:1"></el-input>
          </div>
          <div style="font-size:11px;color:#909399;margin-top:4px;padding-left:80px">
            已有变量：<el-tag v-for="key in getAvailableKeys(visualEditIdx)" :key="key" size="mini" style="margin:2px">{{ key }}</el-tag>
          </div>
        </template>

        <!-- 通用：预览当前会生成的JSON -->
        <div style="margin-top:14px;padding:8px;background:#f5f7fa;border-radius:4px">
          <div style="font-size:11px;color:#909399;margin-bottom:4px">将写入的JSON串：</div>
          <code style="font-size:11px;color:#303133;word-break:break-all;white-space:pre-wrap">{{ previewVeJson() }}</code>
        </div>
      </div>
      <div slot="footer">
        <el-button size="small" @click="visualEditVisible = false">取消</el-button>
        <el-button type="primary" size="small" @click="applyVisualEdit">写入JSON</el-button>
      </div>
    </el-dialog>
  </el-dialog>
</template>

<script>
import { saveFields, getGeneralTemplateDownloadUrl, parseFieldExcel, exportFieldExcel } from '@/api/meta-gen'

// 各规则类型对应的模板 JSON
const RULE_TEMPLATES = {
  FIXED: { value: '固定值内容' },
  DATE: { format: 'yyyyMMdd', offset: 0 },
  ENUM: { enumKey: 'sex', useDefault: '0' },
  SEQUENCE: { prefix: 'SEQ', start: 1, max: 9999999999, cycle: true, step: 1, updateStrategy: 'PER_LINE', digitLength: 9 },
  RANDOM_CN: { count: 3 },
  RANDOM_NUM: { count: 5 },
  RANDOM_UUID: { upperCase: true },
  REF_FILE: { refFileId: null, columnKey: 'userName', updateStrategy: 'PER_LINE', atEnd: 'LOOP' },
  REF_FIELD: { targetFieldKey: '' },
  SUM: { targetFieldKey: 'tran_amt', format: '9(14)V99' },
  COUNT: { targetFieldKey: 'body_row' },
  BATCH_NO: { prefix: 'B', start: 1, updateStrategy: 'PER_FILE' },
  AMOUNT: { value: 123.45, format: '9(14)V99' },
  EXPR: { pattern: '${dateField}-${batchNo}-${seqField}', desc: '复合表达式示例' }
}

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
      list: [],

      // 可视化编辑弹窗
      visualEditVisible: false,
      visualEditRow: null,
      visualEditIdx: -1,
      veForm: {}
    }
  },
  computed: {
    sectionLabel() {
      const map = { FILENAME: '文件名', HEADER: '文件头', BODY: '文件体', FOOTER: '文件尾' }
      return map[this.section] || this.section
    }
  },
  methods: {
    // ─── 初始化 ────────────────────────────────────────────
    init(modelId, section, fields, modelName) {
      this.modelId = modelId
      this.section = section
      this.modelName = modelName || ''
      this.list = (fields || []).map(f => this.normalizeField({ ...f }))
      this.visible = true
    },

    /** 规范化字段对象（不再解包到 _xxx，直接保留 ruleConfigJson） */
    normalizeField(f) {
      // 兼容旧版字段类型
      if (f.ruleType === 'RANDOM') f.ruleType = 'RANDOM_NUM'
      if (f.ruleType === 'EXPRESSION') f.ruleType = 'EXPR'
      if (f.ruleType === 'SEQ') f.ruleType = 'SEQUENCE'

      // 如果 ruleConfigJson 为空但存在旧版辅助字段，尝试迁移
      if (!f.ruleConfigJson || f.ruleConfigJson === 'null') {
        f.ruleConfigJson = this.migrateOldFields(f)
      }

      // 若 ruleConfigJson 为空，补入模板
      if (!f.ruleConfigJson && f.ruleType && RULE_TEMPLATES[f.ruleType]) {
        f.ruleConfigJson = JSON.stringify(RULE_TEMPLATES[f.ruleType])
      }

      // 兼容：refEnumKey 存在但 ruleConfigJson 中无 enumKey 时自动补上
      if (f.ruleType === 'ENUM' && f.refEnumKey) {
        try {
          const cfg = JSON.parse(f.ruleConfigJson || '{}')
          if (!cfg.enumKey) {
            cfg.enumKey = f.refEnumKey
            f.ruleConfigJson = JSON.stringify(cfg)
          }
        } catch (e) {}
      }

      // 基础默认值
      if (!f.paddingDirection) f.paddingDirection = 'NONE'
      if (!f.paddingChar) f.paddingChar = ' '
      if (!f.level) f.level = 1
      if (!f.ruleType) f.ruleType = 'FIXED'

      // isRequired 辅助
      if (f.isRequired === undefined || f.isRequired === null) f.isRequired = 0
      f._isRequired = f.isRequired === 1

      return f
    },

    /** 从旧版辅助字段迁移为 ruleConfigJson（兼容历史数据） */
    migrateOldFields(f) {
      try {
        const t = f.ruleType
        if (t === 'FIXED' && f._fixedVal !== undefined) {
          return JSON.stringify({ value: f._fixedVal || '' })
        }
        if (t === 'DATE' && f._dateFmt !== undefined) {
          return JSON.stringify({ format: f._dateFmt || 'yyyyMMdd', offset: 0 })
        }
        if (t === 'SEQUENCE' && f._seqPrefix !== undefined) {
          return JSON.stringify({
            prefix: f._seqPrefix || '', start: f._seqStart || 1, step: f._seqStep || 1,
            max: 99999999999, cycle: true, digitLength: f._seqDigitLen || 6,
            updateStrategy: f._seqStrategy || 'PER_LINE'
          })
        }
        if ((t === 'RANDOM_CN' || t === 'RANDOM_NUM') && f._randomCount !== undefined) {
          return JSON.stringify({ count: f._randomCount || 3 })
        }
        if (t === 'RANDOM_UUID' && f._uuidUpperCase !== undefined) {
          return JSON.stringify({ upperCase: f._uuidUpperCase || false })
        }
        if (t === 'BATCH_NO' && f._batchPrefix !== undefined) {
          return JSON.stringify({ prefix: f._batchPrefix || '', start: f._batchStart || 1, updateStrategy: f._batchStrategy || 'PER_FILE' })
        }
        if (t === 'AMOUNT' && f._amtScale !== undefined) {
          const sep = f._amtDecimalMode === 'EXPLICIT' ? '.' : 'V'
          return JSON.stringify({ value: 123.45, format: '9(' + (f._amtScale || 14) + ')' + sep + '9'.repeat(f._amtPrecision || 2) })
        }
        if (t === 'EXPR' && f._exprParams !== undefined) {
          return JSON.stringify({ pattern: f._exprParams || '' })
        }
        // 旧 refEnumKey / refFieldKey 迁移
        if (t === 'ENUM' && f.refEnumKey) return JSON.stringify({ enumKey: f.refEnumKey, useDefault: '0' })
        if (t === 'REF_FIELD' && f.refFieldKey) return JSON.stringify({ targetFieldKey: f.refFieldKey })
        if (t === 'REF_FILE' && f.refFileId) return JSON.stringify({ refFileId: f.refFileId, columnKey: '', updateStrategy: 'PER_LINE', atEnd: 'LOOP' })
      } catch (e) { /* ignore */ }
      return null
    },

    // ─── 选择规则类型后自动填入模板 JSON ──────────────────────
    onRuleTypeChange(row) {
      const tpl = RULE_TEMPLATES[row.ruleType]
      if (tpl) {
        row.ruleConfigJson = JSON.stringify(tpl)
      } else {
        row.ruleConfigJson = null
      }
      // 切换规则类型时清除 refEnumKey，避免残留
      row.refEnumKey = null
    },

    // ─── 可视化编辑弹窗 ───────────────────────────────────────
    openVisualEdit(row, idx) {
      this.visualEditRow = row
      this.visualEditIdx = idx
      // 解析当前 JSON 到表单
      let cfg = {}
      if (row.ruleConfigJson) {
        try { cfg = JSON.parse(row.ruleConfigJson) } catch (e) { cfg = {} }
      }
      // 合并模板默认值
      const tpl = RULE_TEMPLATES[row.ruleType] || {}
      cfg = Object.assign({}, tpl, cfg)

      // AMOUNT 特殊处理：从 format 反解析出 scale/precision/mode
      if (row.ruleType === 'AMOUNT') {
        const fmt = cfg.format || '9(14)V99'
        const mV = fmt.match(/9\((\d+)\)V(\d+)/)
        const mD = fmt.match(/9\((\d+)\)\.(\d+)/)
        if (mV) { cfg._amtScale = parseInt(mV[1]); cfg._amtPrecision = mV[2].length; cfg._amtDecimalMode = 'IMPLICIT' }
        else if (mD) { cfg._amtScale = parseInt(mD[1]); cfg._amtPrecision = mD[2].length; cfg._amtDecimalMode = 'EXPLICIT' }
        else { cfg._amtScale = 14; cfg._amtPrecision = 2; cfg._amtDecimalMode = 'IMPLICIT' }
      }

      this.veForm = cfg
      this.visualEditVisible = true
    },

    /** 根据可视化表单预览将生成的 JSON（实时） */
    previewVeJson() {
      if (!this.visualEditRow) return ''
      try {
        const obj = this.buildVeJson()
        return JSON.stringify(obj, null, 2)
      } catch (e) { return '(无效)' }
    },

    buildAmtFormat() {
      if (!this.veForm._amtScale) return ''
      const sep = this.veForm._amtDecimalMode === 'EXPLICIT' ? '.' : 'V'
      return '9(' + (this.veForm._amtScale || 14) + ')' + sep + '9'.repeat(this.veForm._amtPrecision || 2)
    },

    buildVeJson() {
      const f = this.veForm
      const t = this.visualEditRow.ruleType
      if (t === 'FIXED') return { value: f.value || '' }
      if (t === 'DATE') return { format: f.format || 'yyyyMMdd', offset: f.offset || 0 }
      if (t === 'ENUM') return { enumKey: f.enumKey || '', useDefault: f.useDefault || '0' }
      if (t === 'SEQUENCE') return {
        prefix: f.prefix || '', start: f.start || 1, step: f.step || 1,
        max: f.max || 9999999999, cycle: f.cycle !== undefined ? f.cycle : true,
        digitLength: f.digitLength || 9, updateStrategy: f.updateStrategy || 'PER_LINE'
      }
      if (t === 'RANDOM_CN' || t === 'RANDOM_NUM') return { count: f.count || 3 }
      if (t === 'RANDOM_UUID') return { upperCase: f.upperCase || false }
      if (t === 'REF_FILE') return {
        refFileId: f.refFileId || null, columnKey: f.columnKey || '',
        updateStrategy: f.updateStrategy || 'PER_LINE', atEnd: f.atEnd || 'LOOP'
      }
      if (t === 'REF_FIELD') return { targetFieldKey: f.targetFieldKey || '' }
      if (t === 'SUM') return { targetFieldKey: f.targetFieldKey || '', format: f.format || '9(14)V99' }
      if (t === 'COUNT') return { targetFieldKey: f.targetFieldKey || '' }
      if (t === 'BATCH_NO') return { prefix: f.prefix || '', start: f.start || 1, updateStrategy: f.updateStrategy || 'PER_FILE' }
      if (t === 'AMOUNT') {
        const fmt = this.buildAmtFormat()
        return {
          value: f.value || 0,
          format: fmt,
          scale: (f._amtScale || 14) + (f._amtPrecision || 2), // 总位数
          precision: f._amtPrecision || 2 // 小数位数
        }
      }
      if (t === 'EXPR') return { pattern: f.pattern || '', desc: f.desc || '' }
      return f
    },

    applyVisualEdit() {
      if (!this.visualEditRow) return
      try {
        const obj = this.buildVeJson()
        this.visualEditRow.ruleConfigJson = JSON.stringify(obj)
        this.$set(this.list, this.visualEditIdx, { ...this.visualEditRow })
        this.visualEditVisible = false
        this.$message.success('已写入JSON')
      } catch (e) {
        this.$message.error('JSON序列化失败：' + e.message)
      }
    },

    // ─── 表格辅助 ──────────────────────────────────────────────
    computeOrder(idx) {
      const item = this.list[idx]
      if (!item) return ''
      if (item.level === 1) {
        let count = 0
        for (let i = 0; i <= idx; i++) { if (this.list[i].level === 1) count++ }
        return count + '.0'
      }
      for (let i = idx - 1; i >= 0; i--) {
        if (this.list[i].level === 1) {
          let sub = 0
          for (let j = i + 1; j <= idx; j++) { if (this.list[j].level === 2) sub++ }
          let parentCount = 0
          for (let k = 0; k <= i; k++) { if (this.list[k].level === 1) parentCount++ }
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
      this.list.push(this.normalizeField({
        section: this.section,
        level: 1,
        fieldKey: '',
        fieldName: '',
        length: null,
        isRequired: 0,
        paddingDirection: 'NONE',
        paddingChar: ' ',
        ruleType: 'FIXED',
        ruleConfigJson: JSON.stringify(RULE_TEMPLATES['FIXED']),
        sortIndex: (this.list.length + 1) * 10
      }))
    },

    deleteRow(idx) {
      const item = this.list[idx]
      if (item.level === 1 && item.id) {
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

    onIsRequiredChange(row) {
      row.isRequired = row._isRequired ? 1 : 0
    },

    // ─── 保存 ─────────────────────────────────────────────────
    async handleSave() {
      // 校验每行的 ruleConfigJson 合法性
      const errors = []
      this.list.forEach((f, i) => {
        f.section = this.section
        f.sortIndex = (i + 1) * 10
        if (f.ruleConfigJson && f.ruleConfigJson.trim()) {
          try {
            JSON.parse(f.ruleConfigJson)
          } catch (e) {
            errors.push('第 ' + (i + 1) + ' 行【' + (f.fieldKey || '未命名') + '】的规则配置JSON格式有误：' + e.message)
          }
        }
      })
      if (errors.length > 0) {
        this.$message({ type: 'error', message: errors.join('\n'), duration: 6000, showClose: true })
        return
      }
      this.loading = true
      try {
        await saveFields(this.modelId, this.list, this.section)
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

    // ─── 下载本页字段定义 ─────────────────────────────────────
    handleDownloadCurrentFields() {
      if (!this.list || this.list.length === 0) {
        this.$message.warning('当前没有字段数据可导出')
        return
      }
      const payload = {
        modelName: this.modelName || '未命名模型',
        section: this.section,
        fields: this.list.map(f => ({ ...f }))
      }
      exportFieldExcel(payload).then(res => {
        // responseType:'blob' 时，res 是 axios 响应对象，res.data 才是 Blob 内容
        const blobData = res.data || res
        const blob = new Blob([blobData], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        const disposition = res.headers && res.headers['content-disposition']
        let fileName = (this.modelName || '未命名模型') + '_' + this.section + '_字段定义.xlsx'
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

    // ─── 下载通用模板 ─────────────────────────────────────────
    handleDownloadGeneralTemplate() {
      window.open(getGeneralTemplateDownloadUrl(), '_blank')
    },

    // ─── 上传解析（覆盖模式，带二次确认） ────────────────────
    handleUploadExcel(fileInfo) {
      const rawFile = fileInfo.raw
      if (!rawFile) { this.$message.warning('未获取到文件'); return }

      const doImport = () => {
        const formData = new FormData()
        formData.append('file', rawFile)
        parseFieldExcel(formData).then(res => {
          const parsedFields = res.data || []
          if (parsedFields.length === 0) {
            this.$message.warning('Excel 中未解析到有效字段数据')
            return
          }
          // 直接使用 ruleConfigJson 字段，normalizeField 保证默认值
          this.list = parsedFields.map(item => this.normalizeField({
            section: this.section,
            level: item.level || 1,
            fieldKey: item.fieldKey || '',
            fieldName: item.fieldName || '',
            length: item.length || null,
            isRequired: item.isRequired || 0,
            paddingDirection: item.paddingDirection || 'NONE',
            paddingChar: item.paddingChar || ' ',
            ruleType: this.mapRuleType(item.ruleType),
            ruleConfigJson: item.ruleConfigJson || null,
            sortIndex: (this.list.length + 1) * 10
          }))
          this.$message.success('成功导入 ' + parsedFields.length + ' 个字段定义（已覆盖原有字段）')
        }).catch(e => {
          console.error('Excel 解析失败', e)
          this.$message.error('Excel 解析失败: ' + (e.message || '未知错误'))
        })
      }

      if (this.list && this.list.length > 0) {
        this.$confirm(
          '导入将覆盖当前已有的 ' + this.list.length + ' 个字段定义，操作不可撤销，是否继续？',
          '⚠️ 导入确认',
          {
            confirmButtonText: '确认覆盖导入',
            cancelButtonText: '取消',
            type: 'warning',
            confirmButtonClass: 'el-button--danger'
          }
        ).then(() => {
          doImport()
        }).catch(() => {
          this.$message.info('已取消导入')
        })
      } else {
        doImport()
      }
    },

    mapRuleType(chineseType) {
      if (!chineseType) return 'FIXED'
      const t = chineseType.trim()
      const map = {
        '固定值': 'FIXED', '日期': 'DATE', '枚举': 'ENUM', '序列号': 'SEQUENCE',
        '随机汉字': 'RANDOM_CN', '随机数字': 'RANDOM_NUM', '随机UUID': 'RANDOM_UUID',
        '引用文件': 'REF_FILE', '引用字段': 'REF_FIELD', '汇总金额': 'SUM',
        '统计行数': 'COUNT', '批次号': 'BATCH_NO', '金额': 'AMOUNT', '表达式': 'EXPR',
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
.guide-area { flex: 1; min-width: 280px; padding-left: 16px; overflow-y: auto; background-color: #fafafa; }
.form-footer { margin-top: 16px; text-align: right; }
.table-tip { font-size: 12px; color: #F56C6C; }

/* 说明区样式 */
.guide-content { font-size: 12px; }
.guide-section { margin-bottom: 14px; }
.guide-section-title { font-weight: bold; color: #409EFF; font-size: 13px; margin-bottom: 8px; padding: 4px 0; border-bottom: 1px dashed #dcdfe6; }
.guide-ul { padding-left: 16px; margin: 0; color: #606266; line-height: 2; }
.guide-item { margin-bottom: 12px; padding: 8px; background: #fff; border: 1px solid #ebeef5; border-radius: 4px; }
.guide-title { font-weight: bold; color: #303133; font-size: 12px; margin-bottom: 4px; }
.guide-desc { color: #606266; font-size: 11px; margin-bottom: 4px; line-height: 1.6; }
.guide-json { background: #1e1e1e; color: #d4d4d4; font-family: monospace; font-size: 10px; padding: 5px 8px; border-radius: 3px; margin: 4px 0; word-break: break-all; white-space: pre-wrap; line-height: 1.5; }
.guide-note { color: #909399; font-size: 10px; line-height: 1.8; padding-left: 4px; }
.guide-note code { background: #f0f0f0; padding: 0 3px; border-radius: 2px; color: #e6550d; }

/* 可视化编辑弹窗 */
.visual-edit-body { padding: 4px 0; }
.ve-row { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.ve-label { flex-shrink: 0; width: 76px; font-size: 12px; color: #606266; text-align: right; }
.ve-hint { font-size: 11px; color: #909399; flex-shrink: 0; }
</style>
