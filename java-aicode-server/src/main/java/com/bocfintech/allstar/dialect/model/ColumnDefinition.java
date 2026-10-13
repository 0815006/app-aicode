package com.bocfintech.allstar.dialect.model;

import lombok.Data;

/**
 * 列定义中立模型。
 *
 * <p>typeName 采用中立类型集合：
 * VARCHAR / CHAR / TEXT / SMALLINT / INTEGER / BIGINT / NUMERIC / REAL / DOUBLE /
 * BOOLEAN / DATE / TIME / TIMESTAMP / JSONB / BYTEA / BIT / UUID。
 */
@Data
public class ColumnDefinition {

    /** 列名 */
    private String name;

    /** 中立类型名 */
    private String typeName;

    /** 长度或精度（VARCHAR/CHAR 的长度，NUMERIC 的精度） */
    private Integer length;

    /** 小数位数（NUMERIC 的 scale） */
    private Integer scale;

    /** 是否可空 */
    private boolean nullable = true;

    /** 默认值（原始值，生成目标 DDL 时再按方言归一化） */
    private String defaultValue;

    /** 是否自增 */
    private boolean autoIncrement;

    /** 是否主键 */
    private boolean primaryKey;

    /** 列注释 */
    private String comment;
}
