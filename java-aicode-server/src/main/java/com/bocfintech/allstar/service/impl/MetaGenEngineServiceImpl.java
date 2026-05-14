package com.bocfintech.allstar.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bocfintech.allstar.entity.*;
import com.bocfintech.allstar.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class MetaGenEngineServiceImpl implements MetaGenEngineService {

    @Value("${meta.storage.preview:./storage/preview}")
    private String previewDir;

    @Value("${meta.storage.formal:./storage/formal}")
    private String formalDir;

    @Autowired
    private MetaFileModelService modelService;

    @Autowired
    private MetaFieldDefinitionService fieldService;

    @Autowired
    private MetaEnumLibraryService enumService;

    @Autowired
    private MetaRefFileService refFileService;

    @Autowired
    private MetaSequenceTrackerService seqService;

    @Autowired
    private MetaEntityFileService entityFileService;

    private static final DateTimeFormatter DF_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DF_DATETIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public String preview(Long modelId, String operator) {
        MetaFileModel model = modelService.getById(modelId);
        if (model == null) throw new IllegalArgumentException("模型不存在");

        List<MetaFieldDefinition> fields = fieldService.listByModelId(modelId);
        if (fields.isEmpty()) throw new IllegalArgumentException("模型无字段定义");

        // 预览时只生成3行Body
        GenerationContext ctx = new GenerationContext(model, fields, 3, true, operator);
        ctx.init();

        StringBuilder fullPreviewContent = new StringBuilder();

        // 1. 生成文件名
        String fileName = buildFileName(ctx, "PREVIEW", operator); // 预览文件使用特殊批次名
        fullPreviewContent.append("文件名: ").append(fileName).append(ctx.lineEnding).append(ctx.lineEnding);

        // 2. 生成文件头
        List<MetaFieldDefinition> headerFields = ctx.getSectionFields("HEADER");
        if (model.getHasHeader() != null && model.getHasHeader() == 1 && !headerFields.isEmpty()) {
            fullPreviewContent.append("--- 文件头 (HEADER) ！！最终文件是没有这行的！！---").append(ctx.lineEnding);
            fullPreviewContent.append(generateSectionContent(ctx, headerFields)).append(ctx.lineEnding).append(ctx.lineEnding);
        }

        // 3. 生成文件体 (3行)
        List<MetaFieldDefinition> bodyFields = ctx.getSectionFields("BODY");
        if (!bodyFields.isEmpty()) {
            fullPreviewContent.append("--- 文件体 (BODY) ！！最终文件是没有这行的！！---").append(ctx.lineEnding);
            for (int row = 0; row < 3; row++) {
                StringBuilder line = new StringBuilder();
                Map<String, String> rowValues = new LinkedHashMap<>(); // 用于存储当前行字段值，供子字段替换
                for (MetaFieldDefinition f : bodyFields) {
                    String val = generateFieldValue(ctx, f, row);
                    rowValues.put(f.getFieldKey(), val);
                    if (f.getLevel() != null && f.getLevel() == 1) {
                        line.append(val);
                    }
                }
                // 处理子字段 (level 2)
                String lineStr = substituteLevel2(bodyFields, rowValues, line.toString());
                fullPreviewContent.append(lineStr);
                if (row < 2) fullPreviewContent.append(ctx.lineEnding); // 最后一行不加换行
            }
            fullPreviewContent.append(ctx.lineEnding).append(ctx.lineEnding);
        }


        // 4. 生成文件尾
        List<MetaFieldDefinition> footerFields = ctx.getSectionFields("FOOTER");
        if (model.getHasFooter() != null && model.getHasFooter() == 1 && !footerFields.isEmpty()) {
            fullPreviewContent.append("--- 文件尾 (FOOTER) ！！最终文件是没有这行的！！---").append(ctx.lineEnding);
            fullPreviewContent.append(generateSectionContent(ctx, footerFields)).append(ctx.lineEnding);
        }

        return fullPreviewContent.toString();
    }

    private String generateSectionContent(GenerationContext ctx, List<MetaFieldDefinition> fields) {
        StringBuilder line = new StringBuilder();
        Map<String, String> rowValues = new LinkedHashMap<>();
        for (MetaFieldDefinition f : fields) {
            String val = generateFieldValue(ctx, f, 0);
            rowValues.put(f.getFieldKey(), val);
            if (f.getLevel() != null && f.getLevel() == 1) {
                line.append(val);
            }
        }
        return substituteLevel2(fields, rowValues, line.toString());
    }

    @Override
    public MetaEntityFile generateAsync(Long modelId, Integer rowCount, String batchName, String operator) {
        MetaFileModel model = modelService.getById(modelId);
        if (model == null) throw new IllegalArgumentException("模型不存在");
        if (!"PUBLISHED".equals(model.getStatus())) throw new IllegalStateException("模型未发布，无法生成");
        if (rowCount == null || rowCount <= 0) rowCount = 1;
        if (rowCount > model.getMaxRowsLimit()) throw new IllegalArgumentException("超过最大行数限制: " + model.getMaxRowsLimit());

        List<MetaFieldDefinition> fields = fieldService.listByModelId(modelId); // 获取 fields 列表
        GenerationContext tempCtx = new GenerationContext(model, fields, rowCount, false, operator); // 构建临时 ctx
        tempCtx.init(); // 初始化 ctx，生成 FILENAME 字段值

        String fileName = buildFileName(tempCtx, batchName, operator); // 使用新的调用方式
        MetaEntityFile record = entityFileService.createRecord(modelId, fileName, "FORMAL", operator);
        // 异步执行
        doGenerate(record.getId(), modelId, rowCount, fileName, operator);
        return record;
    }

    @Async
    public void doGenerate(Long recordId, Long modelId, Integer rowCount, String fileName, String operator) {
        long startMs = System.currentTimeMillis();
        MetaFileModel model = modelService.getById(modelId);
        List<MetaFieldDefinition> fields = fieldService.listByModelId(modelId);
        GenerationContext ctx = new GenerationContext(model, fields, rowCount, false, operator);
        ctx.init();

        String encoding = model.getEncoding() != null ? model.getEncoding() : "UTF-8";
        File tmpDir = new File(formalDir + "/tmp");
        tmpDir.mkdirs();

        File headerTmp = new File(tmpDir, recordId + "_header.tmp");
        File bodyTmp = new File(tmpDir, recordId + "_body.tmp");
        File footerTmp = new File(tmpDir, recordId + "_footer.tmp");
        File finalFile = new File(formalDir, fileName);

        try {
            // Step 3: 生成Body (流式写盘 + 统计)
            generateBodyToFile(ctx, bodyTmp, encoding);
            // Step 5: 生成Footer（此时汇总值已出）
            generateFooterToFile(ctx, footerTmp, encoding);
            // Step 6: 生成Header（回填汇总值）
            generateHeaderToFile(ctx, headerTmp, encoding);
            // Step 7: 合并文件
            mergeFiles(finalFile, encoding, headerTmp, bodyTmp, footerTmp);

            // 更新数据库
            MetaEntityFile record = entityFileService.getById(recordId);
            record.setStatus("SUCCESS");
            record.setStoragePath(finalFile.getAbsolutePath());
            record.setRowCount(rowCount);
            record.setDurationMs((int)(System.currentTimeMillis() - startMs));
            entityFileService.updateById(record);

            // 清理临时文件
            headerTmp.delete();
            bodyTmp.delete();
            footerTmp.delete();

            log.info("文件生成成功: {} ({}行, {}ms)", fileName, rowCount, record.getDurationMs());
        } catch (Exception e) {
            log.error("文件生成失败: {}", fileName, e);
            MetaEntityFile record = entityFileService.getById(recordId);
            record.setStatus("FAILED");
            record.setErrorMsg(e.getMessage());
            record.setDurationMs((int)(System.currentTimeMillis() - startMs));
            entityFileService.updateById(record);
            // 清理临时文件
            headerTmp.delete();
            bodyTmp.delete();
            footerTmp.delete();
        }
    }

    @Override
    public MetaEntityFile getStatus(Long taskId) {
        return entityFileService.getById(taskId);
    }

    @Override
    public String getFilePath(Long fileId) {
        MetaEntityFile record = entityFileService.getById(fileId);
        if (record == null) return null;
        if ("RUNNING".equals(record.getStatus())) throw new IllegalStateException("文件正在生成中，请稍后再试");
        return record.getStoragePath();
    }

    @Override
    public void deleteFile(Long fileId, String operator) {
        MetaEntityFile record = entityFileService.getById(fileId);
        if (record == null) return;
        // 删除物理文件
        if (record.getStoragePath() != null) {
            try { Files.deleteIfExists(Paths.get(record.getStoragePath())); } catch (IOException ignored) {}
        }
        if (record.getTempPath() != null) {
            try { Files.deleteIfExists(Paths.get(record.getTempPath())); } catch (IOException ignored) {}
        }
        entityFileService.removeById(fileId);
    }

    // ===================== 文件区块生成 =====================

    private void generateBodyToFile(GenerationContext ctx, File file, String encoding) throws IOException {
        List<MetaFieldDefinition> fields = ctx.getSectionFields("BODY");
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), encoding))) {
            for (int row = 0; row < ctx.totalRows; row++) {
                StringBuilder line = new StringBuilder();
                Map<String, String> rowValues = new LinkedHashMap<>();
                for (MetaFieldDefinition f : fields) {
                    String val = generateFieldValue(ctx, f, row);
                    rowValues.put(f.getFieldKey(), val);
                    if (f.getLevel() != null && f.getLevel() == 1) {
                        line.append(val);
                    }
                }
                // 处理 level 2 子字段替换
                String lineStr = substituteLevel2(fields, rowValues, line.toString());
                writer.write(lineStr);
                if (row < ctx.totalRows - 1) {
                    writer.write(ctx.lineEnding);
                }
                // 每1000行flush一次
                if ((row + 1) % 1000 == 0) {
                    writer.flush();
                }
            }
            writer.flush();
        }
    }

    private void generateFooterToFile(GenerationContext ctx, File file, String encoding) throws IOException {
        List<MetaFieldDefinition> fields = ctx.getSectionFields("FOOTER");
        if (fields.isEmpty()) {
            file.createNewFile();
            return;
        }
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), encoding))) {
            StringBuilder line = new StringBuilder();
            Map<String, String> rowValues = new LinkedHashMap<>(); // 添加 rowValues
            for (MetaFieldDefinition f : fields) {
                String val = generateFieldValue(ctx, f, 0);
                rowValues.put(f.getFieldKey(), val); // 存储字段值
                if (f.getLevel() != null && f.getLevel() == 1) {
                    line.append(val);
                }
            }
            // 处理 level 2 子字段替换
            String lineStr = substituteLevel2(fields, rowValues, line.toString()); // 调用 substituteLevel2
            writer.write(lineStr);
            writer.flush();
        }
    }

    private void generateHeaderToFile(GenerationContext ctx, File file, String encoding) throws IOException {
        List<MetaFieldDefinition> fields = ctx.getSectionFields("HEADER");
        if (fields.isEmpty()) {
            file.createNewFile();
            return;
        }
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), encoding))) {
            StringBuilder line = new StringBuilder();
            Map<String, String> rowValues = new LinkedHashMap<>(); // 添加 rowValues
            for (MetaFieldDefinition f : fields) {
                String val = generateFieldValue(ctx, f, 0);
                rowValues.put(f.getFieldKey(), val); // 存储字段值
                if (f.getLevel() != null && f.getLevel() == 1) {
                    line.append(val);
                }
            }
            // 处理 level 2 子字段替换
            String lineStr = substituteLevel2(fields, rowValues, line.toString()); // 调用 substituteLevel2
            writer.write(lineStr);
            writer.flush();
        }
    }

    private void mergeFiles(File output, String encoding, File header, File body, File footer) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output), encoding))) {
            appendFile(writer, header, encoding);
            appendFile(writer, body, encoding);
            appendFile(writer, footer, encoding);
            writer.flush();
        }
    }

    private void appendFile(BufferedWriter writer, File file, String encoding) throws IOException {
        if (!file.exists() || file.length() == 0) return;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (!first) writer.newLine();
                writer.write(line);
                first = false;
            }
        }
    }

    // ===================== 字段值生成 =====================

    private String generateFieldValue(GenerationContext ctx, MetaFieldDefinition field, int rowIndex) {
        String rawVal;
        String ruleType = field.getRuleType();
        JSONObject ruleCfg = parseRuleConfig(field.getRuleConfigJson());

        switch (ruleType) {
            case "FIXED":
                rawVal = ruleCfg != null ? ruleCfg.getString("value") : "";
                break;
            case "DATE":
                rawVal = generateDate(ruleCfg);
                break;
            case "ENUM":
                rawVal = generateEnum(field.getRefEnumKey(), ruleCfg);
                break;
            case "REF_FILE":
                rawVal = generateRefFile(ctx, field, rowIndex);
                break;
            case "REF_FIELD":
                rawVal = ctx.getFieldValue(field.getRefFieldKey());
                break;
            case "SEQ":
            case "SEQUENCE": // 兼容前端 SEQUENCE 类型
                rawVal = generateSeq(ctx, field, rowIndex);
                break;
            case "SUM":
                rawVal = String.valueOf(ctx.bodySumAmount);
                break;
            case "COUNT":
                rawVal = String.valueOf(ctx.totalRows);
                break;
            case "RANDOM":
                rawVal = generateRandom(ruleCfg);
                break;
            case "RANDOM_NUM": { // 随机数字
                JSONObject numCfg = ruleCfg != null ? ruleCfg : new JSONObject();
                if (numCfg.getString("mode") == null) numCfg.put("mode", "DIGIT");
                rawVal = generateRandom(numCfg);
                break;
            }
            case "RANDOM_CN": { // 随机汉字
                JSONObject cnCfg = ruleCfg != null ? ruleCfg : new JSONObject();
                if (cnCfg.getString("mode") == null) cnCfg.put("mode", "CHINESE");
                rawVal = generateRandom(cnCfg);
                break;
            }
            case "RANDOM_UUID": { // 随机UUID
                JSONObject uuidCfg = ruleCfg != null ? ruleCfg : new JSONObject();
                if (uuidCfg.getString("mode") == null) uuidCfg.put("mode", "UUID");
                rawVal = generateRandom(uuidCfg);
                break;
            }
            case "AMOUNT":
                rawVal = generateAmount(ctx, ruleCfg);
                break;
            case "EXPRESSION":
            case "EXPR": // 兼容前端 EXPR 类型
                rawVal = generateExpression(ctx, ruleCfg);
                break;
            default:
                log.warn("未知规则类型: {} (字段: {}), 将生成空值", ruleType, field.getFieldKey());
                rawVal = "";
        }

        // 存储生成的字段值，供后续 REF_FIELD 等引用
        ctx.setFieldValue(field.getFieldKey(), rawVal);

        // 定长/补齐处理
        return applyPadding(field, rawVal, ctx.encoding);
    }

    private String generateDate(JSONObject cfg) {
        String fmt = cfg != null && cfg.getString("format") != null ? cfg.getString("format") : "yyyyMMdd";
        int offset = cfg != null && cfg.getInteger("offset") != null ? cfg.getInteger("offset") : 0;
        
        LocalDateTime date = LocalDateTime.now().plusDays(offset);
        
        // 处理 D 模式（年中天数）宽度不足问题
        // Java 的 DateTimeFormatter 中 D 表示年中天数（1-366），但只支持 1-2 位
        // 当年中天数超过 99 时（如 5 月 13 日是第 133 天），需要手动处理
        if (fmt.contains("D")) {
            // 手动格式化年中天数为 3 位（补零），因为最大天数是 366
            int dayOfYear = date.getDayOfYear();
            String dayStr = String.format("%03d", dayOfYear);
            // 使用占位符替换 D，格式化后再替换回来
            // 使用一个不会出现的字符序列作为占位符
            String placeholder = "\u0001DAY\u0002"; // 使用控制字符包围的 DAY
            String fmtWithPlaceholder = fmt.replaceAll("D+", placeholder);
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern(fmtWithPlaceholder);
            String result = date.format(dtf);
            return result.replace(placeholder, dayStr);
        }
        
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(fmt);
        return date.format(dtf);
    }

    private String generateEnum(String enumKey, JSONObject cfg) {
        if (enumKey == null) return "";
        MetaEnumLibrary enumLib = enumService.lambdaQuery().eq(MetaEnumLibrary::getEnumKey, enumKey).one();
        if (enumLib == null) return "";
        JSONArray items = JSON.parseArray(enumLib.getItems());
        if (items == null || items.isEmpty()) return "";
        // 随机选一个
        int idx = new Random().nextInt(items.size());
        return items.getJSONObject(idx).getString("val");
    }

    private String generateRefFile(GenerationContext ctx, MetaFieldDefinition field, int rowIndex) {
        // 从 ruleConfigJson 中读取 columnKey 和 refFileId
        JSONObject ruleCfg = parseRuleConfig(field.getRuleConfigJson());
        String columnKey = ruleCfg != null ? ruleCfg.getString("columnKey") : null;
        // refFileId 优先从 ruleCfg 取，兼容从 field 属性取
        Long refFileId = (ruleCfg != null && ruleCfg.getLong("refFileId") != null)
                ? ruleCfg.getLong("refFileId") : field.getRefFileId();
        if (refFileId == null) {
            log.warn("REF_FILE 字段 [{}] 未配置 refFileId，跳过", field.getFieldKey());
            return "";
        }
        if (columnKey == null || columnKey.isEmpty()) {
            log.warn("REF_FILE 字段 [{}] 未配置 columnKey，跳过", field.getFieldKey());
            return "";
        }

        // 缓存 key = refFileId + ":" + columnKey，区分同一文件不同列
        String cacheKey = refFileId + ":" + columnKey;
        List<String> columnData = ctx.refFileColumnCache.get(cacheKey);
        if (columnData == null) {
            MetaRefFile refFile = refFileService.getById(refFileId);
            if (refFile == null) {
                log.warn("REF_FILE 字段 [{}] 引用文件 id={} 不存在", field.getFieldKey(), refFileId);
                return "";
            }
            columnData = loadRefFileColumn(refFile, columnKey, ctx);
            ctx.refFileColumnCache.put(cacheKey, columnData);
        }
        if (columnData.isEmpty()) return "";
        int idx = rowIndex % columnData.size();
        return columnData.get(idx);
    }

    /**
     * 从引用文件加载指定列的所有行数据。
     * <ul>
     *   <li>DELIMITER 模式 column_mapping: {"fieldName": columnIndex, ...}（1-based）</li>
     *   <li>FIXED 模式 column_mapping: {"fieldName": {"start": startByte, "length": byteLen}, ...}（1-based byte offset）</li>
     * </ul>
     */
    private List<String> loadRefFileColumn(MetaRefFile refFile, String columnKey, GenerationContext ctx) {
        List<String> data = new ArrayList<>();
        JSONObject mapping = refFile.getColumnMapping() != null
                ? JSON.parseObject(refFile.getColumnMapping()) : null;

        boolean isDelimiter = "DELIMITER".equals(refFile.getParseType());

        if (isDelimiter) {
            // DELIMITER 模式：column_mapping = {"fieldName": columnIndex, ...}
            int colIdx = 1; // 默认第1列
            if (mapping != null && mapping.containsKey(columnKey)) {
                Integer v = mapping.getInteger(columnKey);
                if (v != null) colIdx = v;
            }
            String delim = refFile.getDelimiter() != null ? refFile.getDelimiter() : ",";
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(refFile.getFilePath()), ctx.encoding))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    String[] cols = line.split(delim, -1);
                    if (colIdx <= cols.length) {
                        data.add(cols[colIdx - 1].trim());
                    }
                }
            } catch (IOException e) {
                log.error("加载引用文件失败 (DELIMITER): {}", refFile.getFilePath(), e);
            }
        } else {
            // FIXED 模式：column_mapping = {"fieldName": {"start": N, "length": M}, ...}（字节位置，1-based）
            int start = 0;
            int length = -1;
            if (mapping != null && mapping.containsKey(columnKey)) {
                JSONObject posObj = mapping.getJSONObject(columnKey);
                if (posObj != null) {
                    start = posObj.getInteger("start") != null ? posObj.getInteger("start") - 1 : 0; // 转为0-based
                    length = posObj.getInteger("length") != null ? posObj.getInteger("length") : -1;
                }
            }
            Charset charset;
            try { charset = Charset.forName(ctx.encoding); } catch (Exception e) { charset = Charset.forName("UTF-8"); }
            final int finalStart = start;
            final int finalLength = length;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(refFile.getFilePath()), ctx.encoding))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    byte[] lineBytes = line.getBytes(charset);
                    if (finalStart >= lineBytes.length) {
                        data.add("");
                        continue;
                    }
                    int end = (finalLength > 0)
                            ? Math.min(finalStart + finalLength, lineBytes.length)
                            : lineBytes.length;
                    String val = new String(lineBytes, finalStart, end - finalStart, charset).trim();
                    data.add(val);
                }
            } catch (IOException e) {
                log.error("加载引用文件失败 (FIXED): {}", refFile.getFilePath(), e);
            }
        }
        return data;
    }

    private String generateSeq(GenerationContext ctx, MetaFieldDefinition field, int rowIndex) {
        String targetId = field.getModelId() + ":" + field.getFieldKey();
        long sequenceValue = seqService.nextValue("SEQ", targetId, ctx.isPreview);

        JSONObject ruleCfg = parseRuleConfig(field.getRuleConfigJson());
        String prefix = "";
        int digitLength = 0;

        if (ruleCfg != null) {
            prefix = ruleCfg.getString("prefix") != null ? ruleCfg.getString("prefix") : "";
            digitLength = ruleCfg.getInteger("digitLength") != null ? ruleCfg.getInteger("digitLength") : 0;
        }

        if (digitLength > 0) {
            return String.format("%s%0" + digitLength + "d", prefix, sequenceValue);
        } else {
            return prefix + sequenceValue;
        }
    }

    private String generateRandom(JSONObject cfg) {
        String mode = cfg != null ? cfg.getString("mode") : "DIGIT";
        int len = cfg != null && cfg.getInteger("length") != null ? cfg.getInteger("length") : 10;
        Random rnd = new Random();
        if ("CHINESE".equals(mode)) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < len; i++) {
                sb.append((char)(0x4e00 + rnd.nextInt(0x9fa5 - 0x4e00)));
            }
            return sb.toString();
        }
        if ("UUID".equals(mode)) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        // DIGIT
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(rnd.nextInt(10));
        }
        return sb.toString();
    }

    private String generateAmount(GenerationContext ctx, JSONObject cfg) {
        // DSL: { "type": "AMOUNT", "config": { "precision": 2, "scale": 14, "decimalMode": "IMPLICIT", "signed": false, "padding": "ZERO_LEFT" } }
        JSONObject config = cfg; // 兼容直接传config
        if (config == null) config = new JSONObject(); // 避免空指针

        int precision = config.getInteger("precision") != null ? config.getInteger("precision") : 2;
        int totalLength = config.getInteger("scale") != null ? config.getInteger("scale") : 16; // 这里的scale现在是总位数
        String decimalMode = config.getString("decimalMode") != null ? config.getString("decimalMode") : "IMPLICIT";
        // boolean signed = config.getBoolean("signed") != null ? config.getBoolean("signed") : false; // 暂时不用

        // 生成随机金额
        Random rnd = new Random();
        // 计算整数部分的最大值，确保生成的随机数不会超过总长度
        long maxIntPart = (long) Math.pow(10, totalLength - precision) - 1;
        long intPart = (long)(rnd.nextDouble() * maxIntPart);
        long fracPart = rnd.nextInt((int) Math.pow(10, precision));

        if ("IMPLICIT".equals(decimalMode)) {
            // 隐式小数点: 1234567 表示 12345.67
            // totalInSmallestUnit = intPart * (long) Math.pow(10, precision) + fracPart;
            // 直接拼接整数和小数部分，然后格式化为总长度
            String combined = String.format("%d%0" + precision + "d", intPart, fracPart);
            // 手动左补零，因为 %0 标志不适用于 %s 转换符
            int currentLength = combined.getBytes(Charset.forName(ctx.encoding)).length;
            if (currentLength < totalLength) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < totalLength - currentLength; i++) {
                    sb.append('0');
                }
                sb.append(combined);
                return sb.toString();
            } else {
                return combined;
            }
        } else {
            // 显式小数点
            // 整数部分长度 = 总长度 - 小数位数 - 1 (小数点)
            int intPartLength = totalLength - precision - 1;
            String intStr = String.format("%0" + intPartLength + "d", intPart);
            String fracStr = String.format("%0" + precision + "d", fracPart);
            return intStr + "." + fracStr;
        }
    }

    private String generateExpression(GenerationContext ctx, JSONObject cfg) {
        String func = cfg != null ? cfg.getString("func") : "CONCAT";
        JSONArray params = cfg != null ? cfg.getJSONArray("params") : null;
        if (params == null) return "";

        if ("CONCAT".equals(func)) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < params.size(); i++) {
                String p = params.getString(i);
                if (p.startsWith("${") && p.endsWith("}")) {
                    String key = p.substring(2, p.length() - 1);
                    String val = ctx.getFieldValue(key);
                    sb.append(val != null ? val : "");
                } else {
                    sb.append(p);
                }
            }
            return sb.toString();
        }
        return "";
    }

    // ===================== 补齐/截断处理 =====================

    private String applyPadding(MetaFieldDefinition field, String rawVal, String encoding) {
        if (rawVal == null) rawVal = "";
        int byteLen = field.getLength() != null ? field.getLength() : -1;
        String paddingDir = field.getPaddingDirection() != null ? field.getPaddingDirection() : "NONE";
        String paddingChar = field.getPaddingChar() != null ? field.getPaddingChar() : " ";

        // 未配置长度或不需要补齐，直接返回原值
        if (byteLen <= 0 || "NONE".equals(paddingDir)) {
            return rawVal;
        }

        Charset charset;
        try {
            charset = Charset.forName(encoding);
        } catch (Exception e) {
            charset = Charset.forName("UTF-8");
        }

        byte[] rawBytes = rawVal.getBytes(charset);
        // 不再限制码值长度，超过部分将被截断
        if (rawBytes.length > byteLen) {
            // 截断到指定字节长度
            byte[] truncated = new byte[byteLen];
            System.arraycopy(rawBytes, 0, truncated, 0, byteLen);
            return new String(truncated, charset);
        }
        if (rawBytes.length == byteLen) {
            return rawVal; // 已满，无需补位
        }

        // 补位字符取第一个字节（要求 paddingChar 为单字节 ASCII 字符，如空格/零）
        byte padByte = (byte) paddingChar.charAt(0);

        int padCount = byteLen - rawBytes.length;
        byte[] padded = new byte[byteLen];

        if ("LEFT".equals(paddingDir)) {
            Arrays.fill(padded, 0, padCount, padByte);
            System.arraycopy(rawBytes, 0, padded, padCount, rawBytes.length);
        } else {
            // RIGHT
            System.arraycopy(rawBytes, 0, padded, 0, rawBytes.length);
            Arrays.fill(padded, rawBytes.length, byteLen, padByte);
        }
        // 将补位后的字节数组重新解码为 String（确保写出时不会二次乱码）
        return new String(padded, charset);
    }

    private String substituteLevel2(List<MetaFieldDefinition> fields, Map<String, String> rowValues, String parentLine) {
        // 对level2字段的值替换到父字段行中（简单占位替换策略）
        // 此处为简化实现：将parentLine按父字段长度展开，用子字段值覆盖对应位置
        return parentLine; // TODO: 完整实现需按偏移量替换
    }

    private JSONObject parseRuleConfig(String json) {
        if (json == null || json.trim().isEmpty()) return null;
        try {
            return JSON.parseObject(json);
        } catch (Exception e) {
            return null;
        }
    }

    private String buildFileName(GenerationContext ctx, String batchName, String operator) {
        // 从 ctx 中获取 FILENAME 区块的字段值来构建文件名
        List<MetaFieldDefinition> fileNameFields = ctx.getSectionFields("FILENAME");
        if (!fileNameFields.isEmpty()) {
            StringBuilder nameBuilder = new StringBuilder();
            for (MetaFieldDefinition f : fileNameFields) {
                // generateFieldValue 已经在 GenerationContext.init() 中调用并存储了结果
                String val = ctx.getFieldValue(f.getFieldKey());
                if (val != null) {
                    nameBuilder.append(val);
                }
            }
            // 检查生成的文件名是否已经包含扩展名
            String generatedName = nameBuilder.toString();
            if (!generatedName.contains(".")) { // 如果没有显式扩展名，则自动添加 .txt
                generatedName += ".txt";
            }
            return generatedName;
        } else {
            // 如果没有配置 FILENAME 区块，则回退到原来的逻辑
            String ts = LocalDateTime.now().format(DF_DATETIME);
            String name = ctx.model.getModelName() + "_" + ts;
            if (batchName != null && !batchName.isEmpty()) name += "_" + batchName;
            return name + ".txt"; // 默认加 .txt 后缀
        }
    }

    // ===================== 内部类: 生成上下文 =====================

    class GenerationContext {
        MetaFileModel model;
        List<MetaFieldDefinition> fields;
        int totalRows;
        boolean isPreview;
        String operator;
        String encoding;
        String lineEnding;

        // 运行时值存储
        Map<String, String> fieldValues = new LinkedHashMap<>();
        Map<Long, List<String>> refFileCache; // 保留旧字段以防止编译错误（已不再使用）
        Map<String, List<String>> refFileColumnCache = new HashMap<>(); // 新缓存: refFileId:fieldKey -> 列数据
        long bodySumAmount = 0;
        long bodyCount = 0;

        GenerationContext(MetaFileModel model, List<MetaFieldDefinition> fields, int totalRows, boolean isPreview, String operator) {
            this.model = model;
            this.fields = fields;
            this.totalRows = totalRows;
            this.isPreview = isPreview;
            this.operator = operator;
            this.encoding = model.getEncoding() != null ? model.getEncoding() : "UTF-8";
            this.lineEnding = model.getLineEndingChar() != null ? model.getLineEndingChar() : "\r\n";
        }

        void init() {
            // 注册虚拟变量
            fieldValues.put("BODY_SUM_AMOUNT", "0");
            fieldValues.put("BODY_COUNT", "0");
            // 预生成FileName区块的值
            for (MetaFieldDefinition f : getSectionFields("FILENAME")) {
                fieldValues.put(f.getFieldKey(), generateFieldValue(this, f, 0));
            }
        }

        List<MetaFieldDefinition> getSectionFields(String section) {
            List<MetaFieldDefinition> list = new ArrayList<>();
            for (MetaFieldDefinition f : fields) {
                if (section.equals(f.getSection())) {
                    list.add(f);
                }
            }
            return list;
        }

        String getFieldValue(String key) {
            return fieldValues.get(key);
        }

        void setFieldValue(String key, String value) {
            fieldValues.put(key, value);
        }
    }
}
