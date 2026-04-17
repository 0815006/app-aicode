<template>
  <div class="vote-container">
    <!-- 左侧任务列表 -->
    <div class="sidebar">
      <div class="sidebar-header">
        <el-input
          v-model="searchQuery"
          placeholder="搜索任务名称"
          prefix-icon="el-icon-search"
          size="small"
          @input="fetchTaskList"
        />
        <el-button
          type="primary"
          icon="el-icon-plus"
          circle
          size="small"
          @click="showCreateDialog = true"
        />
      </div>
      <div class="task-list" v-loading="listLoading">
        <div
          v-for="task in taskList"
          :key="task.id"
          :class="['task-item', { active: currentTask && currentTask.id === task.id }]"
          @click="handleTaskClick(task)"
        >
          <div class="task-title">{{ task.title }}</div>
          <div class="task-info">
            <el-tag size="mini" :type="task.type === '1' ? 'info' : 'success'">
              {{ task.type === '1' ? '直接投票' : '征集投票' }}
            </el-tag>
            <el-tag size="mini" :type="getStatusType(task.status)">
              {{ getStatusName(task.status) }}
            </el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧详情区 -->
    <div class="main-content">
      <div v-if="!currentTask" class="empty-state">
        <el-empty description="请选择一个投票任务" />
      </div>
      <div v-else class="task-detail" :key="currentTask.id">
        <div class="detail-header">
          <h2>{{ currentTask.title }}</h2>
          <div class="task-meta">
            <span>类型：{{ currentTask.type === '1' ? '直接投票' : '征集投票' }}</span>
            <span>每人限投：{{ currentTask.maxVotes }} 票</span>
            <span>截止时间：{{ formatTime(currentTask.voteEndAt) }}</span>
          </div>
        </div>

        <!-- 结果展示区 (截止后显示) -->
        <div v-if="isEnded(currentTask)" class="result-section">
          <h3>投票结果</h3>
          <div class="podium">
            <div v-if="results[1]" class="podium-item silver">
              <div class="rank">2</div>
              <img :src="results[1].coverUrl || defaultCover" class="podium-cover" />
              <div class="podium-name">{{ results[1].title }}</div>
              <div class="podium-votes">{{ results[1].voteCount }} 票</div>
            </div>
            <div v-if="results[0]" class="podium-item gold">
              <div class="rank">1</div>
              <img :src="results[0].coverUrl || defaultCover" class="podium-cover" />
              <div class="podium-name">{{ results[0].title }}</div>
              <div class="podium-votes">{{ results[0].voteCount }} 票</div>
            </div>
            <div v-if="results[2]" class="podium-item bronze">
              <div class="rank">3</div>
              <img :src="results[2].coverUrl || defaultCover" class="podium-cover" />
              <div class="podium-name">{{ results[2].title }}</div>
              <div class="podium-votes">{{ results[2].voteCount }} 票</div>
            </div>
          </div>
          
          <div class="chart-container">
            <div v-for="(item, index) in results" :key="item.id" class="bar-item">
              <div class="bar-label">Top {{ index + 1 }}: {{ item.title }}</div>
              <div class="bar-wrapper">
                <div 
                  class="bar-fill" 
                  :style="{ width: getBarWidth(item.voteCount), backgroundColor: getBarColor(index) }"
                ></div>
                <span class="bar-count">{{ item.voteCount }} 票</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 投票/作品展示区 -->
        <div class="options-section" v-loading="optionsLoading">
          <div class="section-header">
            <h3>{{ currentTask.type === '1' ? '投票选项' : '作品列表' }}</h3>
            <el-button 
              v-if="currentTask.type === '2' && !isUploadEnded(currentTask)" 
              type="primary" 
              size="small" 
              @click="openUploadDialog"
            >
              上传作品
            </el-button>
          </div>

          <div class="options-grid">
            <el-card 
              v-for="option in options" 
              :key="option.id" 
              class="option-card" 
              :body-style="{ padding: '0px' }"
            >
              <img :src="option.coverUrl || defaultCover" class="option-image" @click="previewWork(option)" />
              <div class="option-info">
                <div class="option-title">{{ option.title }}</div>
                <div class="option-author" v-if="currentTask.type === '2'">作者: {{ option.authorName }}</div>
                <div class="option-actions">
                  <span v-if="option.voteCount !== null" class="vote-count">{{ option.voteCount }} 票</span>
                  <div class="action-buttons">
                    <el-button 
                      v-if="currentTask.type === '2' && option.userId === '2036377' && !isUploadEnded(currentTask)"
                      type="text"
                      size="mini"
                      @click="editWork(option)"
                    >
                      编辑
                    </el-button>
                    <el-button 
                      v-if="currentTask.creatorId === '2036377' && option.auditStatus === '0'"
                      type="warning"
                      size="mini"
                      @click="handleAudit(option)"
                    >
                      审核
                    </el-button>
                    <el-button 
                      v-if="!isEnded(currentTask) && option.auditStatus === '1'"
                      :type="isVoted(option.id) ? 'danger' : 'primary'" 
                      size="mini" 
                      @click="handleVote(option)"
                    >
                      {{ isVoted(option.id) ? '撤回' : '投票' }}
                    </el-button>
                  </div>
                </div>
              </div>
            </el-card>
          </div>
        </div>
      </div>
    </div>

    <!-- 发起投票弹窗 -->
    <el-dialog title="发起投票任务" :visible.sync="showCreateDialog" width="600px">
      <el-form :model="createForm" label-width="120px" size="small">
        <el-form-item label="任务名称">
          <el-input v-model="createForm.title" />
        </el-form-item>
        <el-form-item label="任务类型">
          <el-radio-group v-model="createForm.type">
            <el-radio label="1">直接投票</el-radio>
            <el-radio label="2">征集投票</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="每人总票数">
          <el-input-number v-model="createForm.maxVotes" :min="1" />
        </el-form-item>
        <el-form-item v-if="createForm.type === '2'" label="允许提前查看">
          <el-switch v-model="createForm.allowViewEarly" active-value="1" inactive-value="0" />
        </el-form-item>
        <el-form-item v-if="createForm.type === '2'" label="上传截止时间">
          <el-date-picker v-model="createForm.uploadEndAt" type="datetime" placeholder="选择日期时间" value-format="timestamp" />
        </el-form-item>
        <el-form-item label="投票截止时间">
          <el-date-picker v-model="createForm.voteEndAt" type="datetime" placeholder="选择日期时间" value-format="timestamp" />
        </el-form-item>
        
        <!-- 第一类任务直接录入选项 -->
        <div v-if="createForm.type === '1'">
          <el-divider>投票选项</el-divider>
          <div v-for="(opt, index) in createForm.options" :key="index" class="option-input-item">
            <el-input v-model="opt.title" placeholder="选项名称" style="width: 80%" />
            <el-button type="danger" icon="el-icon-delete" circle @click="removeOption(index)" />
          </div>
          <el-button type="text" icon="el-icon-plus" @click="addOption">添加选项</el-button>
        </div>
      </el-form>
      <div slot="footer">
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateTask">确定</el-button>
      </div>
    </el-dialog>

    <!-- 上传作品弹窗 -->
    <el-dialog title="上传作品" :visible.sync="showUploadDialog" width="600px">
      <el-form :model="uploadForm" label-width="100px" size="small">
        <el-form-item label="作品名称">
          <el-input v-model="uploadForm.title" />
        </el-form-item>
        <el-form-item label="队伍名称">
          <el-input v-model="uploadForm.teamName" />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="uploadForm.tags" placeholder="多个标签用逗号分隔" />
        </el-form-item>
        <el-form-item label="作者姓名">
          <el-input v-model="uploadForm.authorName" />
        </el-form-item>
        <el-form-item label="作品介绍">
          <el-input type="textarea" v-model="uploadForm.description" />
        </el-form-item>
        <el-form-item label="封面图">
          <el-upload
            class="upload-demo"
            action="/api/file/upload"
            :on-success="handleCoverSuccess"
            :limit="1"
            :file-list="coverFileList"
            list-type="picture"
          >
            <el-button size="small" type="primary">点击上传</el-button>
            <div slot="tip" class="el-upload__tip">只能上传jpg/png文件，且不超过500kb</div>
          </el-upload>
        </el-form-item>
        <el-form-item label="视频文件">
          <el-upload
            class="upload-demo"
            action="/api/file/upload"
            :on-success="handleVideoSuccess"
            :on-remove="handleVideoRemove"
            :file-list="videoFileList"
            multiple
          >
            <el-button size="small" type="primary">点击上传</el-button>
            <div slot="tip" class="el-upload__tip">支持上传多个视频文件</div>
          </el-upload>
        </el-form-item>
        <el-form-item label="附件">
          <el-upload
            class="upload-demo"
            action="/api/file/upload"
            :on-success="handleAttachmentSuccess"
            :limit="1"
            :file-list="attachmentFileList"
          >
            <el-button size="small" type="primary">点击上传</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUploadWork">提交</el-button>
      </div>
    </el-dialog>

    <!-- 作品详情预览 -->
    <el-dialog :title="previewData.title" :visible.sync="showPreview" width="800px">
      <div class="preview-content">
        <div class="preview-main">
          <!-- 封面图 -->
          <div class="image-box" style="margin-bottom: 20px; text-align: center;">
            <img :src="previewData.coverUrl || defaultCover" style="max-width: 100%; max-height: 300px; border-radius: 4px;" />
          </div>
          <!-- 视频列表 -->
          <div v-if="videoList.length > 0" class="video-container-list">
            <div v-for="(v, i) in videoList" :key="i" class="video-box" style="margin-bottom: 15px;">
              <video :src="v" controls width="100%" style="border-radius: 4px; background: #000;"></video>
            </div>
          </div>
        </div>
        <div class="preview-info">
          <p><strong>队伍名称：</strong>{{ previewData.teamName || '-' }}</p>
          <p><strong>标签：</strong>
            <el-tag v-for="tag in tagList" :key="tag" size="mini" style="margin-right: 5px">{{ tag }}</el-tag>
          </p>
          <p><strong>作者：</strong>{{ previewData.authorName }}</p>
          <p><strong>介绍：</strong><span class="description-text">{{ previewData.description }}</span></p>
          <p v-if="previewData.attachmentUrl">
            <strong>附件：</strong>
            <el-link type="primary" :href="previewData.attachmentUrl" target="_blank">点击下载附件</el-link>
          </p>
          <p>
            <strong>审核状态：</strong>
            <el-tag size="mini" :type="getAuditStatusType(previewData.auditStatus)">
              {{ getAuditStatusName(previewData.auditStatus) }}
            </el-tag>
          </p>
          <p v-if="previewData.auditStatus === '2'" style="color: #F56C6C">
            <strong>驳回理由：</strong>{{ previewData.auditRemark }}
          </p>
        </div>
      </div>
      <div slot="footer" class="preview-footer">
        <!-- 发起人审核按钮 -->
        <template v-if="currentTask && currentTask.creatorId === '2036377' && previewData.auditStatus === '0'">
          <el-button type="success" size="small" @click="handleAuditInPreview('1')">通过</el-button>
          <el-button type="danger" size="small" @click="handleAuditInPreview('2')">驳回</el-button>
        </template>
        <!-- 作者修改按钮 -->
        <template v-if="currentTask && previewData.userId === '2036377' && !isUploadEnded(currentTask)">
          <el-button type="primary" size="small" @click="editWorkInPreview">修改作品</el-button>
        </template>
        <el-button size="small" @click="showPreview = false">关闭</el-button>
      </div>
    </el-dialog>

    <!-- 审核弹窗 -->
    <el-dialog title="审核作品" :visible.sync="showAuditDialog" width="400px">
      <el-form :model="auditForm" label-width="80px" size="small">
        <el-form-item label="审核结果">
          <el-radio-group v-model="auditForm.auditStatus">
            <el-radio label="1">通过</el-radio>
            <el-radio label="2">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="auditForm.auditStatus === '2'" label="驳回理由">
          <el-input type="textarea" v-model="auditForm.auditRemark" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="showAuditDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAudit">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { 
  getTaskList, createTask, getTaskDetail, getOptions, 
  vote, revokeVote, uploadWork, getResults, auditWork 
} from '@/api/vote'

export default {
  name: 'VoteManagement',
  data() {
    return {
      searchQuery: '',
      taskList: [],
      currentTask: null,
      options: [],
      results: [],
      votedIds: [], // 当前用户已投的选项ID
      listLoading: false,
      optionsLoading: false,
      showCreateDialog: false,
      showUploadDialog: false,
      showAuditDialog: false,
      showPreview: false,
      previewData: {},
      createForm: {
        title: '',
        type: '1',
        maxVotes: 1,
        allowViewEarly: '0',
        uploadEndAt: null,
        voteEndAt: null,
        options: [{ title: '' }]
      },
      uploadForm: {
        id: null,
        title: '',
        teamName: '',
        tags: '',
        authorName: '',
        description: '',
        coverUrl: '',
        videoUrl: '',
        attachmentUrl: ''
      },
      coverFileList: [],
      videoFileList: [],
      attachmentFileList: [],
      auditForm: {
        optionId: null,
        auditStatus: '1',
        auditRemark: ''
      },
      defaultCover: 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAASwAAADIAQMAAAB6S9AcAAAAA1BMVEXMzMzK661VAAAAAXRSTlMAQObYZgAAADlJREFUeNrtwTEBAAAAwiD7p7bGDmAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAkB6SRAAF976YvAAAAAElFTkSuQmCC'
    }
  },
  mounted() {
    this.fetchTaskList()
  },
  methods: {
    fetchTaskList() {
      this.listLoading = true
      getTaskList({ title: this.searchQuery }).then(res => {
        this.taskList = res.data || []
      }).catch(err => {
        this.$message.error('获取任务列表失败: ' + err.message)
      }).finally(() => {
        this.listLoading = false
      })
    },
    handleTaskClick(task) {
      this.currentTask = task
      this.options = []
      this.results = []
      this.fetchOptions()
      if (this.isEnded(task)) {
        this.fetchResults()
      }
    },
    openUploadDialog() {
      this.uploadForm = {
        id: null,
        title: '',
        teamName: '',
        tags: '',
        authorName: '',
        description: '',
        coverUrl: '',
        videoUrl: '',
        attachmentUrl: ''
      }
      this.coverFileList = []
      this.videoFileList = []
      this.attachmentFileList = []
      this.showUploadDialog = true
    },
    fetchOptions() {
      this.optionsLoading = true
      getOptions(this.currentTask.id).then(res => {
        this.options = res.data || []
        // 根据后端返回的 voted 状态初始化 votedIds
        this.votedIds = this.options.filter(opt => opt.voted).map(opt => opt.id)
      }).catch(err => {
        this.$message.error('获取选项失败: ' + err.message)
      }).finally(() => {
        this.optionsLoading = false
      })
    },
    fetchResults() {
      getResults(this.currentTask.id).then(res => {
        this.results = res.data || []
      }).catch(err => {
        console.error('获取结果失败:', err)
      })
    },
    addOption() {
      this.createForm.options.push({ title: '' })
    },
    removeOption(index) {
      this.createForm.options.splice(index, 1)
    },
    handleCreateTask() {
      createTask(this.createForm).then(() => {
        this.$message.success('创建成功')
        this.showCreateDialog = false
        this.fetchTaskList()
      })
    },
    handleUploadWork() {
      // 将视频列表转为 JSON 字符串
      const videoUrls = this.videoFileList.map(f => f.response ? f.response.data : f.url)
      this.uploadForm.videoUrl = JSON.stringify(videoUrls)
      
      const data = { ...this.uploadForm, taskId: this.currentTask.id }
      uploadWork(data).then(() => {
        this.$message.success('提交成功，请等待审核')
        this.showUploadDialog = false
        this.fetchOptions()
      })
    },
    handleCoverSuccess(res) {
      this.uploadForm.coverUrl = res.data
    },
    handleVideoSuccess(res, file, fileList) {
      this.videoFileList = fileList
    },
    handleVideoRemove(file, fileList) {
      this.videoFileList = fileList
    },
    handleAttachmentSuccess(res) {
      this.uploadForm.attachmentUrl = res.data
    },
    editWork(option) {
      this.uploadForm = { ...option }
      // 初始化文件列表
      this.coverFileList = option.coverUrl ? [{ name: '封面', url: option.coverUrl }] : []
      this.attachmentFileList = option.attachmentUrl ? [{ name: '附件', url: option.attachmentUrl }] : []
      try {
        const videos = JSON.parse(option.videoUrl || '[]')
        this.videoFileList = videos.map((v, i) => ({ name: `视频${i+1}`, url: v }))
      } catch (e) {
        this.videoFileList = option.videoUrl ? [{ name: '视频', url: option.videoUrl }] : []
      }
      this.showUploadDialog = true
    },
    handleAudit(option) {
      this.auditForm.optionId = option.id
      this.auditForm.auditStatus = '1'
      this.auditForm.auditRemark = ''
      this.showAuditDialog = true
    },
    handleAuditInPreview(status) {
      if (status === '2') {
        this.handleAudit(this.previewData)
      } else {
        auditWork({ optionId: this.previewData.id, auditStatus: '1' }).then(() => {
          this.$message.success('审核通过')
          this.showPreview = false
          this.fetchOptions()
        })
      }
    },
    editWorkInPreview() {
      this.editWork(this.previewData)
      this.showPreview = false
    },
    submitAudit() {
      auditWork(this.auditForm).then(() => {
        this.$message.success('审核完成')
        this.showAuditDialog = false
        this.fetchOptions()
      })
    },
    handleVote(option) {
      if (this.isVoted(option.id)) {
        revokeVote(this.currentTask.id, option.id).then(() => {
          this.$message.success('已撤回')
          this.votedIds = this.votedIds.filter(id => id !== option.id)
          this.fetchOptions()
        })
      } else {
        vote(this.currentTask.id, option.id).then(() => {
          this.$message.success('投票成功')
          this.votedIds.push(option.id)
          this.fetchOptions()
        }).catch(err => {
          this.$message.error(err.message)
        })
      }
    },
    previewWork(option) {
      this.previewData = option
      this.showPreview = true
    },
    isVoted(optionId) {
      return this.votedIds.includes(optionId)
    },
    isEnded(task) {
      if (!task || !task.voteEndAt) return false
      return new Date().getTime() > new Date(task.voteEndAt).getTime()
    },
    isUploadEnded(task) {
      if (!task || !task.uploadEndAt) return false
      return new Date().getTime() > new Date(task.uploadEndAt).getTime()
    },
    getStatusName(status) {
      const map = { '0': '草稿', '1': '进行中', '2': '已结束' }
      return map[status] || '未知'
    },
    getStatusType(status) {
      const map = { '0': 'info', '1': 'success', '2': 'danger' }
      return map[status] || 'info'
    },
    formatTime(time) {
      if (!time) return '-'
      return new Date(time).toLocaleString()
    },
    getAuditStatusName(status) {
      const map = { '0': '待审核', '1': '已通过', '2': '已驳回' }
      return map[status] || '未知'
    },
    getAuditStatusType(status) {
      const map = { '0': 'info', '1': 'success', '2': 'danger' }
      return map[status] || 'info'
    },
    getBarWidth(count) {
      if (!this.results.length) return '0%'
      const max = this.results[0].voteCount || 1
      return (count / max * 80) + '%'
    },
    getBarColor(index) {
      const colors = ['#FFD700', '#C0C0C0', '#CD7F32']
      return colors[index] || '#409EFF'
    }
  },
  computed: {
    videoList() {
      if (!this.previewData.videoUrl) return []
      try {
        const list = JSON.parse(this.previewData.videoUrl)
        return Array.isArray(list) ? list : [this.previewData.videoUrl]
      } catch (e) {
        return [this.previewData.videoUrl]
      }
    },
    tagList() {
      if (!this.previewData.tags) return []
      return this.previewData.tags.split(/[,，]/).filter(t => t.trim())
    }
  }
}
</script>

<style scoped>
.vote-container {
  display: flex;
  width: 100%;
  height: calc(100vh - 60px); /* 减去 Header 高度 */
  background: #f5f7fa;
  overflow: hidden;
}

.sidebar {
  width: 280px;
  flex-shrink: 0;
  background: #fff;
  border-right: 1px solid #e6e6e6;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 15px;
  display: flex;
  gap: 10px;
  border-bottom: 1px solid #f0f0f0;
}

.task-list {
  flex: 1;
  overflow-y: auto;
}

.task-item {
  padding: 15px;
  cursor: pointer;
  border-bottom: 1px solid #f9f9f9;
  transition: background 0.3s;
}

.task-item:hover {
  background: #f0f7ff;
}

.task-item.active {
  background: #ecf5ff;
  border-left: 4px solid #409eff;
}

.task-title {
  font-weight: bold;
  margin-bottom: 8px;
  color: #303133;
}

.task-info {
  display: flex;
  gap: 5px;
}

.main-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.task-detail {
  background: #fff;
  padding: 25px;
  border-radius: 8px;
  min-height: 100%;
}

.detail-header {
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 20px;
  padding-bottom: 15px;
}

.task-meta {
  color: #909399;
  font-size: 14px;
  display: flex;
  gap: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 30px;
}

.options-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.option-card {
  transition: transform 0.3s;
}

.option-card:hover {
  transform: translateY(-5px);
}

.option-image {
  width: 100%;
  height: 150px;
  object-fit: cover;
  cursor: pointer;
}

.option-info {
  padding: 12px;
}

.option-title {
  font-weight: bold;
  margin-bottom: 5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.option-author {
  font-size: 12px;
  color: #909399;
  margin-bottom: 10px;
}

.option-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.vote-count {
  color: #f56c6c;
  font-weight: bold;
}

/* 领奖台样式 */
.podium {
  display: flex;
  justify-content: center;
  align-items: flex-end;
  gap: 20px;
  margin: 40px 0;
  height: 250px;
}

.podium-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 120px;
  position: relative;
}

.podium-cover {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  border: 4px solid #fff;
  box-shadow: 0 4px 10px rgba(0,0,0,0.1);
  margin-bottom: 10px;
}

.rank {
  position: absolute;
  top: -15px;
  font-size: 24px;
  font-weight: bold;
}

.gold .rank { color: #FFD700; }
.silver .rank { color: #C0C0C0; }
.bronze .rank { color: #CD7F32; }

.podium-item::after {
  content: '';
  width: 100%;
  background: #f2f6fc;
  border-radius: 8px 8px 0 0;
}

.gold { height: 220px; }
.silver { height: 180px; }
.bronze { height: 150px; }

.gold::after { height: 100px; background: #fff8e1; }
.silver::after { height: 70px; background: #f5f5f5; }
.bronze::after { height: 50px; background: #efebe9; }

.podium-name {
  font-weight: bold;
  font-size: 14px;
  margin-top: 10px;
  text-align: center;
}

.podium-votes {
  color: #f56c6c;
  font-size: 12px;
}

/* 柱状图样式 */
.chart-container {
  margin-top: 40px;
  max-width: 600px;
  margin-left: auto;
  margin-right: auto;
}

.bar-item {
  margin-bottom: 15px;
}

.bar-label {
  font-size: 13px;
  margin-bottom: 5px;
}

.bar-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
}

.bar-fill {
  height: 12px;
  border-radius: 6px;
  transition: width 1s ease-in-out;
}

.bar-count {
  font-size: 12px;
  color: #606266;
}

.option-input-item {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
}

.preview-info {
  margin-top: 15px;
  line-height: 1.6;
}

.description-text {
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
