package com.bocfintech.allstar.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 投票任务配置表
 */
@Data
@TableName("vote_tasks")
public class VoteTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    /**
     * 任务类型: "1"-直接投票, "2"-征集投票
     */
    private String type;

    /**
     * 每人总票数限制(永久N票)
     */
    private Integer maxVotes;

    /**
     * 截止前允许查看他人作品: "0"-否, "1"-是
     */
    private String allowViewEarly;

    /**
     * 作品上传截止时间(类型2必填)
     */
    private Date uploadEndAt;

    /**
     * 投票截止时间
     */
    private Date voteEndAt;

    /**
     * 发起人工号(7位)
     */
    private String creatorId;

    /**
     * 任务状态: "0"-草稿, "1"-进行中, "2"-已结束
     */
    private String status;

    private Date createTime;
}
