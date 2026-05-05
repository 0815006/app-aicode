package com.bocfintech.allstar.service;

import com.bocfintech.allstar.entity.MediaCrawlTask;
import com.bocfintech.allstar.entity.MyPage;

import java.util.List;
import java.util.Map;

/**
 * 媒体抓取任务 Service 接口
 */
public interface MediaCrawlTaskService extends BaseService<MediaCrawlTask> {

    /**
     * 添加抓取任务
     */
    MediaCrawlTask addTask(String url, String crawlType, Integer minSizeLimit);

    /**
     * 分页查询任务列表
     */
    MyPage<MediaCrawlTask> pageTasks(int page, int size);

    /**
     * 获取任务详情
     */
    MediaCrawlTask getTaskById(Long id);

    /**
     * 获取任务对应的本地媒体文件列表
     */
    Map<String, Object> getTaskMediaFiles(Long taskId);

    /**
     * 获取下一个待处理的任务
     */
    MediaCrawlTask getNextPendingTask();

    /**
     * 更新任务状态
     */
    void updateTaskStatus(Long id, String status);

    /**
     * 更新任务抓取结果
     */
    void updateTaskResult(Long id, String title, String folderName, String status,
                          Integer imgCount, Long imgTotalSize,
                          Integer videoCount, Long videoTotalSize);

    /**
     * 删除任务（连带删除本地文件）
     */
    boolean deleteTask(Long id);
}
