package com.bocfintech.allstar.service;

/**
 * Playwright 抓取引擎服务接口
 */
public interface CrawlEngineService {

    /**
     * 启动抓取引擎，开始处理队列中的任务
     */
    void startEngine();

    /**
     * 处理单个抓取任务
     */
    void processTask(Long taskId);
}
