package com.bocfintech.allstar.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("data_compare_task")
public class DataCompareTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskName;

    // 源库配置快照
    private Long sourceConfigId;
    private String sourceConfigName;
    private String sourceDatabase;

    // 目标库配置快照
    private Long targetConfigId;
    private String targetConfigName;
    private String targetDatabase;

    private String compareType;   // STRUCTURE, ROW_COUNT, FULL_DATA

    private String status;        // RUNNING, SUCCESS, FAILED, STOPPED

    // 统计字段
    private Integer totalTables;
    private Integer matchedTables;
    private Integer sourceOnlyCount;
    private Integer targetOnlyCount;
    private Integer structureDiffCount;
    private Integer rowCountDiffCount;
    private Integer fullDataDiffCount;

    private String createdBy;
    private LocalDateTime createdTime;
    private LocalDateTime finishedTime;
    private Integer durationSeconds;
    private String remark;

    @TableField(exist = false)
    private java.util.List<DataCompareResult> results;
}
