package com.bocfintech.allstar.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.bocfintech.allstar.bean.ResultBean;
import com.bocfintech.allstar.entity.PerfTaskTran;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/performance/tran")
@Api(tags = "性能测试交易解析接口")
@Slf4j
public class PerformanceTranController {

    @PostMapping("/parseDataExcel")
    @ApiOperation("解析数据准备调查表Excel")
    public ResultBean<List<com.bocfintech.allstar.entity.PerfDataDetail>> parseDataExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResultBean.error("文件不能为空");
        }

        final List<Map<Integer, String>> allData = new ArrayList<>();
        try {
            EasyExcel.read(file.getInputStream(), new AnalysisEventListener<Map<Integer, String>>() {
                @Override
                public void invoke(Map<Integer, String> data, AnalysisContext context) {
                    String currentSheetName = context.readSheetHolder().getSheetName();
                    if (currentSheetName != null && currentSheetName.contains("数据准备")) {
                        log.info("Found target sheet: {}, row: {}", currentSheetName, data);
                        allData.add(data);
                    }
                }
                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {}
            }).headRowNumber(0).doReadAll();

            if (allData.isEmpty()) {
                log.warn("No data found in sheets containing '数据准备'");
                return ResultBean.error("未找到名称包含“数据准备”的Sheet页");
            }

            List<com.bocfintech.allstar.entity.PerfDataDetail> details = new ArrayList<>();
            boolean isDataSection = false;
            int startCol = 0;

            for (int i = 0; i < allData.size(); i++) {
                Map<Integer, String> row = allData.get(i);
                
                // 识别标题行，支持多种可能的标题文字，且支持列偏移
                int headerCol = -1;
                for (Map.Entry<Integer, String> entry : row.entrySet()) {
                    String val = entry.getValue();
                    if (val != null && (val.contains("数据分类") || val.contains("英文表名") || val.contains("表名"))) {
                        headerCol = entry.getKey();
                        // 如果当前列是“英文表名”或“表名”，且不是第一列，则尝试向前推一列作为“数据分类”列
                        if ((val.contains("英文表名") || val.contains("表名")) && headerCol > 0) {
                            // 检查前一列是否是“数据分类”或者为空（可能是合并单元格或偏移）
                            headerCol--;
                        }
                        break;
                    }
                }

                if (headerCol != -1) {
                    log.info("Found header at row {}, startCol {}", i, headerCol);
                    isDataSection = true;
                    startCol = headerCol;
                    continue;
                }

                if (isDataSection) {
                    if (isEmptyRow(row)) {
                        log.info("Skipping empty row at {}", i);
                        continue;
                    }
                    
                    com.bocfintech.allstar.entity.PerfDataDetail detail = new com.bocfintech.allstar.entity.PerfDataDetail();
                    String dataTypeStr = row.get(startCol);
                    // 只要包含“2”或者“基础”就认为是基础数据，否则默认为核心业务表(1)
                    detail.setDataType((dataTypeStr != null && (dataTypeStr.contains("2") || dataTypeStr.contains("基础"))) ? 2 : 1);
                    detail.setTableNameEn(row.get(startCol + 1));
                    detail.setTableNameCn(row.get(startCol + 2));
                    detail.setTableRowsCount(toLong(row.get(startCol + 3)));
                    detail.setTableGrowthRate(toBigDecimal(row.get(startCol + 4)));
                    detail.setTargetRowsCount(toLong(row.get(startCol + 5)));
                    detail.setDataDistDesc(row.get(startCol + 6));
                    detail.setPrepMethod(row.get(startCol + 7));
                    
                    log.info("Parsed detail at row {}: {}", i, detail);
                    
                    // 只有当英文表名不为空时才添加
                    if (detail.getTableNameEn() != null && !detail.getTableNameEn().trim().isEmpty()) {
                        details.add(detail);
                    } else {
                        log.warn("Skipping row {} because tableNameEn is empty at col {}", i, startCol + 1);
                    }
                }
            }
            log.info("Total details parsed: {}", details.size());

            return ResultBean.success(details);
        } catch (IOException e) {
            return ResultBean.error("解析失败：" + e.getMessage());
        }
    }

    private Long toLong(String val) {
        if (val == null || val.trim().isEmpty()) return null;
        try {
            String cleanVal = val.trim().replaceAll("[^0-9.\\-Ee]", "");
            if (cleanVal.isEmpty()) return null;
            // 如果带小数点或科学计数法，先转为 double 再转为 long
            if (cleanVal.contains(".") || cleanVal.toLowerCase().contains("e")) {
                return (long) Double.parseDouble(cleanVal);
            }
            return Long.parseLong(cleanVal);
        } catch (Exception e) {
            return null;
        }
    }

    @PostMapping("/parseBatchExcel")
    @ApiOperation("解析批量作业调查表Excel")
    public ResultBean<Map<String, Object>> parseBatchExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResultBean.error("文件不能为空");
        }

        final List<Map<Integer, String>> allData = new ArrayList<>();
        try {
            EasyExcel.read(file.getInputStream(), new AnalysisEventListener<Map<Integer, String>>() {
                @Override
                public void invoke(Map<Integer, String> data, AnalysisContext context) {
                    String currentSheetName = context.readSheetHolder().getSheetName();
                    if (currentSheetName != null && currentSheetName.contains("调查表")) {
                        allData.add(data);
                    }
                }
                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {}
            }).headRowNumber(0).doReadAll();

            if (allData.isEmpty()) {
                return ResultBean.error("未找到名称包含“调查表”的Sheet页");
            }

            List<com.bocfintech.allstar.entity.PerfTaskBatch> batches = new ArrayList<>();
            Map<String, String> summary = new HashMap<>();
            boolean isBatchSection = false;

            for (int i = 0; i < allData.size(); i++) {
                Map<Integer, String> row = allData.get(i);
                
                // 解析前15行的汇总字段
                if (i < 15) {
                    for (int colIndex = 0; colIndex < 20; colIndex++) { // 遍历列
                        String cellVal = row.get(colIndex);
                        if (cellVal == null || cellVal.trim().isEmpty()) continue;
                        
                        // 检查关键字
                        // 由于是合并单元格，EasyExcel 读取时，合并区域只有左上角第一个单元格有值，其余为 null
                        // 我们需要寻找关键字，然后向右搜索第一个非空单元格作为它的值
                        if (cellVal.contains("预估整体批量时长") || cellVal.contains("预估整体数据量") || 
                            cellVal.contains("并行度") || cellVal.contains("最大并行数")) {
                            
                            String key = "";
                            if (cellVal.contains("预估整体批量时长")) key = "batchTotalDuration";
                            else if (cellVal.contains("预估整体数据量")) key = "batchTotalDataVolume";
                            else if (cellVal.contains("并行度")) key = "batchParallelDegree";
                            else if (cellVal.contains("最大并行数")) key = "batchMaxParallelCount";

                            // 向右搜索第一个非空单元格
                            for (int nextCol = colIndex + 1; nextCol < colIndex + 10 && nextCol < 30; nextCol++) {
                                String val = row.get(nextCol);
                                if (val != null && !val.trim().isEmpty()) {
                                    summary.put(key, val.trim());
                                    break;
                                }
                            }
                        }
                    }
                }

                String col0 = row.get(0);
                if (col0 != null && col0.contains("作业编号")) {
                    isBatchSection = true;
                    continue;
                }

                if (isBatchSection) {
                    if (isEmptyRow(row)) continue;
                    
                    com.bocfintech.allstar.entity.PerfTaskBatch batch = new com.bocfintech.allstar.entity.PerfTaskBatch();
                    batch.setJobNo(row.get(0));
                    batch.setJobName(row.get(1));
                    batch.setJobCount(row.get(2));
                    batch.setJobParallelMode(row.get(3));
                    batch.setJobDesc(row.get(4));
                    batch.setJobTriggerCond(row.get(5));
                    batch.setJobPreName(row.get(6));
                    batch.setJobConcurrentNames(row.get(7));
                    batch.setJobFrequency(row.get(8));
                    
                    batch.setJobDataType(row.get(9));
                    batch.setJobDataVolume(row.get(10));
                    batch.setJobActualDuration(row.get(11));
                    batch.setJobDuration(row.get(12));
                    batch.setJobExecTimePoint(row.get(13));
                    
                    batch.setIsMixedLink(row.get(14));
                    batch.setMixedTranNames(row.get(15));
                    batch.setHasRetry(row.get(16));
                    batch.setRetryDesc(row.get(17));
                    batch.setSelectReason(row.get(18));
                    
                    batches.add(batch);
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("batches", batches);
            result.put("summary", summary);
            return ResultBean.success(result);
        } catch (IOException e) {
            return ResultBean.error("解析失败：" + e.getMessage());
        }
    }

    @PostMapping("/parseExcel")
    @ApiOperation("解析业务交易调查表Excel")
    public ResultBean<Map<String, Object>> parseExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResultBean.error("文件不能为空");
        }

        final List<Map<Integer, String>> allData = new ArrayList<>();
        try {
            // 使用 EasyExcel 读取数据
            EasyExcel.read(file.getInputStream(), new AnalysisEventListener<Map<Integer, String>>() {
                @Override
                public void invoke(Map<Integer, String> data, AnalysisContext context) {
                    String currentSheetName = context.readSheetHolder().getSheetName();
                    if (currentSheetName != null && currentSheetName.contains("业务交易调查表")) {
                        int rowIndex = context.readRowHolder().getRowIndex();
                        log.info("Sheet: {}, Row Index: {}, Data: {}", currentSheetName, rowIndex, data);
                        allData.add(data);
                    }
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                }
            }).headRowNumber(0).doReadAll();

            if (allData.isEmpty()) {
                return ResultBean.error("未找到名称包含“业务交易调查表”的Sheet页或Sheet页内无数据");
            }

            log.info("Starting to parse {} rows from target sheet", allData.size());
            
            List<PerfTaskTran> trans = new ArrayList<>();
            Map<String, BigDecimal> summary = new HashMap<>();
            
            boolean isTranSection = false;
            boolean isSummarySection = false;

            for (int i = 0; i < allData.size(); i++) {
                Map<Integer, String> row = allData.get(i);
                log.info("Processing row {}: {}", i, row);
                
                String col0 = row.get(0);
                
                // 检查是否是汇总区域的标题行
                if (col0 != null && col0.contains("系统汇总统计")) {
                    log.info("Found summary section at row index {}", i);
                    isSummarySection = true;
                    isTranSection = false;
                    continue;
                }

                // 检查是否是交易列表的起始标题行
                if (col0 != null && col0.contains("产品名称/模块名称*")) {
                    log.info("Found transaction section header at row index {}", i);
                    isTranSection = true;
                    // 标题占两行，所以跳过当前行和下一行
                    i++; 
                    continue;
                }

                if (isTranSection && !isSummarySection) {
                    // 如果遇到空行且还没到汇总区，可能交易列表结束
                    if (isEmptyRow(row)) {
                        log.info("Empty row at {}, skipping", i);
                        continue;
                    }

                    // 解析交易行
                    PerfTaskTran tran = new PerfTaskTran();
                    tran.setModuleName(row.get(0));
                    tran.setTranName(row.get(1));
                    tran.setTranCode(row.get(2));
                    tran.setInterfaceType(row.get(3));
                    
                    log.info("Parsed basic info: module={}, name={}", tran.getModuleName(), tran.getTranName());
                    
                    // 生产现状 (5-9列)
                    tran.setTranDailyVol(toBigDecimal(row.get(4)));
                    tran.setTranPeakHourVol(toBigDecimal(row.get(5)));
                    tran.setTranPeakTps(toBigDecimal(row.get(6)));
                    tran.setTranAvgRt(toBigDecimal(row.get(7)));
                    tran.setTranMaxRt(toBigDecimal(row.get(8)));
                    
                    // 目标指标 (10-15列)
                    tran.setTargetDailyVol(toBigDecimal(row.get(9)));
                    tran.setTargetPeakHourVol(toBigDecimal(row.get(10)));
                    tran.setTargetTps(toBigDecimal(row.get(11)));
                    tran.setTargetRt(toBigDecimal(row.get(12)));
                    tran.setTargetMaxRt(toBigDecimal(row.get(13)));
                    tran.setTargetSuccessRate(toBigDecimal(row.get(14)));
                    
                    // 思考时间 (16列)
                    tran.setTargetThinkTime(toBigDecimal(row.get(15)));
                    
                    // 是否选中 (17列)
                    String selected = row.get(16);
                    tran.setIsSelected("是".equals(selected) || "1".equals(selected) ? 1 : 0);
                    
                    // 选取原因 (18列)
                    tran.setSelectReason(row.get(17));
                    
                    // 指标来源 (19列)
                    String sourceStr = row.get(18);
                    if (sourceStr != null) {
                        if (sourceStr.contains("实测")) tran.setIndicatorSource(1);
                        else if (sourceStr.contains("采样")) tran.setIndicatorSource(2);
                        else if (sourceStr.contains("经验")) tran.setIndicatorSource(3);
                        else tran.setIndicatorSource(1);
                    } else {
                        tran.setIndicatorSource(1);
                    }
                    
                    // 推算过程 (20列)
                    tran.setCalculationProcess(row.get(19));
                    
                    trans.add(tran);
                } else if (isSummarySection) {
                    // 汇总字段：用户数总数	日均在线用户数	日交易峰值TPS*	年交易峰值TPS*
                    // 汇总标题行的下一行是数据
                    if (col0 != null && col0.contains("用户数总数")) {
                        log.info("Skipping summary header row {}", i);
                        continue;
                    }
                    
                    if (isEmptyRow(row)) continue;
                    
                    summary.put("totalUserCount", toBigDecimal(row.get(0)));
                    summary.put("dailyOnlineUserCount", toBigDecimal(row.get(1)));
                    summary.put("dailyPeakTps", toBigDecimal(row.get(2)));
                    summary.put("annualPeakTps", toBigDecimal(row.get(3)));
                    log.info("Parsed summary: {}", summary);
                    break; // 解析完汇总就结束
                }
            }

            log.info("Final parsed trans size: {}", trans.size());
            Map<String, Object> result = new HashMap<>();
            result.put("trans", trans);
            result.put("summary", summary);
            
            return ResultBean.success(result);
        } catch (IOException e) {
            log.error("解析Excel失败", e);
            return ResultBean.error("解析失败：" + e.getMessage());
        }
    }

    private boolean isEmptyRow(Map<Integer, String> row) {
        if (row == null || row.isEmpty()) return true;
        for (String val : row.values()) {
            if (val != null && !val.trim().isEmpty()) return false;
        }
        return true;
    }

    private BigDecimal toBigDecimal(String val) {
        if (val == null || val.trim().isEmpty()) return null;
        try {
            // 移除单位（如“万”、“毫秒”、“秒”、“笔/秒”等），保留数字、小数点、负号和科学计数法
            String cleanVal = val.trim()
                    .replaceAll("[^0-9.\\-Ee]", ""); 
            if (cleanVal.isEmpty()) return null;
            return new BigDecimal(cleanVal);
        } catch (Exception e) {
            log.warn("Failed to parse BigDecimal from: {}", val);
            return null;
        }
    }
}
