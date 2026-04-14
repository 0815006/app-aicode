package com.bocfintech.allstar.entity;

import lombok.Data;

import java.util.Objects;

@Data
public class ColumnMeta {
    private String columnName;
    private String dataType;
    private Boolean isNullable;
    private String columnDefault;
    private String columnKey;
    private String extra;
    private Integer ordinalPosition;
    private String columnComment;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ColumnMeta that = (ColumnMeta) obj;
        return Objects.equals(columnName, that.columnName) &&
                Objects.equals(dataType, that.dataType) &&
                Objects.equals(isNullable, that.isNullable) &&
                Objects.equals(columnDefault, that.columnDefault) &&
                Objects.equals(columnKey, that.columnKey) &&
                Objects.equals(extra, that.extra);
    }
}
