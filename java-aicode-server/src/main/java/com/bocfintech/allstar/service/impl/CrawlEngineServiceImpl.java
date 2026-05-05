package com.bocfintech.allstar.service.impl;

import com.bocfintech.allstar.entity.MediaCrawlTask;
import com.bocfintech.allstar.service.CrawlEngineService;
import com.bocfintech.allstar.service.MediaCrawlTaskService;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

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

            // 使用 ConcurrentHashMap 存储图片数据（URL -> 字节数组）
            Map<String, byte[]> mediaDataMap = new ConcurrentHashMap<>();
            // 存储媒体类型（URL -> contentType）
            Map<String, String> mediaContentTypeMap = new ConcurrentHashMap<>();

            // 设置请求拦截，收集媒体资源
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
                        try {
                            // 使用 Playwright 的 response.body() 直接获取字节流，避免二次请求
                            byte[] body = response.body();
                            if (body != null && body.length > 0) {
                                mediaDataMap.put(url, body);
                                mediaContentTypeMap.put(url, contentType);
                                log.debug("任务 {}: 拦截到媒体资源: {} ({} bytes)", taskId, url, body.length);
                            }
                        } catch (Exception e) {
                            log.warn("任务 {}: 获取媒体内容失败: {}", taskId, url, e);
                        }
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

            // 等待 DOM 加载完成
            try {
                page.waitForLoadState(LoadState.DOMCONTENTLOADED);
                // 给 SPA 一点额外时间更新 title
                page.waitForTimeout(1000);
                log.info("任务 {}: DOM 加载完成", taskId);
            } catch (Exception e) {
                log.warn("任务 {}: 等待 DOM 加载异常", taskId, e);
            }

            // 模拟步进式滚动，触发懒加载
            log.info("任务 {}: 开始步进式滚动以触发懒加载", taskId);
            try {
                page.evaluate("async () => {" +
                    "  await new Promise((resolve) => {" +
                    "    let totalHeight = 0;" +
                    "    let distance = 200;" +
                    "    let timer = setInterval(() => {" +
                    "      let scrollHeight = document.body.scrollHeight;" +
                    "      window.scrollBy(0, distance);" +
                    "      totalHeight += distance;" +
                    "      if(totalHeight >= scrollHeight) {" +
                    "        clearInterval(timer);" +
                    "        resolve();" +
                    "      }" +
                    "    }, 100);" +
                    "  });" +
                    "});");
                // 滚动完成后等待一下，确保所有懒加载触发完成
                page.waitForTimeout(2000);
                // 滚回顶部
                page.evaluate("window.scrollTo(0, 0)");
                page.waitForTimeout(500);
                log.info("任务 {}: 步进式滚动完成", taskId);
            } catch (Exception e) {
                log.warn("任务 {}: 页面滚动异常", taskId, e);
            }

            // 获取网页标题（优化版）
            String pageTitle = "";
            try {
                pageTitle = page.title();
                // 如果标题为空，尝试从 h1 获取
                if (pageTitle == null || pageTitle.isEmpty()) {
                    try {
                        pageTitle = page.innerText("h1");
                    } catch (Exception e) {
                        log.debug("任务 {}: 从 h1 获取标题失败", taskId);
                    }
                }
                // 如果还是为空，尝试从 og:title meta 获取
                if (pageTitle == null || pageTitle.isEmpty()) {
                    try {
                        String ogTitle = page.getAttribute("meta[property='og:title']", "content");
                        if (ogTitle != null && !ogTitle.isEmpty()) {
                            pageTitle = ogTitle;
                        }
                    } catch (Exception e) {
                        log.debug("任务 {}: 从 og:title 获取标题失败", taskId);
                    }
                }
                // 最后尝试从 title 标签获取
                if (pageTitle == null || pageTitle.isEmpty()) {
                    try {
                        Object titleContent = page.evaluate("() => document.querySelector('title')?.textContent");
                        if (titleContent != null) {
                            pageTitle = titleContent.toString();
                        }
                    } catch (Exception e) {
                        log.debug("任务 {}: 从 title 标签获取标题失败", taskId);
                    }
                }
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

            // 处理 Data URI (Base64 内嵌图片)
            try {
                List<ElementHandle> imgs = page.querySelectorAll("img[src^='data:image']");
                for (ElementHandle img : imgs) {
                    try {
                        String src = img.getAttribute("src");
                        if (src != null && src.startsWith("data:image/")) {
                            // 解析 Base64 数据
                            String base64Data = src.substring(src.indexOf(",") + 1);
                            String contentType = src.substring(5, src.indexOf(";"));
                            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                            String dataUriKey = "data-uri-" + System.currentTimeMillis() + "-" + imgs.indexOf(img);
                            mediaDataMap.put(dataUriKey, imageBytes);
                            mediaContentTypeMap.put(dataUriKey, contentType);
                            log.debug("任务 {}: 提取到 Data URI 图片: {} bytes", taskId, imageBytes.length);
                        }
                    } catch (Exception e) {
                        log.warn("任务 {}: 处理 Data URI 失败", taskId, e);
                    }
                }
            } catch (Exception e) {
                log.warn("任务 {}: 遍历 Data URI 图片异常", taskId, e);
            }

            // 处理懒加载属性（data-src, data-original 等）
            try {
                // 处理 data-src
                List<ElementHandle> lazyImgs = page.querySelectorAll("img[data-src], img[data-original], img[data-lazy]");
                for (ElementHandle img : lazyImgs) {
                    try {
                        String dataSrc = img.getAttribute("data-src");
                        if (dataSrc == null || dataSrc.isEmpty()) {
                            dataSrc = img.getAttribute("data-original");
                        }
                        if (dataSrc == null || dataSrc.isEmpty()) {
                            dataSrc = img.getAttribute("data-lazy");
                        }
                        if (dataSrc != null && !dataSrc.isEmpty() && !dataSrc.startsWith("data:")) {
                            // 尝试通过 Playwright 加载这个 URL
                            log.debug("任务 {}: 发现懒加载图片: {}", taskId, dataSrc);
                            // 使用 JavaScript 设置 src 属性来触发加载
                            try {
                                img.evaluate("(element, newSrc) => { element.src = newSrc; }", dataSrc);
                                page.waitForTimeout(500); // 等待图片加载
                            } catch (Exception e) {
                                log.debug("任务 {}: 设置懒加载图片 src 失败: {}", taskId, dataSrc);
                            }
                        }
                    } catch (Exception e) {
                        log.warn("任务 {}: 处理懒加载属性失败", taskId, e);
                    }
                }
            } catch (Exception e) {
                log.warn("任务 {}: 遍历懒加载图片异常", taskId, e);
            }

            // 下载媒体资源
            int imgCount = 0;
            long imgTotalSize = 0;
            int videoCount = 0;
            long videoTotalSize = 0;
            int minSizeBytes = (task.getMinSizeLimit() != null ? task.getMinSizeLimit() : 0) * 1024;

            log.info("任务 {}: 开始保存媒体资源，共 {} 个资源", taskId, mediaDataMap.size());
            for (Map.Entry<String, byte[]> entry : mediaDataMap.entrySet()) {
                String mediaUrl = entry.getKey();
                byte[] mediaBytes = entry.getValue();
                String contentType = mediaContentTypeMap.get(mediaUrl);
                
                if (mediaBytes == null || mediaBytes.length == 0) {
                    continue;
                }

                // 检查文件大小
                if (mediaBytes.length < minSizeBytes) {
                    log.debug("任务 {}: 文件太小，跳过: {} ({} bytes)", taskId, mediaUrl, mediaBytes.length);
                    continue;
                }

                try {
                    // 从 URL 提取文件名
                    String fileName;
                    if (mediaUrl.startsWith("data-uri-")) {
                        // Data URI 生成的文件名
                        if (contentType != null && contentType.startsWith("image/")) {
                            String ext = contentType.replace("image/", "");
                            fileName = "img_datauri_" + imgCount + "." + ext;
                        } else {
                            fileName = "media_datauri_" + System.currentTimeMillis() + ".bin";
                        }
                    } else {
                        String pathStr = new URL(mediaUrl).getPath();
                        fileName = pathStr.substring(pathStr.lastIndexOf('/') + 1);
                        if (fileName.isEmpty() || !fileName.contains(".")) {
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
                    }

                    // 清理文件名中的特殊字符
                    fileName = sanitizeFileName(fileName);

                    if (contentType != null && contentType.startsWith("image/")) {
                        // 保存图片
                        Path targetPath = Paths.get(imgDirPath, fileName);
                        try (FileOutputStream fos = new FileOutputStream(targetPath.toFile())) {
                            fos.write(mediaBytes);
                            imgCount++;
                            imgTotalSize += mediaBytes.length;
                            log.debug("任务 {}: 保存图片: {} ({} bytes)", taskId, fileName, mediaBytes.length);
                        }
                        log.info("任务 {}: 图片保存完成: {}", taskId, fileName);
                    } else if (contentType != null && contentType.startsWith("video/")) {
                        // 保存视频
                        Path targetPath = Paths.get(vidDirPath, fileName);
                        try (FileOutputStream fos = new FileOutputStream(targetPath.toFile())) {
                            fos.write(mediaBytes);
                            videoCount++;
                            videoTotalSize += mediaBytes.length;
                            log.debug("任务 {}: 保存视频: {} ({} bytes)", taskId, fileName, mediaBytes.length);
                        }
                        log.info("任务 {}: 视频保存完成: {}", taskId, fileName);
                    }
                } catch (Throwable t) {
                    log.warn("任务 {}: 保存媒体文件失败: {}", taskId, mediaUrl, t);
                }
            }

            log.info("任务 {}: 媒体资源保存完成", taskId);

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
