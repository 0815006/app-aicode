package com.bocfintech.allstar.service.impl;

import com.bocfintech.allstar.entity.ParkingBook;
import com.bocfintech.allstar.entity.ParkingRecord;
import com.bocfintech.allstar.util.AesUtils;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;

/**
 * 银行邮箱发送服务 - 基于 Playwright 自动化
 * <p>
 * 登录银行邮箱 → 新建邮件 → 填写收件人/主题/正文 → 发送
 * <p>
 * 实现机制：
 * Playwright 通过 Chrome DevTools Protocol (CDP) 驱动 Chromium 浏览器。
 * 每次调用 launch() 启动一个独立 Chromium 进程，操作完成后关闭。
 * 和 {@code Mail163Test} 共享同一份 Chromium 安装：
 * {@code %LOCALAPPDATA%\ms-playwright\chromium-1097\chrome.exe}
 * <p>
 * 页面结构（与163邮箱的关键区别）：
 * <pre>
 *   银行企业邮箱                vs    163 免费邮箱
 *   ──────────                      ──────────
 *   登录：主页面直接填写              登录：iframe 内填写（动态 ID）
 *   写信：固定 iframe #ifrm_compose_0 写信：动态 iframe + 复杂选择器
 *   ID 全部固定                      ID 几乎全是动态的
 *   → 用 CSS #id 选择器即可          → 需组合 name/class/maxlength 等属性
 * </pre>
 */
@Service
@Slf4j
public class BankEmailPlaywrightService {

    /*
     * ==================== 登录页 URL ====================
     * 中国银行企业邮箱 WebMail 登录地址
     */
    private static final String LOGIN_URL = "https://web.mail.bank-of-china.com/webmail/login/login.do";

    /*
     * ==================== 登录页元素（主页面直接定位，无 iframe） ====================
     *
     * 登录页 DOM 结构：
     *
     *   form id="loginForm"
     *     ├── input id="usernumber"       ← 用户名/工号输入框
     *     ├── input id="password"         ← 密码输入框
     *     └── button/input id="login_otp" ← 登录按钮
     *
     * 与163的关键区别：银行邮箱的登录表单在主页面，不在 iframe 里。
     * ID 全部是固定的，直接用 #id 选择器即可。
     */

    /** 用户名输入框 — {@code <input id="usernumber">} */
    private static final String USERNAME_ID = "#usernumber";

    /** 密码输入框 — {@code <input id="password">} */
    private static final String PASSWORD_ID = "#password";

    /** 登录按钮 — {@code <button id="login_otp">} */
    private static final String LOGIN_BTN_ID = "#login_otp";

    /*
     * ==================== 写信页元素 ====================
     *
     * 点击登录后进入邮箱主页，页面结构：
     *
     *   主页面
     *     ├── button id="btn_compose"                     ← "新建邮件"按钮
     *     └── 写信后弹出一个编辑区域，在 iframe 内
     *          └── iframe id="ifrm_compose_0"              ← 编辑区整个在这个 iframe 里
     *               ├── input id="rib_input_1"             ← 收件人输入框
     *               ├── input id="txtsubject"              ← 主题输入框
     *               ├── textarea/div id="txtContent"       ← 正文编辑区
     *               └── button/input id="btnSend"          ← 发送按钮
     *
     * 关键点：写信编辑区在 iframe 里，必须用 FrameLocator 进入 iframe 再操作。
     */

    /** 新建邮件按钮 — {@code <button id="btn_compose">} */
    private static final String COMPOSE_BTN_ID = "#btn_compose";

    /** 写信编辑区 iframe — {@code <iframe id="ifrm_compose_0">} （ID 固定） */
    private static final String MAIL_IFRAME = "#ifrm_compose_0";

    /** 收件人输入框（在 iframe 内） — {@code <input id="rib_input_1">} */
    private static final String TO_ID = "#rib_input_1";

    /** 主题输入框（在 iframe 内） — {@code <input id="txtsubject">} */
    private static final String SUBJECT_ID = "#txtsubject";

    /** 正文编辑区（在 iframe 内） — {@code <textarea id="txtContent">} */
    private static final String BODY_ID = "#txtContent";

    /** 发送按钮（在 iframe 内） — {@code <button id="btnSend">} */
    private static final String SEND_BTN_ID = "#btnSend";

    /**
     * 异步发送预约结果通知邮件。
     * <p>
     * 根据 parking_book 配置，解密邮箱密码后登录银行邮箱发送通知。
     * 使用 {@code @Async} 注解，在独立线程中执行，不阻塞主流程。
     * <p>
     * 完整流程（8步）：
     * <ol>
     *   <li>打开登录页面</li>
     *   <li>填写用户名+密码，点击登录</li>
     *   <li>点击"新建邮件"按钮</li>
     *   <li>切换到编辑区 iframe</li>
     *   <li>填写收件人</li>
     *   <li>填写主题</li>
     *   <li>填写正文</li>
     *   <li>点击发送</li>
     * </ol>
     *
     * @param config parking_book 配置（含邮箱账号、加密密码、收件人、邮件开关）
     * @param record 预约记录（含日期、车位等信息，用于拼装邮件内容）
     */
    @Async
    public void sendParkingNotification(ParkingBook config, ParkingRecord record) {
        // ---- 参数校验 ----
        if (config == null || record == null) {
            log.warn("邮件通知：config 或 record 为空，跳过发送");
            return;
        }
        if (config.getEmailEnabled() == null || config.getEmailEnabled() != 1) {
            log.info("邮件通知：用户 {} 未开启邮件通知，跳过", config.getEmpNo());
            return;
        }

        // ---- 获取配置 ----
        String emailUser = config.getEmailUser();
        if (emailUser == null || emailUser.isEmpty()) {
            emailUser = config.getEmpNo(); // 默认取工号作为邮箱用户名
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

        // ---- 解密邮箱密码 ----
        // 数据库中存储的是 AES 加密后的密码，使用前需解密
        String emailPassword;
        try {
            emailPassword = AesUtils.decrypt(encryptedPassword);
        } catch (Exception e) {
            log.error("邮件通知：用户 {} 邮箱密码解密失败", config.getEmpNo(), e);
            return;
        }

        // ---- 组装邮件内容 ----
        String appointmentDate = nvl(record.getAppointmentDate(), "暂无");
        String parkingPosition = nvl(record.getParkingPosition(), "暂无");
        String subject = "车辆预约成功 " + appointmentDate + " 位置：" + parkingPosition;
        String content = subject; // 正文和主题一致

        log.info("邮件通知：开始发送 → 用户={}, 收件人={}, 主题={}", config.getEmpNo(), recipient, subject);

        /*
         * ==================== Playwright 浏览器操作 ====================
         *
         * 调用链：
         *   Playwright.create()         → 启动 Playwright Server (Node.js 进程)
         *   chromium().launch()         → Playwright Server 启动 Chromium 子进程
         *   browser.newContext()         → 创建独立浏览器会话（无痕窗口）
         *   context.newPage()            → 打开新标签页
         *
         * 浏览器路径（Windows）：
         *   %LOCALAPPDATA%\ms-playwright\chromium-1097\chrome.exe
         *
         * try-with-resources 确保 Playwright 实例在方法结束时自动关闭。
         */
        try (Playwright playwright = Playwright.create()) {

            /*
             * launch() 启动 Chromium 进程：
             *   headless=true  → 后台运行，无可见窗口（服务器环境必须用 true）
             *   slowMo=50      → 每步操作间隔50ms，模拟人类速度，降低被反爬拦截的风险
             *
             * 内网部署注意：
             *   如 launch() 失败，通常是因为缺少 VC++ 运行时。
             *   安装 vc_redist.x64.exe 即可解决：
             *     https://aka.ms/vc14/vc_redist.x64.exe
             */
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setSlowMo(50));

            // BrowserContext = 独立的无痕会话，Cookie/Storage 完全隔离
            // 即使多个账号同时发邮件（不同 Context），登录态也不会串
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setViewportSize(1920, 1080));

            Page page = context.newPage();

            try {
                /*
                 * ==== 步骤1：打开登录页面 ====
                 *
                 * URL: https://web.mail.bank-of-china.com/webmail/login/login.do
                 *
                 * 登录表单在主页面 DOM 中，无 iframe 嵌套。
                 * waitForLoadState(NETWORKIDLE) 等待网络请求全部完成，
                 * 比固定 waitForTimeout(3000) 更可靠（网速慢时不会过早操作）。
                 */
                log.info("邮件通知：打开登录页面 {} ...", LOGIN_URL);
                page.navigate(LOGIN_URL);
                page.waitForLoadState(LoadState.NETWORKIDLE);

                /*
                 * ==== 步骤2：登录 ====
                 *
                 * 页面元素定位（css selector → 真实 DOM）：
                 *
                 *   #usernumber → <input id="usernumber">  用户名/工号输入框
                 *   #password   → <input id="password">    密码输入框
                 *   #login_otp  → <button id="login_otp">  登录按钮
                 *
                 * 填写顺序：用户名 → 密码 → 点击登录。
                 * fill() 会自动完成：聚焦 → 全选 → 清空 → 输入文字。
                 */
                log.info("邮件通知：执行登录 user={} ...", emailUser);
                page.fill(USERNAME_ID, emailUser);
                page.fill(PASSWORD_ID, emailPassword);
                page.click(LOGIN_BTN_ID);

                // 登录后等待页面跳转完成
                page.waitForLoadState(LoadState.NETWORKIDLE);
                page.waitForTimeout(2000); // 额外等待登录后跳转动画/重定向

                /*
                 * ==== 步骤3：点击"新建邮件" ====
                 *
                 * 页面元素：
                 *   #btn_compose → <button id="btn_compose">新建邮件</button>
                 *
                 * 点击后页面会加载一个写信编辑区（在 iframe 里），见步骤4。
                 */
                log.info("邮件通知：点击新建邮件...");
                page.click(COMPOSE_BTN_ID);
                page.waitForLoadState(LoadState.NETWORKIDLE);

                /*
                 * ==== 步骤4：切换到编辑区 iframe ====
                 *
                 * 写信编辑区是独立 iframe：
                 *
                 *   主页面
                 *     └── iframe id="ifrm_compose_0"    ← 我们进入这里
                 *          ├── input id="rib_input_1"     ← 收件人
                 *          ├── input id="txtsubject"      ← 主题
                 *          ├── textarea id="txtContent"   ← 正文
                 *          └── button id="btnSend"        ← 发送
                 *
                 * FrameLocator 让后续操作都在 iframe 上下文内执行，
                 * 相当于在浏览器 DevTools 里切换到了 iframe 的控制台。
                 * 这和163邮箱一样——正文编辑器都在 iframe 里。
                 */
                FrameLocator composeFrame = page.frameLocator(MAIL_IFRAME);

                /*
                 * ==== 步骤5：填写收件人 ====
                 *
                 * 页面元素（iframe 内）：
                 *   #rib_input_1 → <input id="rib_input_1">  收件人邮箱地址
                 *
                 * 填写后按 Tab 触发邮箱格式校验（失去焦点 → JS 验证邮箱格式）。
                 * 如果不按 Tab，发送时可能被拦截提示"收件人格式错误"。
                 */
                log.info("邮件通知：输入收件人 {}", recipient);
                Locator toField = composeFrame.locator(TO_ID);
                toField.click();                   // 聚焦输入框
                toField.fill(recipient);           // 填入收件人地址
                toField.press("Tab");              // 触发格式校验
                page.waitForTimeout(1000);         // 等待 JS 校验完成

                /*
                 * ==== 步骤6：填写主题 ====
                 *
                 * 页面元素（iframe 内）：
                 *   #txtsubject → <input id="txtsubject">  邮件主题
                 *
                 * 主题内容从预约记录动态生成：
                 *   "车辆预约成功 2026-06-09 位置：4号楼B124"
                 */
                log.info("邮件通知：输入主题 {}", subject);
                composeFrame.locator(SUBJECT_ID).fill(subject);

                /*
                 * ==== 步骤7：填写正文 ====
                 *
                 * 页面元素（iframe 内）：
                 *   #txtContent → <textarea id="txtContent">  邮件正文
                 *
                 * 正文内容和主题相同，都是预约成功+日期+车位信息。
                 */
                log.info("邮件通知：输入正文...");
                composeFrame.locator(BODY_ID).fill(content);

                /*
                 * ==== 步骤8：点击发送 ====
                 *
                 * 页面元素（iframe 内）：
                 *   #btnSend → <button id="btnSend">发送</button>
                 *
                 * 点击后等待页面响应完成。
                 */
                log.info("邮件通知：点击发送...");
                composeFrame.locator(SEND_BTN_ID).click();
                page.waitForLoadState(LoadState.NETWORKIDLE);

                log.info("邮件通知：发送成功！用户={}, 收件人={}", config.getEmpNo(), recipient);

            } catch (Exception e) {
                // 发送失败时记录日志并保存页面截图用于排查
                log.error("邮件通知：发送失败 用户={}", config.getEmpNo(), e);
                try {
                    page.screenshot(new Page.ScreenshotOptions()
                            .setPath(Paths.get("email_error_" + config.getEmpNo() + ".png")));
                } catch (Exception ignored) {}
            } finally {
                // 确保浏览器资源释放（无论成功失败）
                context.close();
                browser.close();
            }
        } catch (Exception e) {
            // launch() 失败（浏览器未安装、缺少运行库等）
            log.error("邮件通知：浏览器启动失败 用户={}", config.getEmpNo(), e);
        }
    }

    /**
     * 空值处理：value 为 null 或空字符串时返回默认值。
     */
    private static String nvl(String value, String defaultValue) {
        return (value == null || value.isEmpty()) ? defaultValue : value;
    }
}
