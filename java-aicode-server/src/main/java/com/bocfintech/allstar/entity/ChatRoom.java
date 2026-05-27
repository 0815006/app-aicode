package com.bocfintech.allstar.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("chat_room")
public class ChatRoom {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String title;

    private String creator;

    /** 状态：0-正常，1-已销毁（逻辑删除） */
    private Integer status;

    @TableField(value = "created_at")
    private Date createdAt;

    @TableField(value = "updated_at")
    private Date updatedAt;
}
