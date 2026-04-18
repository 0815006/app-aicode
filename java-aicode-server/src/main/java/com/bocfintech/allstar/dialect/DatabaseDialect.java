package com.bocfintech.allstar.dialect;

import com.bocfintech.allstar.entity.DbConnectionConfig;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 数据库方言接口，用于屏蔽不同数据库之间的语法差异
 */
public interface DatabaseDialect {
    
    /**
     * 构建 JDBC 连接 URL
     */
    String buildJdbcUrl(DbConnectionConfig config);
    
    /**
     * 标识符引用符号处理
     */
    String quote(String name);
    
    /**
     * 获取查询所有表信息的 SQL
     * 预期返回列：TABLE_NAME, TABLE_COMMENT, DATA_LENGTH_MB
     */
    String getTableInfoListSql(String databaseName);
    
    /**
     * 获取建表语句 DDL
     */
    String getCreateTableSql(Connection conn, String databaseName, String tableName) throws SQLException;
    
    /**
     * 获取查询字段元数据的 SQL
     * 预期返回列：COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_KEY, EXTRA, ORDINAL_POSITION, COLUMN_COMMENT
     */
    String getColumnMetaSql(String databaseName, String tableName);
    
    /**
     * 获取查询主键列名的 SQL
     */
    String getPrimaryKeyColumnSql(String databaseName, String tableName);

    /**
     * 获取清空表的 SQL
     */
    default String getTruncateTableSql(String tableName) {
        return "TRUNCATE TABLE " + quote(tableName);
    }

    /**
     * 获取删除表的 SQL
     */
    default String getDropTableSql(String tableName) {
        return "DROP TABLE IF EXISTS " + quote(tableName);
    }
}
