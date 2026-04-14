package com.bocfintech.allstar.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TableInfo {

    /**
     * 表名（英文）
     */
    private String tableName;

    /**
     * 表中文名（注释）
     */
    private String tableComment;

    /**
     * 记录数（行数）
     */
    private Long rowCount;

    /**
     * 存储空间（MB），保留两位小数
     */
    private Double dataLengthMB;
}
