<template>
  <el-dialog title="枚举库管理" :visible.sync="visible" width="700px" append-to-body @close="handleClose">
    <div style="margin-bottom: 12px">
      <el-button type="primary" size="small" icon="el-icon-plus" @click="handleAdd">新增枚举</el-button>
    </div>
    <el-table :data="enumList" border size="small" v-loading="loading">
      <el-table-column prop="enumKey" label="Key" width="150" />
      <el-table-column prop="enumName" label="名称" width="150" />
      <el-table-column label="枚举项 (val=desc)">
        <template slot-scope="scope">
          <template v-if="scope.row.items">
            <el-tag v-for="(item, idx) in parseItems(scope.row.items)" :key="idx" size="mini" style="margin:2px">
              {{ item.val }}={{ item.desc }}
            </el-tag>
          </template>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template slot-scope="scope">
          <el-button type="text" size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button type="text" size="small" style="color:#f56c6c" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="editForm.id ? '编辑枚举' : '新增枚举'" :visible.sync="innerVisible" width="500px" append-to-body>
      <el-form :model="editForm" label-width="80px" size="small">
        <el-form-item label="Key">
          <el-input v-model="editForm.enumKey" placeholder="如 sex" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="editForm.enumName" placeholder="如 性别" />
        </el-form-item>
        <el-form-item label="枚举项">
          <div v-for="(item, idx) in editItems" :key="idx" style="display:flex;gap:8px;margin-bottom:4px">
            <el-input v-model="item.val" placeholder="值" style="width:100px" />
            <el-input v-model="item.desc" placeholder="描述" style="flex:1" />
            <el-button type="text" icon="el-icon-delete" style="color:#f56c6c" @click="editItems.splice(idx,1)" />
          </div>
          <el-button type="text" icon="el-icon-plus" @click="editItems.push({val:'',desc:''})">添加项</el-button>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button size="small" @click="innerVisible = false">取消</el-button>
        <el-button size="small" type="primary" @click="handleSaveEnum">保存</el-button>
      </div>
    </el-dialog>
  </el-dialog>
</template>

<script>
import { listEnums, saveEnum, deleteEnum } from '@/api/meta-gen'

export default {
  name: 'EnumManageDialog',
  props: { dialogVisible: Boolean },
  data() {
    return {
      enumList: [],
      loading: false,
      innerVisible: false,
      editForm: {},
      editItems: []
    }
  },
  computed: {
    visible: {
      get() { return this.dialogVisible },
      set(v) { this.$emit('update:dialogVisible', v) }
    }
  },
  watch: {
    dialogVisible(v) { if (v) this.loadData() }
  },
  methods: {
    parseItems(json) {
      try { return JSON.parse(json) || [] } catch (e) { return [] }
    },
    loadData() {
      this.loading = true
      listEnums().then(res => { this.enumList = res.data || [] }).finally(() => { this.loading = false })
    },
    handleAdd() {
      this.editForm = {}
      this.editItems = [{ val: '', desc: '' }]
      this.innerVisible = true
    },
    handleEdit(row) {
      this.editForm = { ...row }
      this.editItems = this.parseItems(row.items)
      this.innerVisible = true
    },
    handleSaveEnum() {
      const items = this.editItems.filter(i => i.val)
      this.editForm.items = JSON.stringify(items)
      saveEnum(this.editForm).then(() => {
        this.$message.success('保存成功')
        this.innerVisible = false
        this.loadData()
        this.$emit('changed')
      })
    },
    handleDelete(row) {
      this.$confirm('确定删除枚举 "' + row.enumKey + '" 吗？', '提示', { type: 'warning' }).then(() => {
        deleteEnum(row.id).then(() => {
          this.$message.success('删除成功')
          this.loadData()
          this.$emit('changed')
        })
      }).catch(() => {})
    },
    handleClose() {
      this.$emit('update:dialogVisible', false)
    }
  }
}
</script>
