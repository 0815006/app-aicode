package com.bocfintech.allstar.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 投票流水记录表
 */
@Data
@TableName("vote_records")
public class VoteRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 投票人工号(7位)
     */
    private String userId;

    private Long taskId;

    private Long optionId;

    /**
     * 改投状态位: "0"-有效, "1"-已撤回/改投
     */
    private String isDeleted;

    private Date createTime;

    private Date lastTime;
}
