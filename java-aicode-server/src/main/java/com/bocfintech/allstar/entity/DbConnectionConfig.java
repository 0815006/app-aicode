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
    private String dbType;        // MySQL / TDSQL / GaussDB / PostgreSQL
    private String host;
    private Integer port;
    private String dbUser;        // 登录用户名，映射列 db_user
    private String dbPass;        // AES加密存储，映射列 db_pass
    private String databaseName;
    private String charset;

    private String createdBy;     // 7位员工号
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private String status;        // ACTIVE / INACTIVE
}
