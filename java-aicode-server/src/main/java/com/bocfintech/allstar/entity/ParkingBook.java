package com.bocfintech.allstar.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 车位预约配置信息表
 */
@Data
@TableName("parking_book")
public class ParkingBook {

    @TableId(type = IdType.INPUT) // 主键由业务传入 (emp_no)
    private String empNo;         // 7位工号

    private String username;       // 用户中文名称

    private String passHash;   // 密码哈希

    private Integer autoBook;      // 0:关闭, 1:开启

    @TableField(fill = FieldFill.INSERT)
    private Date createTime= new Date(); // 设置默认值       // 创建时间

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date lastTime= new Date();   // 设置默认值         // 更新时间
}
