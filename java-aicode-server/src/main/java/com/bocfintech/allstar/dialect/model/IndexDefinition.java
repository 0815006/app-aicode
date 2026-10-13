package com.bocfintech.allstar.dialect.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 索引/唯一约束定义中立模型（不含主键，主键由 {@link TableDefinition#getPrimaryKeys()} 表达）。
 */
@Data
public class IndexDefinition {

    /** 索引名 */
    private String name;

    /** 是否唯一 */
    private boolean unique;

    /** 索引列（按顺序） */
    private List<String> columns = new ArrayList<>();
}
