package com.bocfintech.allstar.dialect;

import com.bocfintech.allstar.entity.DbConnectionConfig;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MysqlDialect implements DatabaseDialect {

    @Override
    public String buildJdbcUrl(DbConnectionConfig config) {
        return String.format(
                "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowMultiQueries=true",
                config.getHost(), config.getPort(), config.getDatabaseName()
        );
    }

    @Override
    public String quote(String name) {
        return "`" + name + "`";
    }

    @Override
    public String getTableInfoListSql(String databaseName) {
        return "SELECT TABLE_NAME, TABLE_COMMENT, " +
                "ROUND((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024, 2) AS DATA_LENGTH_MB " +
                "FROM information_schema.TABLES " +
                "WHERE TABLE_SCHEMA = '" + databaseName + "' " +
                "ORDER BY TABLE_NAME";
    }

    @Override
    public String getCreateTableSql(Connection conn, String databaseName, String tableName) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE " + quote(tableName))) {
            if (rs.next()) {
                return rs.getString(2);
            }
        }
        return null;
    }

    @Override
    public String getColumnMetaSql(String databaseName, String tableName) {
        return "SELECT " +
                "COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT, " +
                "COLUMN_KEY, EXTRA, ORDINAL_POSITION, COLUMN_COMMENT " +
                "FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = '" + databaseName + "' AND TABLE_NAME = '" + tableName + "' " +
                "ORDER BY ORDINAL_POSITION";
    }

    @Override
    public String getPrimaryKeyColumnSql(String databaseName, String tableName) {
        return "SELECT COLUMN_NAME " +
                "FROM information_schema.KEY_COLUMN_USAGE " +
                "WHERE TABLE_SCHEMA = '" + databaseName + "' AND TABLE_NAME = '" + tableName + "' AND CONSTRAINT_NAME = 'PRIMARY' " +
                "ORDER BY ORDINAL_POSITION LIMIT 1";
    }
}
