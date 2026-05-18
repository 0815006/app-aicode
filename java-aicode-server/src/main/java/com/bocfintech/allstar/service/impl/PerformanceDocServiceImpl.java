package com.bocfintech.allstar.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bocfintech.allstar.dto.DocGenerateContext;
import com.bocfintech.allstar.entity.*;
import com.bocfintech.allstar.mapper.*;
import com.bocfintech.allstar.service.PerformanceDocService;
import com.deepoove.poi.XWPFTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
public class PerformanceDocServiceImpl implements PerformanceDocService {

    @Value("${performance.template-path}")
    private String templatePath;

    @Value("${performance.doc-path}")
    private String docPath;

    @Autowired private PerfTaskMapper taskMapper;
    @Autowired private PerfTaskTranMapper tranMapper;
    @Autowired private PerfTaskBatchMapper batchMapper;
    @Autowired private PerfTaskSceneMapper sceneMapper;
    @Autowired private PerfTaskSceneDetailMapper sceneDetailMapper;
    @Autowired private PerfDataPlanMapper dataPlanMapper;
    @Autowired private PerfDataDetailMapper dataDetailMapper;
    @Autowired private PerformanceResourceInfoMapper resourceMapper;

    private static final String TEMPLATE_FILENAME = "性能测试方案模板.docx";

    @Override
    public String generateDoc(Long taskId) {
        DocGenerateContext ctx = assembleData(taskId);
        if (ctx.getTask() == null) {
            throw new RuntimeException("任务不存在: " + taskId);
        }

        File docDir = new File(docPath);
        if (!docDir.exists()) {
            docDir.mkdirs();
        }

        File tmpl = new File(templatePath + TEMPLATE_FILENAME);
        if (!tmpl.exists()) {
            throw new RuntimeException("模板文件不存在: " + templatePath + TEMPLATE_FILENAME);
        }

        String safeName = sanitizeFilename(ctx.getTask().getTaskName());
        String batchNo = ctx.getTask().getBatchNo() != null ? ctx.getTask().getBatchNo() : "";
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String outputFilename = safeName + "_" + batchNo + "_方案_" + timestamp + ".docx";

        try {
            Map<String, Object> dataModel = buildDataModel(ctx);
            XWPFTemplate template = XWPFTemplate.compile(tmpl).render(dataModel);
            template.writeToFile(docPath + outputFilename);
            log.info("方案文档生成成功: {}", outputFilename);
            return outputFilename;
        } catch (Exception e) {
            log.error("方案文档生成失败, taskId={}", taskId, e);
            throw new RuntimeException("文档生成失败: " + e.getMessage(), e);
        }
    }

    private DocGenerateContext assembleData(Long taskId) {
        DocGenerateContext ctx = new DocGenerateContext();

        ctx.setTask(taskMapper.selectById(taskId));

        ctx.setTranList(tranMapper.selectList(
                new LambdaQueryWrapper<PerfTaskTran>().eq(PerfTaskTran::getTaskId, taskId)));

        ctx.setBatchList(batchMapper.selectList(
                new LambdaQueryWrapper<PerfTaskBatch>().eq(PerfTaskBatch::getTaskId, taskId)));

        ctx.setDataPlan(dataPlanMapper.selectOne(
                new LambdaQueryWrapper<PerfDataPlan>().eq(PerfDataPlan::getTaskId, taskId)));

        ctx.setDataDetailList(dataDetailMapper.selectList(
                new LambdaQueryWrapper<PerfDataDetail>().eq(PerfDataDetail::getTaskId, taskId)));

        List<PerfTaskScene> scenes = sceneMapper.selectList(
                new LambdaQueryWrapper<PerfTaskScene>().eq(PerfTaskScene::getTaskId, taskId));
        List<DocGenerateContext.SceneWithDetails> sceneWithDetails = new ArrayList<>();
        if (scenes != null) {
            for (PerfTaskScene scene : scenes) {
                DocGenerateContext.SceneWithDetails swd = new DocGenerateContext.SceneWithDetails();
                swd.setScene(scene);
                swd.setSceneDetailList(sceneDetailMapper.selectList(
                        new LambdaQueryWrapper<PerfTaskSceneDetail>().eq(PerfTaskSceneDetail::getSceneId, scene.getId())));
                sceneWithDetails.add(swd);
            }
        }
        ctx.setSceneList(sceneWithDetails);

        PerfTask task = ctx.getTask();
        if (task != null) {
            ctx.setResourceList(resourceMapper.selectList(
                    new LambdaQueryWrapper<PerformanceResourceInfo>()
                            .eq(PerformanceResourceInfo::getProductId, task.getProductId())
                            .eq(StringUtils.hasText(task.getBatchNo()), PerformanceResourceInfo::getBatchNo, task.getBatchNo())
                            .eq(PerformanceResourceInfo::getFileSource, "资源申请表")));
        }
        return ctx;
    }

    private Map<String, Object> buildDataModel(DocGenerateContext ctx) {
        Map<String, Object> map = new HashMap<>();

        PerfTask t = ctx.getTask();
        if (t != null) {
            map.put("task_name", nvl(t.getTaskName()));
            map.put("test_task_no", nvl(t.getTestTaskNo()));
            map.put("prod_task_no", nvl(t.getProdTaskNo()));
            map.put("product_id", nvl(t.getProductId()));
            map.put("batch_no", nvl(t.getBatchNo()));
            map.put("req_no", nvl(t.getReqNo()));
            map.put("proj_name", nvl(t.getProjName()));
            map.put("proj_no", nvl(t.getProjNo()));
            map.put("test_dept", nvl(t.getTestDept()));
            map.put("dev_dept", nvl(t.getDevDept()));
            map.put("perf_manager", nvl(t.getPerfManager()));
            map.put("test_arch", nvl(t.getTestArch()));
            map.put("project_manager", nvl(t.getProjectManager()));
            map.put("total_user_count", nvl(t.getTotalUserCount()));
            map.put("daily_online_user_count", nvl(t.getDailyOnlineUserCount()));
            map.put("daily_peak_tps", nvl(t.getDailyPeakTps()));
            map.put("annual_peak_tps", nvl(t.getAnnualPeakTps()));
            map.put("selected_tran_tps_sum", nvl(t.getSelectedTranTpsSum()));
            map.put("start_time", formatDate(t.getStartTime()));
            map.put("end_time", formatDate(t.getEndTime()));
            map.put("batch_total_duration", nvl(t.getBatchTotalDuration()));
            map.put("batch_total_data_volume", nvl(t.getBatchTotalDataVolume()));
            map.put("batch_parallel_degree", nvl(t.getBatchParallelDegree()));
            map.put("batch_max_parallel_count", nvl(t.getBatchMaxParallelCount()));
        }

        map.put("tran_list", ctx.getTranList() != null ? ctx.getTranList() : Collections.emptyList());
        map.put("batch_list", ctx.getBatchList() != null ? ctx.getBatchList() : Collections.emptyList());
        map.put("data_detail_list", ctx.getDataDetailList() != null ? ctx.getDataDetailList() : Collections.emptyList());
        map.put("resource_list", ctx.getResourceList() != null ? ctx.getResourceList() : Collections.emptyList());

        PerfDataPlan plan = ctx.getDataPlan();
        if (plan != null) {
            map.put("model_analysis", nvl(plan.getModelAnalysis()));
            map.put("data_constraint", nvl(plan.getDataConstraint()));
            map.put("data_source_desc", nvl(plan.getDataSourceDesc()));
            map.put("prep_method_desc", nvl(plan.getPrepMethodDesc()));
            map.put("cleaning_rule", nvl(plan.getCleaningRule()));
        }

        map.put("scene_list", ctx.getSceneList() != null ? ctx.getSceneList() : Collections.emptyList());
        System.out.println("打印："+map.toString());
        return map;
    }

    private String nvl(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    private String formatDate(Date date) {
        if (date == null) return "";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    private String sanitizeFilename(String name) {
        if (name == null) return "未命名";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    @Override
    public List<DocFileInfo> listDocs() {
        List<DocFileInfo> fileList = new ArrayList<>();
        File directory = new File(docPath);
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".docx")) {
                        DocFileInfo info = new DocFileInfo();
                        info.setFileName(file.getName());
                        info.setFileSize(file.length());
                        info.setLastModified(new Date(file.lastModified()));
                        fileList.add(info);
                    }
                }
            }
        }
        fileList.sort((a, b) -> b.getLastModified().compareTo(a.getLastModified()));
        return fileList;
    }

    @Override
    public boolean deleteDoc(String fileName) {
        File file = new File(docPath + fileName);
        if (file.exists() && file.isFile()) {
            return file.delete();
        }
        return false;
    }
}
