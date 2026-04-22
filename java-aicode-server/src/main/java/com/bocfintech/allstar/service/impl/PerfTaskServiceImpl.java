package com.bocfintech.allstar.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bocfintech.allstar.entity.*;
import com.bocfintech.allstar.mapper.*;
import com.bocfintech.allstar.service.PerfTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class PerfTaskServiceImpl extends BaseServiceImpl<PerfTaskMapper, PerfTask> implements PerfTaskService {

    @Autowired
    private PerfTaskTranMapper tranMapper;
    @Autowired
    private PerfTaskBatchMapper batchMapper;
    @Autowired
    private PerfTaskDataMapper dataMapper;
    @Autowired
    private PerfTaskSceneMapper sceneMapper;
    @Autowired
    private PerfDataPlanMapper dataPlanMapper;
    @Autowired
    private PerfDataDetailMapper dataDetailMapper;

    @Override
    public List<PerfTask> listTasks(String batchNo, String productId) {
        LambdaQueryWrapper<PerfTask> query = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(batchNo)) {
            query.eq(PerfTask::getBatchNo, batchNo);
        }
        if (StringUtils.hasText(productId)) {
            query.eq(PerfTask::getProductId, productId);
        }
        query.orderByDesc(PerfTask::getCreateTime);
        return list(query);
    }

    @Override
    public Map<String, Object> getTaskDetail(Long taskId) {
        Map<String, Object> result = new HashMap<>();
        PerfTask task = getById(taskId);
        result.put("task", task);
        if (task != null) {
            result.put("trans", tranMapper.selectList(new LambdaQueryWrapper<PerfTaskTran>().eq(PerfTaskTran::getTaskId, taskId)));
            result.put("batches", batchMapper.selectList(new LambdaQueryWrapper<PerfTaskBatch>().eq(PerfTaskBatch::getTaskId, taskId)));
            result.put("datas", dataDetailMapper.selectList(new LambdaQueryWrapper<PerfDataDetail>().eq(PerfDataDetail::getTaskId, taskId)));
            result.put("scenes", sceneMapper.selectList(new LambdaQueryWrapper<PerfTaskScene>().eq(PerfTaskScene::getTaskId, taskId)));
        }
        return result;
    }

    @Override
    public PerfTask recognizeTaskInfo(String titleText, String timeText, String reqText) {
        PerfTask task = new PerfTask();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // 识别标题信息
        if (StringUtils.hasText(titleText)) {
            String[] lines = titleText.split("\\r?\\n");
            if (lines.length > 0) {
                task.setTaskName(lines[0].trim());
            }
            task.setTestTaskNo(getValueByRegex(titleText, "测试任务编号:\\s*(.*)"));
            task.setProdTaskNo(getValueByRegex(titleText, "生产任务编号:\\s*(.*)"));
            task.setProductId(getValueByRegex(titleText, "牵头组件:\\s*(.*)"));
        }

        // 识别时间与人员信息
        if (StringUtils.hasText(timeText)) {
            task.setBatchNo(getValueByRegex(timeText, "批次:\\s*(.*)"));
            task.setTestDept(getValueByRegex(timeText, "测试部门:\\s*(.*)"));
            String startTimeStr = getValueByRegex(timeText, "测试开始时间:\\s*(.*)");
            String endTimeStr = getValueByRegex(timeText, "测试结束时间:\\s*(.*)");
            try {
                if (StringUtils.hasText(startTimeStr)) task.setStartTime(sdf.parse(startTimeStr));
                if (StringUtils.hasText(endTimeStr)) task.setEndTime(sdf.parse(endTimeStr));
            } catch (Exception e) {
                log.error("Parse date error", e);
            }
            task.setPerfManager(getValueByRegex(timeText, "性能测试经理:\\s*([^/\\n\\r]+)"));
            task.setTestArch(getValueByRegex(timeText, "测试架构师:\\s*([^/\\n\\r]+)"));
            task.setProjectManager(getValueByRegex(timeText, "项目经理:\\s*([^/\\n\\r]+)"));
            task.setDevDept(getValueByRegex(timeText, "开发部门:\\s*(.*)"));
        }

        // 识别需求与项目信息
        if (StringUtils.hasText(reqText)) {
            task.setReqNo(getValueByRegex(reqText, "需求编号:\\s*(.*)"));
            task.setProjName(getValueByRegex(reqText, "项目名称:\\s*(.*)"));
            task.setProjNo(getValueByRegex(reqText, "项目编号:\\s*(.*)"));
        }

        return task;
    }

    private String getValueByRegex(String text, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    @Override
    @Transactional
    public void saveOrUpdateTask(PerfTask task) {
        if (task.getId() == null) {
            task.setStatus(10);
            save(task);
        } else {
            updateById(task);
        }
    }

    @Override
    @Transactional
    public void saveOrUpdateTrans(Long taskId, List<PerfTaskTran> trans, PerfTask summary) {
        // 计算选中交易的 TPS 之和
        java.math.BigDecimal tpsSum = java.math.BigDecimal.ZERO;
        if (trans != null) {
            for (PerfTaskTran tran : trans) {
                if (tran.getIsSelected() != null && tran.getIsSelected() == 1 && tran.getTargetTps() != null) {
                    tpsSum = tpsSum.add(tran.getTargetTps());
                }
            }
        }

        // 更新任务表的汇总字段
        if (summary == null) {
            summary = new PerfTask();
        }
        summary.setId(taskId);
        summary.setSelectedTranTpsSum(tpsSum);
        updateById(summary);
        
        tranMapper.delete(new LambdaQueryWrapper<PerfTaskTran>().eq(PerfTaskTran::getTaskId, taskId));
        if (trans != null) {
            for (PerfTaskTran tran : trans) {
                tran.setTaskId(taskId);
                tranMapper.insert(tran);
            }
        }
    }

    @Override
    @Transactional
    public void saveOrUpdateBatches(Long taskId, List<PerfTaskBatch> batches, PerfTask summary) {
        // 更新任务表的批量汇总字段
        if (summary != null) {
            summary.setId(taskId);
            updateById(summary);
        }
        
        batchMapper.delete(new LambdaQueryWrapper<PerfTaskBatch>().eq(PerfTaskBatch::getTaskId, taskId));
        if (batches != null) {
            for (PerfTaskBatch batch : batches) {
                batch.setTaskId(taskId);
                batchMapper.insert(batch);
            }
        }
    }

    @Override
    @Transactional
    public void saveOrUpdateDatas(Long taskId, List<PerfTaskData> datas) {
        dataMapper.delete(new LambdaQueryWrapper<PerfTaskData>().eq(PerfTaskData::getTaskId, taskId));
        if (datas != null) {
            for (PerfTaskData data : datas) {
                data.setTaskId(taskId);
                dataMapper.insert(data);
            }
        }
    }

    @Override
    public PerfDataPlan getDataPlan(Long taskId) {
        return dataPlanMapper.selectOne(new LambdaQueryWrapper<PerfDataPlan>().eq(PerfDataPlan::getTaskId, taskId));
    }

    @Override
    @Transactional
    public void saveDataPlan(PerfDataPlan plan) {
        if (plan.getId() == null) {
            PerfDataPlan exist = getDataPlan(plan.getTaskId());
            if (exist != null) {
                plan.setId(exist.getId());
                dataPlanMapper.updateById(plan);
            } else {
                dataPlanMapper.insert(plan);
            }
        } else {
            dataPlanMapper.updateById(plan);
        }
    }

    @Override
    @Transactional
    public void saveDataDetails(Long taskId, List<PerfDataDetail> details) {
        dataDetailMapper.delete(new LambdaQueryWrapper<PerfDataDetail>().eq(PerfDataDetail::getTaskId, taskId));
        if (details != null) {
            for (PerfDataDetail detail : details) {
                detail.setTaskId(taskId);
                dataDetailMapper.insert(detail);
            }
        }
    }

    @Override
    @Transactional
    public void saveOrUpdateScenes(Long taskId, List<PerfTaskScene> scenes) {
        sceneMapper.delete(new LambdaQueryWrapper<PerfTaskScene>().eq(PerfTaskScene::getTaskId, taskId));
        if (scenes != null) {
            for (PerfTaskScene scene : scenes) {
                scene.setTaskId(taskId);
                sceneMapper.insert(scene);
            }
        }
    }
}
