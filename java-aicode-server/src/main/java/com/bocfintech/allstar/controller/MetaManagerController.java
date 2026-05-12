package com.bocfintech.allstar.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bocfintech.allstar.bean.ResultBean;
import com.bocfintech.allstar.entity.*;
import com.bocfintech.allstar.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@Slf4j
@RequestMapping("/api/meta")
public class MetaManagerController {

    @Autowired
    private MetaFileModelService modelService;

    @Autowired
    private MetaFieldDefinitionService fieldService;

    @Autowired
    private MetaEnumLibraryService enumService;

    @Autowired
    private MetaRefFileService refFileService;

    @Autowired
    private MetaGenEngineService engineService;

    @Value("${meta.template-path}")
    private String templatePath;

    // ===================== 模型管理 =====================

    @GetMapping("/models")
    public ResultBean<IPage<MetaFileModel>> listModels(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(value = "token", required = false) String token) {
        String empNo = getEmpNo(token);
        List<MetaFileModel> list = modelService.listByUser(empNo);
        // 手动分页
        int total = list.size();
        int from = (page - 1) * size;
        int to = Math.min(from + size, total);
        IPage<MetaFileModel> result = new Page<>(page, size, total);
        if (from < total) {
            result.setRecords(list.subList(from, to));
        }
        return ResultBean.success(result);
    }

    @PostMapping("/models")
    public ResultBean<MetaFileModel> createModel(@RequestBody MetaFileModel model,
                                                  @RequestHeader(value = "token", required = false) String token) {
        String empNo = getEmpNo(token);
        modelService.saveModel(model, empNo);
        return ResultBean.success(model);
    }

    @GetMapping("/models/{id}")
    public ResultBean<ModelDetail> getModelDetail(@PathVariable Long id,
                                                   @RequestHeader(value = "token", required = false) String token) {
        String empNo = getEmpNo(token);
        MetaFileModel model = modelService.getById(id);
        if (model == null) return ResultBean.error("模型不存在");
        List<MetaFieldDefinition> fields = fieldService.listByModelId(id);
        ModelDetail detail = new ModelDetail();
        detail.setModel(model);
        detail.setFields(fields);
        return ResultBean.success(detail);
    }

    @PutMapping("/models/{id}")
    public ResultBean<String> updateModel(@PathVariable Long id, @RequestBody MetaFileModel model,
                                           @RequestHeader(value = "token", required = false) String token) {
        String empNo = getEmpNo(token);
        model.setId(id);
        modelService.updateModel(model, empNo);
        return ResultBean.success("更新成功");
    }

    @DeleteMapping("/models/{id}")
    public ResultBean<String> deleteModel(@PathVariable Long id,
                                           @RequestHeader(value = "token", required = false) String token) {
        String empNo = getEmpNo(token);
        MetaFileModel model = modelService.getById(id);
        if (model == null) return ResultBean.error("模型不存在");
        if (!model.getOwnerId().equals(empNo)) return ResultBean.error("无权删除");
        // 删除关联字段
        fieldService.lambdaUpdate().eq(MetaFieldDefinition::getModelId, id).remove();
        modelService.removeById(id);
        return ResultBean.success("删除成功");
    }

    @PostMapping("/models/{id}/publish")
    public ResultBean<String> publishModel(@PathVariable Long id,
                                            @RequestHeader(value = "token", required = false) String token) {
        String empNo = getEmpNo(token);
        // 发布前校验字段
        List<MetaFieldDefinition> fields = fieldService.listByModelId(id);
        List<String> errors = fieldService.validateFields(id, fields);
        if (!errors.isEmpty()) {
            return ResultBean.error("字段校验失败: " + String.join("; ", errors));
        }
        modelService.publish(id, empNo);
        return ResultBean.success("发布成功");
    }

    @PostMapping("/models/{id}/share")
    public ResultBean<String> shareModel(@PathVariable Long id, @RequestBody ShareRequest req,
                                          @RequestHeader(value = "token", required = false) String token) {
        String empNo = getEmpNo(token);
        modelService.share(id, req.getSharedWith(), empNo);
        return ResultBean.success("共享成功");
    }

    // ===================== 字段管理 =====================

    @PostMapping("/models/{modelId}/fields")
    public ResultBean<String> saveFields(@PathVariable Long modelId, @RequestBody List<MetaFieldDefinition> fields,
                                          @RequestHeader(value = "token", required = false) String token) {
        String empNo = getEmpNo(token);
        // 校验模型归属
        MetaFileModel model = modelService.getById(modelId);
        if (model == null) return ResultBean.error("模型不存在");
        if ("PUBLISHED".equals(model.getStatus())) return ResultBean.error("已发布模型请先退回草稿");

        fieldService.batchSave(modelId, fields);
        return ResultBean.success("保存成功");
    }

    @PostMapping("/fields/validate")
    public ResultBean<List<String>> validateFields(@RequestBody ValidateRequest req,
                                                    @RequestHeader(value = "token", required = false) String token) {
        String empNo = getEmpNo(token);
        List<String> errors = fieldService.validateFields(req.getModelId(), req.getFields());
        return ResultBean.success(errors);
    }

    @GetMapping("/fields/sum-targets/{modelId}")
    public ResultBean<List<MetaFieldDefinition>> getSumTargets(@PathVariable Long modelId,
                                                                @RequestHeader(value = "token", required = false) String token) {
        String empNo = getEmpNo(token);
        return ResultBean.success(fieldService.getSumTargets(modelId));
    }

    // ===================== 枚举库管理 =====================

    @GetMapping("/enums")
    public ResultBean<List<MetaEnumLibrary>> listEnums(@RequestHeader(value = "token", required = false) String token) {
        String empNo = getEmpNo(token);
        return ResultBean.success(enumService.listAll());
    }

    @GetMapping("/enums/keys")
    public ResultBean<List<String>> getEnumKeys(@RequestHeader(value = "token", required = false) String token) {
        String empNo = getEmpNo(token);
        return ResultBean.success(enumService.getAllKeys());
    }

    @PostMapping("/enums")
    public ResultBean<MetaEnumLibrary> saveEnum(@RequestBody MetaEnumLibrary enumLib,
                                                  @RequestHeader(value = "token", required = false) String token) {
        String empNo = getEmpNo(token);
        enumService.saveOrUpdate(enumLib);
        return ResultBean.success(enumLib);
    }

    @DeleteMapping("/enums/{id}")
    public ResultBean<String> deleteEnum(@PathVariable Long id,
                                          @RequestHeader(value = "token", required = false) String token) {
        String empNo = getEmpNo(token);
        enumService.removeById(id);
        return ResultBean.success("删除成功");
    }

    // ===================== 引用文件管理 =====================

    @GetMapping("/resources")
    public ResultBean<List<MetaRefFile>> listRefFiles(@RequestHeader(value = "token", required = false) String token) {
        String empNo = getEmpNo(token);
        return ResultBean.success(refFileService.listAll());
    }

    @PostMapping("/resources/upload")
    public ResultBean<MetaRefFile> uploadRefFile(@RequestParam("file") MultipartFile file,
                                                   @RequestParam("refName") String refName,
                                                   @RequestHeader(value = "token", required = false) String token) {
        String empNo = getEmpNo(token);
        try {
            File dir = new File(templatePath);
            dir.mkdirs();
            String fileName = UUID.randomUUID().toString().substring(0, 8) + "_" + file.getOriginalFilename();
            File dest = new File(dir, fileName);
            file.transferTo(dest);
            MetaRefFile refFile = refFileService.uploadAndSave(refName, dest.getAbsolutePath(), "DELIMITER", ",", null);
            return ResultBean.success(refFile);
        } catch (IOException e) {
            log.error("上传失败", e);
            return ResultBean.error("上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/resources/define")
    public ResultBean<String> defineRefFile(@RequestBody MetaRefFile refFile,
                                             @RequestHeader(value = "token", required = false) String token) {
        String empNo = getEmpNo(token);
        refFileService.saveOrUpdate(refFile);
        return ResultBean.success("保存成功");
    }

    // ===================== 模板管理 =====================

    @GetMapping("/template/downloadByKeyword")
    public void downloadTemplateByKeyword(@RequestParam String keyword,
                                           @RequestHeader(value = "token", required = false) String token,
                                           HttpServletResponse response) {
        String empNo = getEmpNo(token);
        File directory = new File(templatePath);
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().contains(keyword)) {
                        downloadFile(file, response);
                        return;
                    }
                }
            }
        }
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        try {
            response.getWriter().write("{\"code\":404,\"message\":\"未找到匹配的模板文件\"}");
        } catch (IOException ignored) {}
    }

    @GetMapping("/template/general/download")
    public void downloadGeneralTemplate(HttpServletResponse response) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + URLEncoder.encode("通用字段模板.xlsx", "UTF-8") + "\"");

            // 表头：与上传解析格式一致
            List<List<String>> head = new ArrayList<>();
            head.add(Collections.singletonList("字段英文名"));
            head.add(Collections.singletonList("字段描述"));
            head.add(Collections.singletonList("长度(字节)"));
            head.add(Collections.singletonList("是否必填"));
            head.add(Collections.singletonList("规则类型"));
            head.add(Collections.singletonList("配置内容"));
            head.add(Collections.singletonList("说明"));

            // 数据行：涵盖所有规则类型（14种），配满示例配置
            List<List<String>> data = new ArrayList<>();

            data.add(Arrays.asList("fixField", "固定值示例", "10", "是", "固定值", "HELLO", "直接填写固定文本内容，生成恒定不变的值"));
            data.add(Arrays.asList("dateField", "日期示例", "8", "是", "日期", "yyyyMMdd", "按指定格式生成当前日期；格式支持：yyyyMMdd/yyyyMMddHHmmss/yyMMdd/MMdd/HHmmss，offset=0表示当天"));
            data.add(Arrays.asList("enumField", "枚举示例", "2", "否", "枚举", "gender", "填写枚举库中已定义的枚举Key，每次从枚举值列表中随机选取一个"));
            data.add(Arrays.asList("bodySeqNo", "序列号示例", "12", "是", "序列号", "前缀=S,位数=11,起始=1,步长=1,策略=PER_LINE", "带前缀S的12位定长序列号，每行+1，支持循环；前缀+数字位总长需≤字段长度"));
            data.add(Arrays.asList("randCnField", "随机汉字示例", "6", "否", "随机汉字", "count=3", "随机生成指定个数的汉字，count控制汉字数量"));
            data.add(Arrays.asList("randNumField", "随机数字示例", "10", "否", "随机数字", "count=5", "随机生成指定个数的数字，count控制数字位数"));
            data.add(Arrays.asList("randUuidField", "随机UUID示例", "36", "否", "随机UUID", "upperCase=true", "随机生成UUID字符串，可通过upperCase控制是否大写"));
            data.add(Arrays.asList("refFileField", "引用文件示例", "20", "否", "引用文件", "refFileId=10,columnKey=userName", "从素材文件中按行引用指定列的数据，updateStrategy=PER_LINE每行取下一行，atEnd=LOOP到头循环"));
            data.add(Arrays.asList("refFieldField", "引用字段示例", "10", "否", "引用字段", "fixField", "引用同一报文前面已定义的字段值，targetFieldKey填目标字段的英文名"));
            data.add(Arrays.asList("headTotalSum", "汇总金额示例", "18", "否", "汇总金额", "targetFieldKey=tran_amt,format=9(14)V99", "仅文件头/文件尾使用；引擎先处理Body累加金额，最后回写Header/Footer缓冲区；format指定格式"));
            data.add(Arrays.asList("headTotalCount", "统计行数示例", "6", "否", "统计行数", "targetFieldKey=body_row", "仅文件头/文件尾使用；自动统计Body数据总行数，值为最终生成行数"));
            data.add(Arrays.asList("batchNoField", "批次号示例", "8", "是", "批次号", "前缀=B,起始=1,策略=PER_FILE", "全局批次号，每个文件只递增一次（PER_FILE）；与序列号不同，序列号每行递增（PER_LINE）"));
            data.add(Arrays.asList("tranAmt", "金额示例", "17", "是", "金额", "1234567890123.45", "按金融金额格式生成；自动解析整数位和小数位数，支持format如9(14)V99控制格式"));
            data.add(Arrays.asList("exprField", "表达式示例", "35", "否", "表达式", "PROJ_${dateField}_${batchNo}", "使用${变量名}语法拼接已有字段生成复合值；常用于动态文件名、报文ID等场景"));

            EasyExcel.write(response.getOutputStream())
                    .head(head)
                    .sheet("通用字段模板")
                    .doWrite(data);
        } catch (IOException e) {
            log.error("生成通用模板失败", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                response.getWriter().write("{\"code\":500,\"message\":\"生成通用模板失败\"}");
            } catch (IOException ignored) {}
        }
    }

    @PostMapping("/template/upload")
    public ResultBean<String> uploadTemplate(@RequestParam("file") MultipartFile file,
                                              @RequestHeader(value = "token", required = false) String token) {
        String empNo = getEmpNo(token);
        if (file.isEmpty()) {
            return ResultBean.error("文件不能为空");
        }
        try {
            File directory = new File(templatePath);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            String originalFilename = file.getOriginalFilename();
            File dest = new File(templatePath + originalFilename);
            file.transferTo(dest);
            return ResultBean.success("上传成功");
        } catch (IOException e) {
            log.error("模板文件上传失败", e);
            return ResultBean.error("上传失败：" + e.getMessage());
        }
    }

    @GetMapping("/template/list")
    public ResultBean<List<TemplateFileInfo>> listTemplates(@RequestHeader(value = "token", required = false) String token) {
        String empNo = getEmpNo(token);
        List<TemplateFileInfo> result = new ArrayList<>();
        File directory = new File(templatePath);
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile()) {
                        TemplateFileInfo info = new TemplateFileInfo();
                        info.setFileName(f.getName());
                        info.setFileSize(f.length());
                        info.setLastModified(new Date(f.lastModified()));
                        result.add(info);
                    }
                }
            }
        }
        result.sort((a, b) -> b.getLastModified().compareTo(a.getLastModified()));
        return ResultBean.success(result);
    }

    @GetMapping("/template/download")
    public void downloadTemplate(@RequestParam String fileName,
                                  @RequestHeader(value = "token", required = false) String token,
                                  HttpServletResponse response) {
        String empNo = getEmpNo(token);
        File file = new File(templatePath + fileName);
        if (!file.exists() || !file.isFile()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            try {
                response.getWriter().write("{\"code\":404,\"message\":\"文件不存在\"}");
            } catch (IOException ignored) {}
            return;
        }
        downloadFile(file, response);
    }

    @DeleteMapping("/template/delete")
    public ResultBean<String> deleteTemplate(@RequestParam String fileName,
                                              @RequestHeader(value = "token", required = false) String token) {
        String empNo = getEmpNo(token);
        File file = new File(templatePath + fileName);
        if (!file.exists() || !file.isFile()) {
            return ResultBean.error("文件不存在");
        }
        if (file.delete()) {
            return ResultBean.success("删除成功");
        } else {
            return ResultBean.error("删除失败");
        }
    }

    @PostMapping("/fields/parseExcel")
    public ResultBean<List<Map<String, Object>>> parseFieldExcel(@RequestParam("file") MultipartFile file,
                                                                   @RequestHeader(value = "token", required = false) String token) {
        String empNo = getEmpNo(token);
        try {
            List<Map<String, Object>> result = new ArrayList<>();
            // 使用 EasyExcel 读取
            File tempFile = File.createTempFile("field_template_", ".xlsx");
            file.transferTo(tempFile);
            List<List<String>> rows = EasyExcel.read(tempFile).sheet(0).doReadSync();
            if (rows.size() > 1) {
                // 第一行是表头，从第二行开始
                for (int i = 1; i < rows.size(); i++) {
                    List<String> row = rows.get(i);
                    if (row == null || row.isEmpty()) continue;
                    Map<String, Object> field = new LinkedHashMap<>();
                    field.put("sortIndex", (i - 1) * 10);
                    field.put("level", 1);
                    // 列0: 字段英文名
                    field.put("fieldKey", row.size() > 0 ? nvl(row.get(0), "") : "");
                    // 列1: 字段描述
                    field.put("fieldName", row.size() > 1 ? nvl(row.get(1), "") : "");
                    // 列2: 长度(字节)
                    Integer length = null;
                    if (row.size() > 2 && !isEmpty(row.get(2))) {
                        try { length = Integer.parseInt(row.get(2).trim()); } catch (NumberFormatException ignored) {}
                    }
                    field.put("length", length);
                    // 列3: 是否必填
                    Integer isRequired = 0;
                    if (row.size() > 3 && !isEmpty(row.get(3))) {
                        String val = row.get(3).trim();
                        if ("1".equals(val) || "是".equals(val) || "必填".equals(val) || "true".equalsIgnoreCase(val)) {
                            isRequired = 1;
                        }
                    }
                    field.put("isRequired", isRequired);
                    // 列4: 规则类型 - 支持中文映射
                    String rawRuleType = row.size() > 4 ? nvl(row.get(4), "").trim() : "";
                    String ruleType = mapRuleType(rawRuleType);
                    field.put("ruleType", ruleType);
                    // 列5: 配置内容
                    field.put("configValue", row.size() > 5 ? nvl(row.get(5), "") : "");
                    // 列6: 说明
                    field.put("remark", row.size() > 6 ? nvl(row.get(6), "") : "");

                    result.add(field);
                }
            }
            tempFile.delete();
            return ResultBean.success(result);
        } catch (Exception e) {
            log.error("Excel 解析失败", e);
            return ResultBean.error("Excel 解析失败: " + e.getMessage());
        }
    }

    @PostMapping("/fields/exportExcel")
    public void exportFieldExcel(@RequestBody Map<String, Object> body,
                                  HttpServletResponse response) {
        try {
            String modelName = (String) body.getOrDefault("modelName", "未命名模型");
            String section = (String) body.getOrDefault("section", "BODY");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> fields = (List<Map<String, Object>>) body.get("fields");

            // 文件名：模型名称_section中文_字段定义_时间戳.xlsx
            String sectionLabel = section;
            switch (section) {
                case "FILENAME": sectionLabel = "文件名"; break;
                case "HEADER": sectionLabel = "文件头"; break;
                case "BODY": sectionLabel = "文件体"; break;
                case "FOOTER": sectionLabel = "文件尾"; break;
                default: break;
            }
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String fileName = modelName + "_" + sectionLabel + "_字段定义_" + timestamp + ".xlsx";

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + URLEncoder.encode(fileName, "UTF-8") + "\"");

            // 表头
            List<List<String>> head = new ArrayList<>();
            head.add(Collections.singletonList("字段英文名"));
            head.add(Collections.singletonList("字段描述"));
            head.add(Collections.singletonList("长度(字节)"));
            head.add(Collections.singletonList("是否必填"));
            head.add(Collections.singletonList("规则类型"));
            head.add(Collections.singletonList("配置内容"));
            head.add(Collections.singletonList("说明"));

            List<List<String>> data = new ArrayList<>();
            if (fields != null) {
                for (Map<String, Object> f : fields) {
                    List<String> row = new ArrayList<>();
                    // 字段英文名
                    row.add(nvl((String) f.get("fieldKey"), ""));
                    // 字段描述
                    row.add(nvl((String) f.get("fieldName"), ""));
                    // 长度
                    Object lengthObj = f.get("length");
                    row.add(lengthObj != null ? String.valueOf(lengthObj) : "");
                    // 是否必填
                    Object isRequired = f.get("isRequired");
                    row.add(isRequired != null && (Integer.valueOf(String.valueOf(isRequired)) == 1) ? "是" : "否");
                    // 规则类型 → 中文
                    String ruleType = nvl((String) f.get("ruleType"), "");
                    row.add(ruleTypeToChinese(ruleType));
                    // 配置内容 → 提取用户可读的配置
                    String configJson = nvl((String) f.get("ruleConfigJson"), "");
                    Object configValue = f.get("configValue");
                    String configDisplay = configValue != null ? String.valueOf(configValue) : extractConfigDisplay(ruleType, configJson);
                    row.add(configDisplay);
                    // 说明
                    row.add(nvl((String) f.get("remark"), ""));

                    data.add(row);
                }
            }

            EasyExcel.write(response.getOutputStream())
                    .head(head)
                    .sheet(sectionLabel + "字段定义")
                    .doWrite(data);
        } catch (IOException e) {
            log.error("导出字段Excel失败", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                response.getWriter().write("{\"code\":500,\"message\":\"导出失败\"}");
            } catch (IOException ignored) {}
        }
    }

    private String ruleTypeToChinese(String ruleType) {
        if (ruleType == null) return "";
        switch (ruleType) {
            case "FIXED": return "固定值";
            case "DATE": return "日期";
            case "ENUM": return "枚举";
            case "SEQUENCE": return "序列号";
            case "RANDOM_CN": return "随机汉字";
            case "RANDOM_NUM": return "随机数字";
            case "RANDOM_UUID": return "随机UUID";
            case "REF_FILE": return "引用文件";
            case "REF_FIELD": return "引用字段";
            case "SUM": return "汇总金额";
            case "COUNT": return "统计行数";
            case "BATCH_NO": return "批次号";
            case "AMOUNT": return "金额";
            case "EXPR": return "表达式";
            default: return ruleType;
        }
    }

    private String extractConfigDisplay(String ruleType, String configJson) {
        if (configJson == null || configJson.isEmpty()) return "";
        try {
            com.alibaba.fastjson.JSONObject cfg = com.alibaba.fastjson.JSONObject.parseObject(configJson);
            switch (ruleType) {
                case "FIXED": return cfg.getString("value");
                case "DATE": return cfg.getString("format");
                case "ENUM": return "";
                case "SEQUENCE":
                    return "前缀=" + cfg.getString("prefix") +
                           ",位数=" + (cfg.getInteger("digitLength") != null ? cfg.getInteger("digitLength") : 6) +
                           ",起始=" + (cfg.getInteger("start") != null ? cfg.getInteger("start") : 1) +
                           ",步长=" + (cfg.getInteger("step") != null ? cfg.getInteger("step") : 1) +
                           ",策略=" + (cfg.getString("updateStrategy") != null ? cfg.getString("updateStrategy") : "PER_LINE");
                case "RANDOM_CN":
                case "RANDOM_NUM": return "count=" + (cfg.getInteger("count") != null ? cfg.getInteger("count") : 3);
                case "RANDOM_UUID": return "upperCase=" + (cfg.getBoolean("upperCase") != null && cfg.getBoolean("upperCase"));
                case "REF_FILE": return "refFileId=" + cfg.getString("refFileId") +
                                    ",columnKey=" + cfg.getString("columnKey");
                case "REF_FIELD": return cfg.getString("targetFieldKey");
                case "SUM": return "targetFieldKey=" + cfg.getString("targetFieldKey") +
                                ",format=" + cfg.getString("format");
                case "COUNT": return "targetFieldKey=" + cfg.getString("targetFieldKey");
                case "BATCH_NO":
                    return "前缀=" + cfg.getString("prefix") +
                           ",起始=" + (cfg.getInteger("start") != null ? cfg.getInteger("start") : 1) +
                           ",策略=" + (cfg.getString("updateStrategy") != null ? cfg.getString("updateStrategy") : "PER_FILE");
                case "AMOUNT": return cfg.getString("format");
                case "EXPR": return cfg.getString("pattern");
                default: return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    // ===================== 工具方法 =====================

    private void downloadFile(File file, HttpServletResponse response) {
        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + URLEncoder.encode(file.getName(), "UTF-8") + "\"");
            byte[] buffer = new byte[4096];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            os.flush();
        } catch (IOException e) {
            log.error("文件下载失败", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            try {
                response.getWriter().write("{\"code\":500,\"message\":\"文件下载失败\"}");
            } catch (IOException ignored) {}
        }
    }

    private String mapRuleType(String chineseType) {
        if (chineseType == null || chineseType.isEmpty()) return "FIXED";
        switch (chineseType) {
            case "固定值": return "FIXED";
            case "日期": return "DATE";
            case "枚举": return "ENUM";
            case "序列号": return "SEQUENCE";
            case "随机汉字": return "RANDOM_CN";
            case "随机数字": return "RANDOM_NUM";
            case "随机UUID": return "RANDOM_UUID";
            case "引用文件": return "REF_FILE";
            case "引用字段": return "REF_FIELD";
            case "汇总金额": return "SUM";
            case "统计行数": return "COUNT";
            case "批次号": return "BATCH_NO";
            case "金额": return "AMOUNT";
            case "表达式": return "EXPR";
            // 兼容旧版中文名称
            case "随机值": return "RANDOM_NUM";
            default: return chineseType.toUpperCase();
        }
    }

    private String nvl(String val, String defaultVal) {
        return val != null ? val : defaultVal;
    }

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    private String getEmpNo(String token) {
        return StringUtils.hasText(token) ? token.trim() : "anonymous";
    }

    // ===================== DTO 内部类 =====================

    public static class ModelDetail {
        private MetaFileModel model;
        private List<MetaFieldDefinition> fields;

        public MetaFileModel getModel() { return model; }
        public void setModel(MetaFileModel model) { this.model = model; }
        public List<MetaFieldDefinition> getFields() { return fields; }
        public void setFields(List<MetaFieldDefinition> fields) { this.fields = fields; }
    }

    public static class ShareRequest {
        private String sharedWith;
        public String getSharedWith() { return sharedWith; }
        public void setSharedWith(String sharedWith) { this.sharedWith = sharedWith; }
    }

    public static class ValidateRequest {
        private Long modelId;
        private List<MetaFieldDefinition> fields;
        public Long getModelId() { return modelId; }
        public void setModelId(Long modelId) { this.modelId = modelId; }
        public List<MetaFieldDefinition> getFields() { return fields; }
        public void setFields(List<MetaFieldDefinition> fields) { this.fields = fields; }
    }

    public static class TemplateFileInfo {
        private String fileName;
        private long fileSize;
        private Date lastModified;

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        public Date getLastModified() { return lastModified; }
        public void setLastModified(Date lastModified) { this.lastModified = lastModified; }
    }
}
