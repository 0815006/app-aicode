package com.bocfintech.allstar.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 预约记录表
 */
@Data
@TableName("parking_record")
public class ParkingRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String empNo;          // 7位工号

    private String result;         // 成功/取消
    private String resultDesc;         // 描述

    private String parkingPosition;// 车位位置

    private String plateNo;        // 车牌号

    private String parkingType;    // A/B

    private String appointmentDate;  // 预约日期

    private String username;       // 用户姓名

    private String department;     // 部门

    private String phone;          // 手机号

    @TableField(fill = FieldFill.INSERT)
    private Date createTime= new Date(); // 设置默认值       // 创建时间
}
