package com.bocfintech.allstar.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bocfintech.allstar.bean.ResultBean;
import com.bocfintech.allstar.entity.VoteOption;
import com.bocfintech.allstar.entity.VoteTask;
import com.bocfintech.allstar.service.VoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vote")
@Api(tags = "投票管理接口")
public class VoteController {

    @Autowired
    private VoteService voteService;

    @GetMapping("/tasks")
    @ApiOperation("获取投票任务列表")
    public ResultBean<List<VoteTask>> getTasks(@RequestParam(required = false) String title,
                                               @RequestParam(required = false) String status) {
        LambdaQueryWrapper<VoteTask> wrapper = Wrappers.lambdaQuery(VoteTask.class);
        if (title != null && !title.isEmpty()) {
            wrapper.like(VoteTask::getTitle, title);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(VoteTask::getStatus, status);
        }
        wrapper.orderByDesc(VoteTask::getCreateTime);
        return ResultBean.success(voteService.list(wrapper));
    }

    @PostMapping("/tasks")
    @ApiOperation("创建投票任务")
    public ResultBean createTask(@RequestBody Map<String, Object> params,
                                 @RequestHeader("token") String userId) {
        try {
            VoteTask task = new VoteTask();
            task.setTitle((String) params.get("title"));
            task.setType((String) params.get("type"));
            task.setMaxVotes(parseInteger(params.get("maxVotes")));
            task.setAllowViewEarly((String) params.get("allowViewEarly"));
            task.setCreatorId(userId);
            
            // 处理时间
            if (params.get("uploadEndAt") != null) {
                task.setUploadEndAt(new java.util.Date(parseLong(params.get("uploadEndAt"))));
            }
            if (params.get("voteEndAt") != null) {
                task.setVoteEndAt(new java.util.Date(parseLong(params.get("voteEndAt"))));
            }

            List<VoteOption> options = null;
            if (params.get("options") != null) {
                // 这里简化处理，实际可能需要更复杂的转换
                options = com.alibaba.fastjson.JSON.parseArray(
                        com.alibaba.fastjson.JSON.toJSONString(params.get("options")), 
                        VoteOption.class
                );
            }

            voteService.createTask(task, options);
            return ResultBean.success();
        } catch (Exception e) {
            return ResultBean.error("创建失败：" + e.getMessage());
        }
    }

    @PutMapping("/tasks/{id}")
    @ApiOperation("更新投票任务")
    public ResultBean updateTask(@PathVariable Long id,
                                 @RequestBody Map<String, Object> params,
                                 @RequestHeader("token") String userId) {
        try {
            voteService.updateTask(id, params, userId);
            return ResultBean.success();
        } catch (Exception e) {
            return ResultBean.error("更新失败：" + e.getMessage());
        }
    }

    @GetMapping("/tasks/{id}")
    @ApiOperation("获取任务详情")
    public ResultBean<VoteTask> getTaskDetail(@PathVariable Long id) {
        try {
            return ResultBean.success(voteService.getTaskDetail(id));
        } catch (Exception e) {
            return ResultBean.error("获取详情失败：" + e.getMessage());
        }
    }

    @GetMapping("/options/{id}")
    @ApiOperation("获取单个选项/作品详情")
    public ResultBean<VoteOption> getOptionDetail(@PathVariable Long id) {
        try {
            return ResultBean.success(voteService.getOptionDetail(id));
        } catch (Exception e) {
            return ResultBean.error("获取作品详情失败：" + e.getMessage());
        }
    }

    @GetMapping("/options")
    @ApiOperation("获取选项/作品列表")
    public ResultBean<List<VoteOption>> getOptions(@RequestParam Long taskId,
                                                   @RequestHeader("token") String userId) {
        try {
            return ResultBean.success(voteService.getOptionsByTaskId(taskId, userId));
        } catch (Exception e) {
            return ResultBean.error("获取列表失败：" + e.getMessage());
        }
    }

    @PostMapping("/vote")
    @ApiOperation("投票")
    public ResultBean vote(@RequestBody Map<String, Object> params,
                           @RequestHeader("token") String userId) {
        try {
            Long taskId = Long.valueOf(params.get("taskId").toString());
            Long optionId = Long.valueOf(params.get("optionId").toString());
            voteService.vote(userId, taskId, optionId);
            return ResultBean.success();
        } catch (Exception e) {
            return ResultBean.error(e.getMessage());
        }
    }

    @PostMapping("/revoke")
    @ApiOperation("撤回投票")
    public ResultBean revoke(@RequestBody Map<String, Object> params,
                             @RequestHeader("token") String userId) {
        try {
            Long taskId = Long.valueOf(params.get("taskId").toString());
            Long optionId = Long.valueOf(params.get("optionId").toString());
            voteService.revokeVote(userId, taskId, optionId);
            return ResultBean.success();
        } catch (Exception e) {
            return ResultBean.error(e.getMessage());
        }
    }

    @PostMapping("/upload")
    @ApiOperation("上传作品")
    public ResultBean upload(@RequestBody VoteOption option,
                             @RequestHeader("token") String userId) {
        try {
            option.setUserId(userId);
            voteService.uploadWork(option);
            return ResultBean.success();
        } catch (Exception e) {
            return ResultBean.error("上传失败：" + e.getMessage());
        }
    }

    @PostMapping("/audit")
    @ApiOperation("审核作品")
    public ResultBean audit(@RequestBody Map<String, Object> params) {
        try {
            Long optionId = Long.valueOf(params.get("optionId").toString());
            String auditStatus = (String) params.get("auditStatus");
            String auditRemark = (String) params.get("auditRemark");
            voteService.auditWork(optionId, auditStatus, auditRemark);
            return ResultBean.success();
        } catch (Exception e) {
            return ResultBean.error("审核失败：" + e.getMessage());
        }
    }

    @GetMapping("/results")
    @ApiOperation("获取投票结果")
    public ResultBean<List<VoteOption>> getResults(@RequestParam Long taskId) {
        try {
            return ResultBean.success(voteService.getVoteResults(taskId));
        } catch (Exception e) {
            return ResultBean.error("获取结果失败：" + e.getMessage());
        }
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }
}
