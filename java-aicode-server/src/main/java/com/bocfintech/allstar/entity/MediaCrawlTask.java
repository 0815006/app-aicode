package com.bocfintech.allstar.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/**
 * 媒体抓取任务实体类
 */
@TableName("media_crawl_task")
public class MediaCrawlTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 目标网页网址 */
    private String url;

    /** 网页标题(截取前20位) */
    private String title;

    /** 采集类型: IMAGE-仅图片, VIDEO-仅视频, BOTH-图片和视频 */
    private String crawlType;

    /** 最小文件过滤限制(KB) */
    private Integer minSizeLimit;

    /** 状态: PENDING-未处理, PROCESSING-抓取中, SUCCESS-已提取, FAILED-无法访问 */
    private String status;

    /** 下载图片数量 */
    private Integer imgCount;

    /** 图片总大小(Bytes) */
    private Long imgTotalSize;

    /** 下载视频数量 */
    private Integer videoCount;

    /** 视频总大小(Bytes) */
    private Long videoTotalSize;

    /** 生成的目录名(标题前20位+时间戳) */
    private String folderName;

    /** 创建人工号 */
    private String createdBy;

    /** 任务添加日期 */
    private Date createTime;

    /** 最后状态更新时间 */
    private Date updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCrawlType() {
        return crawlType;
    }

    public void setCrawlType(String crawlType) {
        this.crawlType = crawlType;
    }

    public Integer getMinSizeLimit() {
        return minSizeLimit;
    }

    public void setMinSizeLimit(Integer minSizeLimit) {
        this.minSizeLimit = minSizeLimit;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getImgCount() {
        return imgCount;
    }

    public void setImgCount(Integer imgCount) {
        this.imgCount = imgCount;
    }

    public Long getImgTotalSize() {
        return imgTotalSize;
    }

    public void setImgTotalSize(Long imgTotalSize) {
        this.imgTotalSize = imgTotalSize;
    }

    public Integer getVideoCount() {
        return videoCount;
    }

    public void setVideoCount(Integer videoCount) {
        this.videoCount = videoCount;
    }

    public Long getVideoTotalSize() {
        return videoTotalSize;
    }

    public void setVideoTotalSize(Long videoTotalSize) {
        this.videoTotalSize = videoTotalSize;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
