<template>
  <el-dialog
    :title="dialogTitle"
    :visible.sync="visible"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    @close="handleClose"
    width="50%">
    <!-- 模式切换与已有配置选择 -->
    <div style="display: flex; align-items: center; margin-bottom: 20px; gap: 20px;">
      <el-radio-group v-model="mode" @change="handleModeChange">
        <el-radio label="create">新建数据库配置</el-radio>
        <el-radio label="edit">修改数据库配置</el-radio>
      </el-radio-group>

      <!-- 修改模式下的下拉选择 -->
      <el-select v-if="mode === 'edit'" v-model="selectedConfigId" placeholder="请选择已有配置" size="small" @change="loadSelectedConfig">
        <el-option
          v-for="config in configList"
          :key="config.id"
          :label="config.configName"
          :value="config.id"
        />
      </el-select>
    </div>

    <!-- 表单 -->
    <el-form ref="form" :model="form" :rules="rules" label-width="120px">
      <el-form-item label="配置名称" prop="configName">
        <el-input v-model="form.configName" placeholder="请输入配置名称" />
      </el-form-item>
      <el-form-item label="数据库类型" prop="dbType">
        <el-select v-model="form.dbType" placeholder="请选择数据库类型">
          <el-option label="MySQL" value="MySQL" />
          <el-option label="TDSQL" value="TDSQL" />
        </el-select>
      </el-form-item>
      <el-form-item label="主机地址" prop="host">
        <el-input v-model="form.host" placeholder="请输入主机地址" />
      </el-form-item>
      <el-form-item label="端口" prop="port">
        <el-input-number v-model="form.port" :min="1" :max="65535" placeholder="请输入端口" />
      </el-form-item>
      <el-form-item label="登录用户名" prop="username">
        <el-input v-model="form.username" placeholder="请输入用户名" />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
      </el-form-item>
      <el-form-item label="数据库名" prop="databaseName">
        <el-input v-model="form.databaseName" placeholder="请输入数据库名" />
      </el-form-item>
      <el-form-item label="字符集" prop="charset">
        <el-input v-model="form.charset" placeholder="请输入字符集" />
      </el-form-item>
    </el-form>

    <div slot="footer" class="dialog-footer" style="text-align: center;">
      <span v-if="testStatus !== null" :style="{ color: testStatus ? '#67C23A' : '#F56C6C', marginRight: '10px', fontSize: '14px' }">
        <i :class="testStatus ? 'el-icon-success' : 'el-icon-error'"></i>
        {{ testStatus ? '连接成功' : '连接失败' }}
      </span>
      <el-button :loading="testLoading" @click="handleTestConnection">测试连接</el-button>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="loading" @click="submitForm">提交</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { getDbConfigList, createDbConfig, updateDbConfig, testDbConnection } from '@/api/migration'

export default {
  name: 'CreateDbConfigDialog',
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      mode: 'create', // 默认是新建模式
      selectedConfigId: null,
      configList: [], // 用于下拉选择的配置列表
      dialogTitle: '新建数据库配置',
      form: {
        id: null,
        configName: '',
        dbType: '',
        host: '',
        port: 3306,
        username: '',
        password: '',
        databaseName: '',
        charset: 'utf8mb4'
      },
      rules: {
        configName: [
          { required: true, message: '配置名称不能为空', trigger: 'blur' }
        ],
        dbType: [
          { required: true, message: '请选择数据库类型', trigger: 'change' }
        ],
        host: [
          { required: true, message: '主机地址不能为空', trigger: 'blur' }
        ],
        port: [
          { required: true, message: '端口不能为空', trigger: 'blur' }
        ],
        username: [
          { required: true, message: '用户名不能为空', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '密码不能为空', trigger: 'blur' }
        ],
        databaseName: [
          { required: true, message: '数据库名不能为空', trigger: 'blur' }
        ]
      },
      loading: false,
      testLoading: false,
      testStatus: null
    }
  },
  watch: {
    visible(newVal) {
      if (newVal) {
        this.mode = 'create'
        this.selectedConfigId = null
        this.testStatus = null
        this.form = {
          id: null,
          configName: '',
          dbType: '',
          host: '',
          port: 3306,
          username: '',
          password: '',
          databaseName: '',
          charset: 'utf8mb4'
        }
        this.fetchConfigList()
      }
    }
  },
  methods: {
    fetchConfigList() {
      getDbConfigList()
        .then(res => {
          if (res.data && res.data.code === 200) {
            this.configList = res.data.data
          } else {
            this.$message.error(res.data.message || '获取配置列表失败')
          }
        })
        .catch(err => {
          this.$message.error('请求失败')
          console.error(err)
        })
    },
    handleModeChange(mode) {
      this.mode = mode
      this.dialogTitle = mode === 'create' ? '新建数据库配置' : '修改数据库配置'
      this.testStatus = null
      if (mode === 'create') {
        this.selectedConfigId = null
        this.form = {
          id: null,
          configName: '',
          dbType: '',
          host: '',
          port: 3306,
          username: '',
          password: '',
          databaseName: '',
          charset: 'utf8mb4'
        }
      }
    },
    loadSelectedConfig(id) {
      const selected = this.configList.find(config => config.id === id)
      this.testStatus = null
      if (selected) {
        this.form = {
          id: selected.id,
          configName: selected.configName,
          dbType: selected.dbType,
          host: selected.host,
          port: selected.port,
          username: selected.username,
          password: selected.password,
          databaseName: selected.databaseName,
          charset: selected.charset
        }
      }
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (valid) {
          this.loading = true
          const config = {
            id: this.form.id,
            configName: this.form.configName,
            dbType: this.form.dbType,
            host: this.form.host,
            port: this.form.port,
            username: this.form.username,
            password: this.form.password,
            databaseName: this.form.databaseName,
            charset: this.form.charset
          }

          if (this.mode === 'edit') {
            if (!config.id) {
              this.$message.error('配置ID不能为空')
              this.loading = false
              return
            }

            updateDbConfig(config)
              .then(res => {
                if (res.data && res.data.code === 200) {
                  this.$message.success('修改数据库配置成功')
                  this.$emit('update:visible', false)
                  this.$emit('update-success')
                } else {
                  this.$message.error(res.data.message || '修改失败')
                }
              })
              .catch(err => {
                this.$message.error('请求失败')
                console.error(err)
              })
              .finally(() => {
                this.loading = false
              })
          } else {
            createDbConfig(config)
              .then(res => {
                if (res.data && res.data.code === 200) {
                  this.$message.success('新增数据库配置成功')
                  this.$emit('update:visible', false)
                  this.$emit('create-success')
                } else {
                  this.$message.error(res.data.message || '新增失败')
                }
              })
              .catch(err => {
                this.$message.error('请求失败')
                console.error(err)
              })
              .finally(() => {
                this.loading = false
              })
          }
        } else {
          this.$message.warning('请检查输入内容')
          return false
        }
      })
    },
    handleClose() {
      this.$emit('update:visible', false)
    },
    handleTestConnection() {
      this.$refs.form.validate(valid => {
        if (valid) {
          this.testLoading = true
          this.testStatus = null
          testDbConnection(this.form)
            .then(res => {
              this.testStatus = res.data && res.data.code === 200
              if (this.testStatus) {
                this.$message.success('连接测试成功')
              } else {
                this.$message.error(res.data.message || '连接测试失败')
              }
            })
            .catch(err => {
              this.testStatus = false
              this.$message.error('连接测试请求失败')
            })
            .finally(() => {
              this.testLoading = false
            })
        } else {
          this.$message.warning('请检查输入内容')
        }
      })
    }
  }
}
</script>
