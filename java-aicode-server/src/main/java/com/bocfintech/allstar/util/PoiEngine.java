package com.bocfintech.allstar.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.impl.values.XmlValueDisconnectedException;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通用 Word (docx) 模板填充引擎
 * <p>
 * 基于 Apache POI XWPF，提供以下核心能力：
 * <ol>
 *   <li>段落/表格中的单值占位符替换</li>
 *   <li>动态表格行填充（含点号的列表占位符自动识别为表格循环行）</li>
 *   <li>图片占位符替换（Base64 → 图片）</li>
 *   <li>跨 XWPFRun 断裂占位符的识别和替换（段落级文本重组算法）</li>
 *   <li>空值/null 的优雅降级处理（默认"未获取到数据"，蓝色标记）</li>
 * </ol>
 *
 * <p><b>占位符规范（支持三种语法）：</b>
 * <ul>
 *   <li>{@code ${key}} / {@code $[key]} / {@code {{key}}} — 无点号，单值替换</li>
 *   <li>{@code ${listName.fieldName}} / {@code $[listName.fieldName]} / {@code {{listName.fieldName}}} — 包含点号，表格循环行</li>
 *   <li>图片：{@code ${imageKey}} / {@code $[imageKey]} / {@code {{imageKey}}}</li>
 * </ul>
 *
 * <h3>用法一：统一 dataModel（推荐，兼容 poi-tl 风格）</h3>
 * <pre>{@code
 * Map<String, Object> dataModel = new LinkedHashMap<>();
 * dataModel.put("name", "张三");                        // String → 单值
 *
 * List<Map<String, Object>> tranList = new ArrayList<>();
 * Map<String, Object> row = new LinkedHashMap<>();
 * row.put("serialNumber", "1");
 * row.put("moduleName", "模块A");
 * tranList.add(row);
 * dataModel.put("tran_list", tranList);                // List → 表格
 *
 * try (InputStream is = new FileInputStream("template.docx");
 *      OutputStream os = new FileOutputStream("output.docx")) {
 *     PoiEngine.fillModel(is, os, dataModel);
 * }
 * }</pre>
 *
 * <h3>用法二：拆分的 singleData / listData（旧 API，保持兼容）</h3>
 * <pre>{@code
 * Map<String, String> singleData = new HashMap<>();
 * singleData.put("name", "张三");
 *
 * Map<String, List<Map<String, String>>> listData = new HashMap<>();
 * List<Map<String, String>> rows = new ArrayList<>();
 * Map<String, String> row1 = new HashMap<>();
 * row1.put("serialNumber", "1");
 * rows.add(row1);
 * listData.put("tran_list", rows);
 *
 * PoiEngine.fill(is, os, singleData, listData);
 * }</pre>
 *
 * @since 2026-05-22
 */
@Slf4j
@SuppressWarnings("unused")
public final class PoiEngine {

    private PoiEngine() {
    }

    // ==================== 常量 ====================

    /**
     * 组合占位符正则：同时匹配 ${...}、$[...]、{{...}} 三种语法
     * <ul>
     *   <li>group(1)：${key} 中的 key</li>
     *   <li>group(2)：$[key] 中的 key</li>
     *   <li>group(3)：{{key}} 中的 key</li>
     * </ul>
     */
    private static final Pattern PLACEHOLDER_PATTERN =
            Pattern.compile("\\$\\{([^}]+)\\}|\\$\\[([^\\]]+)\\]|\\{\\{([^}]+)\\}\\}");

    /** null 值默认替换文本 */
    private static final String NULL_VALUE_DEFAULT = "未获取到数据";

    /** null 值标记颜色（蓝色） */
    private static final String NULL_VALUE_COLOR = "0000FF";

    /** 需手工补充红色 */
    private static final String MANUAL_FILL_COLOR = "C00000";

    // ==================== 公共入口（新：统一 dataModel API） ====================

    /**
     * 使用统一 dataModel 填充模板（不含图片）。
     * <p>dataModel 兼容 poi-tl 风格：</p>
     * <ul>
     *   <li>value 为 String / Number / Boolean → 单值替换（对应无点号占位符）</li>
     *   <li>value 为 List<Map> → 列表/表格填充（对应含点号占位符）</li>
     * </ul>
     * <p>模板中支持 {@code ${key}}、{@code $[key]}、{@code {{key}}} 三种语法。</p>
     *
     * @param templateInputStream 模板输入流
     * @param outputStream        输出流
     * @param dataModel           统一数据模型（key → String 或 List<Map>）
     */
    public static void fillModel(InputStream templateInputStream,
                                  OutputStream outputStream,
                                  Map<String, Object> dataModel) {
        fillModel(templateInputStream, outputStream, dataModel, null);
    }

    /**
     * 使用统一 dataModel 填充模板（含图片）。
     *
     * @param templateInputStream 模板输入流
     * @param outputStream        输出流
     * @param dataModel           统一数据模型
     * @param imageData           图片数据，key 对应占位符中的 key
     */
    public static void fillModel(InputStream templateInputStream,
                                  OutputStream outputStream,
                                  Map<String, Object> dataModel,
                                  Map<String, ImageInfo> imageData) {
        Map<String, String> singleData = new LinkedHashMap<>();
        Map<String, List<Map<String, String>>> listData = new LinkedHashMap<>();
        splitDataModel(dataModel, singleData, listData);
        fillInternal(templateInputStream, outputStream, singleData, listData, imageData);
    }

    // ==================== 公共入口（旧：拆分 API，保留向后兼容） ====================

    /**
     * 填充模板（不含图片）—— 兼容旧调用。
     *
     * @param templateInputStream 模板输入流
     * @param outputStream        输出流
     * @param singleData          单值数据
     * @param listData            列表数据
     */
    public static void fill(InputStream templateInputStream,
                            OutputStream outputStream,
                            Map<String, String> singleData,
                            Map<String, List<Map<String, String>>> listData) {
        fill(templateInputStream, outputStream, singleData, listData, null);
    }

    /**
     * 填充模板（含图片）—— 兼容旧调用。
     *
     * @param templateInputStream 模板输入流
     * @param outputStream        输出流
     * @param singleData          单值数据
     * @param listData            列表数据
     * @param imageData           图片数据
     */
    public static void fill(InputStream templateInputStream,
                            OutputStream outputStream,
                            Map<String, String> singleData,
                            Map<String, List<Map<String, String>>> listData,
                            Map<String, ImageInfo> imageData) {
        fillInternal(templateInputStream, outputStream, singleData, listData, imageData);
    }

    // ==================== 内部核心处理 ====================

    private static void fillInternal(InputStream templateInputStream,
                                     OutputStream outputStream,
                                     Map<String, String> singleData,
                                     Map<String, List<Map<String, String>>> listData,
                                     Map<String, ImageInfo> imageData) {
        XWPFDocument document = null;
        try {
            document = new XWPFDocument(templateInputStream);

            // 1. 处理 singleData 中以 IMAGE_ 开头的 base64 图片（在文本替换前执行）
            if (singleData != null && !singleData.isEmpty()) {
                handleImageBase64FromSingleData(document, singleData);
            }

            // 2. 图片（通过 ImageInfo）
            if (imageData != null && !imageData.isEmpty()) {
                replaceImagePlaceholders(document, imageData);
            }

            // 3. 表格：优先匹配动态表格（含 xxx.yyy 点号占位符），否则静态替换
            Set<String> listNames = listData != null ? listData.keySet() : Collections.<String>emptySet();
            for (XWPFTable table : document.getTables()) {
                String detectedListName = detectListName(table, listNames);
                if (detectedListName != null && listData != null) {
                    handleDynamicTable(table, detectedListName, listData.get(detectedListName));
                } else {
                    handleStaticTable(table, singleData);
                }
            }

            // 4. 段落
            if (singleData != null && !singleData.isEmpty()) {
                replaceParagraphPlaceholders(document, singleData);
            }

            // 5. 输出
            document.write(outputStream);
            outputStream.flush();

        } catch (Exception e) {
            log.error("Word 模板填充失败", e);
            throw new RuntimeException("Word 模板填充失败: " + e.getMessage(), e);
        } finally {
            closeQuietly(document);
        }
    }

    // ==================== dataModel 拆分 ====================

    /**
     * 将统一 dataModel 拆分为 singleData（单值）和 listData（列表）。
     * <p>规则：</p>
     * <ul>
     *   <li>value 为 null → singleData 空字符串</li>
     *   <li>value 为 List → listData（内部 Map 的 value 转为 String）</li>
     *   <li>其他 → singleData（toString）</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    private static void splitDataModel(Map<String, Object> dataModel,
                                       Map<String, String> singleData,
                                       Map<String, List<Map<String, String>>> listData) {
        if (dataModel == null) return;
        for (Map.Entry<String, Object> entry : dataModel.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value == null) {
                singleData.put(key, "");
            } else if (value instanceof List) {
                List<?> rawList = (List<?>) value;
                List<Map<String, String>> rows = new ArrayList<>();
                for (Object item : rawList) {
                    if (item instanceof Map) {
                        Map<String, Object> rowMap = (Map<String, Object>) item;
                        Map<String, String> stringRow = new LinkedHashMap<>();
                        for (Map.Entry<String, Object> rowEntry : rowMap.entrySet()) {
                            Object v = rowEntry.getValue();
                            stringRow.put(rowEntry.getKey(), v != null ? v.toString() : "");
                        }
                        rows.add(stringRow);
                    }
                }
                if (!rows.isEmpty()) {
                    listData.put(key, rows);
                }
            } else {
                singleData.put(key, value.toString());
            }
        }
    }

    // ==================== 段落替换 ====================

    private static void replaceParagraphPlaceholders(XWPFDocument document, Map<String, String> dataMap) {
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            List<XWPFRun> runs = paragraph.getRuns();
            if (runs == null || runs.isEmpty()) continue;

            String paragraphText = getParagraphFullText(paragraph);
            if (paragraphText.isEmpty()) continue;

            Matcher matcher = PLACEHOLDER_PATTERN.matcher(paragraphText);
            StringBuffer sb = new StringBuffer();
            boolean hasMissing = false;

            while (matcher.find()) {
                String rawKey = extractPlaceholderKey(matcher);
                if (rawKey == null) continue;

                String value;

                // 有点号表示列表字段，段落中列表占位符替换为标记文本
                if (rawKey.contains(".")) {
                    value = "[" + rawKey + "]";
                } else {
                    value = dataMap.get(rawKey);
                }

                if (value != null) {
                    if (rawKey.startsWith("delete")) {
                        matcher.appendReplacement(sb, "");
                    } else if (rawKey.startsWith("IMAGE_PLACEHOLDER")) {
                        matcher.appendReplacement(sb, "");
                    } else if (rawKey.startsWith("IMAGE_DESC")) {
                        String descValue = dataMap.get(rawKey);
                        matcher.appendReplacement(sb,
                                Matcher.quoteReplacement(descValue != null ? descValue : ""));
                    } else {
                        matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
                    }
                } else {
                    if (rawKey.startsWith("IMAGE_") || rawKey.startsWith("delete")) {
                        matcher.appendReplacement(sb, "");
                    } else {
                        matcher.appendReplacement(sb, Matcher.quoteReplacement(NULL_VALUE_DEFAULT));
                        hasMissing = true;
                    }
                }
            }
            matcher.appendTail(sb);

            String replacedText = sb.toString();
            if (replacedText.equals(paragraphText)) continue;

            for (int i = runs.size() - 1; i >= 0; i--) {
                paragraph.removeRun(i);
            }
            if (!replacedText.isEmpty()) {
                XWPFRun newRun = paragraph.createRun();
                setTextWithNewLines(newRun, replacedText);
                if (hasMissing) newRun.setColor(NULL_VALUE_COLOR);
            }
        }
    }

    // ==================== 表格处理 ====================

    /**
     * 从表格模板行（第 2 行）检测第一个包含点号的 listName。
     * <p>支持三种占位符语法，例如：</p>
     * <ul>
     *   <li>{@code ${tran_list.serialNumber}} → 返回 "tran_list"</li>
     *   <li>{@code $[tran_list.serialNumber]} → 返回 "tran_list"</li>
     *   <li>{@code {{tran_list.serialNumber}}} → 返回 "tran_list"</li>
     * </ul>
     */
    private static String detectListName(XWPFTable table, Set<String> candidateListNames) {
        if (table.getRows().size() < 2) return null;
        XWPFTableRow templateRow = table.getRow(1);
        if (templateRow == null) return null;

        String rowText = getRowFullText(templateRow);
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(rowText);
        while (matcher.find()) {
            String rawKey = extractPlaceholderKey(matcher);
            if (rawKey == null) continue;
            // 有点号 → 列表
            int dotIdx = rawKey.indexOf('.');
            if (dotIdx > 0) {
                String listName = rawKey.substring(0, dotIdx);
                if (candidateListNames.isEmpty() || candidateListNames.contains(listName)) {
                    return listName;
                }
            }
        }
        return null;
    }

    /**
     * 处理动态表格：根据列表数据行数自动复制模板行并填充。
     */
    private static void handleDynamicTable(XWPFTable table, String listName,
                                            List<Map<String, String>> rows) {
        XWPFTableRow templateRow = table.getRow(1);
        if (templateRow == null) return;

        int rowCount = (rows != null) ? rows.size() : 0;
        int cellCount = templateRow.getTableCells().size();

        if (rowCount > 0) {
            for (int i = 0; i < rowCount; i++) {
                XWPFTableRow newRow = table.createRow();
                ensureRowCellCount(newRow, cellCount);
                copyRowAndFill(templateRow, newRow, listName, rows.get(i));
            }
            table.removeRow(1);
        } else {
            fillEmptyTemplateRow(templateRow, listName);
        }
    }

    /**
     * 复制模板行到新行，替换所有含点号的占位符为当前行数据。
     */
    private static void copyRowAndFill(XWPFTableRow templateRow, XWPFTableRow newRow,
                                        String listName, Map<String, String> rowData) {
        List<XWPFTableCell> templateCells = templateRow.getTableCells();

        for (int j = 0; j < templateCells.size(); j++) {
            XWPFTableCell templateCell = templateCells.get(j);
            XWPFTableCell newCell = newRow.getCell(j);

            clearCellContent(newCell);

            for (XWPFParagraph templatePara : templateCell.getParagraphs()) {
                XWPFParagraph newPara = newCell.addParagraph();
                newPara.setAlignment(templatePara.getAlignment());

                String cellText = getParagraphFullText(templatePara);
                if (cellText.isEmpty()) continue;

                // 替换所有 ${listName.xxx} / $[listName.xxx] / {{listName.xxx}} → 实际值
                String replacedText = replaceListPlaceholders(cellText, listName, rowData);

                XWPFRun newRun = newPara.createRun();
                List<XWPFRun> templateRuns = templatePara.getRuns();
                if (templateRuns != null && !templateRuns.isEmpty()) {
                    copyRunStyle(templateRuns.get(0), newRun);
                }
                newRun.setText(replacedText, 0);
            }
        }
    }

    /**
     * 替换文本中所有与指定 listName 相关的点号占位符。
     * <p>支持三种语法：{@code ${listName.field}}、{@code $[listName.field]}、{@code {{listName.field}}}</p>
     */
    private static String replaceListPlaceholders(String text, String listName, Map<String, String> rowData) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String rawKey = extractPlaceholderKey(matcher);
            if (rawKey == null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }

            int dotIdx = rawKey.indexOf('.');
            if (dotIdx > 0) {
                String ln = rawKey.substring(0, dotIdx);
                if (ln.equals(listName)) {
                    String fieldName = rawKey.substring(dotIdx + 1);
                    String value = rowData.get(fieldName);
                    matcher.appendReplacement(sb,
                            Matcher.quoteReplacement(value != null ? value : NULL_VALUE_DEFAULT));
                    continue;
                }
            }
            // 不是目标列表的占位符，原样保留
            matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 静态表格替换（无列表行循环）。
     * <p>只替换无点号的单值占位符，含点号的列表占位符原样保留。</p>
     */
    private static void handleStaticTable(XWPFTable table, Map<String, String> dataMap) {
        if (dataMap == null || dataMap.isEmpty()) return;

        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                for (XWPFParagraph paragraph : cell.getParagraphs()) {
                    String paragraphText = getParagraphFullText(paragraph);
                    if (paragraphText.isEmpty()) continue;

                    Matcher m = PLACEHOLDER_PATTERN.matcher(paragraphText);
                    StringBuffer sb = new StringBuffer();
                    while (m.find()) {
                        String rawKey = extractPlaceholderKey(m);
                        if (rawKey == null) {
                            m.appendReplacement(sb, Matcher.quoteReplacement(m.group(0)));
                            continue;
                        }
                        // 有点号 = 列表占位符，静态表格中不做替换
                        if (rawKey.contains(".")) {
                            m.appendReplacement(sb, Matcher.quoteReplacement(m.group(0)));
                        } else {
                            String val = dataMap.get(rawKey);
                            m.appendReplacement(sb, Matcher.quoteReplacement(
                                    val != null ? val : NULL_VALUE_DEFAULT));
                        }
                    }
                    m.appendTail(sb);

                    String replaced = sb.toString();
                    if (replaced.equals(paragraphText)) continue;

                    List<XWPFRun> runs = paragraph.getRuns();
                    for (int i = runs.size() - 1; i >= 0; i--) {
                        paragraph.removeRun(i);
                    }
                    if (!replaced.isEmpty()) {
                        XWPFRun newRun = paragraph.createRun();
                        newRun.setText(replaced, 0);
                    }
                }
            }
        }
    }

    /**
     * 列表为空时，将模板行中所有含点号的占位符替换为"未获取到数据"。
     * <p>支持三种语法：{@code ${listName.xxx}}、{@code $[listName.xxx]}、{@code {{listName.xxx}}}</p>
     */
    private static void fillEmptyTemplateRow(XWPFTableRow row, String listName) {
        for (XWPFTableCell cell : row.getTableCells()) {
            for (XWPFParagraph paragraph : cell.getParagraphs()) {
                String text = getParagraphFullText(paragraph);
                if (!containsListPlaceholder(text, listName)) continue;

                List<XWPFRun> runs = paragraph.getRuns();
                for (int i = runs.size() - 1; i >= 0; i--) {
                    paragraph.removeRun(i);
                }
                XWPFRun newRun = paragraph.createRun();
                newRun.setText(NULL_VALUE_DEFAULT, 0);
                newRun.setColor(NULL_VALUE_COLOR);
            }
        }
    }

    /**
     * 检查文本中是否包含指定 listName 的点号占位符（任意语法）。
     */
    private static boolean containsListPlaceholder(String text, String listName) {
        Matcher m = PLACEHOLDER_PATTERN.matcher(text);
        while (m.find()) {
            String rawKey = extractPlaceholderKey(m);
            if (rawKey != null) {
                int dotIdx = rawKey.indexOf('.');
                if (dotIdx > 0 && rawKey.substring(0, dotIdx).equals(listName)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ==================== 图片 ====================

    /**
     * 处理 singleData 中以 IMAGE_ 开头的 base64 图片占位符。
     * <p>识别 {@code ${IMAGE_xxx}} / {@code $[IMAGE_xxx]} / {@code {{IMAGE_xxx}}} 三种语法，
     * 当 value 是 base64 编码字符串时，解码并插入图片。</p>
     * <p>IMAGE_DESC_xxx 描述占位符不在此处理，由段落文本替换阶段处理。</p>
     */
    private static void handleImageBase64FromSingleData(XWPFDocument document, Map<String, String> singleData) {
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            List<XWPFRun> runs = paragraph.getRuns();
            if (runs == null) continue;

            for (int i = 0; i < runs.size(); i++) {
                XWPFRun run = runs.get(i);
                String text = run.getText(0);
                if (text == null) continue;

                for (Map.Entry<String, String> entry : singleData.entrySet()) {
                    String key = entry.getKey();
                    // 只处理 IMAGE_ 开头且不是 IMAGE_DESC 或 IMAGE_PLACEHOLDER 的 key
                    if (!key.startsWith("IMAGE_")
                            || key.startsWith("IMAGE_DESC")
                            || key.startsWith("IMAGE_PLACEHOLDER")) {
                        continue;
                    }
                    String base64Value = entry.getValue();
                    if (base64Value == null || base64Value.isEmpty()) continue;

                    // 三种占位符语法
                    String[] placeholders = {
                            "${" + key + "}",
                            "$[" + key + "]",
                            "{{" + key + "}}"
                    };
                    for (String ph : placeholders) {
                        if (text.contains(ph)) {
                            text = text.replace(ph, "");
                            run.setText("", 0);
                            try {
                                byte[] imageBytes = base64ToBytes(base64Value);
                                String suffix = detectImageSuffix(imageBytes);
                                int pictureType = getPictureType(suffix);
                                try (InputStream is = new ByteArrayInputStream(imageBytes)) {
                                    run.addPicture(is, pictureType, key,
                                            Units.toEMU(450), Units.toEMU(320));
                                }
                            } catch (Exception e) {
                                log.error("base64 图片插入失败, key={}", key, e);
                                run.setText("图片加载失败");
                                run.setColor(MANUAL_FILL_COLOR);
                            }
                            // 占位符后面可能还有文本，写入剩余文本
                            if (!text.isEmpty()) {
                                XWPFRun remainingRun = paragraph.createRun();
                                remainingRun.setText(text, 0);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 将 base64 字符串解码为字节数组。
     */
    private static byte[] base64ToBytes(String base64Str) {
        return Base64.getDecoder().decode(base64Str);
    }

    /**
     * 根据图片字节的魔术头检测图片格式后缀。
     *
     * @return "png" / "jpg" / "gif" / "bmp"，默认 "png"
     */
    private static String detectImageSuffix(byte[] bytes) {
        if (bytes == null || bytes.length < 4) return "png";
        // PNG: 89 50 4E 47
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
            return "png";
        }
        // JPEG: FF D8 FF
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        // GIF: 47 49 46
        if (bytes[0] == 0x47 && bytes[1] == 0x49 && bytes[2] == 0x46) {
            return "gif";
        }
        // BMP: 42 4D
        if (bytes[0] == 0x42 && bytes[1] == 0x4D) {
            return "bmp";
        }
        return "png";
    }

    private static void replaceImagePlaceholders(XWPFDocument document, Map<String, ImageInfo> imageData) {
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            List<XWPFRun> runs = paragraph.getRuns();
            if (runs == null) continue;

            for (int i = 0; i < runs.size(); i++) {
                XWPFRun run = runs.get(i);
                String text = run.getText(0);
                if (text == null) continue;

                for (Map.Entry<String, ImageInfo> entry : imageData.entrySet()) {
                    String key = entry.getKey();
                    ImageInfo info = entry.getValue();

                    // 描述占位符（三种语法）
                    String[] descPlaceholders = {
                            "${" + key + "_DESC}",
                            "$[" + key + "_DESC]",
                            "{{" + key + "_DESC}}"
                    };
                    for (String descPH : descPlaceholders) {
                        if (text.contains(descPH)) {
                            text = text.replace(descPH,
                                    info.getDescription() != null ? info.getDescription() : "");
                        }
                    }

                    // 图片占位符（三种语法）
                    String[] imgPlaceholders = {
                            "${" + key + "}",
                            "$[" + key + "]",
                            "{{" + key + "}}"
                    };
                    for (String imgPH : imgPlaceholders) {
                        if (text.contains(imgPH)) {
                            text = text.replace(imgPH, "");
                            run.setText("", 0);
                            try (InputStream is = new ByteArrayInputStream(info.getImageBytes())) {
                                run.addPicture(is, getPictureType(info.getSuffix()), info.getFileName(),
                                        Units.toEMU(info.getWidthMm()), Units.toEMU(info.getHeightMm()));
                            } catch (Exception e) {
                                log.error("图片插入失败, key={}", key, e);
                                run.setText("图片加载失败");
                                run.setColor(MANUAL_FILL_COLOR);
                            }
                            if (!text.isEmpty()) {
                                XWPFRun remainingRun = paragraph.createRun();
                                remainingRun.setText(text, 0);
                            }
                        }
                    }
                }
            }
        }
    }

    private static int getPictureType(String suffix) {
        if (suffix == null) return XWPFDocument.PICTURE_TYPE_PNG;
        switch (suffix.toLowerCase()) {
            case "png":  return XWPFDocument.PICTURE_TYPE_PNG;
            case "jpg": case "jpeg": return XWPFDocument.PICTURE_TYPE_JPEG;
            case "gif":  return XWPFDocument.PICTURE_TYPE_GIF;
            case "bmp":  return XWPFDocument.PICTURE_TYPE_BMP;
            default:     return XWPFDocument.PICTURE_TYPE_PNG;
        }
    }

    // ==================== 占位符工具 ====================

    /**
     * 从组合正则的 Matcher 中提取占位符 key。
     * <p>三种语法的捕获组分别位于 group(1)、group(2)、group(3)，返回第一个非空的。</p>
     */
    private static String extractPlaceholderKey(Matcher matcher) {
        String key = matcher.group(1);
        if (key != null) return key;
        key = matcher.group(2);
        if (key != null) return key;
        return matcher.group(3);
    }

    // ==================== 文本工具 ====================

    private static String getCellFullText(XWPFTableCell cell) {
        StringBuilder sb = new StringBuilder();
        for (XWPFParagraph p : cell.getParagraphs()) sb.append(getParagraphFullText(p));
        return sb.toString();
    }

    private static String getParagraphFullText(XWPFParagraph paragraph) {
        StringBuilder sb = new StringBuilder();
        for (XWPFRun run : paragraph.getRuns()) {
            String text = run.getText(0);
            if (text != null) sb.append(text);
        }
        return sb.toString();
    }

    private static String getRowFullText(XWPFTableRow row) {
        StringBuilder sb = new StringBuilder();
        for (XWPFTableCell cell : row.getTableCells()) sb.append(getCellFullText(cell));
        return sb.toString();
    }

    private static void clearCellContent(XWPFTableCell cell) {
        List<XWPFParagraph> paragraphs = cell.getParagraphs();
        for (int i = paragraphs.size() - 1; i >= 0; i--) cell.removeParagraph(i);
    }

    private static void ensureRowCellCount(XWPFTableRow row, int targetCount) {
        int current = row.getTableCells().size();
        for (int i = current; i < targetCount; i++) row.createCell();
    }

    // ==================== 样式 ====================

    private static void copyRunStyle(XWPFRun source, XWPFRun target) {
        target.setBold(source.isBold());
        target.setItalic(source.isItalic());
        target.setStrikeThrough(source.isStrikeThrough());
        int fontSize = source.getFontSize();
        if (fontSize > 0) target.setFontSize(fontSize);
        String fontFamily = source.getFontFamily();
        if (fontFamily != null) target.setFontFamily(fontFamily);
        String color = source.getColor();
        if (color != null) target.setColor(color);
        UnderlinePatterns underline = source.getUnderline();
        if (underline != null) target.setUnderline(underline);
    }

    private static void setTextWithNewLines(XWPFRun run, String text) {
        if (text == null || text.isEmpty()) { run.setText("", 0); return; }
        try {
            if (text.contains("\n")) {
                String[] lines = text.split("\n");
                for (int i = 0; i < lines.length; i++) {
                    if (i > 0 && !lines[i].isEmpty()) run.addCarriageReturn();
                    run.setText(lines[i], i == 0 ? 0 : -1);
                }
            } else {
                run.setText(text, 0);
            }
        } catch (XmlValueDisconnectedException e) {
            log.warn("XWPFRun 已断开连接");
        }
    }

    private static void closeQuietly(XWPFDocument doc) {
        if (doc != null) try { doc.close(); } catch (IOException ignored) {}
    }

    // ==================== ImageInfo ====================

    public static class ImageInfo {
        private byte[] imageBytes;
        private String fileName;
        private String suffix;
        private double widthMm = 120;
        private double heightMm = 80;
        private String description;

        public ImageInfo() {}
        public ImageInfo(byte[] imageBytes, String fileName, String suffix, double widthMm, double heightMm) {
            this.imageBytes = imageBytes; this.fileName = fileName; this.suffix = suffix;
            this.widthMm = widthMm; this.heightMm = heightMm;
        }
        public byte[] getImageBytes() { return imageBytes; }
        public void setImageBytes(byte[] imageBytes) { this.imageBytes = imageBytes; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getSuffix() { return suffix; }
        public void setSuffix(String suffix) { this.suffix = suffix; }
        public double getWidthMm() { return widthMm; }
        public void setWidthMm(double widthMm) { this.widthMm = widthMm; }
        public double getHeightMm() { return heightMm; }
        public void setHeightMm(double heightMm) { this.heightMm = heightMm; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
