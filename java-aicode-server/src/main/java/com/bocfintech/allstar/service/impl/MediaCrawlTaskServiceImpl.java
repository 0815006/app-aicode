package com.bocfintech.allstar.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bocfintech.allstar.entity.MediaCrawlTask;
import com.bocfintech.allstar.entity.MyPage;
import com.bocfintech.allstar.mapper.MediaCrawlTaskMapper;
import com.bocfintech.allstar.service.MediaCrawlTaskService;
import com.bocfintech.allstar.util.MyPageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 媒体抓取任务 Service 实现类
 */
@Service
public class MediaCrawlTaskServiceImpl extends BaseServiceImpl<MediaCrawlTaskMapper, MediaCrawlTask> implements MediaCrawlTaskService {

    private static final Logger log = LoggerFactory.getLogger(MediaCrawlTaskServiceImpl.class);

    @Value("${app.crawl.image-base-path}")
    private String imageBasePath;

    @Value("${app.crawl.video-base-path}")
    private String videoBasePath;

    @Override
    public MediaCrawlTask addTask(String url, String crawlType, Integer minSizeLimit) {
        MediaCrawlTask task = new MediaCrawlTask();
        task.setUrl(url);
        task.setCrawlType(crawlType != null ? crawlType : "IMAGE");
        task.setMinSizeLimit(minSizeLimit != null ? minSizeLimit : 0);
        task.setStatus("PENDING");
        task.setImgCount(0);
        task.setImgTotalSize(0L);
        task.setVideoCount(0);
        task.setVideoTotalSize(0L);
        task.setCreateTime(new Date());
        task.setUpdateTime(new Date());
        mapper.insert(task);
        return task;
    }

    @Override
    public MyPage<MediaCrawlTask> pageTasks(int page, int size) {
        Page<MediaCrawlTask> paget = new Page<>(page, size);
        paget.addOrder(com.baomidou.mybatisplus.core.metadata.OrderItem.desc("create_time"));
        IPage<MediaCrawlTask> ipage = mapper.selectPage(paget, null);
        return MyPageUtil.getMypage(ipage);
    }

    @Override
    public MediaCrawlTask getTaskById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public Map<String, Object> getTaskMediaFiles(Long taskId) {
        MediaCrawlTask task = mapper.selectById(taskId);
        if (task == null || task.getFolderName() == null) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        String folderName = task.getFolderName();

        // 扫描图片目录
        String imgDirPath = imageBasePath + File.separator + folderName;
        File imgDir = new File(imgDirPath);
        List<String> imgFiles = new ArrayList<>();
        if (imgDir.exists() && imgDir.isDirectory()) {
            File[] files = imgDir.listFiles((dir, name) -> {
                String lower = name.toLowerCase();
                return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                       lower.endsWith(".png") || lower.endsWith(".gif") ||
                       lower.endsWith(".bmp") || lower.endsWith(".webp") ||
                       lower.endsWith(".svg");
            });
            if (files != null) {
                imgFiles = Arrays.stream(files)
                        .map(File::getName)
                        .sorted()
                        .collect(Collectors.toList());
            }
        }

        // 扫描视频目录
        String vidDirPath = videoBasePath + File.separator + folderName;
        File vidDir = new File(vidDirPath);
        List<String> vidFiles = new ArrayList<>();
        if (vidDir.exists() && vidDir.isDirectory()) {
            File[] files = vidDir.listFiles((dir, name) -> {
                String lower = name.toLowerCase();
                return lower.endsWith(".mp4") || lower.endsWith(".webm") ||
                       lower.endsWith(".avi") || lower.endsWith(".mov") ||
                       lower.endsWith(".flv") || lower.endsWith(".mkv");
            });
            if (files != null) {
                vidFiles = Arrays.stream(files)
                        .map(File::getName)
                        .sorted()
                        .collect(Collectors.toList());
            }
        }

        result.put("folderName", folderName);
        result.put("imageBasePath", imageBasePath);
        result.put("videoBasePath", videoBasePath);
        result.put("imgDirPath", imgDirPath);
        result.put("vidDirPath", vidDirPath);
        result.put("imgFiles", imgFiles);
        result.put("vidFiles", vidFiles);
        result.put("imgCount", imgFiles.size());
        result.put("vidCount", vidFiles.size());

        return result;
    }

    @Override
    public MediaCrawlTask getNextPendingTask() {
        LambdaQueryWrapper<MediaCrawlTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MediaCrawlTask::getStatus, "PENDING")
               .orderByAsc(MediaCrawlTask::getCreateTime)
               .last("LIMIT 1");
        return mapper.selectOne(wrapper);
    }

    @Override
    public void updateTaskStatus(Long id, String status) {
        MediaCrawlTask task = new MediaCrawlTask();
        task.setId(id);
        task.setStatus(status);
        task.setUpdateTime(new Date());
        mapper.updateById(task);
    }

    @Override
    public void updateTaskResult(Long id, String title, String folderName, String status,
                                  Integer imgCount, Long imgTotalSize,
                                  Integer videoCount, Long videoTotalSize) {
        MediaCrawlTask task = new MediaCrawlTask();
        task.setId(id);
        task.setTitle(title);
        task.setFolderName(folderName);
        task.setStatus(status);
        task.setImgCount(imgCount);
        task.setImgTotalSize(imgTotalSize);
        task.setVideoCount(videoCount);
        task.setVideoTotalSize(videoTotalSize);
        task.setUpdateTime(new Date());
        mapper.updateById(task);
    }

    @Override
    public boolean deleteTask(Long id) {
        MediaCrawlTask task = mapper.selectById(id);
        if (task == null) {
            return false;
        }

        // 删除本地文件目录
        if (task.getFolderName() != null) {
            // 删除图片目录
            String imgDirPath = imageBasePath + File.separator + task.getFolderName();
            File imgDir = new File(imgDirPath);
            if (imgDir.exists() && imgDir.isDirectory()) {
                deleteDirectory(imgDir);
                log.info("已删除图片目录: {}", imgDirPath);
            }

            // 删除视频目录
            String vidDirPath = videoBasePath + File.separator + task.getFolderName();
            File vidDir = new File(vidDirPath);
            if (vidDir.exists() && vidDir.isDirectory()) {
                deleteDirectory(vidDir);
                log.info("已删除视频目录: {}", vidDirPath);
            }
        }

        // 删除数据库记录
        int rows = mapper.deleteById(id);
        log.info("已删除任务记录: id={}, 影响行数={}", id, rows);
        return rows > 0;
    }

    /**
     * 递归删除目录及其所有内容
     */
    private void deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }
}
