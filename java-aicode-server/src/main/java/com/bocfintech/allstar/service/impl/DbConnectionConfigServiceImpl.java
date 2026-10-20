package com.bocfintech.allstar.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bocfintech.allstar.dialect.DatabaseDialect;
import com.bocfintech.allstar.dialect.DialectFactory;
import com.bocfintech.allstar.dialect.model.TableDefinition;
import com.bocfintech.allstar.entity.DbConnectionConfig;
import com.bocfintech.allstar.entity.TableInfo;
import com.bocfintech.allstar.mapper.DbConnectionConfigMapper;
import com.bocfintech.allstar.service.DbConnectionConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DbConnectionConfigServiceImpl
        extends ServiceImpl<DbConnectionConfigMapper, DbConnectionConfig>
        implements DbConnectionConfigService {

    /**
     * 查询当前用户的所有有效配置
     */
    @Override
    public List<DbConnectionConfig> listByUser(String createdBy) {
        return lambdaQuery()
                .eq(DbConnectionConfig::getCreatedBy, createdBy)
                .eq(DbConnectionConfig::getStatus, "ACTIVE")
                .orderByDesc(DbConnectionConfig::getCreatedTime)
                .list();
    }

    /**
     * 保存新配置（自动填充审计字段）
     */
    @Override
    public boolean saveConfig(DbConnectionConfig config, String operator) {
        config.setCreatedBy(operator);
        config.setStatus("ACTIVE");
        config.setCreatedTime(LocalDateTime.now());
        config.setUpdatedTime(LocalDateTime.now());
        return save(config);
    }

    /**
     * 更新配置（仅允许修改非敏感字段，密码可选更新）
     */
    @Override
    public boolean updateConfig(DbConnectionConfig config, String operator) {
        DbConnectionConfig existing = getById(config.getId());
        if (existing == null) {
            throw new IllegalArgumentException("配置不存在");
        }
        if (!existing.getCreatedBy().equals(operator)) {
            throw new SecurityException("无权修改他人配置");
        }

        // 保留原密码，除非新传了非空密码
        if (config.getDbPass() == null || config.getDbPass().isEmpty()) {
            config.setDbPass(existing.getDbPass());
        }

        config.setUpdatedTime(LocalDateTime.now());
        return updateById(config);
    }

    /**
     * 测试数据库连接是否可用
     */
    @Override
    public boolean testConnection(DbConnectionConfig config) {
        DatabaseDialect dialect = DialectFactory.getDialect(config);
        String jdbcUrl = dialect.buildJdbcUrl(config);
        String username = config.getDbUser();
        String password = decryptPassword(config.getDbPass());

        log.info("测试连接: {}@{}:{}/{}", config.getDbUser(), config.getHost(), config.getPort(), config.getDatabaseName());
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            return !conn.isClosed();
        } catch (Exception e) {
            log.error("连接失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取数据库所有表信息（表名、中文名、记录数、存储空间）
     */
    @Override
    public List<TableInfo> getTableInfoList(DbConnectionConfig config) {
        DatabaseDialect dialect = DialectFactory.getDialect(config);
        String jdbcUrl = dialect.buildJdbcUrl(config);
        String username = config.getDbUser();
        String password = decryptPassword(config.getDbPass());
        String databaseName = config.getDatabaseName();

        List<TableInfo> result = new ArrayList<>();
        String sql = dialect.getTableInfoListSql(databaseName);

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TableInfo info = new TableInfo();
                    String tableName = rs.getString("TABLE_NAME");
                    info.setTableName(tableName);
                    info.setTableComment(rs.getString("TABLE_COMMENT"));
                    info.setDataLengthMB(rs.getDouble("DATA_LENGTH_MB"));
                    
                    // 获取精确行数
                    info.setRowCount(getAccurateRowCount(conn, dialect, tableName));
                    
                    result.add(info);
                }
            }
        } catch (SQLException e) {
            log.error("查询表信息失败", e);
            throw new RuntimeException("查询数据库表信息失败：" + e.getMessage(), e);
        }

        return result;
    }

    private long getAccurateRowCount(Connection conn, DatabaseDialect dialect, String tableName) {
        String sql = "SELECT COUNT(*) FROM " + dialect.quote(tableName);
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            log.warn("无法获取表 {} 的精确行数: {}", tableName, e.getMessage());
        }
        return 0L;
    }

    /**
     * 迁移单个表的结构（不含数据）
     */
    @Override
    public void migrateTableStructure(DbConnectionConfig sourceConfig,
                                      DbConnectionConfig targetConfig,
                                      String tableName) {
        DatabaseDialect srcDialect = DialectFactory.getDialect(sourceConfig);
        DatabaseDialect tgtDialect = DialectFactory.getDialect(targetConfig);

        // 1. 在目标库先删除表（如果存在）
        executeSqlOnConfig(targetConfig, tgtDialect, tgtDialect.getDropTableSql(tableName));

        // 2. 生成并执行建表语句（同构走原始 DDL，异构走中立模型转换）
        if (srcDialect.getCompatibilityGroup().equals(tgtDialect.getCompatibilityGroup())) {
            String createSql = getCreateTableSql(sourceConfig, srcDialect, tableName);
            if (createSql == null || createSql.isEmpty()) {
                throw new IllegalArgumentException("无法获取源表结构：" + tableName);
            }
            executeSqlOnConfig(targetConfig, tgtDialect, createSql);
        } else {
            TableDefinition def = readTableDefinition(sourceConfig, srcDialect, tableName);
            if (def == null || def.getColumns().isEmpty()) {
                throw new IllegalArgumentException("无法读取源表结构：" + tableName);
            }
            def.setTableName(tableName);
            List<String> statements = tgtDialect.buildCreateTableStatements(def);
            executeStatementsOnConfig(targetConfig, tgtDialect, statements);
        }
    }

    /**
     * 迁移表结构 + 数据
     */
    @Override
    public void migrateTableData(DbConnectionConfig sourceConfig,
                                 DbConnectionConfig targetConfig,
                                 String tableName) {
        // 1. 先迁移结构（含删除旧表）
        migrateTableStructure(sourceConfig, targetConfig, tableName);

        // 2. 迁移数据
        DatabaseDialect srcDialect = DialectFactory.getDialect(sourceConfig);
        List<String> columns = getColumnNames(sourceConfig, srcDialect, tableName);
        if (columns.isEmpty()) {
            throw new RuntimeException("源表不存在或无字段：" + tableName);
        }
        pumpData(sourceConfig, targetConfig, tableName, columns);
    }

    /**
     * 仅迁移数据（要求字段名一致）
     */
    @Override
    public void migrateTableDataOnly(DbConnectionConfig sourceConfig,
                                     DbConnectionConfig targetConfig,
                                     String tableName) {
        DatabaseDialect srcDialect = DialectFactory.getDialect(sourceConfig);
        DatabaseDialect tgtDialect = DialectFactory.getDialect(targetConfig);

        // 1. 获取源库和目标库的字段列表
        List<String> sourceColumns = getColumnNames(sourceConfig, srcDialect, tableName);
        List<String> targetColumns = getColumnNames(targetConfig, tgtDialect, tableName);

        if (sourceColumns.isEmpty()) {
            throw new RuntimeException("源表不存在或无字段：" + tableName);
        }
        if (targetColumns.isEmpty()) {
            throw new RuntimeException("目标表不存在：" + tableName);
        }

        // 2. 校验字段名是否一致（不考虑顺序）
        if (sourceColumns.size() != targetColumns.size() || !sourceColumns.containsAll(targetColumns)) {
            throw new RuntimeException("源表与目标表字段不一致，无法迁移数据");
        }

        // 3. 清空目标表
        truncateTable(targetConfig, tableName);

        // 4. 迁移数据
        pumpData(sourceConfig, targetConfig, tableName, sourceColumns);
    }

    /**
     * 核心数据流转逻辑：从源库读取并批量写入目标库
     */
    private void pumpData(DbConnectionConfig sourceConfig, DbConnectionConfig targetConfig, String tableName, List<String> columns) {
        DatabaseDialect srcDialect = DialectFactory.getDialect(sourceConfig);
        DatabaseDialect tgtDialect = DialectFactory.getDialect(targetConfig);

        String sourceJdbcUrl = srcDialect.buildJdbcUrl(sourceConfig);
        String targetJdbcUrl = tgtDialect.buildJdbcUrl(targetConfig);

        String srcColumnsStr = columns.stream().map(srcDialect::quote).collect(Collectors.joining(", "));
        String tgtColumnsStr = columns.stream().map(tgtDialect::quote).collect(Collectors.joining(", "));
        String placeholders = columns.stream().map(c -> "?").collect(Collectors.joining(", "));
        
        String selectSql = "SELECT " + srcColumnsStr + " FROM " + srcDialect.quote(tableName);
        String insertSql = "INSERT INTO " + tgtDialect.quote(tableName) + " (" + tgtColumnsStr + ") VALUES (" + placeholders + ")";

        log.info("开始迁移数据: {} -> {}", sourceConfig.getDatabaseName(), targetConfig.getDatabaseName());

        try (Connection srcConn = DriverManager.getConnection(sourceJdbcUrl, sourceConfig.getDbUser(), decryptPassword(sourceConfig.getDbPass()));
             Connection tgtConn = DriverManager.getConnection(targetJdbcUrl, targetConfig.getDbUser(), decryptPassword(targetConfig.getDbPass()));
             PreparedStatement selectPs = srcConn.prepareStatement(selectSql);
             PreparedStatement insertPs = tgtConn.prepareStatement(insertSql);
             ResultSet rs = selectPs.executeQuery()) {

            // 读取目标表列类型，用于值类型自适应（跨方言下布尔/数值差异）
            Map<String, String> targetTypes = new HashMap<>();
            try (ResultSet colRs = tgtConn.getMetaData().getColumns(targetConfig.getDatabaseName(), null, tableName, null)) {
                while (colRs.next()) {
                    targetTypes.put(colRs.getString("COLUMN_NAME").toLowerCase(), colRs.getString("TYPE_NAME"));
                }
            }

            tgtConn.setAutoCommit(false);
            int count = 0;
            int batchSize = 1000;
            int columnCount = columns.size();

            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    String colName = columns.get(i - 1);
                    String tgtType = targetTypes.get(colName.toLowerCase());
                    Object val = rs.getObject(i);
                    if (isJsonType(tgtType)) {
                        // PostgreSQL 不会把 varchar 隐式转换为 jsonb，需以 unknown 类型发送字符串
                        insertPs.setObject(i, val == null ? null : val.toString(), Types.OTHER);
                    } else if (isBitType(tgtType)) {
                        // PG bit 类型不接受 byte[]/boolean，需以位串字符串按 unknown 类型发送
                        insertPs.setObject(i, toBitString(val), Types.OTHER);
                    } else {
                        insertPs.setObject(i, adaptValue(val, tgtType));
                    }
                }
                insertPs.addBatch();
                count++;
                
                if (count % batchSize == 0) {
                    insertPs.executeBatch();
                    tgtConn.commit();
                }
            }
            insertPs.executeBatch();
            tgtConn.commit();
            log.info("数据迁移完成，共 {} 条记录", count);
        } catch (SQLException e) {
            log.error("数据流转异常", e);
            throw new RuntimeException("数据迁移执行失败：" + e.getMessage(), e);
        }
    }

    /**
     * 依据目标列类型对值做自适应转换，解决跨方言下布尔/数值类型不兼容问题。
     *
     * <p>典型场景：MySQL {@code tinyint(1)} 列被 Connector/J 读为 {@link Boolean}，
     * 而目标 PostgreSQL 列（由中立模型 SMALLINT 生成）为 {@code smallint}，
     * 直接写入布尔会报 "column is of type smallint but expression is of type boolean"。
     */
    private Object adaptValue(Object val, String targetType) {
        if (val == null || targetType == null) {
            return val;
        }
        String t = targetType.toLowerCase();
        boolean tgtBoolean = t.equals("bool") || t.equals("boolean");
        boolean tgtNumeric = t.equals("tinyint") || t.equals("smallint") || t.equals("mediumint")
                || t.equals("int") || t.equals("integer") || t.equals("bigint")
                || t.equals("int2") || t.equals("int4") || t.equals("int8")
                || t.equals("numeric") || t.equals("decimal") || t.equals("number")
                || t.equals("float") || t.equals("double") || t.equals("real")
                || t.equals("float4") || t.equals("float8") || t.equals("serial");

        if (val instanceof Boolean) {
            // 目标为数值列时，布尔转 1/0
            if (tgtNumeric) {
                return ((Boolean) val) ? 1 : 0;
            }
            return val;
        }
        if (val instanceof Number) {
            // 目标为布尔列时，非 0 转 true
            if (tgtBoolean) {
                return ((Number) val).intValue() != 0;
            }
            return val;
        }
        if (val instanceof java.util.Date) {
            // YEAR 等读回 Date，但目标列映射为数值类型时取其年份
            if (tgtNumeric) {
                java.util.Calendar c = java.util.Calendar.getInstance();
                c.setTime((java.util.Date) val);
                return c.get(java.util.Calendar.YEAR);
            }
            return val;
        }
        return val;
    }

    /**
     * 判断目标列类型是否为 JSON 类型（PostgreSQL json/jsonb、MySQL json）
     */
    private boolean isJsonType(String targetType) {
        if (targetType == null) {
            return false;
        }
        String t = targetType.toLowerCase();
        return t.equals("json") || t.equals("jsonb");
    }

    /**
     * 判断目标列类型是否为位类型（PostgreSQL bit/bit varying、MySQL bit）
     */
    private boolean isBitType(String targetType) {
        if (targetType == null) {
            return false;
        }
        String t = targetType.toLowerCase();
        return t.equals("bit") || t.equals("varbit") || t.startsWith("bit(") || t.startsWith("bit varying");
    }

    /**
     * 将值转换为 PostgreSQL 位串字面量（如 "1"、"101"）
     */
    private String toBitString(Object val) {
        if (val == null) {
            return null;
        }
        if (val instanceof Boolean) {
            return ((Boolean) val) ? "1" : "0";
        }
        if (val instanceof Number) {
            return Long.toBinaryString(((Number) val).longValue());
        }
        if (val instanceof byte[]) {
            byte[] bytes = (byte[]) val;
            StringBuilder sb = new StringBuilder();
            for (int k = 0; k < bytes.length; k++) {
                String b = Integer.toBinaryString(bytes[k] & 0xFF);
                if (k > 0) {
                    while (b.length() < 8) {
                        b = "0" + b;
                    }
                }
                sb.append(b);
            }
            return sb.length() == 0 ? "0" : sb.toString();
        }
        return val.toString();
    }

    private List<String> getColumnNames(DbConnectionConfig config, DatabaseDialect dialect, String tableName) {
        String jdbcUrl = dialect.buildJdbcUrl(config);
        String username = config.getDbUser();
        String password = decryptPassword(config.getDbPass());
        List<String> columns = new ArrayList<>();
        
        Set<String> seenNames = new HashSet<>();
        
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             ResultSet rs = conn.getMetaData().getColumns(config.getDatabaseName(), null, tableName, null)) {
            while (rs.next()) {
                String colName = rs.getString("COLUMN_NAME");
                if (seenNames.add(colName.toLowerCase())) {
                    columns.add(colName);
                }
            }
        } catch (SQLException e) {
            log.error("获取字段列表失败: {}", e.getMessage());
        }
        return columns;
    }

    /**
     * 清空目标表数据（TRUNCATE）
     */
    @Override
    public void truncateTable(DbConnectionConfig config, String tableName) {
        DatabaseDialect dialect = DialectFactory.getDialect(config);
        executeSqlOnConfig(config, dialect, dialect.getTruncateTableSql(tableName));
    }

    /**
     * 删除目标表（DROP TABLE）
     */
    @Override
    public void dropTable(DbConnectionConfig config, String tableName) {
        DatabaseDialect dialect = DialectFactory.getDialect(config);
        executeSqlOnConfig(config, dialect, dialect.getDropTableSql(tableName));
    }

    // ------------------- 工具方法 -------------------

    private String decryptPassword(String encryptedPassword) {
        // TODO: 使用 AesUtil.decrypt(encryptedPassword, "your-key")
        return encryptedPassword; // 临时返回原值
    }

    private String getCreateTableSql(DbConnectionConfig config, DatabaseDialect dialect, String tableName) {
        String jdbcUrl = dialect.buildJdbcUrl(config);
        String username = config.getDbUser();
        String password = decryptPassword(config.getDbPass());

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            return dialect.getCreateTableSql(conn, config.getDatabaseName(), tableName);
        } catch (SQLException e) {
            log.error("获取建表语句失败: {}", e.getMessage());
            throw new RuntimeException("无法获取表结构：" + tableName + "。" + e.getMessage(), e);
        }
    }

    private void executeSqlOnConfig(DbConnectionConfig config, DatabaseDialect dialect, String sql) {
        String jdbcUrl = dialect.buildJdbcUrl(config);
        String username = config.getDbUser();
        String password = decryptPassword(config.getDbPass());

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement()) {

            stmt.execute(sql);
            log.info("执行SQL成功: {}", sql);
        } catch (SQLException e) {
            log.error("执行SQL失败: {}", sql, e);
            throw new RuntimeException("执行失败：" + e.getMessage(), e);
        }
    }

    /**
     * 读取表结构为中立模型（异构迁移用）
     */
    private TableDefinition readTableDefinition(DbConnectionConfig config, DatabaseDialect dialect, String tableName) {
        String jdbcUrl = dialect.buildJdbcUrl(config);
        try (Connection conn = DriverManager.getConnection(jdbcUrl, config.getDbUser(), decryptPassword(config.getDbPass()))) {
            return dialect.readTableDefinition(conn, config.getDatabaseName(), tableName);
        } catch (SQLException e) {
            log.error("读取表结构失败: {}", tableName, e);
            throw new RuntimeException("读取表结构失败：" + tableName + "。" + e.getMessage(), e);
        }
    }

    /**
     * 在目标库按顺序执行多条语句（异构迁移的建表、注释、索引）
     */
    private void executeStatementsOnConfig(DbConnectionConfig config, DatabaseDialect dialect, List<String> statements) {
        if (statements == null || statements.isEmpty()) {
            throw new RuntimeException("未能生成目标建库语句");
        }
        String jdbcUrl = dialect.buildJdbcUrl(config);
        try (Connection conn = DriverManager.getConnection(jdbcUrl, config.getDbUser(), decryptPassword(config.getDbPass()));
             Statement stmt = conn.createStatement()) {
            for (String sql : statements) {
                stmt.execute(sql);
                log.info("执行SQL成功: {}", sql);
            }
        } catch (SQLException e) {
            log.error("建表失败", e);
            throw new RuntimeException("建表失败：" + e.getMessage(), e);
        }
    }
}
