package com.bocfintech.allstar.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 投票选项作品表
 */
@Data
@TableName("vote_options")
public class VoteOption {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    /**
     * 贡献者/作者工号(7位)
     */
    private String userId;

    /**
     * 队伍名称
     */
    private String teamName;

    /**
     * 标签
     */
    private String tags;

    private String title;

    /**
     * 作者姓名(展示用)
     */
    private String authorName;

    private String description;

    /**
     * 作品封面OSS地址
     */
    private String coverUrl;

    /**
     * 视频链接或信息
     */
    private String videoUrl;

    /**
     * 附件地址
     */
    private String attachmentUrl;

    /**
     * 审核状态: "0"-待审核, "1"-已通过, "2"-驳回
     */
    private String auditStatus;

    private String auditRemark;

    /**
     * 最终汇总票数(冗余字段用于展示)
     */
    private Integer voteCount;

    private Date lastTime;

    /**
     * 当前用户是否已投 (非数据库字段)
     */
    @TableField(exist = false)
    private Boolean voted;
}
