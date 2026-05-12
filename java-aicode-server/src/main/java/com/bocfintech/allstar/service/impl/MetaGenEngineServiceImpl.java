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

        GenerationContext ctx = new GenerationContext(model, fields, 3, true, operator);
        ctx.init();

        StringBuilder sb = new StringBuilder();
        // 生成3行Body (Header/Footer暂略于预览，仅Body校验格式)
        List<MetaFieldDefinition> bodyFields = ctx.getSectionFields("BODY");
        for (int row = 0; row < 3; row++) {
            StringBuilder line = new StringBuilder();
            for (MetaFieldDefinition f : bodyFields) {
                if (f.getLevel() != null && f.getLevel() == 1) {
                    line.append(generateFieldValue(ctx, f, row));
                }
            }
            // 处理子字段 (level 2)
            for (MetaFieldDefinition f : bodyFields) {
                if (f.getLevel() != null && f.getLevel() == 2) {
                    // 子字段值替换到父字段中
                    String val = generateFieldValue(ctx, f, row);
                    // 子字段值不做单独行拼接，在父字段占位时处理
                }
            }
            sb.append(line.toString());
            if (row < 2) sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public MetaEntityFile generateAsync(Long modelId, Integer rowCount, String batchName, String operator) {
        MetaFileModel model = modelService.getById(modelId);
        if (model == null) throw new IllegalArgumentException("模型不存在");
        if (!"PUBLISHED".equals(model.getStatus())) throw new IllegalStateException("模型未发布，无法生成");
        if (rowCount == null || rowCount <= 0) rowCount = 1;
        if (rowCount > model.getMaxRowsLimit()) throw new IllegalArgumentException("超过最大行数限制: " + model.getMaxRowsLimit());

        String fileName = buildFileName(model, batchName, operator);
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
            for (MetaFieldDefinition f : fields) {
                String val = generateFieldValue(ctx, f, 0);
                line.append(val);
            }
            writer.write(line.toString());
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
            for (MetaFieldDefinition f : fields) {
                String val = generateFieldValue(ctx, f, 0);
                line.append(val);
            }
            writer.write(line.toString());
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
                rawVal = field.getFieldKey(); // 固定值即key本身
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
            case "AMOUNT":
                rawVal = generateAmount(ruleCfg);
                break;
            case "EXPRESSION":
                rawVal = generateExpression(ctx, ruleCfg);
                break;
            default:
                rawVal = "";
        }

        // 定长/补齐处理
        return applyPadding(field, rawVal, ctx.encoding);
    }

    private String generateDate(JSONObject cfg) {
        String fmt = cfg != null && cfg.getString("format") != null ? cfg.getString("format") : "yyyyMMdd";
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(fmt);
        return LocalDateTime.now().format(dtf);
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
        if (ctx.refFileCache == null) ctx.refFileCache = new HashMap<>();
        Long refFileId = field.getRefFileId();
        if (refFileId == null) return "";

        List<String> columnData = ctx.refFileCache.get(refFileId);
        if (columnData == null) {
            // 加载引用文件
            MetaRefFile refFile = refFileService.getById(refFileId);
            if (refFile == null) return "";
            columnData = loadRefFileColumn(refFile, field);
            ctx.refFileCache.put(refFileId, columnData);
        }
        if (columnData.isEmpty()) return "";
        int idx = rowIndex % columnData.size();
        return columnData.get(idx);
    }

    private List<String> loadRefFileColumn(MetaRefFile refFile, MetaFieldDefinition field) {
        List<String> data = new ArrayList<>();
        JSONObject mapping = JSON.parseObject(refFile.getColumnMapping());
        if (mapping == null) return data;
        // 查找当前fieldKey对应的列索引
        Integer colIdx = null;
        for (String key : mapping.keySet()) {
            if (key.equals(field.getRefFieldKey()) || key.equals(field.getFieldKey())) {
                colIdx = mapping.getInteger(key);
                break;
            }
        }
        if (colIdx == null) colIdx = 1;

        try (BufferedReader reader = new BufferedReader(new FileReader(refFile.getFilePath()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] cols;
                if ("DELIMITER".equals(refFile.getParseType())) {
                    String delim = refFile.getDelimiter() != null ? refFile.getDelimiter() : ",";
                    cols = line.split(delim, -1);
                } else {
                    // FIXED: 简单按位置取
                    cols = new String[]{line};
                }
                if (colIdx <= cols.length) {
                    data.add(cols[colIdx - 1].trim());
                }
            }
        } catch (IOException e) {
            log.error("加载引用文件失败: {}", refFile.getFilePath(), e);
        }
        return data;
    }

    private String generateSeq(GenerationContext ctx, MetaFieldDefinition field, int rowIndex) {
        String targetId = field.getModelId() + ":" + field.getFieldKey();
        return String.valueOf(seqService.nextValue("SEQ", targetId, ctx.isPreview));
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

    private String generateAmount(JSONObject cfg) {
        // DSL: { "type": "AMOUNT", "config": { "precision": 2, "scale": 14, "decimalMode": "IMPLICIT", "signed": false, "padding": "ZERO_LEFT" } }
        JSONObject config = cfg != null ? cfg.getJSONObject("config") : null;
        if (config == null) config = cfg; // 兼容直接传config
        int precision = config != null && config.getInteger("precision") != null ? config.getInteger("precision") : 2;
        int scale = config != null && config.getInteger("scale") != null ? config.getInteger("scale") : 14;
        String decimalMode = config != null ? config.getString("decimalMode") : "IMPLICIT";
        boolean signed = config != null && Boolean.TRUE.equals(config.getBoolean("signed"));

        // 生成随机金额
        Random rnd = new Random();
        long maxInt = (long) Math.pow(10, scale) - 1;
        long intPart = (long)(rnd.nextDouble() * maxInt);
        long fracPart = rnd.nextInt((int) Math.pow(10, precision));
        long totalInSmallestUnit = intPart * (long) Math.pow(10, precision) + fracPart;

        if ("IMPLICIT".equals(decimalMode)) {
            // 隐式小数点: 1234567 表示 12345.67
            return String.format("%0" + scale + "d", totalInSmallestUnit);
        } else {
            // 显式小数点
            String intStr = String.format("%0" + (scale - precision) + "d", intPart);
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
        int byteLen = field.getLength() != null ? field.getLength() : rawVal.length();
        String paddingDir = field.getPaddingDirection() != null ? field.getPaddingDirection() : "NONE";
        String paddingChar = field.getPaddingChar() != null ? field.getPaddingChar() : " ";

        Charset charset;
        try {
            charset = Charset.forName(encoding);
        } catch (Exception e) {
            charset = Charset.forName("UTF-8");
        }

        byte[] rawBytes = rawVal.getBytes(charset);
        if (rawBytes.length > byteLen) {
            // 溢出：报错，不截断
            throw new RuntimeException("字段 [" + field.getFieldKey() + "] 生成内容字节长度 " + rawBytes.length + " 超过定义长度 " + byteLen);
        }

        if (rawBytes.length == byteLen || "NONE".equals(paddingDir)) {
            return rawVal;
        }

        int padCount = byteLen - rawBytes.length;
        byte padByte = paddingChar.getBytes(charset)[0];
        byte[] padded = new byte[byteLen];

        if ("LEFT".equals(paddingDir)) {
            Arrays.fill(padded, 0, padCount, padByte);
            System.arraycopy(rawBytes, 0, padded, padCount, rawBytes.length);
        } else {
            // RIGHT
            System.arraycopy(rawBytes, 0, padded, 0, rawBytes.length);
            Arrays.fill(padded, rawBytes.length, byteLen, padByte);
        }
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

    private String buildFileName(MetaFileModel model, String batchName, String operator) {
        String ts = LocalDateTime.now().format(DF_DATETIME);
        String name = model.getModelName() + "_" + ts;
        if (batchName != null && !batchName.isEmpty()) name += "_" + batchName;
        name += ".txt";
        return name;
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
        Map<Long, List<String>> refFileCache;
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
