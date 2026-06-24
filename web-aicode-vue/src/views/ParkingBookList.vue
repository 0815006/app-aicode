<!-- src/views/ParkingBookList.vue -->
<template>
  <div class="parking-list-container">
    <div class="page-layout">
      <div class="main-content" :class="{ 'main-content--full': !showGuide }">
        <h2 class="page-title">车位预约</h2>


        <!-- 操作按钮 -->
        <div class="toolbar">
      <el-button type="primary" icon="el-icon-plus" @click="handleAdd">
        新增
      </el-button>
      <el-button type="success" icon="el-icon-refresh" @click="fetchData" :loading="loading">
        刷新
      </el-button>
      <el-link
        type="primary"
        :underline="false"
        class="system-link"
        @click="openParkingSystem"
      >
        <i class="el-icon-monitor"></i> 车位管理系统
        <i class="el-icon-top-right system-link-arrow"></i>
      </el-link>
    </div>

    <!-- 数据表格 -->
    <el-table
      :data="list"
      border
      size="small"
      style="width: 100%"
      :default-sort="{ prop: 'createTime', order: 'descending' }"
    >
      <el-table-column prop="empNo" label="工号" width="120" sortable />
      <el-table-column prop="username" label="姓名" width="120" />
      <el-table-column label="自动预约" width="120">
        <template slot-scope="scope">
          <el-tag :type="scope.row.autoBook === 1 ? 'success' : 'info'">
            {{ scope.row.autoBook === 1 ? '开启' : '关闭' }}
          </el-tag>
          <div v-if="scope.row.autoBook === 0 && scope.row.nextAutoBookDate" style="font-size: 10px; color: #909399; margin-top: 4px;">
            恢复: {{ scope.row.nextAutoBookDate }}
          </div>
        </template>
      </el-table-column>
      <!-- <el-table-column prop="nextAutoBookDate" label="下次重启预约日期" width="100" /> -->
      <el-table-column prop="lastAppointmentDate" label="最近预约入园日期" width="120" />
      <el-table-column prop="lastAppointmentResult" label="最近预约入园结果" width="120" />
      <el-table-column label="车位位置" width="150">
        <template slot-scope="scope">
          <el-link
            v-if="canNavigateToScreen(scope.row.lastParkingPosition)"
            type="primary"
            :underline="false"
            style="font-size: inherit"
            @click="goToParkingScreen(scope.row.lastParkingPosition)"
          >
            {{ scope.row.lastParkingPosition }}
          </el-link>
          <span v-else>{{ scope.row.lastParkingPosition || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="parkingType" label="车牌类型" width="100" />
      <el-table-column prop="plateNo" label="车牌号" width="120" />
      <el-table-column label="邮件通知" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.emailEnabled === 1 ? 'success' : 'info'">
            {{ scope.row.emailEnabled === 1 ? '开启' : '关闭' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="100" sortable >
            <template slot-scope="scope">
                {{ formatDate(scope.row.createTime) }}
            </template>
      </el-table-column>
      <el-table-column prop="lastTime" label="最后更新" width="100" sortable >
            <template slot-scope="scope">
                {{ formatDate(scope.row.lastTime) }}
            </template>
      </el-table-column>
      <el-table-column label="操作" min-width="180">
        <template slot-scope="scope">
          <el-button size="mini" type="warning" @click="handleEdit(scope.row)">
            编辑
          </el-button>
          <!-- <el-button size="mini" type="danger" @click="handleDelete(scope.row.empNo)">
            删除
          </el-button> -->
            <el-button
            size="mini"
            type="info"
            icon="el-icon-search"
            @click="handleQueryRecords(scope.row.empNo, scope.row.username)"
            >
            查询
            </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 预约记录弹窗 -->
    <el-dialog
    :visible.sync="recordDialogVisible"
    :title="`【${currentUsername}】的预约记录`"
    width="900px"
    @opened="fetchRecordList"
    >
    <!-- 查询条件 -->
    <div class="record-filter-row">
        <el-date-picker
        v-model="recordPage.appointmentDate"
        type="date"
        value-format="yyyy-MM-dd"
        placeholder="预约日期"
        size="small"
        style="width: 160px"
        @change="handleRecordQuery"
        />
        <el-select
        v-model="recordPage.result"
        placeholder="结果"
        size="small"
        style="width: 120px"
        clearable
        @change="handleRecordQuery"
        >
        <el-option label="已成功预约" value="已成功预约" />
        <el-option label="未预约" value="未预约" />
        <el-option label="未中签" value="未中签" />
        <el-option label="不入园" value="不入园" />
        </el-select>
        <el-button type="primary" size="small" @click="handleRecordQuery">查询</el-button>
        <el-button size="small" @click="resetRecordQuery">重置</el-button>
    </div>

    <!-- 记录表格 -->
    <el-table
        :data="recordList"
        border
        size="small"
        style="width: 100%"
        :loading="recordLoading"
    >
        <el-table-column prop="appointmentDate" label="预约日期" width="100" sortable >
            <template slot-scope="scope">
                {{ formatDate(scope.row.appointmentDate) }}
            </template>
        </el-table-column>
        <el-table-column prop="result" label="结果" width="150">
        <template slot-scope="scope">
            <el-tag :type="scope.row.result === '成功' ? 'success' : 'warning'">
            {{ scope.row.result }}
            </el-tag>
        </template>
        </el-table-column>
        <el-table-column prop="resultDesc" label="结果描述" min-width="130" show-overflow-tooltip />
        <el-table-column label="车位位置" width="130">
          <template slot-scope="scope">
            <el-link
              v-if="canNavigateToScreen(scope.row.parkingPosition)"
              type="primary"
              :underline="false"
              style="font-size: inherit"
              @click="goToParkingScreen(scope.row.parkingPosition)"
            >
              {{ scope.row.parkingPosition }}
            </el-link>
            <span v-else>{{ scope.row.parkingPosition || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="plateNo" label="车牌号" width="100" />
        <el-table-column prop="parkingType" label="停车证类型" width="80" />
        <el-table-column prop="createTime" label="创建时间" width="100" sortable >
            <template slot-scope="scope">
                {{ formatDate(scope.row.createTime) }}
            </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div style="margin-top: 20px; text-align: right">
        <el-pagination
        @size-change="size => { recordPage.pageSize = size; handleRecordQuery() }"
        @current-change="page => { recordPage.pageNum = page; handleRecordQuery() }"
        :current-page="recordPage.pageNum"
        :page-sizes="[10, 20, 50]"
        :page-size="recordPage.pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="recordTotal"
        />
    </div>
    </el-dialog>


    <!-- 新增/修改弹窗 -->
    <el-dialog :title="formTitle" :visible.sync="formVisible" width="500px">
      <el-form :model="form" :rules="rules" ref="form" label-width="90px">
        <el-form-item label="工号" prop="empNo">
        <el-input
            v-model="form.empNo"
            :disabled="isEditing"
            placeholder="请输入7位数字工号"
            @input="handleEmpNoInput"
            clearable
        />
        <!-- 新增时：提示必须7位 -->
        <div v-if="!isEditing && form.empNo && form.empNo.length !== 7" class="empno-tip">
            工号必须是7位数字
        </div>
        </el-form-item>
        <el-form-item label="姓名" prop="username">
          <el-input ref="usernameInput" v-model="form.username" placeholder="中文姓名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="留空不修改(域用户密码-明文)" />
        </el-form-item>
        <el-form-item label="自动预约" prop="autoBook">
          <el-switch
            v-model="form.autoBook"
            :active-value="1"
            :inactive-value="0"
            active-text="开启"
            inactive-text="关闭"
          />
        </el-form-item>
        <el-form-item v-if="form.autoBook === 0" label="恢复日期" prop="nextAutoBookDate">
          <el-date-picker
            v-model="form.nextAutoBookDate"
            type="date"
            placeholder="选择下次自动开启日期"
            value-format="yyyy-MM-dd"
            style="width: 100%"
          />
          <div class="empno-tip">到达此日期早晨8点，系统将自动为您重新开启预约</div>
        </el-form-item>
        <el-divider content-position="left">邮件通知设置</el-divider>
        <el-form-item label="邮件通知" prop="emailEnabled">
          <el-switch
            v-model="form.emailEnabled"
            :active-value="1"
            :inactive-value="0"
            active-text="开启"
            inactive-text="关闭"
          />
        </el-form-item>
        <el-form-item label="发件账号" prop="emailUser">
          <el-input v-model="form.emailUser" placeholder="自动取工号作为邮箱账号" disabled />
          <div class="empno-tip">发件邮箱账号与工号一致，无需手动填写</div>
        </el-form-item>
        <el-form-item v-if="form.emailEnabled === 1" label="邮箱密码" prop="emailPassword">
          <el-input v-model="form.emailPassword" type="password" placeholder="请输入邮箱密码（明文）" />
        </el-form-item>
        <el-form-item v-if="form.emailEnabled === 1" label="收件人邮箱" prop="emailRecipient">
          <el-input v-model="form.emailRecipient" placeholder="xxxxxxx@bank-of-china.com" />
        </el-form-item>
      </el-form>

      <div slot="footer">
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitLoading">确定</el-button>
      </div>
    </el-dialog>

      </div>
      <div class="toggle-btn" :class="{ 'toggle-btn--collapsed': !showGuide }" @click="toggleGuide" :title="showGuide ? '折叠右侧面板' : '展开右侧面板'">
        <i :class="showGuide ? 'el-icon-d-arrow-right' : 'el-icon-d-arrow-left'"></i>
      </div>
      <div class="guide-area" v-show="showGuide">
        <div class="guide-content">
          <h3>📋 车位预约指南</h3>

          <div class="guide-item">
            <div class="guide-title">☆ 密码校验说明</div>
            <div class="guide-desc">
              保存配置时，系统会使用您填写的<strong>密码</strong>去车位管理系统校验登录。
              该密码为<strong>车位管理系统的登录密码（即域密码）</strong>，非邮箱密码。
              若保存失败，通常表示密码校验不通过，请确认域密码是否正确。
            </div>
          </div>

          <div class="guide-item">
            <div class="guide-title">☆ 自动预约与定时任务</div>
            <div class="guide-desc">
              开启"自动预约"后，系统将按以下时间节点自动执行（<strong>节假日不执行</strong>）：<br/>
              <span style="color:#409EFF;">● 每天 8:00</span> — 检查是否有暂停用户需要恢复预约，将其自动开启。<br/>
              <span style="color:#409EFF;">● 每天 8:30</span> — 为所有开启自动预约的用户执行预约流程（登录→申请→查询结果并记录）。<br/>
              <span style="color:#409EFF;">● 每天 17:05</span> — 再次检查当天预约记录，若结果有变化则记录并邮件通知。
            </div>
          </div>

          <div class="guide-item">
            <div class="guide-title">☆ 恢复预约设置技巧</div>
            <div class="guide-desc">
              恢复日期当天<strong>上午 8:00</strong> 系统会自动重新开启预约，<strong>8:30</strong> 执行预约。
              但预约的<strong>入园日期是下一天</strong>，因此：<br/>
              如果您开车上班日期是<strong>周二</strong>，恢复日期应设置为<strong>周一</strong>，这样周一恢复→预约→周二入园。
            </div>
          </div>

          <el-divider content-position="left">邮件通知设置</el-divider>

          <div class="guide-item">
            <div class="guide-title">☆ 发件账号</div>
            <div class="guide-desc">
              发件邮箱账号与工号一致，系统会自动填充，无需手动填写。
            </div>
          </div>
          <div class="guide-item">
            <div class="guide-title">☆ 收件人邮箱</div>
            <div class="guide-desc">
              请填写完整的企业邮箱地址，格式如：xxxxxxx@bank-of-china.com
            </div>
          </div>
          <div class="guide-item">
            <div class="guide-title">☆ 邮箱密码说明</div>
            <div class="guide-desc">
              邮箱密码为<strong>登录邮箱的密码</strong>，不是域密码。填域密码无法发送邮件，且不会有报错提示。
            </div>
          </div>
          <div class="guide-item" style="color: #F56C6C; font-weight: bold; margin-top: 12px; line-height: 1.8; font-size: 15px;">
            ☆ 特别遗憾：<br/>
            原车位管理系统，没有「申请当天」和「取消当天」的功能。
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getParkingList, saveOrUpdateParking ,getParkingRecordList  } from '@/api/parking'
import CryptoJS from 'crypto-js'

export default {
  name: 'ParkingBookList',
  data() {
    return {
      list: [],
      showGuide: true, // 指南面板是否显示
      isEditing: false, // 标记是否为编辑模式
      loading: false,
      submitLoading: false,
      formVisible: false,
      formTitle: '',
      form: {
        empNo: '',
        username: '',
        password: '',
        autoBook: 0,
        nextAutoBookDate: '',
        emailEnabled: 0,
        emailUser: '',
        emailPassword: '',
        emailRecipient: ''
      },
      rules: {
        empNo: [
            { required: true, message: '请输入工号', trigger: 'blur' },
            { pattern: /^\d{7}$/, message: '工号必须是7位数字', trigger: 'blur' }
        ],
        username: [
            { required: true, message: '请输入姓名', trigger: 'blur' }
        ]
      },
          // ✅ 新增：预约记录弹窗相关
        recordDialogVisible: false,
        currentEmpNo: '',        // 当前查看的用户工号
        currentUsername: '',     // 当前查看的用户名
        recordList: [],          // 预约记录列表
        recordLoading: false,
        recordTotal: 0,
        recordPage: {
        pageNum: 1,
        pageSize: 10,
        appointmentDate: '',
        result: ''
        }
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    // 判断车位位置文本是否匹配已知楼层前缀，只有匹配的才显示为可点击链接
    canNavigateToScreen(positionText) {
      if (!positionText) return false
      const knownPrefixes = ['4号楼B1层', '5号楼B1层', '5号楼B2层']
      return knownPrefixes.some(p => positionText.includes(p))
    },

    // 从车位位置文本解析楼层ID和车位编号，跳转到停车大屏
    // 车位位置格式示例："4号楼B1层B124"、"5号楼B2层C056"、"5号楼B2层B区B045"
    goToParkingScreen(positionText) {
      if (!positionText) return
      const floorMap = {
        '4号楼B1层': '4F-B1',
        '5号楼B1层': '5F-B1',
        '5号楼B2层': '5F-B2'
      }
      let floorId = ''
      let spaceLabel = ''
      for (const [key, value] of Object.entries(floorMap)) {
        if (positionText.includes(key)) {
          floorId = value
          spaceLabel = positionText.substring(positionText.indexOf(key) + key.length).trim()
          // 5号楼B2层特殊处理：合并A区B区后，车位位置含"B区"/"A区"标记
          // 格式如"5号楼B2层B区B045"，需去掉中间的区标记，只保留车位编号
          if (floorId === '5F-B2') {
            spaceLabel = spaceLabel.replace(/^[AB]区/, '')
          }
          break
        }
      }
      if (!floorId) {
        this.$router.push({ name: 'ParkingScreen' })
        return
      }
      this.$router.push({
        name: 'ParkingScreen',
        query: { floor: floorId, space: spaceLabel }
      })
    },

    // 切换指南面板显示/隐藏
    toggleGuide() {
      this.showGuide = !this.showGuide
    },

    // 处理工号输入：只允许数字，限制7位
    handleEmpNoInput(value) {
    const newValue = value.replace(/\D/g, '')
    const limitedValue = newValue.slice(0, 7)
    this.form.empNo = limitedValue
    // 邮箱账号自动同步工号
    this.form.emailUser = limitedValue

    // 自动聚焦姓名（仅新增时）
    if (!this.isEditing && limitedValue.length === 7) {
        this.$nextTick(() => {
        const usernameInput = this.$refs.usernameInput
        if (usernameInput) {
            const inputEl = usernameInput.$el.querySelector('input')
            if (inputEl) {
            inputEl.focus()
            }
        }
        })
    }
    },

    // 打开预约记录弹窗
    handleQueryRecords(empNo, username) {
        this.currentEmpNo = empNo
        this.currentUsername = username
        this.recordDialogVisible = true
        // @opened 会触发 fetchRecordList
    },

    // 获取预约记录列表
    async fetchRecordList() {
        this.recordLoading = true
        try {
            // 替换
            const res = await getParkingRecordList({
            empId: this.currentEmpNo,
            appointmentDate: this.recordPage.appointmentDate,
            result: this.recordPage.result,
            pageNum: this.recordPage.pageNum,
            pageSize: this.recordPage.pageSize
            })

        if (res.code === 200) {
            const data = res.data || {}
            this.recordList = data.list || []
            this.recordTotal = data.total || 0
        } else {
            this.$message.error(res.message || '查询失败')
        }
        } catch (error) {
        this.$message.error('网络请求失败')
        console.error(error)
        } finally {
        this.recordLoading = false
        }
    },

    // 查询预约记录（带条件）
    handleRecordQuery() {
        this.recordPage.pageNum = 1
        this.fetchRecordList()
    },

    // 重置查询条件
    resetRecordQuery() {
        this.recordPage.appointmentDate = ''
        this.recordPage.result = ''
        this.handleRecordQuery()
    },

    openParkingSystem() {
      window.open('http://22.189.55.96:81/', '_blank')
    },

    async fetchData() {
      this.loading = true
      try {
        const res = await getParkingList()
        if (res.code === 200) {
          this.list = res.data || []
          if (this.list.length > 0) {
            this.$message.success(`共查询到 ${this.list.length} 条记录`)
          }
        } else {
          this.$message.error(res.message || '查询失败')
        }
      } catch (error) {
        this.$message.error('网络请求失败，请检查接口')
        console.error(error)
      } finally {
        this.loading = false
      }
    },

    handleAdd() {
      this.formTitle = '新增车位预约'
      this.form = { empNo: '', username: '', password: '', autoBook: 0, nextAutoBookDate: '', emailEnabled: 0, emailUser: '', emailPassword: '', emailRecipient: '' }
      this.isEditing = false
      this.formVisible = true
    },

    handleEdit(row) {
      this.formTitle = '修改车位预约'
      this.form = {
        ...row,
        password: '',        // 密码不回显，修改时可留空
        emailPassword: '',   // 邮箱密码不回显，修改时可留空
        emailUser: row.empNo || row.emailUser || '',  // 邮箱账号取工号
        emailEnabled: row.emailEnabled || 0,
        emailRecipient: row.emailRecipient || ''
      }
      this.isEditing = true
      this.formVisible = true
    },

    // 格式化日期 YYYY-mm-DD
    formatDate(dateString) {
    return dateString ? dateString.split('T')[0] : ''
    },


    // AES-CBC 加密
    encryptPassword(password) {
      if (!password) return ''
      const key = CryptoJS.enc.Utf8.parse('ihaierForTodoKey')
      const iv = CryptoJS.enc.Utf8.parse('ihaierForTodo_Iv')
      const encrypted = CryptoJS.AES.encrypt(password, key, {
        iv: iv,
        mode: CryptoJS.mode.CBC,
        padding: CryptoJS.pad.Pkcs7
      })
      return encrypted.toString()
    },

    async submitForm() {
    this.$refs.form.validate(async valid => {
        if (!valid) return

        this.submitLoading = true
        const { empNo, username, password, autoBook, nextAutoBookDate, emailEnabled, emailUser, emailPassword, emailRecipient } = this.form
        const passHash = this.encryptPassword(password)
        const encEmailPassword = this.encryptPassword(emailPassword)

        const data = { empNo, username, autoBook, nextAutoBookDate, emailEnabled, emailUser, emailRecipient }
        if (passHash) data.passHash = passHash
        if (encEmailPassword) data.emailPassword = encEmailPassword

        try {
        const res = await saveOrUpdateParking(data)  // ✅ 接收响应

        // ✅ 判断业务成功
        if (res.code === 200) {
            this.$message.success('保存成功')
            this.formVisible = false
            this.fetchData()
        } else {
            // ❌ 业务失败，显示后端提示
            this.$message.error(res.message || '保存失败，请检查用户密码')
        }
        } catch (error) {
        // ❌ 网络异常、超时、500 等
        this.$message.error('网络请求失败，请检查接口。网如果好的那就是密码错误！')
        console.error(error)
        } finally {
        this.submitLoading = false
        }
    })
    },

    async handleDelete(empNo) {
      try {
        await this.$confirm('确认删除该用户？', '提示', { type: 'warning' })
        // TODO: 调用删除接口
        this.$message.success('删除成功')
        this.fetchData()
      } catch (err) {
        // 取消删除
      }
    }
  }
}
</script>

<style scoped>
.parking-list-container {
  height: calc(100% - 32px);
  box-sizing: border-box;
  overflow: hidden;
  padding: 20px;
  margin: 16px;
  border-radius: 14px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  border: 1px solid #dbe7f7;
  box-shadow: 0 10px 22px rgba(21, 52, 105, 0.08);
}

.page-layout {
  display: flex;
  height: 100%;
  position: relative;
}

.main-content {
  flex: 3;
  padding-right: 20px;
  border-right: 1px solid #ebeef5;
  overflow-y: auto;
}

.main-content::-webkit-scrollbar {
  width: 6px;
}

.main-content::-webkit-scrollbar-thumb {
  background: #c8d8f0;
  border-radius: 8px;
}

.guide-area {
  flex: 1;
  padding-left: 20px;
  overflow-y: auto;
  background-color: #fafafa;
  min-width: 260px;
}

.guide-area::-webkit-scrollbar {
  width: 6px;
}

.guide-area::-webkit-scrollbar-thumb {
  background: #c8d8f0;
  border-radius: 8px;
}

/* 折叠按钮（与停车大屏一致） */
.toggle-btn {
  position: absolute;
  left: calc(75% - 14px);
  top: 50%;
  transform: translateY(-50%);
  z-index: 10;
  width: 28px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  cursor: pointer;
  color: #909399;
  font-size: 14px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.toggle-btn:hover {
  color: #409EFF;
  border-color: #409EFF;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.2);
}

.toggle-btn--collapsed {
  left: calc(100% - 30px);
}

/* 右侧折叠时，左侧全宽 */
.main-content--full {
  border-right: none;
  padding-right: 0;
  flex: 1;
}

.guide-content h3 {
  font-size: 15px;
  color: #303133;
  margin-top: 0;
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 2px solid #409EFF;
}
.guide-content ::v-deep .el-divider__text {
  font-size: 13px;
  font-weight: 600;
  color: #409EFF;
  background-color: #fafafa;
}

.guide-item {
  margin-bottom: 14px;
  line-height: 1.6;
}

.guide-title {
  font-weight: bold;
  color: #303133;
  font-size: 13px;
  margin-bottom: 4px;
}

.guide-desc {
  color: #606266;
  font-size: 12px;
  padding-left: 10px;
}

.page-title {
  margin: 0 0 18px;
  font-size: 22px;
  color: #1f2d3d;
}

.toolbar {
  margin-bottom: 18px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.toolbar .el-button + .el-button,
.toolbar .el-button + .el-link {
  margin-left: 0;
}

.record-filter-row {
  margin-bottom: 15px;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.empno-tip {
  font-size: 12px;
  color: #8a96aa;
  margin-top: 5px;
}

::v-deep .el-table {
  border-radius: 10px;
  overflow: hidden;
}

::v-deep .el-table th {
  background: #f3f8ff;
  color: #42526a;
}

::v-deep .el-dialog {
  border-radius: 12px;
  overflow: hidden;
}

::v-deep .el-dialog__header {
  background: #f6f9ff;
  border-bottom: 1px solid #e1ebf8;
}

::v-deep .el-button {
  border-radius: 8px;
}

.system-link {
 font-size: 15px;
 font-weight: 600;
 color: #409EFF;
 padding: 6px 14px;
 border-radius: 8px;
 background: linear-gradient(135deg, #ecf5ff 0%, #d9ecff 100%);
 border: 1px solid #b3d8ff;
 transition: all 0.25s ease;
 letter-spacing: 0.5px;
}
.system-link:hover {
 color: #fff;
 background: linear-gradient(135deg, #409EFF 0%, #66b1ff 100%);
 border-color: #409EFF;
 box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
 transform: translateY(-1px);
}
.system-link-arrow {
 font-size: 12px;
 margin-left: 2px;
 opacity: 0.7;
 transition: opacity 0.25s;
}
.system-link:hover .system-link-arrow {
 opacity: 1;
}
</style>
