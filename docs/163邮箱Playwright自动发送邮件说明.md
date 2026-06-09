# 163 邮箱 Playwright 自动发送邮件程序说明

## 概述

`Mail163Test` 是一个基于 **Playwright for Java** 的 163 邮箱自动化测试程序，通过驱动 Chromium 浏览器实现：自动登录邮箱 → 写邮件 → 发送 → 关闭浏览器。

实现机制与项目中的 `BankEmailPlaywrightService` 完全一致，区别在于本程序针对 **mail.163.com（网易邮箱）** 适配了页面元素定位策略。

- **文件位置**: `java-aicode-server/src/test/java/com/bocfintech/allstar/test/Mail163Test.java`
- **运行方式**: 直接执行 `main` 方法
- **依赖**: Playwright 1.41.0（Chromium 浏览器由 Playwright 自动管理）

---

## 整体流程（7 步）

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ 1.打开登录页  │→   │ 2.登录邮箱    │→   │ 3.点击"写信"  │→   │ 4.填写收件人  │
│ mail.163.com │    │  用户名+密码   │    │ 左侧导航栏    │    │ 收件人输入框  │
└──────────────┘    └──────────────┘    └──────────────┘    └──────────────┘
                                                                   ↓
┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  完成！       │←   │ 7.点击发送    │←   │ 6.填写正文    │←   │ 5.填写主题    │
│  关闭浏览器   │    │ 发送按钮      │    │ 正文编辑器    │    │ 主题输入框    │
└──────────────┘    └──────────────┘    └──────────────┘    └──────────────┘
```

---

## 各步骤实现细节

### 步骤 1：打开登录页

```
URL: https://mail.163.com/
```

直接导航到 163 邮箱首页。页面标题为 `"163网易免费邮-你的专业电子邮局"`。

### 步骤 2：登录邮箱

登录表单**位于一个动态 ID 的 `<iframe>` 中**，这是实现过程中最关键也最容易出错的一步。

#### 页面结构分析

```html
<!-- 主页面：只包含一个 iframe，无表单元素 -->
<html>
  <body>
    <iframe id="x-URS-iframe1781010295396.641"
            src="https://dl.reg.163.com/webzj/v1.0.1/pub/index_dl2_new.html">
      <!-- 真正的登录表单在 iframe 内部 -->
      <form id="login-form">
        <input name="email"    placeholder="邮箱账号或手机号码">
        <input name="password" placeholder="输入密码">
        <a id="dologin">登&nbsp;&nbsp;录</a>
      </form>
    </iframe>
  </body>
</html>
```

#### 关键难点

| 难点 | 说明 | 解决方案 |
|------|------|----------|
| iframe ID 含动态后缀 | 每次加载 ID 都不同，如 `x-URS-iframe1781010295396.641` | 使用 CSS 前缀匹配：`iframe[id^='x-URS-iframe']` |
| iframe 内元素不可见 | 主页面有 0 个 input，输入框全在 iframe 里 | 使用 `FrameLocator` 在 iframe 内定位 |
| 登录按钮是 `&nbsp;` 分隔 | `<a id="dologin">登&nbsp;&nbsp;录</a>`，文字不是 `"登录"` | 直接用 `#dologin`（ID 是稳定的） |
| 按钮初始 disabled | 带 `btndisabled` class，需先填写账号密码 | 163 的 JS 在输入后会**自动移除** disabled 状态 |

#### 选择器对照

| 元素 | 选择器 | 真实 DOM 依据 |
|------|--------|--------------|
| 登录 iframe | `iframe[id^='x-URS-iframe']` | `<iframe id="x-URS-iframe{动态后缀}">` |
| 邮箱输入框 | `input[name='email']` | `<input name="email" placeholder="邮箱账号或手机号码">` |
| 密码输入框 | `input[name='password']` | `<input name="password" placeholder="输入密码">` |
| 登录按钮 | `#dologin` | `<a id="dologin">登&nbsp;&nbsp;录</a>` |

#### 代码逻辑

```java
// 1. 用前缀匹配定位动态ID的iframe
FrameLocator loginFrame = page.frameLocator("iframe[id^='x-URS-iframe']");

// 2. 等待iframe内元素可见（15秒超时）
loginFrame.locator("input[name='email']").waitFor(...);

// 3. 在iframe内依次填写并点击
loginFrame.locator("input[name='email']").fill(USERNAME);
loginFrame.locator("input[name='password']").fill(PASSWORD);
loginFrame.locator("#dologin").click();
```

### 步骤 3：点击"写信"

登录成功后页面跳转到邮箱主页，左侧导航栏有写信按钮。

#### 页面结构分析

```html
<nav>
  <ul>
    <!-- "收 信" 和 "写 信" 共用相同的 class 和结构 -->
    <li role="button">                          <!-- 收信 -->
      <span class="om0"><b class="..."></b></span>
      <span class="oz0">收 信</span>            <!-- ← 注意：也用 span.oz0 -->
    </li>
    <li role="button" class="mD0">              <!-- 写信：父级多了 mD0 -->
      <span class="om0"><b class="..."></b></span>
      <span class="oz0">写 信</span>            <!-- ← 注意：文字有空格 -->
    </li>
  </ul>
</nav>
```

#### 关键难点

| 难点 | 说明 | 解决方案 |
|------|------|----------|
| `span.oz0` 匹配两个元素 | `"收 信"` 和 `"写 信"` 都用 `class="oz0"` | 加文字过滤：`contains(text(),'写')` |
| 文字中间有空格 | `"写 信"` 不是 `"写信"` | 用 `contains(text(),'写')` 而非 `text()='写信'` |

#### 选择器

| 元素 | 选择器 | 真实 DOM 依据 |
|------|--------|--------------|
| 写信按钮 | `//span[@class='oz0' and contains(text(),'写')]` | `<span class="oz0">写 信</span>` |

### 步骤 4：填写收件人

#### 页面结构

```html
<div class="nui-editableAddr nui-editableAddr-edit">
  <input class="nui-editableAddr-ipt" type="text" role="combobox"
         aria-label="收件人地址输入框，请输入邮件地址，多人时地址请以分号隔开">
  <span class="nui-editableAddr-txt">W</span>
</div>
```

#### 选择器

| 元素 | 选择器 | 真实 DOM 依据 |
|------|--------|--------------|
| 收件人输入框 | `input.nui-editableAddr-ipt` | `<input class="nui-editableAddr-ipt">` |

#### 代码逻辑

```java
page.fill("input.nui-editableAddr-ipt", recipient);
page.press("input.nui-editableAddr-ipt", "Tab"); // 触发163邮箱格式校验
```

### 步骤 5：填写主题

#### 页面结构

```html
<input id="{动态前缀}_subjectInput"
       class="nui-ipt-input"
       type="text"
       maxlength="256"
       autocomplete="off">
```

#### 关键难点

| 难点 | 说明 | 解决方案 |
|------|------|----------|
| 页面有两个 `input.nui-ipt-input` | 顶部搜索框和主题框**共用** `class="nui-ipt-input"` | 用 `maxlength="256"` 区分（搜索框没有此属性） |
| ID 是动态的 | `1781011984728_subjectInput` 每次不同 | 不用 ID，用 `maxlength` 属性选择 |

#### 选择器

| 元素 | 选择器 | 真实 DOM 依据 |
|------|--------|--------------|
| 主题输入框 | `input.nui-ipt-input[maxlength='256']` | `<input class="nui-ipt-input" maxlength="256">` |

### 步骤 6：填写正文

#### 页面结构

163 写信页使用 **APP-editor** 富文本编辑器组件，正文编辑区嵌套在 iframe 中：

```html
<div id="_mail_editor_0_403" class="js-component-editor">
  <div class="APP-editor APP-editor-basic">
    <!-- 工具栏... -->
    <iframe class="APP-editor-iframe">
      <html>
        <body>
          <!-- 可编辑的正文区 -->
        </body>
      </html>
    </iframe>
  </div>
</div>
```

#### 关键难点

| 难点 | 说明 | 解决方案 |
|------|------|----------|
| 正文在 iframe 内 | APP-editor 的编辑区是独立 iframe | 用 `FrameLocator` 进入 iframe，在 `body` 上 fill |
| 不同版本差异 | 某些版本/上下文正文可能是 `div#spnEditorContent` | 先尝试 iframe，找不到再尝试 div |

#### 选择器

| 元素 | 选择器 | 真实 DOM 依据 |
|------|--------|--------------|
| 正文编辑器 iframe | `iframe[class*='APP-editor']` | `<iframe class="APP-editor-iframe">` |
| iframe 内编辑区 | `body` | iframe 内 `<body>` 为 contenteditable |
| 备选：主页面正文区 | `div[id*='spnEditor']` | `<div id="spnEditorContent">` |

### 步骤 7：点击发送

#### 页面结构

```html
<div role="button" id="_mail_button_0_377"
     class="js-component-button nui-mainBtn nui-btn nui-btn-hasIcon">
  <span class="nui-btn-icon"><b class="..."></b></span>
  <span class="nui-btn-text">发送</span>           <!-- ← 我们定位这个 -->
</div>
```

#### 关键难点

| 难点 | 说明 | 解决方案 |
|------|------|----------|
| 按钮是 `<div>` 不是 `<button>` | 163 用 `div[role="button"]` 模拟按钮 | 直接定位 `span.nui-btn-text` |
| 父级 ID 是动态的 | `_mail_button_0_377` 每次不同 | 不使用 ID，用 class+text |

#### 选择器

| 元素 | 选择器 | 真实 DOM 依据 |
|------|--------|--------------|
| 发送按钮 | `//span[@class='nui-btn-text' and contains(text(),'发送')]` | `<span class="nui-btn-text">发送</span>` |

---

## 选择器总览

| 步骤 | 元素 | 选择器 | 类型 |
|------|------|--------|------|
| 登录 | 登录 iframe | `iframe[id^='x-URS-iframe']` | CSS 前缀匹配 |
| 登录 | 邮箱 | `input[name='email']` | CSS |
| 登录 | 密码 | `input[name='password']` | CSS |
| 登录 | 登录按钮 | `#dologin` | CSS ID |
| 写信 | 写信按钮 | `//span[@class='oz0' and contains(text(),'写')]` | XPath + text |
| 写信 | 收件人 | `input.nui-editableAddr-ipt` | CSS class |
| 写信 | 主题 | `input.nui-ipt-input[maxlength='256']` | CSS 属性 |
| 写信 | 正文 | `iframe[class*='APP-editor']` → `body` | CSS + FrameLocator |
| 写信 | 发送 | `//span[@class='nui-btn-text' and contains(text(),'发送')]` | XPath + text |

---

## 关键技术策略

### 1. iframe 跨域操作

程序中有两个关键 iframe 需要处理：

| 位置 | iframe 识别方式 | Playwright 操作 |
|------|----------------|-----------------|
| 登录页 | `iframe[id^='x-URS-iframe']` | `page.frameLocator()` |
| 正文编辑 | `iframe[class*='APP-editor']` | `page.frameLocator()` |

### 2. 动态 ID 处理

163 邮箱使用 React/Vue 等框架，大部分 ID 是动态生成的（格式如 `xxx-1781010295396.641`）。程序**完全不依赖**这些动态 ID，统一使用以下稳定属性：

| 稳定属性 | 使用场景 |
|----------|----------|
| `name` | 登录表单的 email/password |
| `class` | 收件人、主题、发送按钮 |
| `maxlength` | 区分主题框和搜索框 |
| `text content` | 区分"写 信"和"收 信" |
| `id 前缀` | 定位登录 iframe |

### 3. 等待策略

不做盲目 `sleep`，而是使用 Playwright 的 `waitForSelector`：

```java
// 正确：等待元素真实可见（最多15秒）
page.waitForSelector("input.nui-editableAddr-ipt",
    new Page.WaitForSelectorOptions().setTimeout(10000));

// 辅助：在关键步骤间加短延迟确保 JS 动画完成
sleep(1000);  // Tab 切换、按钮状态更新等
```

### 4. 错误处理

- 每一步都有 `try-catch`，失败时打印**详细异常信息**
- 失败时自动保存**全页截图** `mail163_error_yyyyMMdd_HHmmss.png`
- 浏览器资源使用 `try-with-resources` 确保**必定关闭**

---

## 运行环境

| 项目 | 版本/说明 |
|------|----------|
| Java | 1.8 |
| Playwright | 1.41.0 |
| 浏览器 | Chromium（Playwright 自动安装） |
| OS | Windows 10 |
| 运行模式 | `headless=false`（有头模式，可观察过程） |

---

## 使用方法

### 1. 修改账号密码

打开 `Mail163Test.java`，修改第 28-30 行：

```java
private static final String USERNAME = "your_email@163.com";  // → 真实邮箱
private static final String PASSWORD = "your_password";       // → 真实密码或授权码
```

### 2. 运行

直接执行 `Mail163Test.main()` 方法。

### 3. 观察输出

控制台会打印每一步的带时间戳日志：

```
[21:02:29] 步骤1: 打开163邮箱登录页面 https://mail.163.com/
[21:02:34] 步骤2: 执行登录...
[21:02:39] 使用登录iframe: iframe[id^='x-URS-iframe']
[21:02:41] 登录表单已可见
[21:02:42] 填写邮箱账号: 0815006@163.com
...
[21:03:15] ========== ✅ 邮件发送成功！ ==========
```

---

## 踩坑记录（定位过程中的实际问题和解决）

| # | 问题 | 原因 | 解决 |
|---|------|------|------|
| 1 | `input[name='email']` 超时找不到 | 输入框在 iframe 里，主页面一个 input 都没有 | 用 `FrameLocator` + `iframe[id^='x-URS-iframe']` |
| 2 | `#login-form` 超时不可见 | 表单在 iframe 内，`waitForSelector` 在主页面等不到 | 不等待 form，直接等待 iframe 内的 input |
| 3 | 点击写信点到了收件箱 | `span.oz0` 匹配了两个元素，"收 信"在前 | 加文字过滤 `contains(text(),'写')` |
| 4 | 主题框定位到搜索框 | 两者用同一个 class `nui-ipt-input` | 用独有的 `maxlength='256'` 区分 |
| 5 | `//span[text()='登录']` 匹配不到登录按钮 | 按钮文字是 `登&nbsp;&nbsp;录`（带空格实体） | 改用 `#dologin`（ID 稳定） |
| 6 | `//*[contains(text(),'写信')]` 匹配不到写信按钮 | 文字是 `"写 信"`（含空格），不是 `"写信"` | 改用 `contains(text(),'写')` |
| 7 | 正文 `fill` 失败 | 旧选择器 `#spnEditorContent` 在写信页不存在，正文在 `iframe.APP-editor-iframe` 里 | 改为优先定位 `iframe[class*='APP-editor']`，在 iframe 内 `body` 上 fill |

---

## 与 BankEmailPlaywrightService 的对比

| 对比项 | BankEmailPlaywrightService | Mail163Test |
|--------|---------------------------|-------------|
| 邮箱平台 | 中国银行企业邮箱 | 网易 163 免费邮箱 |
| 登录 URL | `web.mail.bank-of-china.com` | `mail.163.com` |
| 登录方式 | 主页面直接填写 | iframe 内填写（动态 ID） |
| 写信按钮 | `#btn_compose` | `//span[@class='oz0' and contains(text(),'写')]` |
| 正文编辑区 | 固定 iframe `#ifrm_compose_0` | 动态 iframe `iframe[class*='APP-editor']` |
| 邮件内容 | 动态生成（预约结果） | 固定的测试内容 |
| 运行方式 | Spring `@Async` 服务 | 独立 `main` 方法 |

---

## Playwright 驱动浏览器的底层机制

### 核心架构

```
┌─────────────────────────────────────────────────────────┐
│ Java 应用层                                              │
│ Mail163Test.java                                        │
│ BankEmailPlaywrightService.java                         │
│                                                         │
│ com.microsoft.playwright (1.41.0)                       │
│ ├─ Playwright.create()    创建 Playwright 实例           │
│ ├─ BrowserType.launch()   启动浏览器进程                  │
│ ├─ BrowserContext         独立的浏览器会话（无痕模式）      │
│ ├─ Page                   单个标签页                      │
│ ├─ FrameLocator           跨 iframe 元素定位              │
│ └─ Locator                元素定位与操作                  │
└─────────────────┬───────────────────────────────────────┘
                  │ WebSocket (JSON-RPC)
┌─────────────────▼───────────────────────────────────────┐
│ Playwright Server (Node.js 进程)                         │
│ 位置: %TEMP%\playwright-java-{随机ID}\package\          │
│                                                         │
│ ├─ lib/server/             驱动核心                      │
│ ├─ lib/server/frames.js    iframe/Frame 管理             │
│ ├─ lib/server/launcher.js  浏览器启动器                  │
│ └─ lib/server/progress.js  操作超时控制                  │
└─────────────────┬───────────────────────────────────────┘
                  │ Chrome DevTools Protocol (CDP)
                  │ WebSocket 连接 localhost:{随机端口}
┌─────────────────▼───────────────────────────────────────┐
│ Chromium 浏览器进程                                      │
│ 位置: %LOCALAPPDATA%\ms-playwright\chromium-1134\       │
│                                                         │
│ ├─ chrome.exe            浏览器主进程                    │
│ ├─ chrome.dll            核心引擎                        │
│ └─ chrome-headless-shell 无头模式 Shell                  │
│                                                         │
│ 渲染引擎: Blink (Chromium 内置)                          │
│ JS 引擎:  V8                                              │
└─────────────────────────────────────────────────────────┘
```

### CDP 协议工作原理

Playwright 通过 **Chrome DevTools Protocol (CDP)** 与浏览器通信，这和开发者按 F12 打开 DevTools 的原理完全一样：

```
Java 调用                          CDP 协议                             浏览器执行
─────────                         ────────                             ────────────
page.navigate(url)  ──→   Page.navigate({url: "..."})    ──→   浏览器加载页面
page.fill(sel, val) ──→   DOM.getDocument +              ──→   找到元素并填入文本
                          Input.dispatchKeyEvent + 
                          Input.insertText
page.click(sel)    ──→   DOM.querySelector +             ──→   找到元素并触发 click 事件
                          Input.dispatchMouseEvent
locator.waitFor()  ──→   DOM.querySelector +             ──→   轮询直到元素可见
                          Runtime.evaluate
page.screenshot()  ──→   Page.captureScreenshot          ──→   返回 PNG 截图
```

这意味着程序的操作和**真实用户在浏览器里的操作完全等价**——浏览器无法区分是人工点击还是 CDP 指令。

### 关键对象模型

```java
// 1. Playwright — 顶层入口，管理浏览器生命周期
Playwright playwright = Playwright.create();

// 2. Browser — 一个浏览器进程（Chromium/Firefox/WebKit）
Browser browser = playwright.chromium().launch(
    new BrowserType.LaunchOptions()
        .setHeadless(false)   // false=有头模式(显示窗口), true=无头(后台运行)
        .setSlowMo(80));      // 每步操作延迟80ms，模拟人类操作

// 3. BrowserContext — 独立的浏览器会话（类似无痕窗口）
//    不同 Context 之间 Cookie/Storage 完全隔离
BrowserContext context = browser.newContext(
    new Browser.NewContextOptions()
        .setViewportSize(1920, 1080));

// 4. Page — 单个浏览器标签页
Page page = context.newPage();

// 5. FrameLocator — 进入 iframe 操作
FrameLocator loginFrame = page.frameLocator("iframe[id^='x-URS-iframe']");
loginFrame.locator("input[name='email']").fill("user@163.com");
```

### 调用链追踪

```
Mail163Test.main()
  ├─ Playwright.create()               // 启动 Playwright Server (Node.js)
  ├─ playwright.chromium().launch()     // Playwright Server 启动 Chromium 进程
  ├─ browser.newContext()               // 创建 BrowserContext (独立会话)
  ├─ context.newPage()                  // 打开新标签页
  ├─ page.navigate("https://mail.163.com/")
  │   └─ [CDP] Page.navigate → 浏览器加载 URL → 等待 networkidle
  ├─ page.frameLocator("iframe[id^='x-URS-iframe']")
  │   └─ [CDP] 遍历所有 frame，匹配选择器 → 返回 FrameLocator
  ├─ loginFrame.locator("input[name='email']").fill(USERNAME)
  │   └─ [CDP] 在目标 frame 中执行 querySelector → focus → 清空 → insertText
  ├─ loginFrame.locator("#dologin").click()
  │   └─ [CDP] querySelector → scrollIntoViewIfNeeded → dispatchMouseEvent
  ├─ page.fill("input.nui-editableAddr-ipt", recipient)
  │   └─ [CDP] 同上 fill 流程
  ├─ page.waitForSelector("span.nui-btn-text")
  │   └─ [CDP] 轮询 querySelector 直到匹配或超时
  ├─ page.click("//span[...contains(text(),'发送')]")
  │   └─ [CDP] XPath 查询 → dispatchMouseEvent → click
  └─ browser.close()
      └─ [CDP] Browser.close → 关闭浏览器进程 → 清理 Server
```

### 使用什么浏览器

| 项目 | 详情 |
|------|------|
| 浏览器 | **Chromium**（Google Chrome 的开源版本） |
| 版本 | Playwright 1.41.0 搭载的 Chromium 版本（约 121.x） |
| 安装路径 | Windows: `%LOCALAPPDATA%\ms-playwright\chromium-1134\` |
| 可执行文件 | `chrome.exe`（有头模式）/ `chrome-headless-shell.exe`（无头模式） |
| 选择原因 | Playwright 为每个版本精确匹配一个 Chromium 构建，保证 API 100% 兼容 |

> **注意**：Playwright 使用的是 **Chromium** 而不是你电脑上已安装的 Chrome。Chromium 由 Playwright 独立管理，不会影响你日常使用的 Chrome 浏览器，也不需要你提前安装 Chrome。

---

## 内网邮箱适配方案

当需要把同一套机制应用到内网邮箱时，本质上只需要修改两部分：**URL 和选择器**。底层 Playwright 机制完全复用。

### 核心思路

```
外网 163 邮箱                           内网邮箱（如中国银行企业邮箱）
─────────────────                      ─────────────────────────────
登录: mail.163.com           →         登录: http://内网IP/webmail/login
页面: React 动态渲染          →         页面: 可能是 JSP/Vue/jQuery
选择器: 按 DOM 结构定位       →         选择器: 需要重新分析 DOM
流程: 登录→写信→填写→发送     →         流程: 完全一样
机制: Playwright + CDP       →         机制: 完全一样（代码复用）
```

### 适配步骤（以 BankEmailPlaywrightService 为例）

`BankEmailPlaywrightService` 就是这个适配方案的**最佳示范**——它复用了完全相同的 Playwright 机制，只改了目标页面的选择器：

```java
// ========== 完全相同 ==========
try (Playwright playwright = Playwright.create()) {
    Browser browser = playwright.chromium().launch(
        new BrowserType.LaunchOptions().setHeadless(true).setSlowMo(50));
    BrowserContext context = browser.newContext(...);
    Page page = context.newPage();

    // ========== 只有这部分不同 ==========
    // 1. URL 改为内网地址
    page.navigate("http://内网IP/webmail/login/login.do");

    // 2. 选择器改为内网邮箱的 DOM 结构
    page.fill("#usernumber", emailUser);        // 内网邮箱的用户名输入框 ID
    page.fill("#password", emailPassword);       // 内网邮箱的密码输入框 ID
    page.click("#login_otp");                    // 内网邮箱的登录按钮 ID
    page.click("#btn_compose");                  // 内网邮箱的写信按钮 ID

    // 3. 正文编辑区可能也在 iframe 里
    FrameLocator composeFrame = page.frameLocator("#ifrm_compose_0");
    composeFrame.locator("#rib_input_1").fill(recipient);
    composeFrame.locator("#txtsubject").fill(subject);
    composeFrame.locator("#txtContent").fill(content);
    composeFrame.locator("#btnSend").click();

    // ========== 完全相同 ==========
    browser.close();
}
```

### 适配工厂模式（推荐）

如果你的系统需要对接多种邮箱（163、126、QQ、内网等），建议抽象一个接口：

```java
public interface EmailAutomationService {
    void sendEmail(EmailContext ctx);
}

// 163 邮箱实现
public class Mail163Automation implements EmailAutomationService { ... }

// 内网银行邮箱实现
public class BankEmailAutomation implements EmailAutomationService { ... }

// 统一调度
public void sendNotification(ParkingBook config, ParkingRecord record) {
    EmailAutomationService service = getService(config.getEmailType());
    service.sendEmail(buildContext(config, record));
}
```

这样每增加一种邮箱，只需要新增一个实现类，**写一个 main 方法调试选择器，调试通后接入即可**。

### 内网环境特别注意事项

| 注意事项 | 说明 |
|----------|------|
| **网络可达性** | 运行程序的服务器必须能访问内网邮箱的 URL（IP/域名） |
| **浏览器离线安装** | 内网服务器无法访问外网时，Chromium 需要离线安装（见下节） |
| **证书问题** | 内网自签名证书可能被 Chromium 拦截，需要配置 `ignoreHTTPSErrors` |
| **认证方式** | 内网可能有额外认证（AD 域、SSO、验证码等），需额外处理 |
| **headless 模式** | 服务器环境必须用 `setHeadless(true)` |

### 内网证书处理示例

```java
BrowserContext context = browser.newContext(
    new Browser.NewContextOptions()
        .setIgnoreHTTPSErrors(true)  // 忽略内网自签名证书
        .setViewportSize(1920, 1080));
```

---

## Playwright 离线安装部署

在**内网环境**或**无互联网访问**的服务器上部署时，需要提前准备好离线安装包。

### 需要的组件

```
┌──────────────────────────────────────────────────────┐
│ 1. Maven 依赖 (JAR)                                  │
│    com.microsoft.playwright:playwright:1.41.0        │
│    └─ 由 Maven 私服/Nexus 提供，或放到 lib 目录       │
├──────────────────────────────────────────────────────┤
│ 2. Playwright Server (Node.js 驱动包)                │
│    位置：%TEMP%\playwright-java-{随机ID}\package\     │
│    └─ 运行时由 playwright JAR 自动解压                │
│    └─ 已打包在 playwright-1.41.0.jar 内部             │
│    └─ 不需要额外安装 Node.js                          │
├──────────────────────────────────────────────────────┤
│ 3. Chromium 浏览器 (约 380 MB) ★ 关键               │
│    位置：%LOCALAPPDATA%\ms-playwright\chromium-1134\  │
│    └─ 正常情况下由 mvn exec:java -Pinstall-playwright │
│       自动从互联网下载                                 │
│    └─ 内网环境需手动离线安装                           │
└──────────────────────────────────────────────────────┘
```

### 离线安装 Chromium 的方法

#### 方案 A：从有网机器复制（推荐）

```powershell
# 第一步：在有网的开发机上，确保已安装 Playwright 浏览器
mvn exec:java -Pinstall-playwright

# 第二步：打包 Chromium 目录
# 源路径（Windows）:
%LOCALAPPDATA%\ms-playwright\chromium-1134\

# 第三步：复制到内网服务器的相同路径
# 目标路径（Windows Server）:
C:\Users\<用户名>\AppData\Local\ms-playwright\chromium-1134\

# 第四步：验证
dir %LOCALAPPDATA%\ms-playwright\chromium-1134\chrome.exe
```

#### 方案 B：使用 Playwright CLI 指定安装源

Playwright 支持通过环境变量指定浏览器下载源：

```powershell
# 设置国内镜像或内网文件服务器
$env:PLAYWRIGHT_DOWNLOAD_HOST = "http://内网文件服务器/playwright-browsers/"

# 然后执行安装
mvn exec:java -Pinstall-playwright
```

#### 方案 C：手动下载浏览器包

```
浏览器下载地址（官方）:
https://playwright.azureedge.net/builds/chromium/{版本号}/chromium-win64.zip

Chromium 1134 对应下载链接（Playwright 1.41.0）:
https://playwright.azureedge.net/builds/chromium/1134/chromium-win64.zip

下载后解压到:
Windows:  %LOCALAPPDATA%\ms-playwright\chromium-1134\
Linux:    ~/.cache/ms-playwright/chromium-1134/
Mac:      ~/Library/Caches/ms-playwright/chromium-1134/
```

### 离线部署完整清单

| 序号 | 组件 | 大小 | 获取方式 |
|------|------|------|----------|
| 1 | `playwright-1.41.0.jar` | ~2 MB | Maven 私服 或 `~/.m2/repository/` 复制 |
| 2 | Chromium 浏览器 | ~380 MB | 从有网机器复制 `%LOCALAPPDATA%\ms-playwright\chromium-1134\` |
| 3 | 操作系统字体（中文） | ~200 MB | 内网 Windows 通常已自带；Linux 需安装 `fonts-noto-cjk` |
| 4 | VC++ 运行时 | ~25 MB | Windows 通常已自带；Chromium 依赖 `msvcp140.dll` 等 |

### 内网服务器部署步骤

```powershell
# 1. 确保 Java 8+ 已安装
java -version

# 2. 复制 Maven 依赖（或通过内网 Nexus 拉取）
#    确保 ~/.m2/repository/com/microsoft/playwright/playwright/1.41.0/ 下
#    有 playwright-1.41.0.jar

# 3. 复制 Chromium 浏览器
#    将 chromium-1134 文件夹复制到服务器的 %LOCALAPPDATA%\ms-playwright\ 下
xcopy /E /I \\共享服务器\playwright-offline\chromium-1134 %LOCALAPPDATA%\ms-playwright\chromium-1134\

# 4. 验证
dir %LOCALAPPDATA%\ms-playwright\chromium-1134\chrome.exe
# 如果文件存在，Playwright 运行时会自动检测到已安装，不再尝试下载

# 5. 以 headless 模式运行测试
java -cp ... com.bocfintech.allstar.test.Mail163Test
```

### Playwright 浏览器缓存检测机制

```
Playwright.create() 启动时：
  ├─ 检查 %LOCALAPPDATA%\ms-playwright\chromium-1134\ 是否存在
  ├─ 存在 → 直接使用（离线模式）
  ├─ 不存在 → 尝试从 PLAYWRIGHT_DOWNLOAD_HOST 下载
  └─ 下载失败 → 抛出异常："Failed to install browsers"
```

这意味着只要 `chromium-1134` 文件夹存在且完整，Playwright 就**完全不需要联网**。

---

## 常见问题排查

### 浏览器启动失败

```
Caused by: com.microsoft.playwright.PlaywrightException:
Failed to launch chromium because executable doesn't exist at
C:\Users\...\ms-playwright\chromium-1134\chrome-win64\chrome.exe
```

**解决**: Chromium 未安装。外网环境执行 `mvn exec:java -Pinstall-playwright`，内网环境按上述离线方案手动复制。

### 中文乱码

```
原因: Linux 服务器缺少中文字体，导致页面中文显示为方块
解决: yum install -y fonts-noto-cjk  (CentOS)
      apt install -y fonts-noto-cjk  (Ubuntu)
```

### timeout 超时

```
- waiting for locator("...") to be visible
Timeout 10000ms exceeded.
```

**排查思路**（按顺序）：
1. 检查之前步骤是否成功（登录是否真的完成了？）
2. 保存截图 `page.screenshot(...)` 查看页面实际状态
3. 检查选择器是否在 iframe 内（最常见的遗漏）
4. 检查 URL 是否发生了跳转
5. 增加超时时间
