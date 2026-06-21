#!/bin/bash
# ============================================================
#   Tools Manager 项目配置文件 (Linux)
#   修改此文件中的值即可调整项目运行配置
# ============================================================

# --- Java 环境 ---
# JDK 安装路径（需要 JDK 17+）
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk

# --- Maven 环境 ---
# Maven 安装路径（留空则使用系统 PATH 中的 mvn）
export MAVEN_HOME=""

# --- 后端配置 ---
# 后端服务端口
export BACKEND_PORT=8085

# --- 前端配置 ---
# 前端开发服务器端口
export FRONTEND_PORT=5173

# --- 启动选项 ---
# 启动后是否自动打开浏览器（true / false，服务器环境建议 false）
export AUTO_OPEN_BROWSER=false

# 浏览器打开的地址
export BROWSER_URL="http://localhost:${FRONTEND_PORT}"
