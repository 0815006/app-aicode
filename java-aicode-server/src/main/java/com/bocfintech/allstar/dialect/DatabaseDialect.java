package com.bocfintech.allstar.dialect;

import com.bocfintech.allstar.dialect.model.TableDefinition;
import com.bocfintech.allstar.entity.DbConnectionConfig;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

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
     * 获取"取第一个字段"的 SQL（无主键时作为排序依据）
     */
    String getFirstColumnSql(String databaseName, String tableName);

    /**
     * 兼容性分组标识：同组方言之间可进行原始 DDL 同构迁移（如 MySQL/TDSQL 为 MYSQL，PostgreSQL/GaussDB 为 POSTGRES）
     */
    String getCompatibilityGroup();

    /**
     * 读取表结构为中立模型（用于异构迁移）
     */
    TableDefinition readTableDefinition(Connection conn, String databaseName, String tableName) throws SQLException;

    /**
     * 依据中立模型生成目标库建表语句（可能返回多条语句：建表、注释、索引）
     */
    List<String> buildCreateTableStatements(TableDefinition tableDef);

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
