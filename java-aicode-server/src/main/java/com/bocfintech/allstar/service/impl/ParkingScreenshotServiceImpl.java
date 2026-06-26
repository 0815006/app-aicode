package com.bocfintech.allstar.service.impl;

import com.bocfintech.allstar.service.ParkingScreenshotService;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 停车大屏截图服务实现
 * <p>
 * 根据车位位置文本打开本地 ParkingScreen 页面，截取 SVG 车位图区域，
 * 并将截图写入 Windows 系统剪贴板，供邮件粘贴使用。
 * </p>
 *
 * <h3>楼层映射规则</h3>
 * <p>与前端 {@code ParkingBookList.goToParkingScreen()} 完全一致：</p>
 * <pre>
 *   "4号楼B1层B124"    →  floor=4F-B1, space=B124
 *   "5号楼B1层C056"    →  floor=5F-B1, space=C056
 *   "5号楼B2层B区B045" →  floor=5F-B2, space=B045（去掉 B区 标记）
 * </pre>
 *
 * @author allstar
 * @since 2.0
 */
@Service
@Slf4j
public class ParkingScreenshotServiceImpl implements ParkingScreenshotService {

    /** 前端 Vue dev server 地址 */
    private static final String PARKING_SCREEN_URL = "http://localhost:8082/#/parking-screen";

    /** 截图元素选择器：纯 SVG 车位图区域（不含标题栏和右侧信息面板） */
    private static final String SVG_WRAPPER_SELECTOR = ".svg-wrapper";

    /** 截图等待时间（ms）：确保 SVG 渲染 + 定位大头针动画启动 */
    private static final int SCREENSHOT_WAIT_MS = 2000;

    /** 截图文件命名模板 */
    private static final String SCREENSHOT_NAME_PREFIX = "parking_screenshot_";

    /**
     * 楼层映射表 —— 与前端 {@code ParkingBookList.goToParkingScreen()} 完全一致
     */
    private static final Map<String, String> FLOOR_MAP = new LinkedHashMap<>();
    static {
        FLOOR_MAP.put("4号楼B1层", "4F-B1");
        FLOOR_MAP.put("5号楼B1层", "5F-B1");
        FLOOR_MAP.put("5号楼B2层", "5F-B2");
    }

    // ==================== 公共方法 ====================

    @Override
    public String takeScreenshot(Browser browser, String parkingPosition) {
        if (parkingPosition == null || parkingPosition.isEmpty()) {
            log.warn("截图服务：parkingPosition 为空，跳过");
            return null;
        }

        // 1. 解析楼层 ID
        String floorKey = parseFloorKey(parkingPosition);
        if (floorKey == null) {
            log.warn("截图服务：无法识别楼层，parkingPosition={}", parkingPosition);
            return null;
        }
        String floorId = FLOOR_MAP.get(floorKey);

        // 2. 解析车位编号
        String spaceLabel = parseSpaceLabel(parkingPosition, floorKey);

        // 3. 拼接 URL
        String url = PARKING_SCREEN_URL + "?floor=" + floorId + "&space=" + spaceLabel;
        log.info("截图服务：打开页面 {}", url);

        // 4. 新建独立 BrowserContext（不影响邮箱页）
        BrowserContext screenshotCtx = null;
        try {
            screenshotCtx = browser.newContext(
                new Browser.NewContextOptions().setViewportSize(1920, 1080));
            Page screenshotPage = screenshotCtx.newPage();

            // 5. 导航并等待渲染
            screenshotPage.navigate(url);
            screenshotPage.waitForLoadState(LoadState.NETWORKIDLE);
            screenshotPage.waitForTimeout(SCREENSHOT_WAIT_MS);

            // 6. 截取 SVG 区域
            String screenshotPath = SCREENSHOT_NAME_PREFIX
                    + System.currentTimeMillis() + ".png";
            screenshotPage.locator(SVG_WRAPPER_SELECTOR).screenshot(
                new Locator.ScreenshotOptions().setPath(Paths.get(screenshotPath)));
            log.info("截图服务：截图成功 → {}", screenshotPath);
            return screenshotPath;

        } catch (Exception e) {
            log.error("截图服务：截图失败 parkingPosition={}", parkingPosition, e);
            return null;
        } finally {
            if (screenshotCtx != null) {
                try { screenshotCtx.close(); } catch (Exception ignored) {}
            }
        }
    }

    @Override
    public boolean copyToClipboard(String imagePath) {
        if (imagePath == null) {
            return false;
        }
        File file = new File(imagePath);
        if (!file.exists()) {
            log.warn("截图服务：剪贴板写入失败，文件不存在 {}", imagePath);
            return false;
        }
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                log.warn("截图服务：无法读取图片 {}", imagePath);
                return false;
            }
            // 匿名 Transferable：包装 BufferedImage 写入系统剪贴板
            Transferable transferable = new Transferable() {
                @Override
                public DataFlavor[] getTransferDataFlavors() {
                    return new DataFlavor[] { DataFlavor.imageFlavor };
                }
                @Override
                public boolean isDataFlavorSupported(DataFlavor flavor) {
                    return DataFlavor.imageFlavor.equals(flavor);
                }
                @Override
                public Object getTransferData(DataFlavor flavor) {
                    return image;
                }
            };
            Toolkit.getDefaultToolkit()
                   .getSystemClipboard()
                   .setContents(transferable, null);
            log.info("截图服务：图片已写入系统剪贴板");
            return true;
        } catch (Exception e) {
            log.error("截图服务：剪贴板写入失败", e);
            return false;
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 从车位位置文本中匹配楼层中文键
     * <p>
     * "4号楼B1层B124" → "4号楼B1层"<br>
     * "5号楼B2层B区B045" → "5号楼B2层"
     * </p>
     */
    private String parseFloorKey(String positionText) {
        for (String key : FLOOR_MAP.keySet()) {
            if (positionText.contains(key)) {
                return key;
            }
        }
        return null;
    }

    /**
     * 从车位位置文本中提取车位编号
     * <p>
     * "4号楼B1层B124"  → "B124"<br>
     * "5号楼B2层C056"  → "C056"<br>
     * "5号楼B2层B区B045" → "B045"（去掉 "B区" 标记）
     * </p>
     *
     * @param positionText 完整车位位置文本
     * @param floorKey     匹配到的楼层中文键，如 "5号楼B2层"
     * @return 车位编号
     */
    private String parseSpaceLabel(String positionText, String floorKey) {
        int idx = positionText.indexOf(floorKey);
        String raw = positionText.substring(idx + floorKey.length()).trim();
        // 5号楼B2层特殊处理：去掉 A区/B区 标记（与前端 goToParkingScreen() 一致）
        String floorId = FLOOR_MAP.get(floorKey);
        if ("5F-B2".equals(floorId)) {
            raw = raw.replaceAll("^[AB]区", "").trim();
        }
        return raw;
    }
}
