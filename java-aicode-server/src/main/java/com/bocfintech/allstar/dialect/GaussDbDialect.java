package com.bocfintech.allstar.dialect;

import com.bocfintech.allstar.dialect.model.TableDefinition;
import com.bocfintech.allstar.entity.DbConnectionConfig;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * GaussDB 方言实现（基于 PostgreSQL）。
 *
 * <p>建表语句获取依赖 {@code pg_get_tabledef()} 函数；
 * 中立模型的读取与生成复用 {@link PostgresFamilySupport}，与 PostgreSQL 共享同一兼容分组。
 */
public class GaussDbDialect implements DatabaseDialect {

    @Override
    public String buildJdbcUrl(DbConnectionConfig config) {
        return String.format(
                "jdbc:postgresql://%s:%d/%s",
                config.getHost(), config.getPort(), config.getDatabaseName()
        );
    }

    @Override
    public String quote(String name) {
        return "\"" + name + "\"";
    }

    @Override
    public String getTableInfoListSql(String databaseName) {
        return PostgresFamilySupport.getTableInfoListSql();
    }

    @Override
    public String getCreateTableSql(Connection conn, String databaseName, String tableName) throws SQLException {
        String sql = "SELECT pg_get_tabledef('" + tableName + "')";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            throw new SQLException("无法获取 GaussDB 表结构：" + tableName + "。原因：" + e.getMessage() +
                    "。请确保数据库支持 pg_get_tabledef 函数，或者联系管理员开启该权限。");
        }
        return null;
    }

    @Override
    public String getColumnMetaSql(String databaseName, String tableName) {
        return "SELECT " +
                "column_name AS COLUMN_NAME, data_type AS DATA_TYPE, is_nullable AS IS_NULLABLE, column_default AS COLUMN_DEFAULT, " +
                "'' AS COLUMN_KEY, '' AS EXTRA, ordinal_position AS ORDINAL_POSITION, '' AS COLUMN_COMMENT " +
                "FROM information_schema.columns " +
                "WHERE table_catalog = '" + databaseName + "' AND table_schema = 'public' AND table_name = '" + tableName + "' " +
                "ORDER BY ordinal_position";
    }

    @Override
    public String getPrimaryKeyColumnSql(String databaseName, String tableName) {
        return "SELECT a.attname AS COLUMN_NAME " +
                "FROM pg_index i " +
                "JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = ANY(i.indkey) " +
                "WHERE i.indrelid = '" + quote(tableName) + "'::regclass AND i.indisprimary " +
                "LIMIT 1";
    }

    @Override
    public String getFirstColumnSql(String databaseName, String tableName) {
        return PostgresFamilySupport.getFirstColumnSql(databaseName, tableName);
    }

    @Override
    public String getCompatibilityGroup() {
        return PostgresFamilySupport.GROUP;
    }

    @Override
    public TableDefinition readTableDefinition(Connection conn, String databaseName, String tableName) throws SQLException {
        return PostgresFamilySupport.readTableDefinition(conn, databaseName, tableName);
    }

    @Override
    public List<String> buildCreateTableStatements(TableDefinition tableDef) {
        return PostgresFamilySupport.buildCreateTableStatements(tableDef);
    }
}
