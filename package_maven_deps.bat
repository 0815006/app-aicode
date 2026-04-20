@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion
set "INPUT_FILE=export_maven_deps.xml"
if not exist "%INPUT_FILE%" set "INPUT_FILE=package_maven_deps.xml"
set "TEMP_PACK_DIR=maven_sync_bundle"
set "REPO_EXPORT_DIR=%TEMP_PACK_DIR%\repository"
set "PS_SCRIPT=%TEMP%\maven_pack_%RANDOM%%RANDOM%.ps1"

if not exist "%INPUT_FILE%" (
    echo [ERROR] 找不到参数文件 "%INPUT_FILE%"
    echo [HINT] 请提供 export_maven_deps.xml 或 package_maven_deps.xml
    pause
    exit /b
)

echo [1/4] 清理旧目录...
if exist "%TEMP_PACK_DIR%" rd /s /q "%TEMP_PACK_DIR%"
mkdir "%REPO_EXPORT_DIR%"

echo [2/4] 解析依赖并下载 (依赖于 Maven 环境)...
> "%PS_SCRIPT%" echo $ErrorActionPreference = 'Stop'
>> "%PS_SCRIPT%" echo [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
>> "%PS_SCRIPT%" echo $OutputEncoding = [System.Text.Encoding]::UTF8
>> "%PS_SCRIPT%" echo $inputFile = Resolve-Path '%INPUT_FILE%'
>> "%PS_SCRIPT%" echo $repoExportDir = Resolve-Path '%REPO_EXPORT_DIR%'
>> "%PS_SCRIPT%" echo $xmlText = Get-Content -Path $inputFile -Raw -Encoding UTF8
>> "%PS_SCRIPT%" echo $wrappedXml = ([char]60 + 'root' + [char]62) + $xmlText + ([char]60 + '/root' + [char]62)
>> "%PS_SCRIPT%" echo $xml = New-Object System.Xml.XmlDocument
>> "%PS_SCRIPT%" echo $xml.LoadXml($wrappedXml)
>> "%PS_SCRIPT%" echo if (-not $xml.root.dependency) { throw 'No dependency nodes found. Please check XML fragment format.' }
>> "%PS_SCRIPT%" echo $localRepo = if ($env:MAVEN_REPO_LOCAL) { $env:MAVEN_REPO_LOCAL } else { Join-Path $env:USERPROFILE '.m2\repository' }
>> "%PS_SCRIPT%" echo if (-not (Test-Path $localRepo)) { New-Item -ItemType Directory -Path $localRepo -Force ^| Out-Null }
>> "%PS_SCRIPT%" echo Write-Host ("Using local repository: {0}" -f $localRepo) -ForegroundColor DarkCyan
>> "%PS_SCRIPT%" echo foreach ($dep in $xml.root.dependency) {
>> "%PS_SCRIPT%" echo     $g = [string]$dep.groupId
>> "%PS_SCRIPT%" echo     $a = [string]$dep.artifactId
>> "%PS_SCRIPT%" echo     $v = [string]$dep.version
>> "%PS_SCRIPT%" echo     if ([string]::IsNullOrWhiteSpace($g) -or [string]::IsNullOrWhiteSpace($a) -or [string]::IsNullOrWhiteSpace($v)) { continue }
>> "%PS_SCRIPT%" echo     $g = $g.Trim(); $a = $a.Trim(); $v = $v.Trim()
>> "%PS_SCRIPT%" echo     $gav = "$g`:$a`:$v"
>> "%PS_SCRIPT%" echo     Write-Host ("Processing: {0}" -f $gav) -ForegroundColor Cyan
>> "%PS_SCRIPT%" echo     mvn dependency:get "-Dartifact=$gav" -Dtransitive=true
>> "%PS_SCRIPT%" echo     if ($LASTEXITCODE -ne 0) { throw "Maven download failed: $gav" }
>> "%PS_SCRIPT%" echo     $relPath = [IO.Path]::Combine($g.Replace('.', '\'), $a, $v)
>> "%PS_SCRIPT%" echo     $sourcePath = Join-Path $localRepo $relPath
>> "%PS_SCRIPT%" echo     $targetPath = Join-Path $repoExportDir $relPath
>> "%PS_SCRIPT%" echo     if (Test-Path $sourcePath) {
>> "%PS_SCRIPT%" echo         New-Item -ItemType Directory -Force -Path $targetPath ^| Out-Null
>> "%PS_SCRIPT%" echo         Copy-Item -Path (Join-Path $sourcePath '*') -Destination $targetPath -Recurse -Force
>> "%PS_SCRIPT%" echo     } else {
>> "%PS_SCRIPT%" echo         Write-Host ("Source path not found: {0}" -f $sourcePath) -ForegroundColor Yellow
>> "%PS_SCRIPT%" echo     }
>> "%PS_SCRIPT%" echo }

if not exist "%PS_SCRIPT%" (
    echo [ERROR] 生成临时 PowerShell 脚本失败: "%PS_SCRIPT%"
    pause
    exit /b 1
)
for %%I in ("%PS_SCRIPT%") do if %%~zI LSS 20 (
    echo [ERROR] 临时 PowerShell 脚本内容异常，已中止
    del /q "%PS_SCRIPT%" > nul 2>&1
    pause
    exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%PS_SCRIPT%"
if errorlevel 1 (
    echo [ERROR] Maven 依赖解析或复制失败，请检查上方输出
    if exist "%PS_SCRIPT%" del /q "%PS_SCRIPT%"
    pause
    exit /b 1
)
if exist "%PS_SCRIPT%" del /q "%PS_SCRIPT%"

echo [3/4] 清理联网标识文件 (_remote.repositories / _maven.repositories)...
powershell -NoProfile -ExecutionPolicy Bypass -Command "Get-ChildItem -Path '%REPO_EXPORT_DIR%' -Recurse -File | Where-Object { $_.Name -in @('_remote.repositories','_maven.repositories') } | Remove-Item -Force -ErrorAction SilentlyContinue"

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
if errorlevel 1 (
    echo [ERROR] 打包失败：maven_sync.zip
    pause
    exit /b 1
)

echo [INFO] 正在清理临时工作目录...
rd /s /q "%TEMP_PACK_DIR%"

echo ==================================================
echo 完成！生成的压缩包为: maven_sync.zip
echo ==================================================
pause