<template>
  <div class="data-migration-container">
    <!-- 第一部分：配置区域 -->
    <div class="config-section card">
      <div class="section-header">
        <span class="title">数据库配置</span>
        <el-button type="text" icon="el-icon-setting" @click="showCreateDialog = true">维护连接配置</el-button>
      </div>
      <div class="config-row">
        <!-- 源库配置 -->
        <div class="config-item-wrapper">
          <div class="config-item">
            <span class="label">源库：</span>
            <el-select v-model="sourceConfigId" placeholder="请选择源库" size="small" @change="handleSourceConfigChange">
              <el-option v-for="cfg in dbConfigs" :key="cfg.id" :label="cfg.configName" :value="cfg.id" />
            </el-select>
            <el-button type="primary" size="small" @click="handleTestSource" :loading="sourceLoading">测试连接</el-button>
            <span class="status" :class="sourceStatusClass">
              <i :class="sourceStatusIcon"></i> {{ sourceStatusText }}
            </span>
            <template v-if="sourceStatus === true">
              <el-button type="info" size="small" @click="handleLoadSourceTables" :loading="sourceTableLoading">加载表清单</el-button>
              <el-button type="info" size="small" @click="handleLoadSourceDataVolume" :loading="sourceDataLoading" :disabled="!sourceTablesLoaded">加载数据量</el-button>
            </template>
          </div>
          <el-progress :percentage="sourceProgress" :show-text="false" :stroke-width="2" v-if="sourceTableLoading || sourceDataLoading" class="mini-progress"></el-progress>
        </div>

        <div class="divider"></div>

        <!-- 目标库配置 -->
        <div class="config-item-wrapper">
          <div class="config-item">
            <span class="label">目标库：</span>
            <el-select v-model="targetConfigId" placeholder="请选择目标库" size="small" @change="handleTargetConfigChange">
              <el-option v-for="cfg in dbConfigs" :key="cfg.id" :label="cfg.configName" :value="cfg.id" />
            </el-select>
            <el-button type="primary" size="small" @click="handleTestTarget" :loading="targetLoading">测试连接</el-button>
            <span class="status" :class="targetStatusClass">
              <i :class="targetStatusIcon"></i> {{ targetStatusText }}
            </span>
            <template v-if="targetStatus === true">
              <el-button type="info" size="small" @click="handleLoadTargetTables" :loading="targetTableLoading">加载表清单</el-button>
              <el-button type="info" size="small" @click="handleLoadTargetDataVolume" :loading="targetDataLoading" :disabled="!targetTablesLoaded">加载数据量</el-button>
            </template>
          </div>
          <el-progress :percentage="targetProgress" :show-text="false" :stroke-width="2" v-if="targetTableLoading || targetDataLoading" class="mini-progress"></el-progress>
        </div>
      </div>
    </div>

    <!-- 第二部分：核心区域 -->
    <div class="core-section card">
      <div class="operation-bar">
        <el-button type="primary" size="small" @click="compareStructure" :disabled="!canCompare" :loading="compareLoading">表结构比对</el-button>
        <el-button type="success" size="small" @click="compareRowCount" :disabled="!canCompare" :loading="compareLoading">数据量比对</el-button>
        <el-button type="warning" size="small" @click="compareFullData" :disabled="!canCompare" :loading="compareLoading">完整数据比对</el-button>
        <el-button type="info" size="small" @click="handleSaveResults" :disabled="!canCompare || compareResults.length === 0">保存比对结果</el-button>
        <div class="right-tools">
          <el-button icon="el-icon-refresh" size="small" @click="refreshAll">刷新数据</el-button>
        </div>
      </div>

      <div class="result-display-area">
        <el-table :data="mergedTableData" border style="width: 100%" size="small" v-loading="sourceTableLoading || targetTableLoading">
          <!-- 源库表信息 -->
          <el-table-column label="源库表 (Source)" align="center">
            <el-table-column prop="source.tableName" label="表名" min-width="120">
              <template slot-scope="scope">
                <span v-if="scope.row.source">{{ scope.row.source.tableName }}</span>
                <span v-else class="empty-text">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="source.tableComment" label="中文名" min-width="120">
              <template slot-scope="scope">
                <span v-if="scope.row.source">{{ scope.row.source.tableComment }}</span>
                <span v-else class="empty-text">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="source.rowCount" label="记录数" width="80">
              <template slot-scope="scope">
                <span v-if="scope.row.source && sourceDataVolumeLoaded">{{ scope.row.source.rowCount }}</span>
                <span v-else class="empty-text">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="source.dataLengthMB" label="大小(MB)" width="80">
              <template slot-scope="scope">
                <span v-if="scope.row.source && sourceDataVolumeLoaded">{{ scope.row.source.dataLengthMB }}</span>
                <span v-else class="empty-text">-</span>
              </template>
            </el-table-column>
          </el-table-column>

          <!-- 比对状态列 -->
          <el-table-column label="比对状态" width="100" align="center">
            <template slot-scope="scope">
              <div class="status-icons" v-if="scope.row.source">
                <el-tooltip :content="'结构: ' + getStatusText(scope.row.source.tableName, 'structure')" placement="top">
                  <i :class="getStatusIcon(scope.row.source.tableName, 'structure')" 
                     :style="{ color: getStatusColor(scope.row.source.tableName, 'structure'), cursor: hasDetail(scope.row.source.tableName, 'structure') ? 'pointer' : 'default' }"
                     @click="handleIconClick(scope.row.source.tableName, 'structure')"></i>
                </el-tooltip>
                <el-tooltip :content="'数据量: ' + getStatusText(scope.row.source.tableName, 'rowCount')" placement="top">
                  <i :class="getStatusIcon(scope.row.source.tableName, 'rowCount')" 
                     :style="{ color: getStatusColor(scope.row.source.tableName, 'rowCount'), cursor: hasDetail(scope.row.source.tableName, 'rowCount') ? 'pointer' : 'default' }"
                     @click="handleIconClick(scope.row.source.tableName, 'rowCount')"></i>
                </el-tooltip>
                <el-tooltip :content="'完整数据: ' + getStatusText(scope.row.source.tableName, 'fullData')" placement="top">
                  <i :class="getStatusIcon(scope.row.source.tableName, 'fullData')" 
                     :style="{ color: getStatusColor(scope.row.source.tableName, 'fullData'), cursor: hasDetail(scope.row.source.tableName, 'fullData') ? 'pointer' : 'default' }"
                     @click="handleIconClick(scope.row.source.tableName, 'fullData')"></i>
                </el-tooltip>
              </div>
            </template>
          </el-table-column>

          <!-- 迁移操作按钮 -->
          <el-table-column label="迁移" width="150" align="center">
            <template slot-scope="scope">
              <div class="action-buttons-row" v-if="scope.row.source && sourceTablesLoaded">
                <el-tooltip content="迁移结构 (会清空目标表)" placement="top">
                  <el-button type="primary" circle size="mini" @click="migrateStructure(scope.row.source.tableName)" :loading="migratingStatus[scope.row.source.tableName + '_structure']">></el-button>
                </el-tooltip>
                <el-tooltip content="迁移数据 (会清空目标表)" placement="top">
                  <el-button :type="scope.row.target ? 'success' : ''" :disabled="!scope.row.target" circle size="mini" @click="migrateDataOnly(scope.row.source.tableName)" :loading="migratingStatus[scope.row.source.tableName + '_dataOnly']">>></el-button>
                </el-tooltip>
                <el-tooltip content="迁移结构+数据 (会清空目标表)" placement="top">
                  <el-button type="warning" circle size="mini" @click="migrateData(scope.row.source.tableName)" :loading="migratingStatus[scope.row.source.tableName + '_all']">>>></el-button>
                </el-tooltip>
              </div>
            </template>
          </el-table-column>

          <!-- 目标库表信息 -->
          <el-table-column label="目标库表 (Target)" align="center">
            <el-table-column prop="target.tableName" label="表名" min-width="120">
              <template slot-scope="scope">
                <span v-if="scope.row.target">{{ scope.row.target.tableName }}</span>
                <span v-else class="empty-text">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="target.tableComment" label="中文名" min-width="120">
              <template slot-scope="scope">
                <span v-if="scope.row.target">{{ scope.row.target.tableComment }}</span>
                <span v-else class="empty-text">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="target.rowCount" label="记录数" width="80">
              <template slot-scope="scope">
                <span v-if="scope.row.target && targetDataVolumeLoaded">{{ scope.row.target.rowCount }}</span>
                <span v-else class="empty-text">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="target.dataLengthMB" label="大小(MB)" width="80">
              <template slot-scope="scope">
                <span v-if="scope.row.target && targetDataVolumeLoaded">{{ scope.row.target.dataLengthMB }}</span>
                <span v-else class="empty-text">-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template slot-scope="scope">
                <div v-if="scope.row.target">
                  <el-button type="text" size="mini" class="danger-text" @click="truncateTable(scope.row.target.tableName)">清空</el-button>
                  <el-button type="text" size="mini" class="danger-text" @click="dropTable(scope.row.target.tableName)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 第三部分：比对结果操作区 -->
    <div class="result-section card" v-if="compareResults.length > 0">
      <div class="section-header">
        <span class="title">比对结果详情</span>
      </div>
      <el-table :data="compareResults" border style="width: 100%" size="small">
        <el-table-column prop="__compareType" label="比对类型" width="120" />
        <el-table-column prop="tableName" label="表名" width="180" />
        <el-table-column prop="status" label="状态" width="100">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status === '一致' ? 'success' : 'danger'" size="mini">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="difference" label="差异信息" show-overflow-tooltip />
      </el-table>
    </div>

    <!-- 弹窗组件 -->
    <CreateDbConfigDialog :visible.sync="showCreateDialog" @create-success="onCreateSuccess" @update-success="onCreateSuccess" />

    <el-dialog title="比对差异详情" :visible.sync="detailDialogVisible" width="600px">
      <div class="detail-content">
        <pre>{{ currentDetail }}</pre>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button size="small" @click="detailDialogVisible = false">关闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import CreateDbConfigDialog from '@/components/data-migration/CreateDbConfigDialog.vue'
import {
  getDbConfigList,
  testDbConnection,
  getTableInfoList,
  migrateTableStructure,
  migrateTableDataOnly,
  migrateTableData,
  truncateTable,
  dropTable,
  compareTableStructure,
  compareRowCount,
  compareFullData,
  saveCompareTask
} from '@/api/migration'

export default {
  name: 'DataMigration',
  components: {
    CreateDbConfigDialog
  },
  data() {
    return {
      dbConfigs: [],
      sourceConfigId: null,
      targetConfigId: null,
      sourceConfig: null,
      targetConfig: null,
      sourceTables: [],
      targetTables: [],
      compareResults: [],
      sourceLoading: false,
      targetLoading: false,
      sourceTableLoading: false,
      targetTableLoading: false,
      migratingStatus: {},
      compareLoading: false,
      taskSaving: false,
      showCreateDialog: false,
      sourceStatus: null, // null: 未测试, true: 成功, false: 失败
      targetStatus: null,
      sourceTablesLoaded: false,
      targetTablesLoaded: false,
      sourceDataVolumeLoaded: false,
      targetDataVolumeLoaded: false,
      sourceProgress: 0,
      targetProgress: 0,
      sourceDataLoading: false,
      targetDataLoading: false,
      comparisonStatus: {}, // { tableName: { structure: 'success'|'warning'|'error', rowCount: ..., fullData: ..., detail: ... } }
      detailDialogVisible: false,
      currentDetail: ''
    }
  },
  computed: {
    canCompare() {
      return this.sourceConfigId && this.targetConfigId && this.sourceTables.length > 0
    },
    mergedTableData() {
      const map = {}
      this.sourceTables.forEach(t => {
        map[t.tableName] = { source: t, target: null }
      })
      this.targetTables.forEach(t => {
        if (map[t.tableName]) {
          map[t.tableName].target = t
        } else {
          map[t.tableName] = { source: null, target: t }
        }
      })
      return Object.values(map).sort((a, b) => {
        const nameA = (a.source || a.target).tableName
        const nameB = (b.source || b.target).tableName
        return nameA.localeCompare(nameB)
      })
    },
    sourceStatusText() {
      if (this.sourceStatus === null) return '未测试'
      return this.sourceStatus ? '连接成功' : '连接失败'
    },
    sourceStatusClass() {
      if (this.sourceStatus === null) return 'status-none'
      return this.sourceStatus ? 'status-success' : 'status-error'
    },
    sourceStatusIcon() {
      if (this.sourceStatus === null) return 'el-icon-info'
      return this.sourceStatus ? 'el-icon-success' : 'el-icon-error'
    },
    targetStatusText() {
      if (this.targetStatus === null) return '未测试'
      return this.targetStatus ? '连接成功' : '连接失败'
    },
    targetStatusClass() {
      if (this.targetStatus === null) return 'status-none'
      return this.targetStatus ? 'status-success' : 'status-error'
    },
    targetStatusIcon() {
      if (this.targetStatus === null) return 'el-icon-info'
      return this.targetStatus ? 'el-icon-success' : 'el-icon-error'
    }
  },
  created() {
    this.loadDbConfigs()
  },
  methods: {
    onCreateSuccess() {
      this.loadDbConfigs()
    },
    async loadDbConfigs() {
      try {
        const res = await getDbConfigList()
        if (res.data && res.data.code === 200) {
          this.dbConfigs = res.data.data || []
        }
      } catch (error) {
        this.$message.error('获取配置失败')
      }
    },
    handleSourceConfigChange(id) {
      this.sourceConfig = this.dbConfigs.find(c => c.id === id) || null
      this.sourceStatus = null
      this.sourceTables = []
      this.sourceTablesLoaded = false
      this.sourceDataVolumeLoaded = false
      this.sourceProgress = 0
    },
    handleTargetConfigChange(id) {
      this.targetConfig = this.dbConfigs.find(c => c.id === id) || null
      this.targetStatus = null
      this.targetTables = []
      this.targetTablesLoaded = false
      this.targetDataVolumeLoaded = false
      this.targetProgress = 0
    },
    async loadSourceTables() {
      if (!this.sourceConfig) {
        this.sourceTables = []
        return
      }
      this.sourceTableLoading = true
      try {
        const res = await getTableInfoList(this.sourceConfig)
        if (res.data && res.data.code === 200) {
          this.sourceTables = res.data.data || []
        }
      } finally {
        this.sourceTableLoading = false
      }
    },
    async loadTargetTables() {
      if (!this.targetConfig) {
        this.targetTables = []
        return
      }
      this.targetTableLoading = true
      try {
        const res = await getTableInfoList(this.targetConfig)
        if (res.data && res.data.code === 200) {
          this.targetTables = res.data.data || []
        }
      } finally {
        this.targetTableLoading = false
      }
    },
    async handleTestSource() {
      if (!this.sourceConfig) return
      this.sourceLoading = true
      try {
        const res = await testDbConnection(this.sourceConfig)
        this.sourceStatus = res.data && res.data.code === 200
        if (this.sourceStatus) {
          this.$message.success('源库连接成功')
        } else {
          this.$message.error('源库连接失败')
        }
      } catch (error) {
        this.sourceStatus = false
      } finally {
        this.sourceLoading = false
      }
    },
    async handleTestTarget() {
      if (!this.targetConfig) return
      this.targetLoading = true
      try {
        const res = await testDbConnection(this.targetConfig)
        this.targetStatus = res.data && res.data.code === 200
        if (this.targetStatus) {
          this.$message.success('目标库连接成功')
        } else {
          this.$message.error('目标库连接失败')
        }
      } catch (error) {
        this.targetStatus = false
      } finally {
        this.targetLoading = false
      }
    },
    async handleLoadSourceTables() {
      this.sourceProgress = 0
      const timer = setInterval(() => {
        if (this.sourceProgress < 90) this.sourceProgress += 10
      }, 100)
      try {
        await this.loadSourceTables()
        this.sourceTablesLoaded = true
        this.sourceDataVolumeLoaded = false
        this.sourceProgress = 100
        this.$message.success('源库表清单加载成功')
      } finally {
        clearInterval(timer)
      }
    },
    async handleLoadSourceDataVolume() {
      this.sourceProgress = 0
      this.sourceDataLoading = true
      const timer = setInterval(() => {
        if (this.sourceProgress < 90) this.sourceProgress += 5
      }, 100)
      try {
        // 重新加载以获取最新数据量
        await this.loadSourceTables()
        this.sourceDataVolumeLoaded = true
        this.sourceProgress = 100
        this.$message.success('源库数据量加载成功')
      } finally {
        this.sourceDataLoading = false
        clearInterval(timer)
      }
    },
    async handleLoadTargetTables() {
      this.targetProgress = 0
      const timer = setInterval(() => {
        if (this.targetProgress < 90) this.targetProgress += 10
      }, 100)
      try {
        await this.loadTargetTables()
        this.targetTablesLoaded = true
        this.targetDataVolumeLoaded = false
        this.targetProgress = 100
        this.$message.success('目标库表清单加载成功')
      } finally {
        clearInterval(timer)
      }
    },
    async handleLoadTargetDataVolume() {
      this.targetProgress = 0
      this.targetDataLoading = true
      const timer = setInterval(() => {
        if (this.targetProgress < 90) this.targetProgress += 5
      }, 100)
      try {
        await this.loadTargetTables()
        this.targetDataVolumeLoaded = true
        this.targetProgress = 100
        this.$message.success('目标库数据量加载成功')
      } finally {
        this.targetDataLoading = false
        clearInterval(timer)
      }
    },
    refreshAll() {
      // 清空比对状态和结果
      this.comparisonStatus = {}
      this.compareResults = []
      
      if (this.sourceStatus) this.handleLoadSourceTables()
      if (this.targetStatus) this.handleLoadTargetTables()
    },
    async migrateStructure(tableName) {
      if (!this.targetConfig) return this.$message.warning('请选择目标库')
      const key = tableName + '_structure'
      this.$set(this.migratingStatus, key, true)
      try {
        const res = await migrateTableStructure({
          sourceConfig: this.sourceConfig,
          targetConfig: this.targetConfig,
          tableName
        })
        if (res.data && res.data.code === 200) {
          this.$message.success('表结构迁移成功')
          this.loadTargetTables()
        }
      } finally {
        this.$set(this.migratingStatus, key, false)
      }
    },
    async migrateData(tableName) {
      if (!this.targetConfig) return this.$message.warning('请选择目标库')
      const key = tableName + '_all'
      this.$set(this.migratingStatus, key, true)
      try {
        const res = await migrateTableData({
          sourceConfig: this.sourceConfig,
          targetConfig: this.targetConfig,
          tableName
        })
        if (res.data && res.data.code === 200) {
          this.$message.success('结构和数据迁移成功')
          this.loadTargetTables()
        }
      } finally {
        this.$set(this.migratingStatus, key, false)
      }
    },
    async migrateDataOnly(tableName) {
      if (!this.targetConfig) return this.$message.warning('请选择目标库')
      const key = tableName + '_dataOnly'
      this.$set(this.migratingStatus, key, true)
      try {
        const res = await migrateTableDataOnly({
          sourceConfig: this.sourceConfig,
          targetConfig: this.targetConfig,
          tableName
        })
        if (res.data && res.data.code === 200) {
          this.$message.success('数据迁移成功')
          this.loadTargetTables()
        }
      } finally {
        this.$set(this.migratingStatus, key, false)
      }
    },
    async truncateTable(tableName) {
      try {
        await this.$confirm(`确认清空目标表 ${tableName}?`, '警告', { type: 'warning' })
        const res = await truncateTable({ targetConfig: this.targetConfig, tableName })
        if (res.data && res.data.code === 200) {
          this.$message.success('清空成功')
          this.loadTargetTables()
        }
      } catch (err) {}
    },
    async dropTable(tableName) {
      try {
        await this.$confirm(`确认删除目标表 ${tableName}?`, '危险操作', { type: 'error' })
        const res = await dropTable({ targetConfig: this.targetConfig, tableName })
        if (res.data && res.data.code === 200) {
          this.$message.success('删除成功')
          this.loadTargetTables()
        }
      } catch (err) {}
    },
    async compareStructure() {
      await this.doCompare(compareTableStructure, '表结构比对')
    },
    async compareRowCount() {
      await this.doCompare(compareRowCount, '数据量比对')
    },
    async compareFullData() {
      await this.doCompare(compareFullData, '完整数据比对')
    },
    async doCompare(apiFunc, type) {
      this.compareLoading = true
      try {
        const res = await apiFunc({
          sourceConfig: this.sourceConfig,
          targetConfig: this.targetConfig,
          tables: this.sourceTables.map(t => t.tableName)
        })
        if (res.data && res.data.code === 200) {
          const results = res.data.data || []
          
          // 移除当前比对类型的旧结果，准备存入新结果
          this.compareResults = this.compareResults.filter(item => item.__compareType !== type)

          results.forEach(r => {
            if (!this.comparisonStatus[r.tableName]) {
              this.$set(this.comparisonStatus, r.tableName, {})
            }
            let status = 'success'
            if (!r.isMatch) {
              status = r.detail && r.detail.includes('不存在') ? 'error' : 'warning'
              
              // 将不一致的结果添加到详情列表中
              this.compareResults.push({
                __compareType: type,
                tableName: r.tableName,
                status: r.isMatch ? '一致' : '不一致',
                difference: r.detail
              })
            }
            const typeKey = type === '表结构比对' ? 'structure' : (type === '数据量比对' ? 'rowCount' : 'fullData')
            this.$set(this.comparisonStatus[r.tableName], typeKey, status)
            this.$set(this.comparisonStatus[r.tableName], typeKey + 'Detail', r.detail)
          })
          this.$message.success(`${type}完成`)
        }
      } finally {
        this.compareLoading = false
      }
    },
    async handleSaveResults() {
      const now = new Date()
      const format = (n) => String(n).padStart(2, '0')
      const timestamp = `${now.getFullYear()}${format(now.getMonth() + 1)}${format(now.getDate())}_${format(now.getHours())}${format(now.getMinutes())}${format(now.getSeconds())}`
      const taskName = `比对结果_${timestamp}`

      this.taskSaving = true
      try {
        // 映射比对结果详情
        const results = this.compareResults.map(r => {
          const item = {
            tableName: r.tableName,
            message: r.difference
          }
          if (r.__compareType === '表结构比对') {
            item.isStructureMatch = false
            item.structureDiffDetail = r.difference
          } else if (r.__compareType === '数据量比对') {
            item.isRowCountMatch = false
          } else if (r.__compareType === '完整数据比对') {
            item.isFullDataMatch = false
            item.fullDataDiffDetail = r.difference
          }
          return item
        })

        const res = await saveCompareTask({
          taskName: taskName,
          remark: '自动保存',
          sourceConfigId: this.sourceConfigId,
          sourceConfigName: this.sourceConfig ? this.sourceConfig.configName : '',
          targetConfigId: this.targetConfigId,
          targetConfigName: this.targetConfig ? this.targetConfig.configName : '',
          sourceDatabase: this.sourceConfig ? this.sourceConfig.databaseName : '',
          targetDatabase: this.targetConfig ? this.targetConfig.databaseName : '',
          results: results
        })
        if (res.data && res.data.code === 200) {
          this.$message.success(`比对结果已保存为: ${taskName}`)
        }
      } finally {
        this.taskSaving = false
      }
    },
    getStatusIcon(tableName, type) {
      const status = this.comparisonStatus[tableName] ? this.comparisonStatus[tableName][type] : null
      if (status === 'success') return 'el-icon-circle-check'
      if (status === 'warning') return 'el-icon-warning-outline'
      if (status === 'error') return 'el-icon-circle-close'
      return 'el-icon-minus'
    },
    getStatusColor(tableName, type) {
      const status = this.comparisonStatus[tableName] ? this.comparisonStatus[tableName][type] : null
      if (status === 'success') return '#67C23A'
      if (status === 'warning') return '#E6A23C'
      if (status === 'error') return '#F56C6C'
      return '#C0C4CC'
    },
    getStatusText(tableName, type) {
      const status = this.comparisonStatus[tableName] ? this.comparisonStatus[tableName][type] : null
      if (status === 'success') return '一致'
      if (status === 'warning') return '不一致'
      if (status === 'error') return '目标表不存在'
      return '未比对'
    },
    hasDetail(tableName, type) {
      return this.comparisonStatus[tableName] && this.comparisonStatus[tableName][type + 'Detail']
    },
    handleIconClick(tableName, type) {
      if (this.hasDetail(tableName, type)) {
        this.currentDetail = this.comparisonStatus[tableName][type + 'Detail']
        this.detailDialogVisible = true
      }
    }
  }
}
</script>

<style scoped>
.data-migration-container {
  height: 100%;
  box-sizing: border-box;
  overflow: auto;
  padding: 16px;
  background: linear-gradient(180deg, #f7f9ff 0%, #eef3ff 100%);
}
.card {
  background: #fff;
  border-radius: 14px;
  padding: 15px;
  margin-bottom: 15px;
  border: 1px solid #e8eefb;
  box-shadow: 0 10px 22px rgba(27, 56, 112, 0.08);
}
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  border-bottom: 1px solid #edf2ff;
  padding-bottom: 10px;
}
.section-header .title {
  font-size: 16px;
  font-weight: 700;
  color: #23324a;
}
.config-row {
  display: flex;
  align-items: center;
  gap: 20px;
}
.config-item-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.mini-progress {
  margin-top: -5px;
}
.config-item {
  display: flex;
  align-items: center;
  gap: 10px;
}
.config-item .label {
  font-weight: 700;
  white-space: nowrap;
  color: #42506a;
}
.divider {
  width: 1px;
  height: 30px;
  background-color: #dcdfe6;
}
.status {
  font-size: 12px;
  white-space: nowrap;
}
.status-none { color: #909399; }
.status-success { color: #67c23a; }
.status-error { color: #f56c6c; }

.operation-bar {
  margin-bottom: 15px;
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}
.right-tools {
  margin-left: auto;
}
.empty-text {
  color: #c0c4cc;
  font-style: italic;
}
.status-icons {
  display: flex;
  justify-content: center;
  gap: 10px;
  font-size: 16px;
}
.status-icons i {
  cursor: default;
}
.detail-content {
  max-height: 400px;
  overflow-y: auto;
  background: #f6f9ff;
  padding: 10px;
  border-radius: 8px;
  border: 1px solid #e8eefb;
}
.detail-content pre {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
}
.action-buttons-row {
  display: flex;
  justify-content: center;
  gap: 8px;
}
.action-buttons-row .el-button {
  width: 28px;
  height: 28px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
}
.danger-text {
  color: #f56c6c;
}
.danger-text:hover {
  color: #f78989;
}

::v-deep .el-table {
  border-radius: 10px;
  overflow: hidden;
}

::v-deep .el-table th {
  background: #f3f8ff;
  color: #42526a;
}

::v-deep .el-button {
  border-radius: 8px;
}
</style>
