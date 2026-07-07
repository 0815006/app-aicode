package com.bocfintech.allstar.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("shared_file")
public class SharedFile {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("file_name")
    private String fileName;

    @TableField("file_size")
    private Long fileSize;

    @TableField("sub_directory_name")
    private String subDirectoryName;

    @TableField("physical_path")
    private String physicalPath;

    @TableField("uploader_id")
    private String uploaderId;

    @TableField("uploader_ip")
    private String uploaderIp;

    @TableField("download_count")
    private Integer downloadCount;

    @TableField("created_at")
    private Date createdAt;

    @TableField("updated_at")
    private Date updatedAt;
}
