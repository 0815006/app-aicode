:: 部署后台到腾讯云服务器
@echo off
chcp 65001 > nul
set SERVER_IP=129.211.9.238
set SERVER_DIR=/var/www/app-aicode
set PORT=8092
set CONTAINER_NAME=app-aicode
:: 对应本地 target 目录生成的真实 jar 包（无 profile 后缀时默认 aicode-1.0-SNAPSHOT.jar）
set JAR_NAME=aicode-1.0-SNAPSHOT.jar

echo ===================================================
echo [1/4] 进入后端源码目录，开始 Maven 编译打包...
echo ===================================================
cd /d "%~dp0..\java-aicode-server"
call mvn clean package -Dmaven.test.skip=true
set BUILD_RESULT=%errorlevel%
cd /d "%~dp0."

if %BUILD_RESULT% neq 0 (
    echo [错误] Maven 打包失败，终止部署！
    pause
    exit /b
)

echo ===================================================
echo [2/4] 正在将编译产物与 Dockerfile 上传至腾讯云...
echo ===================================================
ssh root@%SERVER_IP% "mkdir -p %SERVER_DIR%"
:: 本地传过去的是长名字 jar，传到服务器后改名为 app.jar
scp "%~dp0..\java-aicode-server\target\%JAR_NAME%" root@%SERVER_IP%:%SERVER_DIR%/app.jar
scp "%~dp0cloud-dockerfile" root@%SERVER_IP%:%SERVER_DIR%/Dockerfile

echo ===================================================
echo [3/4] 创建数据持久化目录...
echo ===================================================
ssh root@%SERVER_IP% "mkdir -p /data/aicode-v1"

echo ===================================================
echo [4/4] 远程连接服务器，通过环境变量注入参数并重启容器...
echo ===================================================
:: -p 8092:8092: 将宿主机 8092 映射到容器内 8092
:: --add-host=host.docker.internal:host-gateway: 让容器通过该域名访问宿主机的 MySQL 3306
:: DATA_ROOT: 持久化目录挂载
:: SERVER_PORT: 与代码保持一致
ssh root@%SERVER_IP% "cd %SERVER_DIR% && docker build -t %CONTAINER_NAME% . && docker stop %CONTAINER_NAME% 2>/dev/null || true && docker rm %CONTAINER_NAME% 2>/dev/null || true && docker run -d --name %CONTAINER_NAME% -p %PORT%:8092 --restart always --add-host=host.docker.internal:host-gateway -v /data/aicode-v1:/data/aicode-v1 -e DB_HOST=host.docker.internal -e DB_PORT=3306 -e DB_NAME=stack_db -e DB_USER=root -e DB_PASSWORD=root -e DATA_ROOT=/data/aicode-v1 -e SERVER_PORT=8092 %CONTAINER_NAME%"

echo ===================================================
echo 恭喜！项目 %CONTAINER_NAME% 后端部署成功！
echo 腾讯云外部 API 端口: %PORT%
echo 数据库连接: 宿主机 MySQL 3306 (stack_db)
echo ===================================================
pause
