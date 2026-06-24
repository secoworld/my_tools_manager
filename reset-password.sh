#!/bin/bash
# ============================================
# 密码重置脚本
# 用法：
#   普通模式：./reset-password.sh
#   Docker 模式：./reset-password.sh --docker <容器名或ID>
# ============================================

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}       密码重置脚本${NC}"
echo -e "${CYAN}========================================${NC}"
echo ""

# 判断模式
DOCKER_MODE=false
CONTAINER_NAME=""

if [ "$1" = "--docker" ]; then
    DOCKER_MODE=true
    CONTAINER_NAME="$2"
    if [ -z "$CONTAINER_NAME" ]; then
        echo -e "${RED}[错误] Docker 模式需要指定容器名或ID${NC}"
        echo -e "用法: $0 --docker <容器名或ID>"
        exit 1
    fi
    echo -e "${YELLOW}Docker 模式：容器 = ${CONTAINER_NAME}${NC}"
    echo ""

    # 检查容器是否在运行
    if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
        echo -e "${RED}[错误] 容器 '${CONTAINER_NAME}' 未运行，请先启动容器${NC}"
        exit 1
    fi

    echo -e "${GREEN}正在启动密码重置工具...${NC}"
    echo ""
    docker exec -it "${CONTAINER_NAME}" java -jar app.jar --reset-password

else
    # 普通模式
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

    # 查找 jar 文件
    JAR_FILE=""

    # 1. 当前目录
    if [ -f "tools-manager.jar" ]; then
        JAR_FILE="tools-manager.jar"
    elif [ -f "tools-manager-0.0.1-SNAPSHOT.jar" ]; then
        JAR_FILE="tools-manager-0.0.1-SNAPSHOT.jar"
    # 2. backend/target 目录
    elif [ -f "${SCRIPT_DIR}/backend/target/tools-manager-0.0.1-SNAPSHOT.jar" ]; then
        JAR_FILE="${SCRIPT_DIR}/backend/target/tools-manager-0.0.1-SNAPSHOT.jar"
    elif [ -f "${SCRIPT_DIR}/backend/target/tools-manager.jar" ]; then
        JAR_FILE="${SCRIPT_DIR}/backend/target/tools-manager.jar"
    # 3. 脚本同目录
    elif [ -f "${SCRIPT_DIR}/tools-manager.jar" ]; then
        JAR_FILE="${SCRIPT_DIR}/tools-manager.jar"
    fi

    if [ -z "$JAR_FILE" ]; then
        echo -e "${RED}[错误] 未找到 tools-manager.jar 文件${NC}"
        echo ""
        echo "请将此脚本放在以下任一位置："
        echo "  1. jar 文件同目录"
        echo "  2. 项目根目录（会查找 backend/target/ 下的 jar）"
        echo ""
        echo "或手动执行："
        echo "  java -jar <path/to/tools-manager.jar> --reset-password"
        exit 1
    fi

    echo -e "${GREEN}使用 JAR 文件: ${JAR_FILE}${NC}"
    echo ""

    # 检查 java 是否可用
    if ! command -v java &> /dev/null; then
        echo -e "${RED}[错误] 未找到 java 命令，请确保 Java 17+ 已安装${NC}"
        exit 1
    fi

    echo -e "${GREEN}正在启动密码重置工具...${NC}"
    echo ""
    java -jar "${JAR_FILE}" --reset-password
fi
