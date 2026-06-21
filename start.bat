@echo off
chcp 65001 >nul
title Tools Manager 一键启动

REM ============================================================
REM   Tools Manager 一键启动脚本
REM   双击运行即可启动前后端服务
REM ============================================================

REM 获取脚本所在目录
set PROJECT_ROOT=%~dp0
cd /d "%PROJECT_ROOT%"

REM 加载配置文件
if not exist "config.bat" (
    echo [错误] 未找到 config.bat 配置文件
    pause
    exit /b 1
)
call config.bat

REM 设置 PATH
set PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%

echo.
echo ========================================
echo    Tools Manager 一键启动
echo ========================================
echo.

REM ---- 环境检查 ----
echo [1/5] 检查运行环境...

echo       - Java...
java -version >nul 2>&1
if errorlevel 1 (
    echo       [失败] 未找到 Java，请检查 config.bat 中的 JAVA_HOME
    echo       当前配置: JAVA_HOME=%JAVA_HOME%
    pause
    exit /b 1
)

echo       - Maven...
mvn -version >nul 2>&1
if errorlevel 1 (
    echo       [失败] 未找到 Maven，请检查 config.bat 中的 MAVEN_HOME
    echo       当前配置: MAVEN_HOME=%MAVEN_HOME%
    pause
    exit /b 1
)

echo       - Node.js...
node -v >nul 2>&1
if errorlevel 1 (
    echo       [失败] 未找到 Node.js，请安装 Node.js 16+
    pause
    exit /b 1
)

echo       [OK] 环境检查通过
echo.

REM ---- 检查前端依赖 ----
echo [2/5] 检查前端依赖...
if not exist "%PROJECT_ROOT%frontend\node_modules" (
    echo       未安装依赖，正在安装...
    cd /d "%PROJECT_ROOT%frontend"
    call npm install
    if errorlevel 1 (
        echo       [失败] 前端依赖安装失败
        pause
        exit /b 1
    )
    cd /d "%PROJECT_ROOT%"
    echo       [OK] 依赖安装完成
) else (
    echo       [OK] 依赖已安装
)
echo.

REM ---- 启动后端 ----
echo [3/5] 启动后端服务 (端口 %BACKEND_PORT%)...
start "Tools Manager - Backend" cmd /k "cd /d "%PROJECT_ROOT%backend" && set PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH% && mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=%BACKEND_PORT%"

echo       后端正在启动，请等待控制台出现 "Started ToolManagerApplication"
echo.

REM ---- 等待后端就绪 ----
echo [4/5] 等待后端服务就绪...
set WAIT_COUNT=0
:WAIT_BACKEND
timeout /t 2 /nobreak >nul
set /a WAIT_COUNT+=2
curl -s -o nul -w "%%{http_code}" http://localhost:%BACKEND_PORT%/api/tools >nul 2>&1
if errorlevel 1 (
    if %WAIT_COUNT% lss 30 (
        goto WAIT_BACKEND
    )
    echo       [警告] 后端可能尚未完全启动，继续启动前端...
) else (
    echo       [OK] 后端已就绪 (等待 %WAIT_COUNT% 秒)
)
echo.

REM ---- 启动前端 ----
echo [5/5] 启动前端服务 (端口 %FRONTEND_PORT%)...
start "Tools Manager - Frontend" cmd /k "cd /d "%PROJECT_ROOT%frontend" && set BACKEND_PORT=%BACKEND_PORT% && set FRONTEND_PORT=%FRONTEND_PORT% && npm run dev"

echo       前端正在启动...
timeout /t 3 /nobreak >nul

REM ---- 打开浏览器 ----
if "%AUTO_OPEN_BROWSER%"=="true" (
    echo.
    echo [完成] 正在打开浏览器...
    start "" "%BROWSER_URL%"
)

echo.
echo ========================================
echo    启动完成！
echo ========================================
echo.
echo    后端地址: http://localhost:%BACKEND_PORT%
echo    前端地址: http://localhost:%FRONTEND_PORT%
echo    H2 控制台: http://localhost:%BACKEND_PORT%/h2-console
echo.
echo    关闭对应终端窗口即可停止对应服务
echo ========================================
echo.
pause
