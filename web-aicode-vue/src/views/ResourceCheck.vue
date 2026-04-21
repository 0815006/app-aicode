<template>
  <!-- 去掉 style="padding: 20px" -->
  <div class="resource-check-container"> <!-- 内部再控制间距 -->
    <h2 class="page-title">资源核查报告</h2>

    <!-- 查询表单 -->
    <el-card shadow="hover" style="margin-bottom: 20px">
      <el-form :inline="true" size="small" style="display: flex; align-items: center;">
        <el-form-item label="产品标识" style="margin-bottom: 0;">
          <el-select
            v-model="productId"
            placeholder="请选择产品标识"
            style="width: 250px"
            filterable
            clearable
            @change="handleQuery"
          >
            <el-option
              v-for="item in productOptions"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item style="margin-bottom: 0;">
          <el-button type="primary" @click="handleQuery" :loading="loading">
            查询
          </el-button>
        </el-form-item>
        <div style="flex: 1;"></div>
        <el-form-item style="margin-bottom: 0; margin-right: 0;">
          <el-button type="success" icon="el-icon-upload" @click="handleUpload">
            上传资源
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 汇总表格 -->
    <el-row :gutter="20" v-if="summaryList.length > 0 || summaryListApply.length > 0">
      <el-col :span="12">
        <el-card shadow="hover" style="margin-bottom: 20px">
          <div slot="header">
            <span style="font-weight: bold">部署方案-资源总量汇总</span>
          </div>
          <el-table
            :data="summaryList"
            border
            size="small"
            style="width: 100%"
            :default-sort="{ prop: 'deploymentLocation', order: 'ascending' }"
          >
            <el-table-column prop="deploymentLocation" label="部署地点" width="120" sortable />
            <el-table-column prop="systemPlatform" label="系统平台" />
            <el-table-column prop="hostCount" label="机器台数" width="80" />
            <el-table-column prop="totalCpu" label="总CPU" width="80" />
            <el-table-column prop="totalMemoryGb" label="总内存" width="80" />
            <el-table-column prop="totalStorageGb" label="总存储" width="80" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" style="margin-bottom: 20px">
          <div slot="header">
            <span style="font-weight: bold">申请表-资源总量汇总</span>
          </div>
          <el-table
            :data="summaryListApply"
            border
            size="small"
            style="width: 100%"
            :default-sort="{ prop: 'deploymentLocation', order: 'ascending' }"
          >
            <el-table-column prop="deploymentLocation" label="部署地点" width="120" sortable />
            <el-table-column prop="systemPlatform" label="系统平台" />
            <el-table-column prop="hostCount" label="机器台数" width="80" />
            <el-table-column prop="totalCpu" label="总CPU" width="80" />
            <el-table-column prop="totalMemoryGb" label="总内存" width="80" />
            <el-table-column prop="totalStorageGb" label="总存储" width="80" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- ✅ 新增：文件级资源汇总（风格统一） -->
    <el-card v-if="fileSummaryList && fileSummaryList.length > 0" shadow="hover" style="margin-top: 20px">
    <div slot="header">
        <span style="font-weight: bold">部署方案-文件级资源汇总（按原始文件名分组）</span>
    </div>
    <el-collapse>
        <el-collapse-item
        v-for="(group, idx) in fileSummaryList"
        :key="idx"
        :name="idx"
        >
        <template slot="title">
            <!-- 文件名 + 上传次数 -->
            <span style="font-weight: bold; color: #409EFF; flex: 1;">
            📄 {{ group.originalFileName }}
            <el-tag size="mini" style="margin-left: 8px">
                上传 {{ group.uploadCount }} 次
            </el-tag>
            </span>

            <!-- 删除按钮：紧贴最右侧 -->
            <el-button
            size="mini"
            type="danger"
            icon="el-icon-delete"
            @click.stop="handleDeleteFile(group.originalFileName)"
            :loading="deleting === group.originalFileName"
            style="margin-left: 0;"
            >
            删除
            </el-button>
        </template>
        <el-table
            :data="group.summary"
            border
            size="small"
            style="width: 100%"
            :default-sort="{ prop: 'deploymentLocation', order: 'ascending' }"
        >
            <el-table-column prop="deploymentLocation" label="部署地点" width="150" sortable />
            <el-table-column prop="systemPlatform" label="系统平台" width="180" />
            <el-table-column prop="hostCount" label="主机数量" width="100" />
            <el-table-column prop="totalCpu" label="CPU总核数" width="100" />
            <el-table-column prop="totalMemoryGb" label="内存总量(GB)" width="120" />
            <el-table-column prop="totalStorageGb" label="存储总量(GB)" width="120" />
        </el-table>
        </el-collapse-item>
    </el-collapse>
    </el-card>

    <!-- ✅ 新增：申请表-文件级资源汇总 -->
    <el-card v-if="fileSummaryListApply && fileSummaryListApply.length > 0" shadow="hover" style="margin-top: 20px">
    <div slot="header">
        <span style="font-weight: bold">申请表-文件级资源汇总（按原始文件名分组）</span>
    </div>
    <el-collapse>
        <el-collapse-item
        v-for="(group, idx) in fileSummaryListApply"
        :key="idx"
        :name="'apply-' + idx"
        >
        <template slot="title">
            <span style="font-weight: bold; color: #67C23A; flex: 1;">
            📄 {{ group.originalFileName }}
            <el-tag size="mini" type="success" style="margin-left: 8px">
                上传 {{ group.uploadCount }} 次
            </el-tag>
            </span>
            <el-button
            size="mini"
            type="danger"
            icon="el-icon-delete"
            @click.stop="handleDeleteFile(group.originalFileName)"
            :loading="deleting === group.originalFileName"
            style="margin-left: 0;"
            >
            删除
            </el-button>
        </template>
        <el-table
            :data="group.summary"
            border
            size="small"
            style="width: 100%"
            :default-sort="{ prop: 'deploymentLocation', order: 'ascending' }"
        >
            <el-table-column prop="deploymentLocation" label="部署地点" width="150" sortable />
            <el-table-column prop="systemPlatform" label="系统平台" width="180" />
            <el-table-column prop="hostCount" label="主机数量" width="100" />
            <el-table-column prop="totalCpu" label="CPU总核数" width="100" />
            <el-table-column prop="totalMemoryGb" label="内存总量(GB)" width="120" />
            <el-table-column prop="totalStorageGb" label="存储总量(GB)" width="120" />
        </el-table>
        </el-collapse-item>
    </el-collapse>
    </el-card>

    <!-- 明细表格 -->
    <el-card v-if="detailList.length > 0" shadow="hover" style="margin-top: 20px">
      <div slot="header">
        <span style="font-weight: bold">部署方案-资源明细配置（相同配置已合并）</span>
      </div>
      <el-table
        :data="detailList"
        border
        size="small"
        style="width: 100%"
        :default-sort="{ prop: 'deploymentLocation', order: 'ascending' }"
      >
        <el-table-column prop="deploymentLocation" label="部署地点" width="150" sortable />
        <el-table-column prop="systemPlatform" label="系统平台" width="180" />
        <el-table-column prop="partitionUsage" label="分区用途" width="380" />
        <el-table-column prop="count" label="机器台数" width="80" />
        <el-table-column prop="cpuCores" label="CPU核心数" width="100" />
        <el-table-column prop="memoryGb" label="内存(GB)" width="100" />
        <el-table-column prop="dedicatedStorageGb" label="独占存储(GB)" width="120" />
        <el-table-column prop="sanStorageGb" label="SAN存储(GB)" width="120" />
        <el-table-column prop="nasStorageGb" label="NAS存储(GB)" width="120" />
      </el-table>
    </el-card>

    <!-- ✅ 新增：申请表-资源明细配置 -->
    <el-card v-if="detailListApply.length > 0" shadow="hover" style="margin-top: 20px">
      <div slot="header">
        <span style="font-weight: bold">申请表-资源明细配置（相同配置已合并）</span>
      </div>
      <el-table
        :data="detailListApply"
        border
        size="small"
        style="width: 100%"
        :default-sort="{ prop: 'deploymentLocation', order: 'ascending' }"
      >
        <el-table-column prop="deploymentLocation" label="部署地点" width="150" sortable />
        <el-table-column prop="systemPlatform" label="系统平台" width="180" />
        <el-table-column prop="partitionUsage" label="分区用途" width="380" />
        <el-table-column prop="count" label="机器台数" width="80" />
        <el-table-column prop="cpuCores" label="CPU核心数" width="100" />
        <el-table-column prop="memoryGb" label="内存(GB)" width="100" />
        <el-table-column prop="dedicatedStorageGb" label="独占存储(GB)" width="120" />
        <el-table-column prop="sanStorageGb" label="SAN存储(GB)" width="120" />
        <el-table-column prop="nasStorageGb" label="NAS存储(GB)" width="120" />
      </el-table>
    </el-card>

    <!-- 无数据提示 -->
    <el-empty v-if="!loading && summaryList.length === 0 && detailList.length === 0 && summaryListApply.length === 0 && detailListApply.length === 0" description="暂无数据" />

    <!-- 上传弹窗 -->
    <upload-resource-dialog ref="uploadDialog" @refresh="handleQuery" />
  </div>
</template>

<script>
import { checkResources, deleteResourceByFile, getProductIds } from '@/api/resource'
import UploadResourceDialog from '@/components/data-migration/UploadResourceDialog.vue'

export default {
  name: 'ResourceCheck',
  components: {
    UploadResourceDialog
  },
  data() {
    return {
      productId: 'BPS-D-AUTO', // 产品ID
      productOptions: [], // 产品列表
      loading: false,
      deleting: '', // 当前正在删除的文件名
      summaryList: [],
      detailList: [],
      fileSummaryList: [],
      summaryListApply: [], // ✅ 申请表汇总
      detailListApply: [], // ✅ 申请表明细
      fileSummaryListApply: [] // ✅ 申请表文件汇总
    }
  },
  mounted() {
    this.fetchProductIds()
  },
  methods: {
    async fetchProductIds() {
      try {
        const res = await getProductIds()
        if (res.code === 200) {
          this.productOptions = res.data || []
          // 如果当前 productId 不在列表中且列表不为空，默认选第一个
          if (this.productOptions.length > 0 && !this.productOptions.includes(this.productId)) {
            this.productId = this.productOptions[0]
          }
          if (this.productId) {
            this.handleQuery()
          }
        }
      } catch (error) {
        console.error('获取产品列表失败', error)
      }
    },
    async handleQuery() {
      if (!this.productId.trim()) {
        this.$message.warning('请输入产品标识')
        return
      }

      this.loading = true
      // 重置所有列表
      this.summaryList = []
      this.detailList = []
      this.fileSummaryList = []
      this.summaryListApply = []
      this.detailListApply = []
      this.fileSummaryListApply = []

      try {
        // 并行调用两次接口
        const [resPlan, resApply] = await Promise.all([
          checkResources(this.productId.trim(), '部署方案'),
          checkResources(this.productId.trim(), '资源申请表')
        ])

        if (resPlan.code === 200) {
          const data = resPlan.data || {}
          this.summaryList = data.summaryList || []
          this.detailList = data.detailList || []
          this.fileSummaryList = data.fileSummaryList || []
        } else {
          this.$message.error('部署方案查询失败: ' + resPlan.message)
        }

        if (resApply.code === 200) {
          const data = resApply.data || {}
          this.summaryListApply = data.summaryList || []
          this.detailListApply = data.detailList || []
          this.fileSummaryListApply = data.fileSummaryList || []
        } else {
          this.$message.error('申请表查询失败: ' + resApply.message)
        }

        if (resPlan.code === 200 && resApply.code === 200) {
          this.$message.success('查询成功')
        }
      } catch (error) {
        this.$message.error('网络请求失败，请检查接口是否可达')
        console.error(error)
      } finally {
        this.loading = false
      }
    },
    handleUpload() {
      this.$refs.uploadDialog.init(this.productId)
    },
    async handleDeleteFile(originalFileName) {
    try {
      await this.$confirm(
        `确定删除文件 "${originalFileName}" 的所有资源数据吗？此操作不可恢复`,
        '警告',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )

      this.deleting = originalFileName

      const res = await deleteResourceByFile(this.productId, originalFileName)

      if (res.code === 200) {
        this.$message.success(res.message || '删除成功')
        // 重新查询数据
        this.handleQuery()
      } else {
        this.$message.error(res.message || '删除失败')
      }
    } catch (error) {
      if (error !== 'cancel') {
        this.$message.error('删除失败，请检查网络或权限')
        console.error(error)
      }
    } finally {
      this.deleting = ''
    }
  }
  }
}
</script>

<style scoped>
.resource-check-container {
  padding: 20px;
  margin: 16px;
  height: calc(100% - 32px);
  overflow-y: auto;
  border-radius: 14px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  border: 1px solid #dbe7f7;
  box-shadow: 0 10px 22px rgba(21, 52, 105, 0.08);
  box-sizing: border-box;
}

.page-title {
  margin: 0 0 18px;
  font-size: 22px;
  color: #1f2d3d;
}

::v-deep .el-card {
  border-radius: 12px;
  border: 1px solid #e2ebf8;
  box-shadow: 0 6px 16px rgba(25, 56, 108, 0.06);
}

::v-deep .el-card__header {
  background: #f6f9ff;
  border-bottom: 1px solid #e6eef9;
}

::v-deep .el-table {
  border-radius: 10px;
  overflow: hidden;
}

::v-deep .el-table th {
  background: #f3f8ff;
  color: #42526a;
}

::v-deep .el-input__inner,
::v-deep .el-button {
  border-radius: 8px;
}

::v-deep .el-collapse-item__header {
  font-weight: 600;
  color: #334155;
}

.resource-check-container::-webkit-scrollbar {
  width: 6px;
}

.resource-check-container::-webkit-scrollbar-thumb {
  background: #c8d8f0;
  border-radius: 8px;
}
</style>
