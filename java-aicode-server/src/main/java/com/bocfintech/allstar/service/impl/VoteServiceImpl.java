package com.bocfintech.allstar.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bocfintech.allstar.entity.VoteOption;
import com.bocfintech.allstar.entity.VoteRecord;
import com.bocfintech.allstar.entity.VoteTask;
import com.bocfintech.allstar.mapper.VoteOptionMapper;
import com.bocfintech.allstar.mapper.VoteRecordMapper;
import com.bocfintech.allstar.mapper.VoteTaskMapper;
import com.bocfintech.allstar.service.VoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class VoteServiceImpl extends BaseServiceImpl<VoteTaskMapper, VoteTask> implements VoteService {

    @Autowired
    private VoteOptionMapper voteOptionMapper;

    @Autowired
    private VoteRecordMapper voteRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTask(VoteTask task, List<VoteOption> options) {
        task.setCreateTime(new Date());
        if (task.getStatus() == null) {
            task.setStatus("1"); // 默认进行中
        }
        this.save(task);

        if ("1".equals(task.getType()) && options != null) {
            for (VoteOption option : options) {
                option.setTaskId(task.getId());
                option.setUserId(task.getCreatorId()); // 设置选项所属用户为发起人
                option.setAuditStatus("1"); // 直接投票无需审核
                option.setVoteCount(0);
                option.setLastTime(new Date());
                voteOptionMapper.insert(option);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTask(Long taskId, Map<String, Object> params, String userId) {
        VoteTask exist = this.findById(taskId);
        if (exist == null) {
            throw new RuntimeException("任务不存在");
        }
        if (exist.getCreatorId() == null || !exist.getCreatorId().equals(userId)) {
            throw new RuntimeException("仅任务发起人可修改");
        }

        if (params.get("title") != null) {
            exist.setTitle(String.valueOf(params.get("title")));
        }
        if (params.get("type") != null) {
            exist.setType(String.valueOf(params.get("type")));
        }
        if (params.get("maxVotes") != null) {
            exist.setMaxVotes(Integer.parseInt(String.valueOf(params.get("maxVotes"))));
        }
        if (params.get("allowViewEarly") != null) {
            exist.setAllowViewEarly(String.valueOf(params.get("allowViewEarly")));
        }

        if (params.containsKey("uploadEndAt")) {
            Long uploadEndAt = parseTimestamp(params.get("uploadEndAt"));
            exist.setUploadEndAt(uploadEndAt == null ? null : new Date(uploadEndAt));
        }
        if (params.containsKey("voteEndAt")) {
            Long voteEndAt = parseTimestamp(params.get("voteEndAt"));
            exist.setVoteEndAt(voteEndAt == null ? null : new Date(voteEndAt));
        }

        this.updateById(exist);
    }

    private Long parseTimestamp(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        return Long.parseLong(text);
    }

    @Override
    public VoteTask getTaskDetail(Long taskId) {
        return this.findById(taskId);
    }

    @Override
    public VoteOption getOptionDetail(Long optionId) {
        return voteOptionMapper.selectById(optionId);
    }

    @Override
    public List<VoteOption> getOptionsByTaskId(Long taskId, String userId) {
        VoteTask task = this.findById(taskId);
        LambdaQueryWrapper<VoteOption> wrapper = Wrappers.lambdaQuery(VoteOption.class)
                .eq(VoteOption::getTaskId, taskId);

        // 如果是第二类任务（征集投票）
        if ("2".equals(task.getType())) {
            // 发起人可以看到所有作品
            if (userId.equals(task.getCreatorId())) {
                // 不加额外过滤条件，查询所有
            } else {
                // 普通用户：看到已通过的作品 OR 自己的作品
                wrapper.and(w -> w.eq(VoteOption::getAuditStatus, "1")
                        .or()
                        .eq(VoteOption::getUserId, userId));
            }
        }
        
        List<VoteOption> options = voteOptionMapper.selectList(wrapper);
        
        // 查询当前用户已投的选项ID
        List<Long> votedOptionIds = voteRecordMapper.selectList(Wrappers.lambdaQuery(VoteRecord.class)
                .eq(VoteRecord::getUserId, userId)
                .eq(VoteRecord::getTaskId, taskId)
                .eq(VoteRecord::getIsDeleted, "0"))
                .stream()
                .map(VoteRecord::getOptionId)
                .collect(Collectors.toList());

        // 隐藏票数逻辑（盲审模式）：仅在投票未截止且不是发起人时隐藏
        Date now = new Date();
        for (VoteOption option : options) {
            // 设置是否已投
            option.setVoted(votedOptionIds.contains(option.getId()));
            
            // 盲审模式处理
            if (now.before(task.getVoteEndAt()) && !userId.equals(task.getCreatorId())) {
                option.setVoteCount(null);
            }
        }
        
        return options;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void vote(String userId, Long taskId, Long optionId) {
        VoteTask task = this.findById(taskId);
        Date now = new Date();
        if (now.after(task.getVoteEndAt())) {
            throw new RuntimeException("投票已截止");
        }

        // 检查用户已投总票数
        Long count = voteRecordMapper.selectCount(Wrappers.lambdaQuery(VoteRecord.class)
                .eq(VoteRecord::getUserId, userId)
                .eq(VoteRecord::getTaskId, taskId)
                .eq(VoteRecord::getIsDeleted, "0"));

        if (count >= task.getMaxVotes()) {
            throw new RuntimeException("已达到最大投票次数限制");
        }

        // 插入投票记录
        VoteRecord record = new VoteRecord();
        record.setUserId(userId);
        record.setTaskId(taskId);
        record.setOptionId(optionId);
        record.setIsDeleted("0");
        record.setCreateTime(new Date());
        record.setLastTime(new Date());
        voteRecordMapper.insert(record);

        // 更新选项票数
        voteOptionMapper.updateVoteCount(optionId, 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeVote(String userId, Long taskId, Long optionId) {
        VoteTask task = this.findById(taskId);
        Date now = new Date();
        if (now.after(task.getVoteEndAt())) {
            throw new RuntimeException("投票已截止，无法撤回");
        }

        // 查找有效投票记录
        VoteRecord record = voteRecordMapper.selectOne(Wrappers.lambdaQuery(VoteRecord.class)
                .eq(VoteRecord::getUserId, userId)
                .eq(VoteRecord::getTaskId, taskId)
                .eq(VoteRecord::getOptionId, optionId)
                .eq(VoteRecord::getIsDeleted, "0")
                .last("LIMIT 1"));

        if (record != null) {
            record.setIsDeleted("1");
            record.setLastTime(new Date());
            voteRecordMapper.updateById(record);

            // 更新选项票数
            voteOptionMapper.updateVoteCount(optionId, -1);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadWork(VoteOption option) {
        VoteTask task = this.findById(option.getTaskId());
        Date now = new Date();
        if (now.after(task.getUploadEndAt())) {
            throw new RuntimeException("作品征集已截止");
        }

        option.setAuditStatus("0"); // 待审核
        option.setVoteCount(0);
        option.setLastTime(new Date());
        
        if (option.getId() != null) {
            voteOptionMapper.updateById(option);
        } else {
            voteOptionMapper.insert(option);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditWork(Long optionId, String auditStatus, String auditRemark) {
        VoteOption option = voteOptionMapper.selectById(optionId);
        if (option != null) {
            option.setAuditStatus(auditStatus);
            option.setAuditRemark(auditRemark);
            option.setLastTime(new Date());
            voteOptionMapper.updateById(option);
        }
    }

    @Override
    public List<VoteOption> getVoteResults(Long taskId) {
        return voteOptionMapper.selectList(Wrappers.lambdaQuery(VoteOption.class)
                .eq(VoteOption::getTaskId, taskId)
                .eq(VoteOption::getAuditStatus, "1")
                .orderByDesc(VoteOption::getVoteCount)
                .last("LIMIT 10"));
    }
}
