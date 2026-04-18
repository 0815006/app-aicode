package com.bocfintech.allstar.service.impl;

import com.bocfintech.allstar.dialect.DatabaseDialect;
import com.bocfintech.allstar.dialect.DialectFactory;
import com.bocfintech.allstar.dialect.GaussDbDialect;
import com.bocfintech.allstar.entity.*;
import com.bocfintech.allstar.service.DataCompareResultService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DataCompareResultServiceImpl implements DataCompareResultService {

    @Override
    public List<CompareResultItem> compareTableStructure(
            DbConnectionConfig sourceConfig,
            DbConnectionConfig targetConfig,
            List<String> tableNames) {

        if (tableNames == null || tableNames.isEmpty()) {
            return Collections.emptyList();
        }

        List<CompareResultItem> results = new ArrayList<>();
        Map<String, List<ColumnMeta>> sourceColumns = getTableColumns(sourceConfig, tableNames);
        Map<String, List<ColumnMeta>> targetColumns = getTableColumns(targetConfig, tableNames);

        for (String tableName : tableNames) {
            CompareResultItem item = new CompareResultItem();
            item.setTableName(tableName);

            List<ColumnMeta> srcCols = sourceColumns.getOrDefault(tableName, Collections.emptyList());
            List<ColumnMeta> tgtCols = targetColumns.getOrDefault(tableName, Collections.emptyList());

            // 比对逻辑
            boolean isMatch = isColumnListEqual(srcCols, tgtCols);
            item.setIsMatch(isMatch);

            if (isMatch) {
                item.setDetail("结构一致");
            } else {
                item.setDetail(generateStructureDiffDetail(srcCols, tgtCols));
            }

            results.add(item);
        }

        return results;
    }

    @Override
    public List<CompareResultItem> compareRowCount(
            DbConnectionConfig sourceConfig,
            DbConnectionConfig targetConfig,
            List<String> tableNames) {

        if (tableNames == null || tableNames.isEmpty()) {
            return Collections.emptyList();
        }

        List<CompareResultItem> results = new ArrayList<>();
        Map<String, Long> sourceCounts = getTableRowCounts(sourceConfig, tableNames);
        Map<String, Long> targetCounts = getTableRowCounts(targetConfig, tableNames);

        for (String tableName : tableNames) {
            CompareResultItem item = new CompareResultItem();
            item.setTableName(tableName);

            Long srcCount = sourceCounts.get(tableName);
            Long tgtCount = targetCounts.get(tableName);

            if (srcCount == null || tgtCount == null) {
                item.setIsMatch(false);
                item.setSourceRowCount(srcCount != null ? srcCount : 0L);
                item.setTargetRowCount(tgtCount != null ? tgtCount : 0L);
                if (srcCount == null && tgtCount == null) {
                    item.setDetail("源库和目标库均无法获取行数（表可能不存在）");
                } else if (srcCount == null) {
                    item.setDetail("源库表不存在或无法访问");
                } else {
                    item.setDetail("目标库表不存在或无法访问");
                }
            } else {
                boolean isMatch = srcCount.equals(tgtCount);
                item.setIsMatch(isMatch);
                item.setSourceRowCount(srcCount);
                item.setTargetRowCount(tgtCount);

                if (isMatch) {
                    item.setDetail("行数一致：" + srcCount);
                } else {
                    item.setDetail("源库：" + srcCount + "，目标库：" + tgtCount);
                }
            }

            results.add(item);
        }

        return results;
    }

    @Override
    public List<CompareResultItem> compareFullData(
            DbConnectionConfig sourceConfig,
            DbConnectionConfig targetConfig,
            List<String> tableNames) {

        if (tableNames == null || tableNames.isEmpty()) {
            return Collections.emptyList();
        }

        List<CompareResultItem> results = new ArrayList<>();

        for (String tableName : tableNames) {
            CompareResultItem item = new CompareResultItem();
            item.setTableName(tableName);

            try {
                String diffDetail = fullDataCompareOneTable(sourceConfig, targetConfig, tableName);
                boolean isMatch = diffDetail == null;
                item.setIsMatch(isMatch);
                item.setDetail(isMatch ? "数据完全一致" : diffDetail);
            } catch (Exception e) {
                item.setIsMatch(false);
                item.setDetail("比对异常：" + e.getMessage());
                log.error("完整数据比对失败: {}.{}", sourceConfig.getDatabaseName(), tableName, e);
            }

            results.add(item);
        }

        return results;
    }

    // ==================== 私有方法 ====================

    /**
     * 获取指定数据库中多个表的字段元信息
     */
    private Map<String, List<ColumnMeta>> getTableColumns(DbConnectionConfig config, List<String> tableNames) {
        Map<String, List<ColumnMeta>> result = new HashMap<>();
        DatabaseDialect dialect = DialectFactory.getDialect(config);
        String jdbcUrl = dialect.buildJdbcUrl(config);
        String username = config.getUsername();
        String password = decryptPassword(config.getPassword());
        String databaseName = config.getDatabaseName();

        String sql = dialect.getColumnMetaSql(databaseName, "?");

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (String tableName : tableNames) {
                // 注意：有些方言的 SQL 可能不支持占位符，这里简单处理
                String currentSql = dialect.getColumnMetaSql(databaseName, tableName);
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(currentSql)) {
                    List<ColumnMeta> columns = new ArrayList<>();
                    while (rs.next()) {
                        ColumnMeta col = new ColumnMeta();
                        col.setColumnName(rs.getString("COLUMN_NAME"));
                        col.setDataType(rs.getString("DATA_TYPE"));
                        col.setIsNullable("YES".equals(rs.getString("IS_NULLABLE")));
                        col.setColumnDefault(rs.getString("COLUMN_DEFAULT"));
                        col.setColumnKey(rs.getString("COLUMN_KEY"));
                        col.setExtra(rs.getString("EXTRA"));
                        col.setOrdinalPosition(rs.getInt("ORDINAL_POSITION"));
                        col.setColumnComment(rs.getString("COLUMN_COMMENT"));
                        columns.add(col);
                    }
                    result.put(tableName, columns);
                }
            }
        } catch (SQLException e) {
            log.error("获取表结构失败", e);
            throw new RuntimeException("获取表字段信息失败：" + e.getMessage(), e);
        }

        return result;
    }

    /**
     * 获取多个表的行数
     */
    private Map<String, Long> getTableRowCounts(DbConnectionConfig config, List<String> tableNames) {
        Map<String, Long> result = new HashMap<>();
        DatabaseDialect dialect = DialectFactory.getDialect(config);
        String jdbcUrl = dialect.buildJdbcUrl(config);
        String username = config.getUsername();
        String password = decryptPassword(config.getPassword());

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement()) {

            for (String tableName : tableNames) {
                String sql = "SELECT COUNT(*) FROM " + dialect.quote(tableName);
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    if (rs.next()) {
                        result.put(tableName, rs.getLong(1));
                    }
                } catch (SQLException e) {
                    log.warn("统计表 {} 行数失败: {}", tableName, e.getMessage());
                }
            }
        } catch (SQLException e) {
            log.error("数据库连接失败或执行异常", e);
        }

        return result;
    }

    /**
     * 完整数据比对（逐行比对，基于主键排序）
     */
    private String fullDataCompareOneTable(
            DbConnectionConfig sourceConfig,
            DbConnectionConfig targetConfig,
            String tableName) {

        DatabaseDialect srcDialect = DialectFactory.getDialect(sourceConfig);
        DatabaseDialect tgtDialect = DialectFactory.getDialect(targetConfig);

        String sourceUrl = srcDialect.buildJdbcUrl(sourceConfig);
        String targetUrl = tgtDialect.buildJdbcUrl(targetConfig);

        // 尝试获取主键
        String pkColumn = getPrimaryKeyColumn(sourceConfig, srcDialect, tableName);
        if (pkColumn == null) {
            pkColumn = getFirstColumn(sourceConfig, srcDialect, tableName);
        }
        if (pkColumn == null) {
            throw new IllegalArgumentException("表不存在或无字段：" + tableName);
        }

        String selectSql = "SELECT * FROM " + srcDialect.quote(tableName) + " ORDER BY " + srcDialect.quote(pkColumn);
        String targetSelectSql = "SELECT * FROM " + tgtDialect.quote(tableName) + " ORDER BY " + tgtDialect.quote(pkColumn);

        return compareDataWithSql(sourceUrl, targetUrl,
                sourceConfig.getUsername(), decryptPassword(sourceConfig.getPassword()),
                targetConfig.getUsername(), decryptPassword(targetConfig.getPassword()),
                selectSql, targetSelectSql, pkColumn);
    }

    /**
     * 获取主键列名
     */
    private String getPrimaryKeyColumn(DbConnectionConfig config, DatabaseDialect dialect, String tableName) {
        String sql = dialect.getPrimaryKeyColumnSql(config.getDatabaseName(), tableName);
        return getColumnBySql(config, dialect, sql);
    }

    /**
     * 获取第一个字段作为排序依据
     */
    private String getFirstColumn(DbConnectionConfig config, DatabaseDialect dialect, String tableName) {
        // 简单起见，这里仍然使用 information_schema，或者可以扩展方言
        String sql = "SELECT COLUMN_NAME FROM information_schema.COLUMNS " +
                     "WHERE TABLE_SCHEMA = '" + config.getDatabaseName() + "' AND TABLE_NAME = '" + tableName + "' " +
                     "ORDER BY ORDINAL_POSITION LIMIT 1";
        if (dialect instanceof GaussDbDialect) {
            sql = "SELECT column_name FROM information_schema.columns " +
                  "WHERE table_catalog = '" + config.getDatabaseName() + "' AND table_name = '" + tableName + "' " +
                  "ORDER BY ordinal_position LIMIT 1";
        }
        return getColumnBySql(config, dialect, sql);
    }

    /**
     * 通用查询单列
     */
    private String getColumnBySql(DbConnectionConfig config, DatabaseDialect dialect, String sql) {
        String jdbcUrl = dialect.buildJdbcUrl(config);
        try (Connection conn = DriverManager.getConnection(jdbcUrl, config.getUsername(), decryptPassword(config.getPassword()));
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            log.warn("查询元数据失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 执行两库SQL并比对结果集，返回首个差异详情
     */
    private String compareDataWithSql(
            String sourceUrl, String targetUrl,
            String srcUser, String srcPass,
            String tgtUser, String tgtPass,
            String srcSql, String tgtSql, String pkColumn) {

        try (Connection srcConn = DriverManager.getConnection(sourceUrl, srcUser, srcPass);
             Connection tgtConn = DriverManager.getConnection(targetUrl, tgtUser, tgtPass);
             PreparedStatement srcPs = srcConn.prepareStatement(srcSql);
             PreparedStatement tgtPs = tgtConn.prepareStatement(tgtSql);
             ResultSet srcRs = srcPs.executeQuery();
             ResultSet tgtRs = tgtPs.executeQuery()) {

            while (true) {
                boolean srcHas = srcRs.next();
                boolean tgtHas = tgtRs.next();

                if (!srcHas && !tgtHas) break;

                // 1. 行数不一致的情况
                if (srcHas != tgtHas) {
                    String side = srcHas ? "源库" : "目标库";
                    Object pkValue = srcHas ? srcRs.getObject(pkColumn) : tgtRs.getObject(pkColumn);
                    return side + "多出记录 (首个差异主键 " + pkColumn + "=" + pkValue + ")";
                }

                // 2. 字段值不一致的情况
                int colCount = srcRs.getMetaData().getColumnCount();
                for (int i = 1; i <= colCount; i++) {
                    Object srcVal = srcRs.getObject(i);
                    Object tgtVal = tgtRs.getObject(i);
                    if (!Objects.equals(srcVal, tgtVal)) {
                        String colName = srcRs.getMetaData().getColumnName(i);
                        Object pkValue = srcRs.getObject(pkColumn);
                        return "主键 " + pkColumn + "=" + pkValue + " 的行不一致: 字段 [" + colName + "] 值不同 (" + srcVal + " -> " + tgtVal + ")";
                    }
                }
            }
            return null; // 一致
        } catch (SQLException e) {
            log.error("完整数据比对异常", e);
            throw new RuntimeException("数据比对执行失败：" + e.getMessage(), e);
        }
    }

    /**
     * 判断两列集合是否完全一致
     */
    private boolean isColumnListEqual(List<ColumnMeta> src, List<ColumnMeta> tgt) {
        if (src.size() != tgt.size()) return false;

        for (int i = 0; i < src.size(); i++) {
            ColumnMeta s = src.get(i);
            ColumnMeta t = tgt.get(i);
            if (!s.equals(t)) return false;
        }
        return true;
    }

    /**
     * 生成结构差异详情
     */
    private String generateStructureDiffDetail(List<ColumnMeta> src, List<ColumnMeta> tgt) {
        Set<String> srcNames = src.stream().map(ColumnMeta::getColumnName).collect(Collectors.toSet());
        Set<String> tgtNames = tgt.stream().map(ColumnMeta::getColumnName).collect(Collectors.toSet());

        List<String> diffs = new ArrayList<>();

        // 1. 缺失字段 (源库有，目标库无)
        Set<String> missing = new HashSet<>(srcNames);
        missing.removeAll(tgtNames);
        if (!missing.isEmpty()) {
            diffs.add("目标库缺失字段: " + String.join(", ", missing));
        }

        // 2. 新增字段 (源库无，目标库有)
        Set<String> extra = new HashSet<>(tgtNames);
        extra.removeAll(srcNames);
        if (!extra.isEmpty()) {
            diffs.add("目标库多余字段: " + String.join(", ", extra));
        }

        // 3. 字段属性差异
        Map<String, ColumnMeta> srcMap = src.stream().collect(Collectors.toMap(ColumnMeta::getColumnName, c -> c));
        Map<String, ColumnMeta> tgtMap = tgt.stream().collect(Collectors.toMap(ColumnMeta::getColumnName, c -> c));

        for (String name : srcNames) {
            if (tgtNames.contains(name)) {
                ColumnMeta s = srcMap.get(name);
                ColumnMeta t = tgtMap.get(name);
                if (!s.equals(t)) {
                    List<String> colDiffs = new ArrayList<>();
                    if (!Objects.equals(s.getDataType(), t.getDataType())) {
                        colDiffs.add("类型(" + s.getDataType() + " -> " + t.getDataType() + ")");
                    }
                    if (!Objects.equals(s.getIsNullable(), t.getIsNullable())) {
                        colDiffs.add("必填(" + (s.getIsNullable() ? "否" : "是") + " -> " + (t.getIsNullable() ? "否" : "是") + ")");
                    }
                    if (!Objects.equals(s.getColumnKey(), t.getColumnKey())) {
                        colDiffs.add("索引(" + s.getColumnKey() + " -> " + t.getColumnKey() + ")");
                    }
                    if (!colDiffs.isEmpty()) {
                        diffs.add("字段 [" + name + "] 差异: " + String.join(", ", colDiffs));
                    }
                }
            }
        }

        return diffs.isEmpty() ? "结构基本一致" : String.join("; ", diffs);
    }

    // ==================== 工具方法 ====================

    private String decryptPassword(String encryptedPassword) {
        // TODO: 使用 AesUtil.decrypt(encryptedPassword, "your-key")
        return encryptedPassword;
    }
}
