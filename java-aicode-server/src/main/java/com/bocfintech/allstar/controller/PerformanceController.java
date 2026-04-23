package com.bocfintech.allstar.controller;

import com.bocfintech.allstar.bean.ResultBean;
import com.bocfintech.allstar.entity.*;
import com.bocfintech.allstar.service.PerfTaskService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "性能测试方案辅助系统")
@RestController
@RequestMapping("/api/performance")
@Slf4j
public class PerformanceController {

    @Autowired
    private PerfTaskService perfTaskService;

    @ApiOperation("获取任务列表")
    @GetMapping("/list")
    public ResultBean<List<PerfTask>> listTasks(@RequestParam(required = false) String batchNo,
                                               @RequestParam(required = false) String productId) {
        return ResultBean.success(perfTaskService.listTasks(batchNo, productId));
    }

    @ApiOperation("获取任务详情")
    @GetMapping("/detail")
    public ResultBean<Map<String, Object>> getTaskDetail(@RequestParam Long taskId) {
        return ResultBean.success(perfTaskService.getTaskDetail(taskId));
    }

    @ApiOperation("识别任务信息")
    @PostMapping("/recognize")
    public ResultBean<PerfTask> recognizeTaskInfo(@RequestBody RecognizeRequest request) {
        return ResultBean.success(perfTaskService.recognizeTaskInfo(request.getTitleText(), request.getTimeText(), request.getReqText()));
    }

    @ApiOperation("保存/更新任务主表")
    @PostMapping("/saveTask")
    public ResultBean<String> saveTask(@RequestBody PerfTask task) {
        perfTaskService.saveOrUpdateTask(task);
        return ResultBean.success("保存成功");
    }

    @ApiOperation("保存/更新联机交易")
    @PostMapping("/saveTrans")
    public ResultBean<String> saveTrans(@RequestParam Long taskId, @RequestBody SaveTransRequest request) {
        perfTaskService.saveOrUpdateTrans(taskId, request.getTrans(), request.getSummary());
        return ResultBean.success("保存成功");
    }

    @Data
    public static class SaveTransRequest {
        private List<PerfTaskTran> trans;
        private PerfTask summary;
    }

    @ApiOperation("保存/更新批量作业")
    @PostMapping("/saveBatches")
    public ResultBean<String> saveBatches(@RequestParam Long taskId, @RequestBody SaveBatchesRequest request) {
        perfTaskService.saveOrUpdateBatches(taskId, request.getBatches(), request.getSummary());
        return ResultBean.success("保存成功");
    }

    @Data
    public static class SaveBatchesRequest {
        private List<PerfTaskBatch> batches;
        private PerfTask summary;
    }

    @ApiOperation("保存/更新数据准备")
    @PostMapping("/saveDatas")
    public ResultBean<String> saveDatas(@RequestParam Long taskId, @RequestBody List<PerfTaskData> datas) {
        perfTaskService.saveOrUpdateDatas(taskId, datas);
        return ResultBean.success("保存成功");
    }

    @ApiOperation("初始化默认场景")
    @GetMapping("/initScenes")
    public ResultBean<List<PerfTaskScene>> initScenes(@RequestParam Long taskId) {
        perfTaskService.initDefaultScenes(taskId);
        return ResultBean.success(perfTaskService.getScenesByTaskId(taskId));
    }

    @ApiOperation("获取场景列表")
    @GetMapping("/getScenes")
    public ResultBean<List<PerfTaskScene>> getScenes(@RequestParam Long taskId) {
        return ResultBean.success(perfTaskService.getScenesByTaskId(taskId));
    }

    @ApiOperation("获取场景明细")
    @GetMapping("/getSceneDetails")
    public ResultBean<List<PerfTaskSceneDetail>> getSceneDetails(@RequestParam Long sceneId) {
        return ResultBean.success(perfTaskService.getSceneDetailsBySceneId(sceneId));
    }

    @ApiOperation("保存所有场景及明细")
    @PostMapping("/saveAllScenes")
    public ResultBean<String> saveAllScenes(@RequestParam Long taskId, @RequestBody List<SceneDTO> scenes) {
        perfTaskService.saveAllScenes(taskId, scenes);
        return ResultBean.success("保存成功");
    }

    @ApiOperation("获取数据准备方案")
    @GetMapping("/getDataPlan")
    public ResultBean<PerfDataPlan> getDataPlan(@RequestParam Long taskId) {
        return ResultBean.success(perfTaskService.getDataPlan(taskId));
    }

    @ApiOperation("保存数据准备方案")
    @PostMapping("/saveDataPlan")
    public ResultBean<String> saveDataPlan(@RequestBody PerfDataPlan plan) {
        perfTaskService.saveDataPlan(plan);
        return ResultBean.success("保存成功");
    }

    @ApiOperation("保存数据准备明细")
    @PostMapping("/saveDataDetails")
    public ResultBean<String> saveDataDetails(@RequestParam Long taskId, @RequestBody List<PerfDataDetail> details) {
        perfTaskService.saveDataDetails(taskId, details);
        return ResultBean.success("保存成功");
    }

    @Data
    public static class RecognizeRequest {
        private String titleText;
        private String timeText;
        private String reqText;
    }
}
