@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion
set "INPUT_FILE=export_maven_deps.xml"
set "TEMP_PACK_DIR=maven_sync_bundle"
set "REPO_EXPORT_DIR=%TEMP_PACK_DIR%\repository"

if not exist %INPUT_FILE% (
    echo [ERROR] 找不到参数文件 %INPUT_FILE%
    pause
    exit /b
)

echo [1/4] 清理旧目录...
if exist %TEMP_PACK_DIR% rd /s /q %TEMP_PACK_DIR%
mkdir "%REPO_EXPORT_DIR%"

echo [2/4] 解析依赖并下载 (依赖于 Maven 环境)...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$xml = [xml]('<root>' + (Get-Content %INPUT_FILE% -Raw -Encoding UTF8) + '</root>');" ^
    "$localRepo = (mvn help:evaluate -Dexpression=settings.localRepository -q -DforceStdout).Trim();" ^
    "if(!$localRepo){ $localRepo = \"$env:USERPROFILE\.m2\repository\" };" ^
    "$xml.root.dependency | ForEach-Object {" ^
        "$g = $_.groupId.Trim(); $a = $_.artifactId.Trim(); $v = $_.version.Trim();" ^
        "$gav = \"${g}:${a}:${v}\";" ^
        "Write-Host \"正在处理: $gav\" -ForegroundColor Cyan;" ^
        "mvn dependency:get -Dartifact=$gav -Dtransitive=true;" ^
        "$relPath = $g.Replace('.', '\') + '\' + $a + '\' + $v;" ^
        "$fullPath = Join-Path $localRepo $relPath;" ^
        "$targetPath = Join-Path '%REPO_EXPORT_DIR%' $relPath;" ^
        "if(Test-Path $fullPath){ " ^
            "New-Item -ItemType Directory -Force -Path $targetPath | Out-Null;" ^
            "Copy-Item -Path \"$fullPath\*\" -Destination $targetPath -Recurse -Force;" ^
        "} else { Write-Host \"找不到文件路径: $fullPath\" -ForegroundColor Red; }" ^
    "}"

echo [3/4] 清理联网标识文件 (_remote.repositories)...
powershell -NoProfile -ExecutionPolicy Bypass -Command "Get-ChildItem -Path '%REPO_EXPORT_DIR%' -Filter '*_remote.repositories' -Recurse | Remove-Item -Force"
powershell -NoProfile -ExecutionPolicy Bypass -Command "Get-ChildItem -Path '%REPO_EXPORT_DIR%' -Filter '*_maven.repositories' -Recurse | Remove-Item -Force"

echo [4/4] 生成内网迁入脚本并打包...
(
echo @echo off
echo chcp 65001 ^> nul
echo echo [INFO] 正在迁入依赖至内网仓库...
echo xcopy /s /e /y "repository\*" "%%USERPROFILE%%\.m2\repository\"
echo echo [SUCCESS] 迁入完成！
echo pause
) > "%TEMP_PACK_DIR%\import_maven_deps.bat"

powershell -NoProfile -ExecutionPolicy Bypass -Command "Compress-Archive -Path '%TEMP_PACK_DIR%\*' -DestinationPath 'maven_sync.zip' -Force"

:: 【新增：自动化清理】
echo [INFO] 正在清理临时工作目录...
rd /s /q "%TEMP_PACK_DIR%"

echo ==================================================
echo 完成！生成的压缩包为: maven_sync.zip
echo ==================================================
pause