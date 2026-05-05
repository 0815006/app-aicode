package com.bocfintech.allstar.controller;

import com.bocfintech.allstar.bean.ResultBean;
import com.bocfintech.allstar.entity.MediaCrawlTask;
import com.bocfintech.allstar.entity.MyPage;
import com.bocfintech.allstar.service.CrawlEngineService;
import com.bocfintech.allstar.service.MediaCrawlTaskService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 媒体抓取 Controller
 */
@Api(tags = "媒体抓取管理")
@RestController
@RequestMapping("/api/media-crawl")
public class MediaCrawlController {

    @Autowired
    private MediaCrawlTaskService taskService;

    @Autowired
    private CrawlEngineService crawlEngineService;

    @ApiOperation("添加抓取任务")
    @PostMapping("/task/add")
    public ResultBean<MediaCrawlTask> addTask(@RequestParam String url,
                                               @RequestParam(defaultValue = "IMAGE") String crawlType,
                                               @RequestParam(defaultValue = "0") Integer minSizeLimit) {
        MediaCrawlTask task = taskService.addTask(url, crawlType, minSizeLimit);
        // 启动抓取引擎
        crawlEngineService.startEngine();
        return ResultBean.success(task);
    }

    @ApiOperation("分页查询任务列表")
    @GetMapping("/task/list")
    public ResultBean<MyPage<MediaCrawlTask>> listTasks(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        MyPage<MediaCrawlTask> result = taskService.pageTasks(page, size);
        return ResultBean.success(result);
    }

    @ApiOperation("获取任务详情")
    @GetMapping("/task/{id}")
    public ResultBean<MediaCrawlTask> getTask(@PathVariable Long id) {
        MediaCrawlTask task = taskService.getTaskById(id);
        if (task == null) {
            return ResultBean.error("任务不存在");
        }
        return ResultBean.success(task);
    }

    @ApiOperation("获取任务对应的本地媒体文件列表")
    @GetMapping("/task/{id}/media-files")
    public ResultBean<Map<String, Object>> getTaskMediaFiles(@PathVariable Long id) {
        Map<String, Object> mediaFiles = taskService.getTaskMediaFiles(id);
        if (mediaFiles == null) {
            return ResultBean.error("任务不存在或尚未抓取");
        }
        return ResultBean.success(mediaFiles);
    }

    @ApiOperation("手动触发抓取引擎")
    @PostMapping("/engine/start")
    public ResultBean<String> startEngine() {
        crawlEngineService.startEngine();
        return ResultBean.success("抓取引擎已启动");
    }

    @ApiOperation("删除任务（连带删除本地文件）")
    @DeleteMapping("/task/{id}")
    public ResultBean<String> deleteTask(@PathVariable Long id) {
        boolean success = taskService.deleteTask(id);
        if (success) {
            return ResultBean.success("任务已删除");
        } else {
            return ResultBean.error("任务不存在或删除失败");
        }
    }
}
