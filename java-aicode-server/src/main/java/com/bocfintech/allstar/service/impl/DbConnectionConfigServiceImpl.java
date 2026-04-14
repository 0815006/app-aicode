package com.bocfintech.allstar.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bocfintech.allstar.entity.DbConnectionConfig;
import com.bocfintech.allstar.entity.TableInfo;
import com.bocfintech.allstar.mapper.DbConnectionConfigMapper;
import com.bocfintech.allstar.service.DbConnectionConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
        if (config.getPassword() == null || config.getPassword().isEmpty()) {
            config.setPassword(existing.getPassword());
        }

        config.setUpdatedTime(LocalDateTime.now());
        return updateById(config);
    }

    /**
     * 测试数据库连接是否可用
     */
    @Override
    public boolean testConnection(DbConnectionConfig config) {
        // TODO: 实际使用 JDBC DriverManager.getConnection 测试
        String jdbcUrl = buildJdbcUrl(config);
        String username = config.getUsername();
        String password = decryptPassword(config.getPassword()); // TODO: 实现解密

        log.info("测试连接: {}@{}:{}/{}", config.getUsername(), config.getHost(), config.getPort(), config.getDatabaseName());
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
        String jdbcUrl = buildJdbcUrl(config);
        String username = config.getUsername();
        String password = decryptPassword(config.getPassword());
        String databaseName = config.getDatabaseName();

        List<TableInfo> result = new ArrayList<>();
        // Java 8 兼容 SQL
        String sql = "SELECT TABLE_NAME, TABLE_COMMENT, " +
                "ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS DATA_LENGTH_MB " +
                "FROM information_schema.TABLES " +
                "WHERE TABLE_SCHEMA = ? " +
                "ORDER BY TABLE_NAME";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, databaseName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TableInfo info = new TableInfo();
                    String tableName = rs.getString("TABLE_NAME");
                    info.setTableName(tableName);
                    info.setTableComment(rs.getString("TABLE_COMMENT"));
                    info.setDataLengthMB(rs.getDouble("DATA_LENGTH_MB"));
                    
                    // 获取精确行数，替代 information_schema 中的估算值
                    info.setRowCount(getAccurateRowCount(conn, tableName));
                    
                    result.add(info);
                }
            }
        } catch (SQLException e) {
            log.error("查询表信息失败", e);
            throw new RuntimeException("查询数据库表信息失败：" + e.getMessage(), e);
        }

        return result;
    }

    private long getAccurateRowCount(Connection conn, String tableName) {
        String sql = "SELECT COUNT(*) FROM `" + tableName + "`";
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
        // 1. 从源库获取建表语句
        String createSql = getCreateTableSql(sourceConfig, tableName);
        if (createSql == null || createSql.isEmpty()) {
            throw new IllegalArgumentException("无法获取源表结构：" + tableName);
        }

        // 2. 在目标库先删除表（如果存在）
        executeSqlOnConfig(targetConfig, "DROP TABLE IF EXISTS `" + tableName + "`");

        // 3. 在目标库执行建表
        executeSqlOnConfig(targetConfig, createSql);
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
        List<String> columns = getColumnNames(sourceConfig, tableName);
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
        // 1. 获取源库和目标库的字段列表
        List<String> sourceColumns = getColumnNames(sourceConfig, tableName);
        List<String> targetColumns = getColumnNames(targetConfig, tableName);

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
        String sourceJdbcUrl = buildJdbcUrl(sourceConfig);
        String targetJdbcUrl = buildJdbcUrl(targetConfig);

        String columnsStr = "`" + String.join("`, `", columns) + "`";
        String placeholders = columns.stream().map(c -> "?").collect(Collectors.joining(", "));
        
        String selectSql = "SELECT " + columnsStr + " FROM `" + tableName + "`";
        String insertSql = "INSERT INTO `" + tableName + "` (" + columnsStr + ") VALUES (" + placeholders + ")";

        log.info("开始迁移数据: {} -> {}", sourceConfig.getDatabaseName(), targetConfig.getDatabaseName());

        try (Connection srcConn = DriverManager.getConnection(sourceJdbcUrl, sourceConfig.getUsername(), decryptPassword(sourceConfig.getPassword()));
             Connection tgtConn = DriverManager.getConnection(targetJdbcUrl, targetConfig.getUsername(), decryptPassword(targetConfig.getPassword()));
             PreparedStatement selectPs = srcConn.prepareStatement(selectSql);
             PreparedStatement insertPs = tgtConn.prepareStatement(insertSql);
             ResultSet rs = selectPs.executeQuery()) {

            tgtConn.setAutoCommit(false);
            int count = 0;
            int batchSize = 1000;
            int columnCount = columns.size();

            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    insertPs.setObject(i, rs.getObject(i));
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

    private List<String> getColumnNames(DbConnectionConfig config, String tableName) {
        String jdbcUrl = buildJdbcUrl(config);
        String username = config.getUsername();
        String password = decryptPassword(config.getPassword());
        List<String> columns = new ArrayList<>();
        
        // 使用 Set 记录已处理的字段名（忽略大小写），避免 MySQL 报错 "Column specified twice"
        Set<String> seenNames = new HashSet<>();
        
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             // 明确指定 catalog (数据库名)，避免获取到其他库的同名表字段
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
        String sql = "TRUNCATE TABLE `" + tableName + "`";
        executeSqlOnConfig(config, sql);
    }

    /**
     * 删除目标表（DROP TABLE）
     */
    @Override
    public void dropTable(DbConnectionConfig config, String tableName) {
        String sql = "DROP TABLE `" + tableName + "`";
        executeSqlOnConfig(config, sql);
    }

    /**
     * 构建 JDBC URL
     */
    private String getJdbcUrl(DbConnectionConfig config) {
        String host = config.getHost();
        Integer port = config.getPort();
        String database = config.getDatabaseName();
        String dbType = config.getDbType();

        if ("TDSQL".equalsIgnoreCase(dbType)) {
            return String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai", host, port, database);
        } else {
            return String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai", host, port, database);
        }
    }

    // ------------------- 工具方法 -------------------

    private String buildJdbcUrl(DbConnectionConfig config) {
        return String.format(
                "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowMultiQueries=true",
                config.getHost(), config.getPort(), config.getDatabaseName()
        );
    }

    private String decryptPassword(String encryptedPassword) {
        // TODO: 使用 AesUtil.decrypt(encryptedPassword, "your-key")
        return encryptedPassword; // 临时返回原值
    }

    private String getCreateTableSql(DbConnectionConfig config, String tableName) {
        String jdbcUrl = buildJdbcUrl(config);
        String username = config.getUsername();
        String password = decryptPassword(config.getPassword());

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement()) {

            try (ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE `" + tableName + "`")) {
                if (rs.next()) {
                    return rs.getString("Create Table");
                }
            }
        } catch (SQLException e) {
            log.error("获取建表语句失败: {}", e.getMessage());
            throw new RuntimeException("无法获取表结构：" + tableName, e);
        }
        return null;
    }

    private void executeSqlOnConfig(DbConnectionConfig config, String sql) {
        String jdbcUrl = buildJdbcUrl(config);
        String username = config.getUsername();
        String password = decryptPassword(config.getPassword());

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement()) {

            stmt.execute(sql);
            log.info("执行SQL成功: {}", sql);
        } catch (SQLException e) {
            log.error("执行SQL失败: {}", sql, e);
            throw new RuntimeException("执行失败：" + e.getMessage(), e);
        }
    }
}
