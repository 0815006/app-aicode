package com.bocfintech.allstar.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("data_compare_result")
public class DataCompareResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private String tableName;
    private String tableComment;

    private Long sourceRowCount;
    private Double sourceDataSizeMb;

    private Long targetRowCount;
    private Double targetDataSizeMb;

    private Boolean isStructureMatch;
    private Boolean isRowCountMatch;
    private Boolean isFullDataMatch;

    private String structureDiffDetail;   // JSON 字符串
    private Long rowCountDiffValue;
    private String fullDataDiffDetail;    // JSON 字符串

    private String status;                // PENDING, COMPARING, DONE, SKIPPED
    private String message;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
