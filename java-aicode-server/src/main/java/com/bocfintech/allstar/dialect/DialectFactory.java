package com.bocfintech.allstar.dialect;

import com.bocfintech.allstar.entity.DbConnectionConfig;

public class DialectFactory {
    
    public static DatabaseDialect getDialect(DbConnectionConfig config) {
        String dbType = config.getDbType();
        if ("GaussDB".equalsIgnoreCase(dbType)) {
            return new GaussDbDialect();
        }
        // 默认返回 MySQL 方言（兼容 TDSQL）
        return new MysqlDialect();
    }
}
