<template>
  <div class="vote-page">
    <aside class="left-panel card">
      <div class="left-title">投票列表</div>
      <div class="left-header">
        <el-input
          v-model.trim="searchQuery"
          placeholder="搜索任务名称"
          prefix-icon="el-icon-search"
          size="small"
          clearable
          @input="fetchTaskList"
        />
        <el-button type="primary" icon="el-icon-plus" circle size="small" @click="openCreateDialog" />
      </div>
      <div class="left-filter">
        <el-radio-group v-model="statusFilter" size="mini">
          <el-radio-button label="all">全部</el-radio-button>
          <el-radio-button label="1">进行中</el-radio-button>
          <el-radio-button label="2">已结束</el-radio-button>
        </el-radio-group>
      </div>
      <div class="task-list" v-loading="listLoading">
        <div
          v-for="task in filteredTaskList"
          :key="task.id"
          :class="['task-item', { active: currentTask && currentTask.id === task.id }]"
          @click="handleTaskClick(task)"
        >
          <div class="task-title">{{ task.title }}</div>
          <div class="task-sub">
            <span>{{ task.type === '1' ? '直接投票' : '征集投票' }}</span>
            <span>{{ formatTime(task.voteEndAt) }}</span>
          </div>
          <div class="task-tags">
            <el-tag size="mini" :type="task.type === '1' ? 'info' : 'success'">{{ task.type === '1' ? '直接投票' : '征集投票' }}</el-tag>
            <el-tag size="mini" :type="getStatusType(task.status)">{{ getStatusName(task.status) }}</el-tag>
          </div>
        </div>
        <el-empty v-if="!listLoading && filteredTaskList.length === 0" description="暂无任务" :image-size="88" />
      </div>
    </aside>

    <section class="right-panel">
      <div v-if="!currentTask" class="card empty-wrap">
        <el-empty description="请选择一个投票任务" />
      </div>

      <div v-else class="task-wrap card">
        <div class="task-header">
          <div>
            <h2>{{ currentTask.title }}</h2>
            <div class="meta-row">
              <el-tag size="mini" :type="currentTask.type === '1' ? 'info' : 'success'">{{ currentTask.type === '1' ? '直接投票' : '征集投票' }}</el-tag>
              <el-tag size="mini" :type="phaseInfo.type">{{ phaseInfo.label }}</el-tag>
              <span>每人可投 {{ currentTask.maxVotes || 1 }} 票</span>
              <span>投票截止：{{ formatTime(currentTask.voteEndAt) }}</span>
              <span v-if="currentTask.type === '2'">上传截止：{{ formatTime(currentTask.uploadEndAt) }}</span>
            </div>
          </div>
          <div class="header-right">
            <el-button v-if="isCreator" type="primary" size="small" icon="el-icon-edit" @click="openEditTaskDialog">
              修改任务
            </el-button>
            <div class="ticket-box">
              <span>已投 {{ votedIds.length }} 票 / 剩余 {{ remainVotes }} 票</span>
              <el-progress :percentage="usedVotesPercent" :stroke-width="8" :show-text="false" />
            </div>
          </div>
        </div>

        <el-alert :title="phaseInfo.tip" :type="phaseInfo.type" :closable="false" show-icon class="phase-alert" />

        <div v-if="showResultBoard" class="result-board">
          <div class="board-title">
            <h3>投票结果</h3>
            <span>Top 10</span>
          </div>
          <div class="podium">
            <div v-if="podiumData[1]" class="podium-item silver">
              <div class="rank">2</div>
              <img :src="podiumData[1].coverUrl || defaultCover" class="podium-cover" />
              <div class="podium-name">{{ podiumData[1].title }}</div>
              <div class="podium-votes">{{ formatVoteCount(podiumData[1].voteCount) }}</div>
            </div>
            <div v-if="podiumData[0]" class="podium-item gold">
              <div class="rank">1</div>
              <img :src="podiumData[0].coverUrl || defaultCover" class="podium-cover" />
              <div class="podium-name">{{ podiumData[0].title }}</div>
              <div class="podium-votes">{{ formatVoteCount(podiumData[0].voteCount) }}</div>
            </div>
            <div v-if="podiumData[2]" class="podium-item bronze">
              <div class="rank">3</div>
              <img :src="podiumData[2].coverUrl || defaultCover" class="podium-cover" />
              <div class="podium-name">{{ podiumData[2].title }}</div>
              <div class="podium-votes">{{ formatVoteCount(podiumData[2].voteCount) }}</div>
            </div>
          </div>

          <div class="chart">
            <div v-for="(item, index) in top10Results" :key="item.id" class="bar-item">
              <div class="bar-title">Top {{ index + 1 }} · {{ item.title }}</div>
              <div class="bar-row">
                <div class="bar-bg">
                  <div class="bar-fill" :style="{ width: getBarWidth(item.voteCount), background: getBarColor(index) }"></div>
                </div>
                <span>{{ formatVoteCount(item.voteCount) }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="option-area" v-loading="optionsLoading">
          <div class="option-header">
            <h3>{{ currentTask.type === '1' ? '投票选项' : '作品列表' }}</h3>
            <div class="option-actions-header">
              <el-input
                v-if="currentTask.type === '2'"
                v-model.trim="workSearch"
                size="small"
                clearable
                placeholder="搜索作品名称/作者/编号"
                prefix-icon="el-icon-search"
                class="work-search"
              />
              <el-button v-if="canUploadWork" type="primary" size="small" @click="openUploadDialog">上传作品</el-button>
            </div>
          </div>

          <div v-if="displayedOptions.length === 0" class="empty-options">
            <el-empty description="当前暂无可展示内容" :image-size="96" />
          </div>

          <div v-else class="card-grid">
            <el-card v-for="option in displayedOptions" :key="option.id" class="work-card" :body-style="{ padding: '0' }" shadow="hover">
              <div class="cover-wrap" @click="previewWork(option)">
                <img :src="option.coverUrl || defaultCover" class="cover-img" />
                <div class="cover-mask"><i class="el-icon-view"></i> 查看详情</div>
              </div>
              <div class="work-body">
                <div class="work-title">{{ option.title }}</div>
                <div class="work-author" v-if="currentTask.type === '2'">作者：{{ option.authorName || '-' }}</div>
                <div class="work-tags">
                  <el-tag v-if="currentTask.type === '2'" size="mini" :type="getAuditStatusType(option.auditStatus)">
                    {{ getAuditStatusName(option.auditStatus) }}
                  </el-tag>
                  <el-tag v-if="shouldShowVoteCount(option)" size="mini" type="danger">{{ formatVoteCount(option.voteCount) }}</el-tag>
                </div>
                <div class="work-footer">
                  <el-button
                    v-if="canVoteOption(option)"
                    :type="isVoted(option.id) ? 'danger' : 'primary'"
                    size="mini"
                    @click="handleVote(option)"
                  >
                    {{ isVoted(option.id) ? '撤回改投' : '投票' }}
                  </el-button>
                  <el-button
                    v-if="canEditOption(option)"
                    type="text"
                    size="mini"
                    @click="editWork(option)"
                  >
                    编辑作品
                  </el-button>
                  <el-button
                    v-if="canAuditOption(option)"
                    type="warning"
                    size="mini"
                    @click="handleAudit(option)"
                  >
                    审核
                  </el-button>
                </div>
              </div>
            </el-card>
          </div>
        </div>
      </div>
    </section>

    <el-dialog :title="editingTaskId ? '修改投票任务' : '发起投票任务'" :visible.sync="showCreateDialog" width="640px" @close="resetCreateForm">
      <el-form :model="createForm" label-width="120px" size="small">
        <el-form-item label="任务名称">
          <el-input v-model.trim="createForm.title" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="任务类型">
          <el-radio-group v-model="createForm.type">
            <el-radio label="1">直接投票</el-radio>
            <el-radio label="2">征集投票</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="每人总票数">
          <el-input-number v-model="createForm.maxVotes" :min="1" :max="20" />
        </el-form-item>
        <el-form-item v-if="createForm.type === '2'" label="提前查看作品">
          <el-switch v-model="createForm.allowViewEarly" active-value="1" inactive-value="0" />
        </el-form-item>
        <el-form-item v-if="createForm.type === '2'" label="上传截止时间">
          <el-date-picker v-model="createForm.uploadEndAt" type="datetime" placeholder="请选择上传截止时间" value-format="timestamp" />
        </el-form-item>
        <el-form-item label="投票截止时间">
          <el-date-picker v-model="createForm.voteEndAt" type="datetime" placeholder="请选择投票截止时间" value-format="timestamp" />
        </el-form-item>

        <template v-if="createForm.type === '1'">
          <el-divider>投票选项</el-divider>
          <div v-for="(opt, index) in createForm.options" :key="index" class="option-input">
            <el-input v-model.trim="opt.title" placeholder="请输入选项名称" maxlength="60" />
            <el-button type="danger" icon="el-icon-delete" circle @click="removeOption(index)" />
          </div>
          <el-button type="text" icon="el-icon-plus" @click="addOption">新增选项</el-button>
        </template>
      </el-form>
      <div slot="footer">
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateTask">{{ editingTaskId ? '保存修改' : '创建任务' }}</el-button>
      </div>
    </el-dialog>

    <el-dialog :title="uploadForm.id ? '修改作品' : '上传作品'" :visible.sync="showUploadDialog" width="660px" @close="resetUploadForm">
      <el-form :model="uploadForm" label-width="110px" size="small">
        <el-form-item label="作品名称">
          <el-input v-model.trim="uploadForm.title" maxlength="80" show-word-limit />
        </el-form-item>
        <el-form-item label="作者姓名">
          <el-input v-model.trim="uploadForm.authorName" maxlength="30" />
        </el-form-item>
        <el-form-item label="队伍名称">
          <el-input v-model.trim="uploadForm.teamName" maxlength="60" />
        </el-form-item>
        <el-form-item label="作品介绍">
          <el-input v-model.trim="uploadForm.description" type="textarea" :rows="4" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model.trim="uploadForm.tags" placeholder="多个标签用逗号分隔" />
        </el-form-item>
        <el-form-item label="封面图">
          <el-upload action="/api/file/upload" :on-success="handleCoverSuccess" :limit="1" :file-list="coverFileList" list-type="picture">
            <el-button size="small" type="primary">上传封面</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="视频信息">
          <el-upload action="/api/file/upload" :on-success="handleVideoSuccess" :on-remove="handleVideoRemove" :file-list="videoFileList" multiple>
            <el-button size="small" type="primary">上传视频</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="附件">
          <el-upload action="/api/file/upload" :on-success="handleAttachmentSuccess" :limit="1" :file-list="attachmentFileList">
            <el-button size="small" type="primary">上传附件</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUploadWork">提交</el-button>
      </div>
    </el-dialog>

    <el-dialog :title="previewData.title || '作品详情'" :visible.sync="showPreview" width="860px">
      <div class="preview-layout">
        <div class="preview-main">
          <img :src="previewData.coverUrl || defaultCover" class="preview-cover" />
          <div v-if="videoList.length > 0" class="video-list">
            <video v-for="(v, i) in videoList" :key="i" :src="v" controls class="video-item"></video>
          </div>
        </div>
        <div class="preview-side">
          <p><strong>作者：</strong>{{ previewData.authorName || '-' }}</p>
          <p><strong>队伍：</strong>{{ previewData.teamName || '-' }}</p>
          <p>
            <strong>标签：</strong>
            <el-tag v-for="tag in tagList" :key="tag" size="mini" style="margin-right: 6px;">{{ tag }}</el-tag>
            <span v-if="tagList.length === 0">-</span>
          </p>
          <p><strong>介绍：</strong>{{ previewData.description || '-' }}</p>
          <p>
            <strong>审核：</strong>
            <el-tag size="mini" :type="getAuditStatusType(previewData.auditStatus)">{{ getAuditStatusName(previewData.auditStatus) }}</el-tag>
          </p>
          <p v-if="previewData.auditStatus === '2' && previewData.auditRemark" class="reject-text">
            <strong>驳回理由：</strong>{{ previewData.auditRemark }}
          </p>
          <p v-if="previewData.attachmentUrl">
            <strong>附件：</strong>
            <el-link :href="previewData.attachmentUrl" target="_blank" type="primary">下载</el-link>
          </p>
        </div>
      </div>
      <div slot="footer">
        <el-button v-if="canEditOption(previewData)" type="primary" size="small" @click="editWorkInPreview">修改作品</el-button>
        <el-button v-if="canAuditOption(previewData)" type="warning" size="small" @click="handleAudit(previewData)">审核</el-button>
        <el-button size="small" @click="showPreview = false">关闭</el-button>
      </div>
    </el-dialog>

    <el-dialog title="审核作品" :visible.sync="showAuditDialog" width="420px">
      <el-form :model="auditForm" label-width="90px" size="small">
        <el-form-item label="审核结果">
          <el-radio-group v-model="auditForm.auditStatus">
            <el-radio label="1">通过</el-radio>
            <el-radio label="2">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="auditForm.auditStatus === '2'" label="驳回理由">
          <el-input v-model.trim="auditForm.auditRemark" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="showAuditDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAudit">提交审核</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  getTaskList,
  createTask,
  updateTask,
  getOptions,
  vote,
  revokeVote,
  uploadWork,
  getResults,
  auditWork
} from '@/api/vote';
import { getCurrentEmpNo } from '@/utils/currentUser';

export default {
  name: 'VoteManagement',
  data() {
    return {
      currentEmpNo: getCurrentEmpNo(),
      nowTs: Date.now(),
      tickTimer: null,
      searchQuery: '',
      statusFilter: 'all',
      workSearch: '',
      taskList: [],
      currentTask: null,
      options: [],
      results: [],
      votedIds: [],
      listLoading: false,
      optionsLoading: false,
      showCreateDialog: false,
      editingTaskId: null,
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
        options: [{ title: '' }, { title: '' }]
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
    };
  },
  computed: {
    filteredTaskList() {
      if (this.statusFilter === 'all') return this.taskList;
      return this.taskList.filter((task) => String(task.status) === this.statusFilter);
    },
    phaseInfo() {
      if (!this.currentTask) return { type: 'info', label: '未开始', tip: '' };
      if (this.isEnded(this.currentTask)) {
        return { type: 'success', label: '已结束', tip: '投票已截止，当前展示最终结果与票数统计。' };
      }
      if (this.currentTask.type === '2' && !this.isUploadEnded(this.currentTask)) {
        return { type: 'warning', label: '征集阶段', tip: '当前可上传/修改作品。投票可按任务配置并行进行。' };
      }
      return { type: 'info', label: '投票中', tip: '当前处于投票阶段，支持改投；截止前默认隐藏总票数。' };
    },
    isCreator() {
      return this.currentTask && String(this.currentTask.creatorId) === this.currentEmpNo;
    },
    remainVotes() {
      if (!this.currentTask) return 0;
      const maxVotes = Number(this.currentTask.maxVotes || 1);
      return Math.max(maxVotes - this.votedIds.length, 0);
    },
    usedVotesPercent() {
      if (!this.currentTask) return 0;
      const maxVotes = Number(this.currentTask.maxVotes || 1);
      if (maxVotes <= 0) return 0;
      return Math.min(Math.round((this.votedIds.length / maxVotes) * 100), 100);
    },
    canUploadWork() {
      return this.currentTask && this.currentTask.type === '2' && !this.isUploadEnded(this.currentTask);
    },
    showResultBoard() {
      return this.currentTask && this.isEnded(this.currentTask) && this.top10Results.length > 0;
    },
    top10Results() {
      const sorted = [...this.results].sort((a, b) => (b.voteCount || 0) - (a.voteCount || 0));
      return sorted.slice(0, 10);
    },
    podiumData() {
      return this.top10Results.slice(0, 3);
    },
    displayedOptions() {
      const keyword = this.workSearch.toLowerCase();
      const list = this.options.filter((opt) => this.canShowOption(opt));
      const filtered = keyword
        ? list.filter((opt) => {
            const source = `${opt.id || ''} ${opt.title || ''} ${opt.authorName || ''}`.toLowerCase();
            return source.includes(keyword);
          })
        : list;

      if (this.isEnded(this.currentTask)) {
        return [...filtered].sort((a, b) => (b.voteCount || 0) - (a.voteCount || 0));
      }
      return filtered;
    },
    videoList() {
      if (!this.previewData.videoUrl) return [];
      try {
        const list = JSON.parse(this.previewData.videoUrl);
        return Array.isArray(list) ? list : [this.previewData.videoUrl];
      } catch (e) {
        return [this.previewData.videoUrl];
      }
    },
    tagList() {
      if (!this.previewData.tags) return [];
      return this.previewData.tags.split(/[,，]/).map((v) => v.trim()).filter(Boolean);
    }
  },
  mounted() {
    this.fetchTaskList();
    this.tickTimer = setInterval(() => {
      this.nowTs = Date.now();
    }, 1000);
    window.addEventListener('emp-no-change', this.handleEmpNoChange);
  },
  beforeDestroy() {
    if (this.tickTimer) {
      clearInterval(this.tickTimer);
      this.tickTimer = null;
    }
    window.removeEventListener('emp-no-change', this.handleEmpNoChange);
  },
  methods: {
    handleEmpNoChange(event) {
      const empNo = event && event.detail ? event.detail.empNo : getCurrentEmpNo();
      this.currentEmpNo = empNo;
      if (this.currentTask) {
        this.fetchOptions();
      }
    },
    async fetchTaskList() {
      this.listLoading = true;
      try {
        const res = await getTaskList({ title: this.searchQuery });
        this.taskList = res.data || [];
      } catch (error) {
        this.$message.error('获取任务列表失败: ' + error.message);
      } finally {
        this.listLoading = false;
      }
    },
    async handleTaskClick(task) {
      this.currentTask = { ...task };
      this.workSearch = '';
      this.options = [];
      this.results = [];
      this.votedIds = [];
      await this.fetchOptions();
      if (this.isEnded(this.currentTask)) {
        await this.fetchResults();
      }
    },
    async fetchOptions() {
      if (!this.currentTask) return;
      this.optionsLoading = true;
      try {
        const res = await getOptions(this.currentTask.id);
        this.options = res.data || [];
        this.votedIds = this.options.filter((opt) => !!opt.voted).map((opt) => opt.id);
      } catch (error) {
        this.$message.error('获取选项失败: ' + error.message);
      } finally {
        this.optionsLoading = false;
      }
    },
    async fetchResults() {
      if (!this.currentTask) return;
      try {
        const res = await getResults(this.currentTask.id);
        this.results = res.data || [];
      } catch (error) {
        this.$message.error('获取结果失败: ' + error.message);
      }
    },
    openCreateDialog() {
      this.resetCreateForm();
      this.showCreateDialog = true;
    },
    openEditTaskDialog() {
      if (!this.currentTask || !this.isCreator) return;
      this.editingTaskId = this.currentTask.id;
      this.createForm = {
        title: this.currentTask.title || '',
        type: String(this.currentTask.type || '1'),
        maxVotes: Number(this.currentTask.maxVotes || 1),
        allowViewEarly: String(this.currentTask.allowViewEarly || '0'),
        uploadEndAt: this.currentTask.uploadEndAt ? Number(this.currentTask.uploadEndAt) : null,
        voteEndAt: this.currentTask.voteEndAt ? Number(this.currentTask.voteEndAt) : null,
        options: [{ title: '' }, { title: '' }]
      };
      this.showCreateDialog = true;
    },
    resetCreateForm() {
      this.editingTaskId = null;
      this.createForm = {
        title: '',
        type: '1',
        maxVotes: 1,
        allowViewEarly: '0',
        uploadEndAt: null,
        voteEndAt: null,
        options: [{ title: '' }, { title: '' }]
      };
    },
    addOption() {
      this.createForm.options.push({ title: '' });
    },
    removeOption(index) {
      if (this.createForm.options.length <= 2) {
        this.$message.warning('至少保留两个选项');
        return;
      }
      this.createForm.options.splice(index, 1);
    },
    validateCreateForm() {
      if (!this.createForm.title) return '请输入任务名称';
      if (!this.createForm.voteEndAt) return '请选择投票截止时间';
      if (this.createForm.type === '2' && !this.createForm.uploadEndAt) return '请选择上传截止时间';
      if (this.createForm.type === '2' && Number(this.createForm.uploadEndAt) >= Number(this.createForm.voteEndAt)) {
        return '上传截止时间必须早于投票截止时间';
      }
      if (this.createForm.type === '1') {
        const validOptions = this.createForm.options.map((opt) => (opt.title || '').trim()).filter(Boolean);
        if (validOptions.length < 2) return '直接投票任务至少需要两个有效选项';
      }
      return '';
    },
    async handleCreateTask() {
      const errorMsg = this.validateCreateForm();
      if (errorMsg) {
        this.$message.warning(errorMsg);
        return;
      }
      const payload = { ...this.createForm };
      payload.options = (payload.options || []).map((opt) => ({ title: (opt.title || '').trim() })).filter((opt) => !!opt.title);
      try {
        if (this.editingTaskId) {
          await updateTask(this.editingTaskId, payload);
          this.$message.success('任务更新成功');
        } else {
          await createTask(payload);
          this.$message.success('创建成功');
        }
        this.showCreateDialog = false;
        await this.fetchTaskList();
        if (this.currentTask) {
          const latest = this.taskList.find((item) => item.id === this.currentTask.id);
          if (latest) {
            this.currentTask = { ...latest };
          }
        }
      } catch (error) {
        this.$message.error((this.editingTaskId ? '更新失败: ' : '创建失败: ') + error.message);
      }
    },
    openUploadDialog() {
      this.resetUploadForm();
      this.showUploadDialog = true;
    },
    resetUploadForm() {
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
      };
      this.coverFileList = [];
      this.videoFileList = [];
      this.attachmentFileList = [];
    },
    validateUploadForm() {
      if (!this.uploadForm.title) return '请输入作品名称';
      if (!this.uploadForm.authorName) return '请输入作者姓名';
      if (!this.uploadForm.description) return '请输入作品介绍';
      if (!this.uploadForm.coverUrl) return '请上传封面图';
      if (this.videoFileList.length === 0 && !this.uploadForm.videoUrl) return '请上传至少一个视频';
      return '';
    },
    async handleUploadWork() {
      if (!this.currentTask) return;
      const errorMsg = this.validateUploadForm();
      if (errorMsg) {
        this.$message.warning(errorMsg);
        return;
      }
      const videoUrls = this.videoFileList.map((file) => (file.response ? file.response.data : file.url)).filter(Boolean);
      const payload = {
        ...this.uploadForm,
        taskId: this.currentTask.id,
        userId: this.currentEmpNo,
        videoUrl: JSON.stringify(videoUrls.length > 0 ? videoUrls : this.safeParseVideoUrl(this.uploadForm.videoUrl))
      };
      try {
        await uploadWork(payload);
        this.$message.success(this.uploadForm.id ? '作品修改成功，已重新进入待审核' : '提交成功，请等待审核');
        this.showUploadDialog = false;
        await this.fetchOptions();
      } catch (error) {
        this.$message.error('提交失败: ' + error.message);
      }
    },
    handleCoverSuccess(res) {
      this.uploadForm.coverUrl = res.data;
    },
    handleVideoSuccess(res, file, fileList) {
      this.videoFileList = fileList;
    },
    handleVideoRemove(file, fileList) {
      this.videoFileList = fileList;
    },
    handleAttachmentSuccess(res) {
      this.uploadForm.attachmentUrl = res.data;
    },
    editWork(option) {
      this.uploadForm = { ...option };
      this.coverFileList = option.coverUrl ? [{ name: '封面', url: option.coverUrl }] : [];
      this.attachmentFileList = option.attachmentUrl ? [{ name: '附件', url: option.attachmentUrl }] : [];
      const videos = this.safeParseVideoUrl(option.videoUrl);
      this.videoFileList = videos.map((url, index) => ({ name: `视频${index + 1}`, url }));
      this.showUploadDialog = true;
    },
    canShowOption(option) {
      if (!this.currentTask) return false;
      if (this.currentTask.type === '1') return true;
      if (option.auditStatus === '1') return true;
      if (this.isCreator) return true;
      if (String(option.userId) === this.currentEmpNo) return true;
      if (this.isEnded(this.currentTask)) return true;
      return false;
    },
    canEditOption(option) {
      if (!this.currentTask || this.currentTask.type !== '2') return false;
      return String(option.userId) === this.currentEmpNo && !this.isUploadEnded(this.currentTask);
    },
    canAuditOption(option) {
      return !!(this.isCreator && this.currentTask && this.currentTask.type === '2' && String(option.auditStatus) === '0');
    },
    shouldShowVoteCount(option) {
      if (!this.currentTask) return false;
      if (this.isEnded(this.currentTask)) return true;
      if (this.currentTask.type === '2' && String(this.currentTask.allowViewEarly) === '1') return true;
      return this.isCreator;
    },
    canVoteOption(option) {
      if (!this.currentTask || this.isEnded(this.currentTask)) return false;
      if (this.currentTask.type === '2' && String(option.auditStatus) !== '1') return false;
      return true;
    },
    async handleVote(option) {
      if (!this.currentTask) return;
      if (this.isVoted(option.id)) {
        try {
          await revokeVote(this.currentTask.id, option.id);
          this.$message.success('已撤回，可改投其他作品');
          this.votedIds = this.votedIds.filter((id) => id !== option.id);
          await this.fetchOptions();
        } catch (error) {
          this.$message.error('撤回失败: ' + error.message);
        }
        return;
      }
      if (this.remainVotes <= 0) {
        this.$message.warning('可用票数不足，请先撤回后再改投');
        return;
      }
      try {
        await vote(this.currentTask.id, option.id);
        this.$message.success('投票成功');
        this.votedIds.push(option.id);
        await this.fetchOptions();
      } catch (error) {
        this.$message.error('投票失败: ' + error.message);
      }
    },
    previewWork(option) {
      this.previewData = option || {};
      this.showPreview = true;
    },
    editWorkInPreview() {
      this.editWork(this.previewData);
      this.showPreview = false;
    },
    handleAudit(option) {
      this.auditForm.optionId = option.id;
      this.auditForm.auditStatus = '1';
      this.auditForm.auditRemark = '';
      this.showAuditDialog = true;
      this.showPreview = false;
    },
    async submitAudit() {
      if (this.auditForm.auditStatus === '2' && !this.auditForm.auditRemark) {
        this.$message.warning('请填写驳回理由');
        return;
      }
      try {
        await auditWork(this.auditForm);
        this.$message.success('审核完成');
        this.showAuditDialog = false;
        await this.fetchOptions();
      } catch (error) {
        this.$message.error('审核失败: ' + error.message);
      }
    },
    isVoted(optionId) {
      return this.votedIds.includes(optionId);
    },
    isEnded(task) {
      if (!task || !task.voteEndAt) return false;
      return this.nowTs > Number(task.voteEndAt);
    },
    isUploadEnded(task) {
      if (!task || !task.uploadEndAt) return false;
      return this.nowTs > Number(task.uploadEndAt);
    },
    safeParseVideoUrl(videoUrl) {
      if (!videoUrl) return [];
      try {
        const arr = JSON.parse(videoUrl);
        return Array.isArray(arr) ? arr : [videoUrl];
      } catch (e) {
        return [videoUrl];
      }
    },
    formatVoteCount(voteCount) {
      return `${Number(voteCount || 0)} 票`;
    },
    formatTime(time) {
      if (!time) return '-';
      const ts = this.normalizeTimestamp(time);
      if (!ts) return '-';
      return new Date(ts).toLocaleString();
    },
    normalizeTimestamp(time) {
      if (time === null || time === undefined || time === '') return 0;
      if (typeof time === 'number') {
        return time < 1000000000000 ? time * 1000 : time;
      }
      if (typeof time === 'string') {
        const trimmed = time.trim();
        if (!trimmed) return 0;
        if (/^\d+$/.test(trimmed)) {
          const numeric = Number(trimmed);
          return numeric < 1000000000000 ? numeric * 1000 : numeric;
        }
        const parsed = Date.parse(trimmed);
        return Number.isNaN(parsed) ? 0 : parsed;
      }
      return 0;
    },
    getStatusName(status) {
      const map = { '0': '草稿', '1': '进行中', '2': '已结束' };
      return map[String(status)] || '未知';
    },
    getStatusType(status) {
      const map = { '0': 'info', '1': 'success', '2': 'danger' };
      return map[String(status)] || 'info';
    },
    getAuditStatusName(status) {
      const map = { '0': '待审核', '1': '已通过', '2': '已驳回' };
      return map[String(status)] || '未知';
    },
    getAuditStatusType(status) {
      const map = { '0': 'info', '1': 'success', '2': 'danger' };
      return map[String(status)] || 'info';
    },
    getBarWidth(count) {
      if (!this.top10Results.length) return '0%';
      const max = Number(this.top10Results[0].voteCount || 1);
      const current = Number(count || 0);
      return `${Math.max((current / max) * 100, 4)}%`;
    },
    getBarColor(index) {
      const rankColors = ['#FFD700', '#C0C0C0', '#CD7F32'];
      return rankColors[index] || '#409EFF';
    }
  }
};
</script>

<style scoped>
.vote-page {
  display: flex;
  gap: 16px;
  height: 100%;
  padding: 14px;
  box-sizing: border-box;
  overflow: hidden;
  background: linear-gradient(180deg, #f7f9ff 0%, #eef3ff 100%);
}

.card {
  background: #fff;
  border-radius: 14px;
  border: 1px solid #e8eefb;
  box-shadow: 0 8px 18px rgba(39, 66, 144, 0.06);
}

.left-panel {
  width: 360px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.left-title {
  padding: 14px 14px 0;
  font-size: 16px;
  font-weight: 700;
  color: #23324a;
}

.left-header {
  display: flex;
  gap: 10px;
  padding: 10px 14px 14px;
  border-bottom: 1px solid #f0f4ff;
}

.left-filter {
  padding: 10px 14px;
  border-bottom: 1px solid #f0f4ff;
}

.task-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.task-item {
  border: 1px solid #edf2ff;
  border-radius: 10px;
  padding: 10px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.task-item:hover {
  transform: translateY(-1px);
  border-color: #cfe0ff;
}

.task-item.active {
  background: #edf5ff;
  border-color: #77afff;
}

.task-title {
  font-weight: 700;
  color: #263248;
  margin-bottom: 6px;
}

.task-sub {
  font-size: 12px;
  color: #8a94a6;
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.task-tags {
  display: flex;
  gap: 6px;
}

.right-panel {
  flex: 1;
  min-width: 0;
}

.empty-wrap,
.task-wrap {
  height: 100%;
}

.empty-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
}

.task-wrap {
  overflow-y: auto;
  padding: 18px;
  box-sizing: border-box;
}

.task-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.task-header h2 {
  margin: 0 0 10px 0;
  color: #1f2d4d;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: #637088;
  font-size: 13px;
  align-items: center;
}

.header-right {
  min-width: 260px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
}

.ticket-box {
  border: 1px solid #e6eeff;
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 13px;
  color: #4b5a78;
}

.phase-alert {
  margin-top: 14px;
}

.result-board {
  margin-top: 18px;
  padding: 16px;
  border-radius: 12px;
  border: 1px solid #ebf1ff;
  background: #fdfefe;
}

.board-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.board-title h3 {
  margin: 0;
}

.podium {
  display: flex;
  justify-content: center;
  align-items: flex-end;
  gap: 20px;
  margin: 26px 0;
  min-height: 220px;
}

.podium-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 130px;
  position: relative;
}

.podium-cover {
  width: 86px;
  height: 86px;
  border-radius: 50%;
  border: 4px solid #fff;
  object-fit: cover;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
}

.rank {
  position: absolute;
  top: -18px;
  font-size: 26px;
  font-weight: 800;
}

.gold .rank {
  color: #ffd700;
}

.silver .rank {
  color: #c0c0c0;
}

.bronze .rank {
  color: #cd7f32;
}

.podium-item::after {
  content: '';
  width: 100%;
  border-radius: 10px 10px 0 0;
}

.gold {
  height: 220px;
}

.silver {
  height: 188px;
}

.bronze {
  height: 160px;
}

.gold::after {
  height: 108px;
  background: #fff5d0;
}

.silver::after {
  height: 82px;
  background: #f2f4f8;
}

.bronze::after {
  height: 62px;
  background: #f6ebe4;
}

.podium-name {
  margin-top: 10px;
  font-weight: 700;
  text-align: center;
  max-width: 120px;
}

.podium-votes {
  font-size: 13px;
  color: #f56c6c;
}

.chart {
  max-width: 760px;
  margin: 0 auto;
}

.bar-item {
  margin-bottom: 12px;
}

.bar-title {
  margin-bottom: 6px;
  color: #435168;
  font-size: 13px;
}

.bar-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.bar-bg {
  flex: 1;
  height: 12px;
  background: #edf2ff;
  border-radius: 20px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 20px;
  transition: width 0.4s ease;
}

.option-area {
  margin-top: 20px;
}

.option-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.option-actions-header {
  display: flex;
  gap: 10px;
  align-items: center;
}

.work-search {
  width: 260px;
}

.empty-options {
  padding: 30px 0 10px;
}

.card-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 14px;
}

.work-card {
  border-radius: 12px;
  overflow: hidden;
}

.cover-wrap {
  position: relative;
  cursor: pointer;
}

.cover-img {
  width: 100%;
  height: 150px;
  object-fit: cover;
  display: block;
}

.cover-mask {
  position: absolute;
  inset: 0;
  background: rgba(20, 33, 61, 0.45);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s ease;
  font-size: 13px;
}

.cover-wrap:hover .cover-mask {
  opacity: 1;
}

.work-body {
  padding: 10px 12px 12px;
}

.work-title {
  font-weight: 700;
  color: #1f2d45;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.work-author {
  font-size: 12px;
  color: #7e8798;
  margin-top: 4px;
}

.work-tags {
  margin-top: 8px;
  display: flex;
  gap: 6px;
  min-height: 24px;
}

.work-footer {
  margin-top: 10px;
  display: flex;
  gap: 8px;
  align-items: center;
}

.option-input {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
}

.preview-layout {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 14px;
}

.preview-cover {
  width: 100%;
  max-height: 320px;
  object-fit: cover;
  border-radius: 8px;
}

.video-list {
  margin-top: 12px;
}

.video-item {
  width: 100%;
  border-radius: 8px;
  margin-bottom: 10px;
  background: #000;
}

.preview-side p {
  margin: 0 0 10px;
  color: #49546a;
  line-height: 1.6;
}

.reject-text {
  color: #f56c6c;
}

@media (max-width: 1100px) {
  .vote-page {
    flex-direction: column;
    height: auto;
  }
  .left-panel {
    width: 100%;
    max-height: 300px;
  }
  .preview-layout {
    grid-template-columns: 1fr;
  }
}
</style>
