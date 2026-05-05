package com.bocfintech.allstar.service.impl;

import com.bocfintech.allstar.entity.MediaCrawlTask;
import com.bocfintech.allstar.service.CrawlEngineService;
import com.bocfintech.allstar.service.MediaCrawlTaskService;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Collections;

/**
 * Playwright 抓取引擎实现类
 * 使用 Playwright for Java 实现页面渲染和媒体资源拦截下载
 */
@Service
public class CrawlEngineServiceImpl implements CrawlEngineService {

    private static final Logger log = LoggerFactory.getLogger(CrawlEngineServiceImpl.class);

    @Autowired
    private MediaCrawlTaskService taskService;

    @Value("${app.crawl.image-base-path}")
    private String imageBasePath;

    @Value("${app.crawl.video-base-path}")
    private String videoBasePath;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);
    private Set<Integer> httpErrorCodes;

    @Override
    @Async
    public void startEngine() {
        if (isShuttingDown.get()) {
            log.info("应用正在关闭，不启动抓取引擎");
            return;
        }
        if (isRunning.compareAndSet(false, true)) {
            log.info("抓取引擎启动，开始处理队列任务...");
            processNextTask();
        } else {
            log.info("抓取引擎已在运行中");
        }
    }

    /**
     * 递归处理下一个待处理任务（串行执行）
     */
    private void processNextTask() {
        // 检查是否正在关闭
        if (isShuttingDown.get()) {
            log.info("应用正在关闭，停止处理任务队列");
            isRunning.set(false);
            return;
        }
        
        try {
            MediaCrawlTask task = taskService.getNextPendingTask();
            log.info("getNextPendingTask 返回任务: {}", task != null ? task.getId() : "null");
            if (task != null) {
                log.info("开始处理任务: id={}, url={}", task.getId(), task.getUrl());
                processTask(task.getId());
                // 继续处理下一个
                processNextTask();
            } else {
                log.info("没有待处理的任务，引擎停止");
                isRunning.set(false);
            }
        } catch (Exception e) {
            log.error("处理任务队列时发生异常", e);
            isRunning.set(false);
        }
    }

    /**
     * 优雅停机
     */
    @PreDestroy
    public void shutdown() {
        log.info("应用开始关闭，设置停机标志...");
        isShuttingDown.set(true);
        // 等待当前任务处理完成（最多等待30秒）
        int waitCount = 0;
        while (isRunning.get() && waitCount < 60) {
            try {
                Thread.sleep(500);
                waitCount++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (isRunning.get()) {
            log.warn("等待超时，强制停止抓取引擎");
        } else {
            log.info("抓取引擎已停止");
        }
    }

    @Override
    public void processTask(Long taskId) {
        // 检查是否正在关闭
        if (isShuttingDown.get()) {
            log.info("应用正在关闭，跳过任务处理: id={}", taskId);
            return;
        }
        
        MediaCrawlTask task = taskService.getTaskById(taskId);
        if (task == null) {
            log.warn("任务不存在: id={}", taskId);
            return;
        }

        // 更新状态为抓取中
        taskService.updateTaskStatus(taskId, "PROCESSING");

        // 初始化错误码集合
        httpErrorCodes = Collections.synchronizedSet(new HashSet<>());

        // 生成文件夹名称：网页标题前20位 + 时间戳
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String folderName = "page_" + timestamp;

        Playwright playwright = null;
        Browser browser = null;
        try {
            // 启动 Playwright
            log.info("任务 {}: 尝试创建 Playwright 实例", taskId);
            playwright = Playwright.create();
            log.info("任务 {}: Playwright 实例创建成功", taskId);
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setTimeout(60000);
            browser = playwright.chromium().launch(launchOptions);

            BrowserContext context = browser.newContext(
                    new Browser.NewContextOptions()
                            .setViewportSize(1920, 1080)
                            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            );

            // 设置请求拦截，收集媒体资源
            Set<String> mediaUrls = new HashSet<>();
            context.onResponse(response -> {
                String contentType = response.headers().get("content-type");
                String url = response.url();
                int status = response.status();
                if (status == 401 || status == 403) {
                    log.warn("任务 {}: 检测到HTTP错误码: {} for URL: {}", taskId, status, url);
                    httpErrorCodes.add(status);
                }
                if (contentType != null && task.getCrawlType() != null) {
                    boolean isImage = contentType.startsWith("image/") &&
                            (task.getCrawlType().equals("IMAGE") || task.getCrawlType().equals("BOTH"));
                    boolean isVideo = contentType.startsWith("video/") &&
                            (task.getCrawlType().equals("VIDEO") || task.getCrawlType().equals("BOTH"));
                    if (isImage || isVideo) {
                        mediaUrls.add(url);
                    }
                }
            });

            Page page = context.newPage();

            log.info("任务 {}: 导航到目标页面: {}", taskId, task.getUrl());

            // 导航到目标页面
            try {
                page.navigate(task.getUrl(), new Page.NavigateOptions()
                        .setTimeout(30000)
                        .setWaitUntil(WaitUntilState.NETWORKIDLE));
                log.info("任务 {}: 页面导航成功", taskId);
            } catch (Exception e) {
                log.warn("任务 {}: 页面加载超时或失败: {}", taskId, task.getUrl());
            }

            log.info("任务 {}: 模拟滚动", taskId);
            // 模拟滚动，触发懒加载
            try {
                page.evaluate("window.scrollTo(0, document.body.scrollHeight)");
                Thread.sleep(2000);
                page.evaluate("window.scrollTo(0, 0)");
                Thread.sleep(1000);
                log.info("任务 {}: 模拟滚动完成", taskId);
            } catch (Exception e) {
                log.warn("任务 {}: 页面滚动异常", taskId, e);
            }

            // 获取网页标题
            String pageTitle = "";
            try {
                pageTitle = page.title();
                log.info("任务 {}: 获取网页标题: {}", taskId, pageTitle);
            } catch (Exception e) {
                log.warn("任务 {}: 获取网页标题失败", taskId, e);
            }

            // 清理标题中的特殊字符，用于文件夹命名
            String safeTitle = sanitizeFolderName(pageTitle);
            if (safeTitle.length() > 20) {
                safeTitle = safeTitle.substring(0, 20);
            }
            folderName = safeTitle + "_" + timestamp;

            log.info("任务 {}: 创建图片和视频目录: {} 和 {}", taskId, imageBasePath + File.separator + folderName, videoBasePath + File.separator + folderName);
            // 创建图片和视频目录
            String imgDirPath = imageBasePath + File.separator + folderName;
            String vidDirPath = videoBasePath + File.separator + folderName;
            Files.createDirectories(Paths.get(imgDirPath));
            Files.createDirectories(Paths.get(vidDirPath));
            log.info("任务 {}: 目录创建成功", taskId);

            // 下载媒体资源
            int imgCount = 0;
            long imgTotalSize = 0;
            int videoCount = 0;
            long videoTotalSize = 0;
            int minSizeBytes = (task.getMinSizeLimit() != null ? task.getMinSizeLimit() : 0) * 1024;

            log.info("任务 {}: 开始下载媒体资源，共 {} 个URL", taskId, mediaUrls.size());
            for (String mediaUrl : mediaUrls) {
                log.info("任务 {}: 尝试下载媒体文件: {}", taskId, mediaUrl);
                try {
                    URL url = new URL(mediaUrl);
                    URLConnection conn = url.openConnection();
                    conn.setConnectTimeout(60000);
                    conn.setReadTimeout(60000);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                    conn.setRequestProperty("Referer", task.getUrl());

                    // 获取文件大小
                    long fileSize = conn.getContentLengthLong();
                    if (fileSize > 0 && fileSize < minSizeBytes) {
                        log.debug("任务 {}: 文件太小，跳过: {} ({} bytes)", taskId, mediaUrl, fileSize);
                        continue;
                    }

                    // 从 URL 提取文件名
                    String pathStr = url.getPath();
                    String fileName = pathStr.substring(pathStr.lastIndexOf('/') + 1);
                    if (fileName.isEmpty() || !fileName.contains(".")) {
                        String contentType = conn.getContentType();
                        if (contentType != null) {
                            if (contentType.startsWith("image/")) {
                                String ext = contentType.replace("image/", "");
                                fileName = "img_" + imgCount + "." + ext;
                            } else if (contentType.startsWith("video/")) {
                                String ext = contentType.replace("video/", "");
                                fileName = "vid_" + videoCount + "." + ext;
                            }
                        }
                        if (fileName.isEmpty() || !fileName.contains(".")) {
                            fileName = "media_" + System.currentTimeMillis() + "_" + (imgCount + videoCount);
                        }
                    }

                    // 清理文件名中的特殊字符
                    fileName = sanitizeFileName(fileName);

                    String contentType = conn.getContentType();
                    if (contentType != null && contentType.startsWith("image/")) {
                        // 下载图片
                        Path targetPath = Paths.get(imgDirPath, fileName);
                        try (InputStream is = conn.getInputStream();
                             FileOutputStream fos = new FileOutputStream(targetPath.toFile())) {
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            long totalBytes = 0;
                            while ((bytesRead = is.read(buffer)) != -1) {
                                fos.write(buffer, 0, bytesRead);
                                totalBytes += bytesRead;
                            }
                            imgCount++;
                            imgTotalSize += totalBytes;
                            log.debug("任务 {}: 下载图片: {} ({} bytes)", taskId, fileName, totalBytes);
                        }
                        log.info("任务 {}: 图片下载完成: {}", taskId, fileName);
                    } else if (contentType != null && contentType.startsWith("video/")) {
                        // 下载视频
                        Path targetPath = Paths.get(vidDirPath, fileName);
                        try (InputStream is = conn.getInputStream();
                             FileOutputStream fos = new FileOutputStream(targetPath.toFile())) {
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            long totalBytes = 0;
                            while ((bytesRead = is.read(buffer)) != -1) {
                                fos.write(buffer, 0, bytesRead);
                                totalBytes += bytesRead;
                            }
                            videoCount++;
                            videoTotalSize += totalBytes;
                            log.debug("任务 {}: 下载视频: {} ({} bytes)", taskId, fileName, totalBytes);
                        }
                        log.info("任务 {}: 视频下载完成: {}", taskId, fileName);
                    }
                } catch (Throwable t) {
                    log.warn("任务 {}: 下载媒体文件失败: {}", taskId, mediaUrl, t);
                }
            }

            log.info("任务 {}: 媒体资源下载完成", taskId);

            // 更新任务结果
            String status;
            if (httpErrorCodes.contains(401) || httpErrorCodes.contains(403)) {
                status = "FAILED_AUTH";
                log.error("任务 {}: 因HTTP认证/权限错误失败: 错误码: {}", taskId, httpErrorCodes);
            } else if (imgCount > 0 || videoCount > 0) {
                status = "SUCCESS";
            } else {
                status = "FAILED";
            }
            taskService.updateTaskResult(taskId, safeTitle, folderName, status,
                    imgCount, imgTotalSize, videoCount, videoTotalSize);

            log.info("任务处理完成: id={}, 图片={}个, 视频={}个, 状态={}",
                    taskId, imgCount, videoCount, status);

            // 关闭浏览器
            page.close();
            context.close();
            browser.close();
            playwright.close();
            log.info("任务 {}: Playwright 资源已关闭", taskId);

        } catch (Exception e) {
            log.error("抓取任务异常: id={}", taskId, e);
            String finalStatus = "FAILED";
            if (httpErrorCodes.contains(401) || httpErrorCodes.contains(403)) {
                finalStatus = "FAILED_AUTH";
                log.error("任务 {}: 因HTTP认证/权限错误失败: 错误码: {}", taskId, httpErrorCodes);
            }
            taskService.updateTaskStatus(taskId, finalStatus);
            if (browser != null) {
                try { browser.close(); } catch (Exception ignored) {}
            }
            if (playwright != null) {
                try { playwright.close(); } catch (Exception ignored) {}
            }
            log.error("任务 {}: 抓取任务异常，Playwright 资源已尝试关闭", taskId);
        }
    }

    /**
     * 清理文件夹名称中的特殊字符
     */
    private String sanitizeFolderName(String name) {
        if (name == null || name.isEmpty()) {
            return "unknown";
        }
        return name.replaceAll("[\\\\/:*?\"<>|%]", "_")
                   .replaceAll("\\s+", "_")
                   .trim();
    }

    /**
     * 清理文件名中的特殊字符
     */
    private String sanitizeFileName(String name) {
        if (name == null || name.isEmpty()) {
            return "file_" + System.currentTimeMillis();
        }
        // 保留最后一个点用于扩展名
        int dotIndex = name.lastIndexOf('.');
        String baseName = dotIndex > 0 ? name.substring(0, dotIndex) : name;
        String ext = dotIndex > 0 ? name.substring(dotIndex) : "";

        baseName = baseName.replaceAll("[\\\\/:*?\"<>|]", "_")
                           .replaceAll("\\s+", "_")
                           .trim();
        if (baseName.length() > 100) {
            baseName = baseName.substring(0, 100);
        }
        return baseName + ext;
    }
}
