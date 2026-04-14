package com.bocfintech.allstar.entity;


import java.util.List;

/**
 * 比对请求参数
 */
public class CompareRequest {
    private DbConnectionConfig sourceConfig;
    private DbConnectionConfig targetConfig;
    private List<String> tables;

    // getter/setter
    public DbConnectionConfig getSourceConfig() {
        return sourceConfig;
    }

    public void setSourceConfig(DbConnectionConfig sourceConfig) {
        this.sourceConfig = sourceConfig;
    }

    public DbConnectionConfig getTargetConfig() {
        return targetConfig;
    }

    public void setTargetConfig(DbConnectionConfig targetConfig) {
        this.targetConfig = targetConfig;
    }

    public List<String> getTables() {
        return tables;
    }

    public void setTables(List<String> tables) {
        this.tables = tables;
    }
}