@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion
set "INPUT_FILE=export_vue_deps.json"
if not exist "%INPUT_FILE%" set "INPUT_FILE=package_vue_deps.json"
set "TEMP_BUILD_DIR=vue_temp_build"
set "EXPORT_BUNDLE=vue_sync_bundle"
set "PS_JSON_SCRIPT=%TEMP%\vue_pkg_%RANDOM%%RANDOM%.ps1"

if not exist "%INPUT_FILE%" (
    echo [ERROR] 找不到参数文件 "%INPUT_FILE%"
    pause
    exit /b
)

echo [1/4] 构建模拟环境...
if exist "%TEMP_BUILD_DIR%" rd /s /q "%TEMP_BUILD_DIR%"
if exist "%EXPORT_BUNDLE%" rd /s /q "%EXPORT_BUNDLE%"
mkdir "%TEMP_BUILD_DIR%"
mkdir "%EXPORT_BUNDLE%"

rem 生成临时的 package.json
> "%PS_JSON_SCRIPT%" echo $ErrorActionPreference = 'Stop'
>> "%PS_JSON_SCRIPT%" echo [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
>> "%PS_JSON_SCRIPT%" echo $inputPath = Resolve-Path '%INPUT_FILE%'
>> "%PS_JSON_SCRIPT%" echo $outputPath = '%TEMP_BUILD_DIR%\package.json'
>> "%PS_JSON_SCRIPT%" echo $raw = Get-Content -Path $inputPath -Raw -Encoding UTF8
>> "%PS_JSON_SCRIPT%" echo $text = $raw.Trim()
>> "%PS_JSON_SCRIPT%" echo if ([string]::IsNullOrWhiteSpace($text)) { throw 'Input file is empty.' }
>> "%PS_JSON_SCRIPT%" echo if (-not $text.StartsWith('{')) { throw 'Input must be a JSON object.' }
>> "%PS_JSON_SCRIPT%" echo $json = $text ^| ConvertFrom-Json -ErrorAction Stop
>> "%PS_JSON_SCRIPT%" echo if ($json.PSObject.Properties.Name -contains 'dependencies') { $deps = $json.dependencies } else { $deps = $json }
>> "%PS_JSON_SCRIPT%" echo $pkg = [ordered]@{ name = 'offline-deps-temp'; version = '1.0.0'; private = $true; dependencies = $deps }
>> "%PS_JSON_SCRIPT%" echo $pkg ^| ConvertTo-Json -Depth 100 ^| Set-Content -Path $outputPath -Encoding UTF8

if not exist "%PS_JSON_SCRIPT%" (
    echo [ERROR] 生成临时 JSON 处理脚本失败: "%PS_JSON_SCRIPT%"
    pause
    exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%PS_JSON_SCRIPT%"
if errorlevel 1 (
    echo [ERROR] 生成临时 package.json 失败，请检查 "%INPUT_FILE%" 是否为合法 JSON
    if exist "%PS_JSON_SCRIPT%" del /q "%PS_JSON_SCRIPT%"
    pause
    exit /b 1
)
if exist "%PS_JSON_SCRIPT%" del /q "%PS_JSON_SCRIPT%"

echo [2/4] 执行模拟安装 (提取全量物理依赖)...
pushd "%TEMP_BUILD_DIR%"
call npm install --no-audit --no-fund
if errorlevel 1 (
    popd
    echo [ERROR] npm install 执行失败
    pause
    exit /b 1
)
popd

echo [3/4] 物理提取 node_modules (解引用拷贝)...
robocopy "%TEMP_BUILD_DIR%\node_modules" "%EXPORT_BUNDLE%\node_modules" /E /R:3 /W:5 /MT:16 /XJD /XJF > nul
if errorlevel 8 (
    echo [ERROR] node_modules 拷贝失败，请检查权限或路径
    pause
    exit /b 1
)

echo [4/4] 生成迁入脚本并打包...
node -v > "%EXPORT_BUNDLE%\node_version_check.txt"
> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo @echo off
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo chcp 65001 ^> nul
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo setlocal enabledelayedexpansion
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo set "SCRIPT_DIR=%%~dp0"
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo set "SOURCE_NODE_MODULES=%%SCRIPT_DIR%%node_modules"
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo if not exist "%%SOURCE_NODE_MODULES%%" ^{
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo     echo [ERROR] 未找到源 node_modules：%%SOURCE_NODE_MODULES%%
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo     pause
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo     exit /b 1
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo ^}
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo set "MATCHED=0"
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo set "FAILED=0"
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo echo [INFO] 开始迁入依赖到 ..\web-*\node_modules ...
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo for /d %%%%i in ("%%SCRIPT_DIR%%..\web-*") do ^{
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo     set /a MATCHED+=1
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo     if not exist "%%%%~fi\node_modules" mkdir "%%%%~fi\node_modules"
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo     echo [INFO] 目标目录：%%%%~fi\node_modules
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo     robocopy "%%SOURCE_NODE_MODULES%%" "%%%%~fi\node_modules" /E /IS /IT /XJD /XJF /R:3 /W:2 ^> nul
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo     if errorlevel 8 ^{
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo         echo [ERROR] 拷贝失败：%%%%~fi\node_modules
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo         set /a FAILED+=1
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo     ^} else ^{
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo         echo [OK] 拷贝完成：%%%%~fi\node_modules
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo     ^}
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo ^}
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo if "%%MATCHED%%"=="0" ^{
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo     echo [ERROR] 未找到匹配目录：..\web-*
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo     echo [HINT] 请将压缩包解压到与 web-* 同级目录后再执行
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo     pause
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo     exit /b 1
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo ^}
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo if not "%%FAILED%%"=="0" ^{
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo     echo [ERROR] 共有 %%FAILED%% 个目录迁入失败
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo     pause
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo     exit /b 1
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo ^}
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo echo [SUCCESS] 所有匹配目录迁入完成，共 %%MATCHED%% 个
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo endlocal
>> "%EXPORT_BUNDLE%\import_vue_deps.bat" echo pause

powershell -NoProfile -ExecutionPolicy Bypass -Command "Compress-Archive -Path '%EXPORT_BUNDLE%\*' -DestinationPath 'vue_sync.zip' -Force"
if errorlevel 1 (
    echo [ERROR] 打包失败：vue_sync.zip
    pause
    exit /b 1
)

echo [INFO] Cleaning up temporary build and export directories...
rd /s /q "%TEMP_BUILD_DIR%"
rd /s /q "%EXPORT_BUNDLE%"

echo ==================================================
echo 完成！生成的压缩包为: vue_sync.zip
echo 请确保内网 Node 版本与外网一致:
node -v
echo ==================================================
pause