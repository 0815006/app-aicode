package com.bocfintech.allstar.dialect;

import com.bocfintech.allstar.entity.DbConnectionConfig;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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
        // GaussDB/PostgreSQL 中 TABLE_SCHEMA 通常为 public
        return "SELECT table_name AS TABLE_NAME, '' AS TABLE_COMMENT, 0 AS DATA_LENGTH_MB " +
                "FROM information_schema.tables " +
                "WHERE table_catalog = '" + databaseName + "' AND table_schema = 'public' " +
                "AND table_type = 'BASE TABLE' " +
                "ORDER BY table_name";
    }

    @Override
    public String getCreateTableSql(Connection conn, String databaseName, String tableName) throws SQLException {
        // 尝试使用 GaussDB 特有的 pg_get_tabledef 函数
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
                "WHERE table_catalog = '" + databaseName + "' AND table_name = '" + tableName + "' " +
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
}
