package com.bocfintech.allstar.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 性能测试场景定义与实测结果表
 */
@Data
@TableName("perf_task_scene")
public class PerfTaskScene {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Integer sceneType;
    private String sceneName;
    private Integer isSelected;
    private String selectReason;
    private BigDecimal targetTps;
    private BigDecimal targetRt;
    private BigDecimal targetSuccessRate;
    private String testSettingJson;
    private BigDecimal actualTps;
    private BigDecimal actualRt;
    @com.baomidou.mybatisplus.annotation.TableField("actual_rt_90")
    private BigDecimal actualRt90;
    private BigDecimal actualSuccessRate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date runStartTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date runEndTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date lastTime;
}
