package com.bocfintech.allstar.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("db_connection_config")
public class DbConnectionConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String configName;
    private String dbType;        // MySQL / TDSQL
    private String host;
    private Integer port;
    private String username;
    private String password;      // AES加密存储
    private String databaseName;
    private String charset;

    private String createdBy;     // 7位员工号
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private String status;        // ACTIVE / INACTIVE
}
