@echo off
chcp 65001 >nul 2>&1
title 🔨 构建 Aicode Server → 内网 Windows 部署
setlocal

echo ==================================================
echo   🔨 构建 Aicode 后端 Server 内网版本
echo ==================================================
echo.

:: ========== 📌 构建环境参数（按需修改，类似 IDEA 的 Maven 配置） ==========
:: Maven 安装目录 (留空则使用 PATH 中的 mvn)
set "MAVEN_HOME="
:: settings.xml 路径 (留空则使用默认 %USERPROFILE%\.m2\settings.xml)
set "MAVEN_CONF="
:: 本地仓库目录 (留空则使用默认 %USERPROFILE%\.m2\repository)
set "MAVEN_REPO="
:: ====================================================

:: ========== 📌 内网部署参数（按需修改） ==========
set "DB_HOST=22.188.9.144"
set "DB_PORT=3306"
set "DB_USER=root"
set "DB_PASSWORD=Star002!"
set "DB_NAME=stack_db"
set "FILE_STORAGE_PATH=D:\data\aicode-v1"
set "SERVER_PORT=8092"
set "DEPLOY_DIR=D:\app\aicode-server"
:: ====================================================

:: 切到项目根目录
set "PROJECT_ROOT=%~dp0.."
cd /d "%PROJECT_ROOT%"
if %errorlevel% neq 0 (
    echo ❌ 无法进入项目根目录！
    pause
    exit /b 1
)
echo 📁 项目根目录: %cd%
echo.

:: ========== Step 1: 校验项目文件 ==========
echo [1/7] 📋 校验项目文件...
if not exist "java-aicode-server\pom.xml" (
    echo ❌ 未找到 java-aicode-server\pom.xml！
    pause
    exit /b 1
)
echo ✅ 项目文件就绪
echo.

echo 📌 内网部署参数:
echo    DB: %DB_HOST%:%DB_PORT%/%DB_NAME%
echo    File Storage: %FILE_STORAGE_PATH%
echo    Server Port: %SERVER_PORT%
echo    Deploy Dir:  %DEPLOY_DIR%
echo.

:: ========== Step 2: 解析 Maven 路径与参数 ==========
echo [2/7] ⚡ 解析 Maven 路径与参数...
echo.

:: 类似 IDEA 的 Maven 配置：优先使用 MAVEN_HOME，其次查找 PATH
set "MVN_CMD=mvn"
if not "%MAVEN_HOME%"=="" (
    set "MVN_CMD=%MAVEN_HOME%\bin\mvn.cmd"
    if not exist "%MVN_CMD%" (
        echo ❌ 指定的 MAVEN_HOME 下未找到 mvn.cmd: %MVN_CMD%
        echo   请检查 MAVEN_HOME 变量（当前值: %MAVEN_HOME%）
        cd /d "%PROJECT_ROOT%"
        pause
        exit /b 1
    )
    echo   ✅ Maven Home:  %MAVEN_HOME%
) else (
    echo   ℹ️  Maven Home:  使用 PATH 中的 mvn（未设置 MAVEN_HOME）
)

:: 构建 Maven 额外参数（settings.xml + local repository）
set "MVN_EXTRA_ARGS="
if not "%MAVEN_CONF%"=="" (
    if not exist "%MAVEN_CONF%" (
        echo ❌ 指定的 settings.xml 不存在: %MAVEN_CONF%
        cd /d "%PROJECT_ROOT%"
        pause
        exit /b 1
    )
    set "MVN_EXTRA_ARGS=%MVN_EXTRA_ARGS% -s "%MAVEN_CONF%""
    echo   ✅ User settings: %MAVEN_CONF%
) else (
    echo   ℹ️  User settings: 使用默认 %%USERPROFILE%%\.m2\settings.xml
)
if not "%MAVEN_REPO%"=="" (
    if not exist "%MAVEN_REPO%" mkdir "%MAVEN_REPO%" >nul 2>&1
    set "MVN_EXTRA_ARGS=%MVN_EXTRA_ARGS% -Dmaven.repo.local="%MAVEN_REPO%""
    echo   ✅ Local repo:    %MAVEN_REPO%
) else (
    echo   ℹ️  Local repo:    使用默认 %%USERPROFILE%%\.m2\repository
)

:: ========== Step 3: Maven 编译打包 ==========
echo.
echo [3/7] ⚡ Maven 编译打包（跳过测试）...
echo.

cd /d "%PROJECT_ROOT%\java-aicode-server"

echo   执行: %MVN_CMD% clean package -Dmaven.test.skip=true %MVN_EXTRA_ARGS%
echo.
call %MVN_CMD% clean package -Dmaven.test.skip=true %MVN_EXTRA_ARGS%
set BUILD_RESULT=%errorlevel%

if %BUILD_RESULT% neq 0 (
    echo.
    echo ❌ Maven 编译失败！请检查 Java 8 环境和 Maven 配置。
    cd /d "%PROJECT_ROOT%"
    pause
    exit /b 1
)
echo.
echo ✅ Maven 编译完成
echo.

:: ========== Step 4: 复制 JAR 到输出目录 ==========
echo [4/7] 📦 复制 JAR 到 bin\aicode-server\ ...

set "OUT_DIR=%PROJECT_ROOT%\bin\aicode-server"
set "JAR_SRC=%PROJECT_ROOT%\java-aicode-server\target\aicode-1.0-SNAPSHOT.jar"
set "JAR_DST=%OUT_DIR%\aicode-server.jar"

if not exist "%JAR_SRC%" (
    echo ❌ 未找到编译产物 %JAR_SRC%
    cd /d "%PROJECT_ROOT%"
    pause
    exit /b 1
)

:: 清理并创建输出目录
if exist "%OUT_DIR%" rmdir /s /q "%OUT_DIR%"
mkdir "%OUT_DIR%" >nul 2>&1

copy /y "%JAR_SRC%" "%JAR_DST%" >nul
echo   %JAR_SRC%
echo   → %JAR_DST%
echo.

:: ========== Step 5: 生成 WinSW 服务定义文件 ==========
echo [5/7] ⚙️  生成 WinSW 服务定义文件...
echo   工作目录: %DEPLOY_DIR%

:: ---- AicodeServer.xml (workingdirectory 指向生产部署目录) ----
(
echo ^<service^>
echo   ^<id^>aicode-server^</id^>
echo   ^<name^>Aicode 全栈平台后端服务^</name^>
echo   ^<description^>Aicode Spring Boot 后端服务 (Java 8^)</description^>
echo   ^<executable^>java^</executable^>
echo   ^<arguments^>-Xms256m -Xmx512m -jar aicode-server.jar^</arguments^>
echo   ^<workingdirectory^>%DEPLOY_DIR:\=\\%^</workingdirectory^>
echo   ^<env name="DB_HOST" value="%DB_HOST%"/^>
echo   ^<env name="DB_PORT" value="%DB_PORT%"/^>
echo   ^<env name="DB_USER" value="%DB_USER%"/^>
echo   ^<env name="DB_PASSWORD" value="%DB_PASSWORD%"/^>
echo   ^<env name="FILE_STORAGE_PATH" value="%FILE_STORAGE_PATH%"/^>
echo   ^<env name="SERVER_PORT" value="%SERVER_PORT%"/^>
echo   ^<log mode="roll-by-size"^>
echo     ^<sizeThreshold^>10240^</sizeThreshold^>
echo     ^<keepFiles^>8^</keepFiles^>
echo   ^</log^>
echo ^</service^>
) > "%OUT_DIR%\AicodeServer.xml"

echo   ✅ AicodeServer.xml 已生成 (workingdirectory=%DEPLOY_DIR%)
echo.

:: ========== Step 6: 创建启停辅助脚本 ==========
echo [6/7] 📝 创建启停辅助脚本...
echo   生成的脚本请在部署目录 (%DEPLOY_DIR%) 中运行

:: ---- startServer.bat (放到部署目录运行) ----
(
echo @echo off
echo chcp 65001 ^>nul 2^>^&1
echo echo ==========================================
echo echo   启动 Aicode Server 服务
echo echo   工作目录: %DEPLOY_DIR%
echo echo ==========================================
echo echo.
echo.
echo :: 切到部署目录
echo cd /d "%DEPLOY_DIR%"
echo.
echo :: 需要以管理员身份运行
echo net session ^>nul 2^>^&1
echo if ^%%errorlevel^%% neq 0 ^(
echo     echo ❌ 请以管理员身份运行此脚本！
echo     pause
echo     exit /b 1
echo ^)
echo.
echo echo [1/3] 安装服务...
echo AicodeServer.exe install
echo if ^%%errorlevel^%% neq 0 ^(
echo     echo 提示: 服务可能已安装，尝试直接启动...
echo ^)
echo.
echo echo [2/3] 启动服务...
echo AicodeServer.exe start
echo if ^%%errorlevel^%% neq 0 ^(
echo     echo ❌ 服务启动失败！
echo     pause
echo     exit /b 1
echo ^)
echo.
echo echo [3/3] 校验服务状态...
echo AicodeServer.exe status
echo.
echo echo ==========================================
echo echo   ✅ Aicode Server 服务已启动
echo echo   访问: http://localhost:%SERVER_PORT%/api/health
echo echo ==========================================
echo pause
) > "%OUT_DIR%\startServer.bat"

:: ---- stopServer.bat (放到部署目录运行) ----
(
echo @echo off
echo chcp 65001 ^>nul 2^>^&1
echo echo ==========================================
echo echo   停止 Aicode Server 服务
echo echo   工作目录: %DEPLOY_DIR%
echo echo ==========================================
echo echo.
echo.
echo :: 切到部署目录
echo cd /d "%DEPLOY_DIR%"
echo.
echo net session ^>nul 2^>^&1
echo if ^%%errorlevel^%% neq 0 ^(
echo     echo ❌ 请以管理员身份运行此脚本！
echo     pause
echo     exit /b 1
echo ^)
echo.
echo echo 正在停止服务...
echo AicodeServer.exe stop
echo echo.
echo echo 正在卸载服务...
echo AicodeServer.exe uninstall
echo echo.
echo echo ✅ 服务已停止并卸载
echo pause
) > "%OUT_DIR%\stopServer.bat"

echo   ✅ startServer.bat / stopServer.bat 已生成
echo.

:: ========== Step 7: 复制 WinSW 可执行文件 ==========
echo [7/7] ⚙️  复制 WinSW 可执行文件...
set "WINSW_SRC=%PROJECT_ROOT%\deploy\WinSW-x64.exe"
set "WINSW_DST=%OUT_DIR%\AicodeServer.exe"

if exist "%WINSW_SRC%" (
    copy /y "%WINSW_SRC%" "%WINSW_DST%" >nul
    echo   deploy\WinSW-x64.exe → %OUT_DIR%\AicodeServer.exe
    echo   ✅ WinSW 已自动部署
) else (
    echo   ⚠️  未找到 deploy\WinSW-x64.exe，请从以下地址下载后放入 deploy 目录:
    echo      https://github.com/winsw/winsw/releases
)
echo.

:: ========== 输出完成信息 ==========
echo ==================================================
echo   🏁  构建完成！内网后端 Server 版本
echo ==================================================
echo.
echo   📌 构建产物目录: %OUT_DIR%\
echo         ├── AicodeServer.exe        (WinSW 可执行文件^)
echo         ├── aicode-server.jar       (Spring Boot JAR^)
echo         ├── AicodeServer.xml        (WinSW 服务定义^)
echo         ├── startServer.bat         (安装 + 启动^)
echo         └── stopServer.bat          (停止 + 卸载^)
echo.
echo   📌 部署步骤:
echo       1. 将 bin\aicode-server\ 下所有文件复制到 %DEPLOY_DIR%\
echo       2. 以管理员身份运行 %DEPLOY_DIR%\startServer.bat
echo       3. 浏览器验证: http://localhost:%SERVER_PORT%/api/health
echo.
echo   💡 修改参数: 编辑本 bat 头部 set 变量，重新构建即可
echo   💡 手动运行: java -jar aicode-server.jar  (开发调试用^)
echo   💡 WinSW 工作目录已配置为: %DEPLOY_DIR%
echo ==================================================
echo.

cd /d "%PROJECT_ROOT%"
pause
exit /b 0
