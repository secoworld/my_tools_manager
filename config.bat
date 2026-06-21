@echo off
REM ============================================================
REM   Tools Manager 项目配置文件
REM   修改此文件中的值即可调整项目运行配置
REM ============================================================

REM --- Java 环境 ---
REM JDK 安装路径（需要 JDK 17+）
set JAVA_HOME=D:\software\jdk-17.0.9

REM --- Maven 环境 ---
REM Maven 安装路径
set MAVEN_HOME=D:\software\apache-maven-3.9.16

REM --- 后端配置 ---
REM 后端服务端口
set BACKEND_PORT=8085

REM --- 前端配置 ---
REM 前端开发服务器端口
set FRONTEND_PORT=5173

REM --- 启动选项 ---
REM 启动后是否自动打开浏览器（true / false）
set AUTO_OPEN_BROWSER=true

REM 浏览器打开的地址（一般不需要修改）
set BROWSER_URL=http://localhost:%FRONTEND_PORT%
