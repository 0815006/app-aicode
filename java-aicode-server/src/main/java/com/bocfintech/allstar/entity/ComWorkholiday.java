package com.bocfintech.allstar.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName(value = "com_workholiday")
public class ComWorkholiday {
    @TableField(value = "date")
    private LocalDate date;

    @TableField(value = "status")
    private String status;

    @TableField(value = "msg")
    private String msg;

}
