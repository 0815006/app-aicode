@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion
set "INPUT_FILE=export_vue_deps.json"
set "TEMP_BUILD_DIR=vue_temp_build"
set "EXPORT_BUNDLE=vue_sync_bundle"

if not exist %INPUT_FILE% (
    echo [ERROR] 找不到参数文件 %INPUT_FILE%
    pause
    exit /b
)

echo [1/4] 构建模拟环境...
if exist %TEMP_BUILD_DIR% rd /s /q %TEMP_BUILD_DIR%
if exist %EXPORT_BUNDLE% rd /s /q %EXPORT_BUNDLE%
mkdir "%TEMP_BUILD_DIR%"
mkdir "%EXPORT_BUNDLE%"

:: 生成临时的 package.json
echo { "dependencies": > %TEMP_BUILD_DIR%\package.json
type %INPUT_FILE% >> %TEMP_BUILD_DIR%\package.json
echo } >> %TEMP_BUILD_DIR%\package.json

echo [2/4] 执行模拟安装 (提取全量物理依赖)...
cd /d "%TEMP_BUILD_DIR%"
call npm install --no-audit --no-fund
cd /d ..

echo [3/4] 物理提取 node_modules (解引用拷贝)...
robocopy "%TEMP_BUILD_DIR%\node_modules" "%EXPORT_BUNDLE%\node_modules" /E /R:3 /W:5 /MT:16 /XJD /XJF > nul

echo [4/4] 生成迁入脚本并打包...
node -v > "%EXPORT_BUNDLE%\node_version_check.txt"
(
echo @echo off
echo chcp 65001 ^> nul
echo echo [INFO] 正在将依赖物理覆盖至当前项目 node_modules...
echo robocopy "node_modules" ".\node_modules" /E /IS /IT /XJD /XJF
echo echo [SUCCESS] 补全完成！
echo pause
) > "%EXPORT_BUNDLE%\import_vue_deps.bat"

powershell -NoProfile -ExecutionPolicy Bypass -Command "Compress-Archive -Path '%EXPORT_BUNDLE%\*' -DestinationPath 'vue_sync.zip' -Force"

:: 【新增：自动化清理】
echo [INFO] 正在清理临时安装环境及打包目录...
rd /s /q "%TEMP_BUILD_DIR%"
rd /s /q "%EXPORT_BUNDLE%"

echo ==================================================
echo 完成！生成的压缩包为: vue_sync.zip
echo 请确保内网 Node 版本与外网一致:
type "%EXPORT_BUNDLE%\node_version_check.txt"
echo ==================================================
pause