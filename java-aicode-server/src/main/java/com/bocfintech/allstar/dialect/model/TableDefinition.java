package com.bocfintech.allstar.dialect.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 表结构中立模型，用于屏蔽不同数据库之间的表结构差异，支撑异构迁移（如 MySQL -> PostgreSQL）。
 */
@Data
public class TableDefinition {

    /** 表名 */
    private String tableName;

    /** 表注释 */
    private String tableComment;

    /** 列定义 */
    private List<ColumnDefinition> columns = new ArrayList<>();

    /** 主键列名（按顺序） */
    private List<String> primaryKeys = new ArrayList<>();

    /** 索引/唯一约束定义（不含主键） */
    private List<IndexDefinition> indexes = new ArrayList<>();
}
