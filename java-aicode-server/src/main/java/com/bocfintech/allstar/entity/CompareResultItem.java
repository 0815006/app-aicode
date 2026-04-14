package com.bocfintech.allstar.entity;

import lombok.Data;

@Data
public class CompareResultItem {

    /**
     * 表名
     */
    private String tableName;

    /**
     * 是否匹配：结构/行数/数据 是否一致
     */
    private Boolean isMatch;

    /**
     * 比对详情（JSON 或 文本描述）
     */
    private String detail;

    /**
     * 源库记录数（可选）
     */
    private Long sourceRowCount;

    /**
     * 目标库记录数（可选）
     */
    private Long targetRowCount;

    /**
     * 存储差异（MB）
     */
    private Double dataLengthDiffMB;
}
