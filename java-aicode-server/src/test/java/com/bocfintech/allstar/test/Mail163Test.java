package com.bocfintech.allstar.test;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 163邮箱发送测试程序
 * 基于 Playwright 自动化浏览器 — 登录 mail.163.com → 写邮件 → 发送 → 关闭浏览器
 * <p>
 * 实现机制与 {@code BankEmailPlaywrightService} 一致，使用 Playwright 驱动 Chromium 浏览器
 * 通过 Chrome DevTools Protocol (CDP) 与浏览器通信，实现自动化邮件发送。
 * 本测试程序针对 163.com（网易邮箱）适配了页面元素定位策略。
 * <p>
 * <b>使用前请务必修改 USERNAME 和 PASSWORD 占位符！</b>
 * <p>
 * 运行方式：直接执行 main 方法即可。
 *
 * @author test
 * @date 2026-06-09
 */
public class Mail163Test {

    // ==================== 请修改为真实账号密码 ====================
    /** 163邮箱登录用户名（完整邮箱地址，如 yourname@163.com） */
    private static final String USERNAME = "0815006@163.com";
    /** 163邮箱登录密码 或 客户端授权码 */
    private static final String PASSWORD = "900130";
    // ============================================================

    /** 收件人 */
    private static final String RECIPIENT = "0815006@163.com";
    /** 邮件主题 */
    private static final String SUBJECT = "预约停车位：4号楼B124";
    /** 邮件正文 */
    private static final String BODY = "停车预约成功";

    /** 163邮箱登录页 */
    private static final String LOGIN_URL = "https://mail.163.com/";

    /** 登录后等待时间（毫秒），给页面充足的加载时间 */
    private static final int WAIT_AFTER_LOGIN = 5000;
    /** 常规等待时间（毫秒） */
    private static final int WAIT_NORMAL = 3000;
    /** 短等待时间（毫秒） */
    private static final int WAIT_SHORT = 1000;

    // 模拟从 yml 中读取的配置（如果你使用 Spring，可以用 @Value("${chrome.executable-path}") 等注入）
    private static final String CHROME_PATH = "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe";
    private static final int CHROME_PORT = 9222;
    private static final String USER_DATA_DIR = "C:\\chrome_dev_profile";

    public static void main(String[] args) {
        printBanner();

        if (!checkPlaceholders()) {
            return;
        }
        Process chromeProcess = null;

        try {
            // ==================== 步骤 A: 由 Java 动态调用命令打开 Chrome ====================
            log("正在启动本地 Chrome 浏览器并开启 CDP 调试端口...");
            
            List<String> command = new ArrayList<>();
            command.add(CHROME_PATH);
            command.add("--remote-debugging-port=" + CHROME_PORT);
            command.add("--user-data-dir=" + USER_DATA_DIR);
            // 建议增加以下参数，确保内网环境中多余的弹窗或更新提示不会打扰自动化
            command.add("--no-first-run");
            command.add("--no-default-browser-check"); 

            ProcessBuilder pb = new ProcessBuilder(command);
            // 将错误流合并，方便排查潜在的启动错误
            pb.redirectErrorStream(true); 
            chromeProcess = pb.start();

            // ==================== 步骤 B: 启动之后，等待几秒时间 ====================
            log("浏览器启动中，等待 5 秒确保端口就绪...");
            sleep(5000); 

            // ==================== 步骤 C: Playwright 连接并操作 ====================
            try (Playwright playwright = Playwright.create()) {
                // 启动 Chromium 浏览器 — headless=false 可观察执行过程
                // Playwright 使用独立的 Chromium（非系统 Chrome），位于:
                //   %LOCALAPPDATA%\ms-playwright\chromium-1134\chrome.exe
                /* 
                Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                        .setHeadless(false)   // 有头模式，方便调试观察
                        .setSlowMo(80));       // 操作间隔80ms，模拟人工操作速度

                BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                        .setViewportSize(1920, 1080));

                Page page = context.newPage();
                */
                // ---- 核心改动：不再 launch 新浏览器，而是通过 CDP 连接已经打开的浏览器 ----
                log("正在连接内网已打开的 Chrome 浏览器...");
                Browser browser = playwright.chromium().connectOverCDP("http://localhost:9222");

                // 注意：接管已打开的浏览器时，通常直接获取它当前已经存在的 context 和 page
                // 如果直接 newContext，部分全局配置（如 headless）将以手动打开的浏览器为准
                BrowserContext context = browser.contexts().get(0); 
                Page page = context.pages().get(0);

                try {
                    // ---- 步骤1: 打开163邮箱登录页 ----
                    log("步骤1: 打开163邮箱登录页面 " + LOGIN_URL);
                    page.navigate(LOGIN_URL);
                    page.waitForLoadState(LoadState.NETWORKIDLE);
                    sleep(WAIT_NORMAL);

                    // ---- 步骤2: 执行登录 ----
                    log("步骤2: 执行登录...");
                    login163(page);

                    // ---- 步骤3: 进入邮箱主页后，点击"写信" ----
                    log("步骤3: 点击写信按钮...");
                    clickComposeButton(page);

                    // ---- 步骤4: 填写收件人 ----
                    log("步骤4: 填写收件人 -> " + RECIPIENT);
                    fillRecipient(page, RECIPIENT);

                    // ---- 步骤5: 填写主题 ----
                    log("步骤5: 填写主题 -> " + SUBJECT);
                    fillSubject(page, SUBJECT);

                    // ---- 步骤6: 填写正文 ----
                    log("步骤6: 填写正文 -> " + BODY);
                    fillBody(page, BODY);

                    // ---- 步骤7: 点击发送 ----
                    log("步骤7: 点击发送按钮...");
                    clickSendButton(page);
                    sleep(WAIT_NORMAL);

                    log("========== ✅ 邮件发送成功！ ==========");

                } catch (Exception e) {
                    log("========== ❌ 邮件发送失败 ==========");
                    System.err.println("异常信息: " + e.getMessage());
                    e.printStackTrace();
                    // 保存错误截图
                    try {
                        String screenshotFile = "mail163_error_"
                                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                                + ".png";
                        page.screenshot(new Page.ScreenshotOptions()
                                .setPath(Paths.get(screenshotFile))
                                .setFullPage(true));
                        log("已保存错误截图: " + screenshotFile);
                    } catch (Exception ignored) {
                    }
                } finally {
                    log("关闭浏览器...");
                    context.close();
                    browser.close();
                }
            } catch (Exception e) {
                System.err.println("浏览器启动失败: " + e.getMessage());
                e.printStackTrace();
            }

            log("========== 163邮箱发送测试结束 ==========");

            // ==================== 步骤 D: 发送完成后，关闭之前，等待几秒时间 ====================
                log("邮件发送完成，等待 3 秒后关闭浏览器...");
                sleep(3000);

        } catch (IOException e) {
            log("启动 Chrome 进程失败，请检查 Yml 中的路径是否正确: " + e.getMessage());
        } catch (Exception e) {
            log("发邮件过程中发生异常: " + e.getMessage());
        } finally {
            // ==================== 步骤 E: 彻底关闭浏览器进程 ====================
            if (chromeProcess != null && chromeProcess.isAlive()) {
                log("正在关闭 Chrome 浏览器进程...");
                // 强行终止 Chrome 进程
                chromeProcess.destroyForcibly(); 
                log("Chrome 浏览器已关闭。");
            }
        }
    }

    // =========================== 登录 ===========================

    /**
     * 登录163邮箱。
     * <p>
     * 页面结构（关键）：
     * <pre>
     *   主页面 https://mail.163.com/
     *     └─ iframe id="x-URS-iframe{动态时间戳}"     ← 登录表单在这里！
     *          └─ form id="login-form"
     *               ├─ input name="email"    placeholder="邮箱账号或手机号码"
     *               ├─ input name="password" placeholder="输入密码"
     *               └─ a     id="dologin"    文字="登&nbsp;&nbsp;录"（含空格实体）
     * </pre>
     * <p>
     * 定位要点：
     * 1. iframe ID 是动态的（如 x-URS-iframe1781010295396.641），用 id 前缀匹配
     * 2. 主页面 DOM 中没有任何 input 元素，所有输入框都在 iframe 里
     * 3. 登录按钮文字是"登&nbsp;&nbsp;录"（&amp;nbsp; 为 HTML 空格实体），
     *    不能用 text()='登录' 匹配，直接用 #dologin（ID 是稳定的）
     * 4. 按钮初始带 btndisabled class，填写账号密码后 163 的 JS 自动移除
     */
    private static void login163(Page page) {
        sleep(WAIT_AFTER_LOGIN);
        log("当前URL: " + page.url());
        log("页面标题: " + page.title());

        try {
            /*
             * ==== 第1步：定位登录 iframe ====
             *
             * iframe 的完整 HTML：
             *   <iframe id="x-URS-iframe1781010295396.641"
             *           src="https://dl.reg.163.com/webzj/v1.0.1/pub/index_dl2_new.html">
             *
             * 问题：id 后缀 "1781010295396.641" 是动态时间戳，每次加载都不同
             * 解决：用 CSS 属性选择器 [id^='x-URS-iframe'] 做前缀匹配
             *   - ^= 表示 "以...开头"
             *   - 不能用 #x-URS-iframe，因为 id 完整值 ≠ "x-URS-iframe"
             */
            String iframeSel = "iframe[id^='x-URS-iframe']";
            FrameLocator loginFrame = page.frameLocator(iframeSel);
            log("使用登录iframe: " + iframeSel);

            /*
             * ==== 第2步：等待邮箱输入框可见 ====
             *
             * 真实 DOM：
             *   <input name="email"
             *          class="j-inputtext dlemail j-nameforslide"
             *          placeholder="邮箱账号或手机号码"
             *          data-loginname="loginEmail"
             *          type="text">
             *
             * 选择器: input[name='email']
             *   - name="email" 是稳定的属性，不会变
             *   - data-loginname 也可用但更长
             *   - placeholder 包含中文，可能有编码问题
             *   - class 中的 j-nameforslide 是动态功能类，不够稳定
             */
            loginFrame.locator("input[name='email']").waitFor(
                    new Locator.WaitForOptions().setTimeout(15000));
            log("登录表单已可见");

            /*
             * ==== 第3步：填写邮箱 ====
             * 在 iframe 内操作，必须通过 loginFrame.locator()，不能用 page.locator()
             * fill() 会自动: 聚焦 → 全选 → 清空 → 输入文字
             */
            log("填写邮箱账号: " + USERNAME);
            loginFrame.locator("input[name='email']").fill(USERNAME);
            sleep(WAIT_SHORT);
            log("账号填写完成");

            /*
             * ==== 第4步：填写密码 ====
             *
             * 真实 DOM：
             *   <input name="password"
             *          class="j-inputtext dlpwd"
             *          placeholder="输入密码"
             *          type="password"
             *          autocomplete="new-password">
             *
             * 注意：页面还有一个隐藏的 <input id="pwdtext" type="text">
             *   它是密码可见/不可见的切换辅助元素，不要误填到它
             *   用 input[name='password'] 精确匹配
             */
            log("填写密码...");
            loginFrame.locator("input[name='password']").fill(PASSWORD);
            sleep(WAIT_SHORT);
            log("密码填写完成");

            /*
             * ==== 第5步：点击登录 ====
             *
             * 真实 DOM：
             *   <a id="dologin"
             *      class="u-loginbtn btncolor tabfocus btndisabled"
             *      href="javascript:void(0);">
             *     登&nbsp;&nbsp;录
             *   </a>
             *
             * 选择器: #dologin（ID 是固定的，不会变）
             *
             * 踩坑：为什么不用文字匹配？
             *   - 文字是 "登&nbsp;&nbsp;录"，包含两个 &nbsp; 空格实体
             *   - 页面渲染后显示为 "登  录"（登 + 两个空格 + 录）
             *   - //a[text()='登录'] 会匹配失败（没有无空格版）
             *   - //a[contains(text(),'登')] 可以匹配，但 #dologin 更简洁
             *
             * btndisabled 说明：
             *   - 按钮初始有 btndisabled class，点击不响应
             *   - 163 的 JS 监听 input 事件，填写账号密码后自动移除该 class
             *   - 所以 fill() 之后稍微 sleep(500) 等 JS 完成状态更新
             */
            log("点击登录按钮...");
            sleep(500); // 等 163 JS 移除 btndisabled class
            loginFrame.locator("#dologin").click();
            log("已点击登录按钮");

            // 等待登录完成，页面跳转至邮箱主页
            page.waitForLoadState(LoadState.NETWORKIDLE);
            sleep(WAIT_AFTER_LOGIN);
            page.waitForLoadState(LoadState.NETWORKIDLE);

            log("登录后URL: " + page.url());
            log("登录后页面标题: " + page.title());

        } catch (Exception e) {
            log("登录异常详情: " + e.toString());
            throw new RuntimeException("163邮箱登录失败: " + e.getMessage(), e);
        }
    }

    // =========================== 写信 ===========================

    /**
     * 点击"写信"按钮打开邮件编辑界面。
     * <p>
     * 页面结构（登录后邮箱主页的左侧导航栏）：
     * <pre>
     *   nav
     *     └─ ul
     *          ├─ li role="button" class="... ra0 nb0"       ← 收信按钮
     *          │    ├─ span class="om0" (icon)
     *          │    └─ span class="oz0" → "收 信"           ← 也用 span.oz0！
     *          │
     *          └─ li role="button" class="... ra0 mD0"       ← 写信按钮
     *               ├─ span class="om0" (icon)
     *               └─ span class="oz0" → "写 信"            ← 文字中间有空格
     * </pre>
     * <p>
     * 定位要点：
     * 1. "收 信" 和 "写 信" 共用一个 class="oz0"
     * 2. 只用 span.oz0 会匹配到两个元素，Playwright 默认取第一个 → 点错！
     * 3. 文字中间有空格（"写 信" 不是 "写信"），不能写 text()='写信'
     * 4. 解决方案：XPath + text 过滤，contains(text(),'写') 只匹配第二个
     */
    private static void clickComposeButton(Page page) {
        try {
            /*
             * 选择器: //span[@class='oz0' and contains(text(),'写')]
             *
             * 拆解：
             *   //span                     找所有 span 元素
             *   [@class='oz0']             限定 class="oz0"（排除 icon 的 om0）
             *   [contains(text(),'写')]    文字中包含"写"字（排除"收 信"）
             *
             * 为什么不用 li.mD0？
             *   mD0 是状态类（可能表示"当前选中"），不够稳定
             *   文字过滤是最直接可靠的方式
             */
            log("等待写信按钮出现...");
            page.waitForSelector("//span[@class='oz0' and contains(text(),'写')]",
                    new Page.WaitForSelectorOptions().setTimeout(15000));
            log("写信按钮已出现，点击...");
            page.click("//span[@class='oz0' and contains(text(),'写')]");
            sleep(WAIT_NORMAL);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            log("写信编辑区已打开");
        } catch (Exception e) {
            throw new RuntimeException("点击写信按钮失败: " + e.getMessage(), e);
        }
    }

    // =========================== 填写收件人 ===========================

    /**
     * 在收件人输入框中填写收件人地址。
     * <p>
     * 真实 DOM（写信编辑区）：
     * <pre>
     *   div class="nui-editableAddr nui-editableAddr-edit"
     *     input class="nui-editableAddr-ipt"
     *           type="text"
     *           role="combobox"
     *           aria-label="收件人地址输入框，请输入邮件地址，多人时地址请以分号隔开"
     *     span  class="nui-editableAddr-txt" → "W" (宽度计算辅助元素)
     * </pre>
     * <p>
     * 定位要点：
     * 1. class="nui-editableAddr-ipt" 在写信页是唯一的，无同名冲突
     * 2. aria-label 包含完整描述文字，但太长容易写错，class 更简洁
     * 3. 填写后按 Tab 触发 163 的邮箱格式校验（否则发送时可能报错）
     */
    private static void fillRecipient(Page page, String recipient) {
        try {
            /*
             * 选择器: input.nui-editableAddr-ipt
             *   - nui-editableAddr-ipt 是收件人输入框的专用 class
             *   - 写信编辑区只有这一个收件人框
             *   - 无需用 aria-label（文字太长）
             */
            log("等待收件人输入框出现...");
            page.waitForSelector("input.nui-editableAddr-ipt",
                    new Page.WaitForSelectorOptions().setTimeout(10000));
            page.fill("input.nui-editableAddr-ipt", recipient);
            // 按 Tab 触发 163 邮箱格式校验：输入框失去焦点 → JS 解析邮箱地址
            // 如果不触发校验直接发送，163 可能提示"收件人格式错误"
            page.press("input.nui-editableAddr-ipt", "Tab");
            sleep(WAIT_SHORT);
        } catch (Exception e) {
            throw new RuntimeException("填写收件人失败: " + e.getMessage(), e);
        }
    }

    // =========================== 填写主题 ===========================

    /**
     * 在主题输入框中填写邮件主题。
     * <p>
     * 真实 DOM（写信编辑区）：
     * <pre>
     *   div id="..."_subjectInput"  ← ID 前缀是动态的（如 1781011984728_）
     *     input class="nui-ipt-input"
     *           type="text"
     *           maxlength="256"       ← 关键区分属性！
     *           autocomplete="off"
     * </pre>
     * <p>
     * 定位要点：
     * 1. 页面有两处使用 class="nui-ipt-input"：
     *    a) 顶部搜索框（没有 maxlength 属性或 maxlength=不同值）
     *    b) 主题输入框（maxlength="256"）
     * 2. maxlength="256" 是主题框独有的，用它精确区分
     * 3. ID 不能用（"_mail_input_0_172_inputId" vs "1781011984728_subjectInput"），
     *    前缀每次都变
     */
    private static void fillSubject(Page page, String subject) {
        try {
            /*
             * 选择器: input.nui-ipt-input[maxlength='256']
             *
             * 拆解：
             *   input.nui-ipt-input      匹配 nui-ipt-input 类的 input
             *   [maxlength='256']        进一步限定 maxlength=256（主题框独有）
             *
             * 踩坑记录：
             *   之前用 input.nui-ipt-input[type='text'] 太泛
             *   → 顶部搜索框也是 type='text' + class='nui-ipt-input'
             *   → 导致 fill 到错误的输入框
             *   → 改用 maxlength='256' 后解决
             */
            log("等待主题输入框出现...");
            page.waitForSelector("input.nui-ipt-input[maxlength='256']",
                    new Page.WaitForSelectorOptions().setTimeout(10000));
            log("填写主题...");
            page.fill("input.nui-ipt-input[maxlength='256']", subject);
            sleep(WAIT_SHORT);
            log("主题填写完成");
        } catch (Exception e) {
            throw new RuntimeException("填写主题失败: " + e.getMessage(), e);
        }
    }

    // =========================== 填写正文 ===========================

    /**
     * 在正文编辑区填写邮件正文。
     * <p>
     * 163 写信页使用 APP-editor 富文本编辑器，正文在 iframe 里：
     * <pre>
     *   div id="_mail_editor_0_403"
     *     div class="APP-editor APP-editor-basic"
     *       div class="APP-editor-tbar"           ← 工具栏（加粗/斜体等）
     *       iframe class="APP-editor-iframe"      ← 正文在这！
     *         html
     *           body                               ← 可编辑区，contenteditable
     * </pre>
     * <p>
     * 定位要点：
     * 1. 正文编辑器的主体是 iframe，不是 div，需要 FrameLocator 进入
     * 2. iframe class 包含 "APP-editor"（完整是 "APP-editor-iframe"）
     *    用 contains 匹配防止 class 名变化
     * 3. 在 iframe 内，直接 fill 到 body 元素（整个 body 是可编辑区）
     * 4. 备选方案：某些版本/上下文正文在 div#spnEditorContent 中
     */
    private static void fillBody(Page page, String body) {
        try {
            log("等待正文编辑器出现...");

            /*
             * 方式1：新版163 — 正文在 iframe.APP-editor-iframe 中
             *
             * 选择器链：
             *   iframe[class*='APP-editor']        找 APP-editor 相关的 iframe
             *     -> body                           iframe 内的 body 就是编辑区
             *
             * [class*='APP-editor'] 是 CSS contains 匹配
             *   class="APP-editor-iframe" 会被匹配到
             *   即使 163 升级后 class 变成 "APP-editor-v2-iframe" 也能适配
             */
            Locator bodyIframe = page.locator("iframe[class*='APP-editor']");
            if (bodyIframe.count() > 0) {
                log("找到正文编辑器iframe，在其中填写...");
                FrameLocator editorFrame = page.frameLocator("iframe[class*='APP-editor']");
                // 等待 iframe 内 body 元素加载
                editorFrame.locator("body").waitFor(
                        new Locator.WaitForOptions().setTimeout(10000));
                // contenteditable 区域先 click 聚焦，再 fill
                editorFrame.locator("body").click();
                editorFrame.locator("body").fill(body);
                sleep(WAIT_SHORT);
                log("正文填写完成(iframe方式)");
                return;
            }

            /*
             * 方式2：旧版/特殊上下文 — 正文在主页面 div 中
             *
             * 选择器: div[id*='spnEditor']
             *   某些页面正文不在 iframe 里，而是 <div id="spnEditorContent">
             *   [id*='spnEditor'] 做 contains 匹配更灵活
             */
            log("未找到iframe，尝试主页面填写...");
            page.waitForSelector("div[id*='spnEditor']",
                    new Page.WaitForSelectorOptions().setTimeout(5000));
            page.click("div[id*='spnEditor']");
            sleep(200);
            page.fill("div[id*='spnEditor']", body);
            sleep(WAIT_SHORT);
            log("正文填写完成(div方式)");

        } catch (Exception e) {
            throw new RuntimeException("填写正文失败: " + e.getMessage(), e);
        }
    }

    // =========================== 发送 ===========================

    /**
     * 点击"发送"按钮。
     * <p>
     * 真实 DOM（写信编辑区工具栏）：
     * <pre>
     *   div role="toolbar" class="nui-toolbar"
     *     div class="nui-toolbar-item"
     *       div role="button" id="_mail_button_0_377"       ← ID 是动态的！
     *           class="nui-mainBtn nui-btn nui-btn-hasIcon"
     *         span class="nui-btn-icon" (icon)
     *         span class="nui-btn-text" → "发送"            ← 我们定位这个
     *       /div
     *       div role="button" → "预 览"
     *       div role="button" → "存草稿"
     *       div role="button" → "取 消"
     * </pre>
     * <p>
     * 定位要点：
     * 1. 按钮是 div[role="button"]，不是 button 或 a 标签（容易被忽略）
     * 2. 按钮 ID 是动态的（_mail_button_0_377），不能依赖
     * 3. span.nui-btn-text 在工具栏中匹配 4 个按钮，
     *    需加文字过滤 contains(text(),'发送')
     * 4. 用 XPath 而非 CSS，因为 CSS 不支持 text 过滤
     */
    private static void clickSendButton(Page page) {
        try {
            /*
             * 选择器: //span[@class='nui-btn-text' and contains(text(),'发送')]
             *
             * 拆解：
             *   //span                         遍历所有 span
             *   [@class='nui-btn-text']        精确匹配 class（排除 icon 的 span）
             *   [contains(text(),'发送')]      文字包含"发送"
             *
             * 为什么先 waitForSelector 再 click？
             *   - 遍历两个步骤确保元素已渲染完毕且可交互
             *   - 如果元素存在但不可见，waitForSelector 会等到超时
             *
             * 备用方案：Ctrl+Enter 快捷键
             *   163 邮箱也支持 Ctrl+Enter 发送邮件
             *   如果按钮定位失败可改 page.keyboard().press("Control+Enter")
             */
            log("等待发送按钮出现...");
            page.waitForSelector("span.nui-btn-text",
                    new Page.WaitForSelectorOptions().setTimeout(10000));
            page.click("//span[@class='nui-btn-text' and contains(text(),'发送')]");
            log("已点击发送按钮");
        } catch (Exception e) {
            throw new RuntimeException("点击发送按钮失败: " + e.getMessage(), e);
        }
    }

    // =========================== 工具方法 ===========================

    /**
     * 检查用户名密码占位符是否已修改
     */
    private static boolean checkPlaceholders() {
        if ("your_email@163.com".equals(USERNAME)) {
            System.err.println("┌─────────────────────────────────────────────────┐");
            System.err.println("│  错误: 请先修改 USERNAME 和 PASSWORD 占位符！    │");
            System.err.println("│  位置: Mail163Test.java 第26-28行                │");
            System.err.println("│  USERNAME → 你的163邮箱地址                       │");
            System.err.println("│  PASSWORD → 你的163邮箱密码或客户端授权码          │");
            System.err.println("└─────────────────────────────────────────────────┘");
            return false;
        }
        return true;
    }

    private static void log(String msg) {
        System.out.println("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + msg);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void printBanner() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║        163邮箱发送测试程序 (Playwright)           ║");
        System.out.println("╠══════════════════════════════════════════════════╣");
        System.out.println("║  收件人: " + padRight(RECIPIENT, 27) + "  ║");
        System.out.println("║  主  题: " + padRight(SUBJECT, 27) + "  ║");
        System.out.println("║  正  文: " + padRight(BODY, 27) + "  ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.println();
    }

    private static String padRight(String s, int n) {
        if (s.length() >= n) return s;
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < n) sb.append(' ');
        return sb.toString();
    }
}
