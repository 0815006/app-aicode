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

    // getter and setter
}
