package com.bocfintech.allstar.service;

import com.microsoft.playwright.Browser;

/**
 * 停车大屏截图服务
 * <p>
 * 根据车位位置文本（如"4号楼B1层B124"），打开本地 ParkingScreen 页面，
 * 截取车位图 SVG 区域，写入系统剪贴板，供邮件粘贴使用。
 * </p>
 *
 * @author allstar
 * @since 2.0
 */
public interface ParkingScreenshotService {

    /**
     * 截图车位图的 SVG 区域并返回截图文件路径
     *
     * @param browser         Playwright Browser 实例（已连接 CDP）
     * @param parkingPosition 车位位置文本，如 "4号楼B1层B124"、"5号楼B2层B区B045"
     * @return 截图 PNG 文件路径，失败返回 null
     */
    String takeScreenshot(Browser browser, String parkingPosition);

    /**
     * 将图片文件写入 Windows 系统剪贴板
     *
     * @param imagePath PNG 图片的绝对路径
     * @return true 成功，false 失败
     */
    boolean copyToClipboard(String imagePath);
}
