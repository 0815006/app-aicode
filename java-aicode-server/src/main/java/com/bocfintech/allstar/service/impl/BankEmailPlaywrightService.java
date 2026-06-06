package com.bocfintech.allstar.service.impl;

import com.bocfintech.allstar.entity.ParkingBook;
import com.bocfintech.allstar.entity.ParkingRecord;
import com.bocfintech.allstar.util.AesUtils;
import com.microsoft.playwright.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;

/**
 * 银行邮箱发送服务 - 基于 Playwright 自动化
 * 登录银行邮箱 → 新建邮件 → 填写收件人/主题/正文 → 发送
 */
@Service
@Slf4j
public class BankEmailPlaywrightService {

    private static final String LOGIN_URL = "https://web.mail.bank-of-china.com/webmail/login/login.do";

    // 页面元素 ID（银行邮箱固定）
    private static final String USERNAME_ID = "#usernumber";
    private static final String PASSWORD_ID = "#password";
    private static final String LOGIN_BTN_ID = "#login_otp";
    private static final String COMPOSE_BTN_ID = "#btn_compose";
    private static final String MAIL_IFRAME = "#ifrm_compose_0";
    private static final String TO_ID = "#rib_input_1";
    private static final String SUBJECT_ID = "#txtsubject";
    private static final String BODY_ID = "#txtContent";
    private static final String SEND_BTN_ID = "#btnSend";

    /**
     * 异步发送预约结果通知邮件
     * 根据 parking_book 配置，解密邮箱密码后登录发送
     *
     * @param config  parking_book 配置（含邮箱账号、密码、收件人）
     * @param record  预约记录（含日期、车位等）
     */
    @Async
    public void sendParkingNotification(ParkingBook config, ParkingRecord record) {
        if (config == null || record == null) {
            log.warn("邮件通知：config 或 record 为空，跳过发送");
            return;
        }
        if (config.getEmailEnabled() == null || config.getEmailEnabled() != 1) {
            log.info("邮件通知：用户 {} 未开启邮件通知，跳过", config.getEmpNo());
            return;
        }

        String emailUser = config.getEmailUser();
        if (emailUser == null || emailUser.isEmpty()) {
            emailUser = config.getEmpNo(); // 默认取工号
        }
        String encryptedPassword = config.getEmailPassword();
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            log.warn("邮件通知：用户 {} 未配置邮箱密码，跳过发送", config.getEmpNo());
            return;
        }
        String recipient = config.getEmailRecipient();
        if (recipient == null || recipient.isEmpty()) {
            log.warn("邮件通知：用户 {} 未配置收件人邮箱，跳过发送", config.getEmpNo());
            return;
        }

        // 解密邮箱密码
        String emailPassword;
        try {
            emailPassword = AesUtils.decrypt(encryptedPassword);
        } catch (Exception e) {
            log.error("邮件通知：用户 {} 邮箱密码解密失败", config.getEmpNo(), e);
            return;
        }

        // 组装主题和正文
        String appointmentDate = nvl(record.getAppointmentDate(), "暂无");
        String parkingPosition = nvl(record.getParkingPosition(), "暂无");
        String subject = "车辆预约成功 " + appointmentDate + " 位置：" + parkingPosition;
        String content = subject; // 正文和主题一致

        log.info("邮件通知：开始发送 → 用户={}, 收件人={}, 主题={}", config.getEmpNo(), recipient, subject);

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setSlowMo(50));
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setViewportSize(1920, 1080));
            Page page = context.newPage();

            try {
                // 步骤1: 打开登录页面
                log.info("邮件通知：打开登录页面...");
                page.navigate(LOGIN_URL);
                page.waitForTimeout(3000);

                // 步骤2: 登录
                log.info("邮件通知：执行登录...");
                page.fill(USERNAME_ID, emailUser);
                page.fill(PASSWORD_ID, emailPassword);
                page.click(LOGIN_BTN_ID);
                page.waitForTimeout(5000);

                // 步骤3: 点击【新建邮件】
                log.info("邮件通知：点击新建邮件...");
                page.click(COMPOSE_BTN_ID);
                page.waitForTimeout(3000);

                // 步骤4: 切换到编辑区 iframe
                FrameLocator composeFrame = page.frameLocator(MAIL_IFRAME);

                // 步骤5: 输入收件人
                log.info("邮件通知：输入收件人 {}", recipient);
                Locator toField = composeFrame.locator(TO_ID);
                toField.click();
                toField.fill(recipient);
                toField.press("Tab"); // 触发格式校验
                page.waitForTimeout(1000);

                // 步骤6: 输入主题
                log.info("邮件通知：输入主题 {}", subject);
                composeFrame.locator(SUBJECT_ID).fill(subject);

                // 步骤7: 输入正文
                log.info("邮件通知：输入正文...");
                composeFrame.locator(BODY_ID).fill(content);

                // 步骤8: 点击发送
                log.info("邮件通知：点击发送...");
                composeFrame.locator(SEND_BTN_ID).click();
                page.waitForTimeout(3000);

                log.info("邮件通知：发送成功！用户={}, 收件人={}", config.getEmpNo(), recipient);

            } catch (Exception e) {
                log.error("邮件通知：发送失败 用户={}", config.getEmpNo(), e);
                try {
                    page.screenshot(new Page.ScreenshotOptions()
                            .setPath(Paths.get("email_error_" + config.getEmpNo() + ".png")));
                } catch (Exception ignored) {}
            } finally {
                context.close();
                browser.close();
            }
        } catch (Exception e) {
            log.error("邮件通知：浏览器启动失败 用户={}", config.getEmpNo(), e);
        }
    }

    private static String nvl(String value, String defaultValue) {
        return (value == null || value.isEmpty()) ? defaultValue : value;
    }
}
