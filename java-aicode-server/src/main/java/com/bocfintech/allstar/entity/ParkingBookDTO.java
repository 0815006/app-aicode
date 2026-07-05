package com.bocfintech.allstar.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParkingBookDTO {
    private String empNo;
    private String username;
    private String passHash;  // 前端加密后传入
    private Integer autoBook;
    private String nextAutoBookDate; // 下次自动开启预约日期 (yyyy-mm-dd)

    private String bookWeekdays;     // 预约星期，逗号分隔 1=周一~7=周日

    private Integer emailEnabled;    // 是否开启邮件通知（0=关闭，1=开启）

    private String emailUser;        // 发送邮件账号用户名（7位工号格式）

    private String emailPassword;    // 发送邮件账号密码（加密存储）

    private String emailRecipient;   // 收件人邮箱地址

    // getter and setter
}
