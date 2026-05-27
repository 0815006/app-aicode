package com.bocfintech.allstar.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("chat_room_member")
public class ChatRoomMember {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "room_id")
    private Long roomId;

    private String username;

    @TableField(value = "joined_at")
    private Date joinedAt;
}
