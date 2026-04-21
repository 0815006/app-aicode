package com.bocfintech.allstar.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("chat_file")
public class ChatFile {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("file_name")
    private String fileName;

    @TableField("storage_name")
    private String storageName;

    @TableField("file_size")
    private Long fileSize;

    @TableField("uploader_id")
    private String uploaderId;

    @TableField("uploader_name")
    private String uploaderName;

    @TableField("create_time")
    private Date createTime;
}
