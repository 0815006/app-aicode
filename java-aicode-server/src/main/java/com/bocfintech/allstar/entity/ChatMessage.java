package com.bocfintech.allstar.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.util.Date;

public class ChatMessage {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String username;

    private String message;

    @TableField(value = "timestamp")
    private Date timestamp;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
