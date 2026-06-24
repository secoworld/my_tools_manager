@echo off
chcp 65001 >nul
REM ============================================
REM 密码重置脚本 (Windows)
REM 用法：
REM   普通模式：reset-password.bat
REM   Docker 模式：reset-password.bat --docker 容器名
REM ============================================

echo ========================================
echo        密码重置脚本
echo ========================================
echo.

REM 判断模式
if "%1"=="--docker" (
    if "%2"=="" (
        echo [错误] Docker 模式需要指定容器名或ID
        echo 用法: %0 --docker 容器名或ID
        pause
        exit /b 1
    )
    echo Docker 模式：容器 = %2
    echo.

    REM 检查容器是否在运行
    docker ps --format "{{.Names}}" | findstr /b /c:"%2" >nul
    if errorlevel 1 (
        echo [错误] 容器 '%2' 未运行，请先启动容器
        pause
        exit /b 1
    )

    echo 正在启动密码重置工具...
    echo.
    docker exec -it %2 java -jar app.jar --reset-password
    goto :end
)

REM 普通模式
set "SCRIPT_DIR=%~dp0"
set "JAR_FILE="

REM 查找 jar 文件
if exist "%CD%\tools-manager.jar" (
    set "JAR_FILE=%CD%\tools-manager.jar"
) else if exist "%CD%\tools-manager-0.0.1-SNAPSHOT.jar" (
    set "JAR_FILE=%CD%\tools-manager-0.0.1-SNAPSHOT.jar"
) else if exist "%SCRIPT_DIR%backend\target\tools-manager-0.0.1-SNAPSHOT.jar" (
    set "JAR_FILE=%SCRIPT_DIR%backend\target\tools-manager-0.0.1-SNAPSHOT.jar"
) else if exist "%SCRIPT_DIR%backend\target\tools-manager.jar" (
    set "JAR_FILE=%SCRIPT_DIR%backend\target\tools-manager.jar"
) else if exist "%SCRIPT_DIR%tools-manager.jar" (
    set "JAR_FILE=%SCRIPT_DIR%tools-manager.jar"
)

if "%JAR_FILE%"=="" (
    echo [错误] 未找到 tools-manager.jar 文件
    echo.
    echo 请将此脚本放在以下任一位置：
    echo   1. jar 文件同目录
    echo   2. 项目根目录（会查找 backend\target\ 下的 jar）
    echo.
    echo 或手动执行：
    echo   java -jar ^<path\to\tools-manager.jar^> --reset-password
    pause
    exit /b 1
)

echo 使用 JAR 文件: %JAR_FILE%
echo.

REM 检查 java 是否可用
where java >nul 2>nul
if errorlevel 1 (
    echo [错误] 未找到 java 命令，请确保 Java 17+ 已安装
    pause
    exit /b 1
)

echo 正在启动密码重置工具...
echo.
java -jar "%JAR_FILE%" --reset-password

:end
