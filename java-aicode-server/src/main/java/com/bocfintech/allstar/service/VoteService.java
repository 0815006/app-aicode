package com.bocfintech.allstar.service;

import com.bocfintech.allstar.entity.VoteOption;
import com.bocfintech.allstar.entity.VoteTask;
import com.bocfintech.allstar.entity.MyPage;

import java.util.List;
import java.util.Map;

public interface VoteService extends BaseService<VoteTask> {

    /**
     * 创建投票任务
     */
    void createTask(VoteTask task, List<VoteOption> options);

    /**
     * 更新投票任务（仅发起人）
     */
    void updateTask(Long taskId, Map<String, Object> params, String userId);

    /**
     * 获取任务详情（包含选项）
     */
    VoteTask getTaskDetail(Long taskId);

    /**
     * 获取单个选项/作品详情
     */
    VoteOption getOptionDetail(Long optionId);

    /**
     * 获取任务下的选项/作品列表
     */
    List<VoteOption> getOptionsByTaskId(Long taskId, String userId);

    /**
     * 投票
     */
    void vote(String userId, Long taskId, Long optionId);

    /**
     * 撤回投票（改投）
     */
    void revokeVote(String userId, Long taskId, Long optionId);

    /**
     * 上传作品（第二类任务）
     */
    void uploadWork(VoteOption option);

    /**
     * 审核作品
     */
    void auditWork(Long optionId, String auditStatus, String auditRemark);

    /**
     * 获取投票结果（Top 10）
     */
    List<VoteOption> getVoteResults(Long taskId);
}
