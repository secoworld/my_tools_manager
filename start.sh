#!/bin/bash
# ============================================================
#   Tools Manager 一键启动脚本 (Linux)
#   用法: chmod +x start.sh && ./start.sh
# ============================================================

set -e

# 获取脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 加载配置文件
if [ ! -f "config.sh" ]; then
    echo "[错误] 未找到 config.sh 配置文件"
    exit 1
fi
source ./config.sh

# 设置 PATH
if [ -n "$MAVEN_HOME" ]; then
    export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"
else
    export PATH="$JAVA_HOME/bin:$PATH"
fi

echo ""
echo "========================================"
echo "    Tools Manager 一键启动"
echo "========================================"
echo ""

# ---- 环境检查 ----
echo "[1/5] 检查运行环境..."

echo "      - Java..."
if ! command -v java &> /dev/null; then
    echo "      [失败] 未找到 Java，请检查 config.sh 中的 JAVA_HOME"
    echo "      当前配置: JAVA_HOME=$JAVA_HOME"
    exit 1
fi

echo "      - Maven..."
if ! command -v mvn &> /dev/null; then
    echo "      [失败] 未找到 Maven，请检查 config.sh 中的 MAVEN_HOME"
    exit 1
fi

echo "      - Node.js..."
if ! command -v node &> /dev/null; then
    echo "      [失败] 未找到 Node.js，请安装 Node.js 16+"
    exit 1
fi

echo "      - npm..."
if ! command -v npm &> /dev/null; then
    echo "      [失败] 未找到 npm"
    exit 1
fi

echo "      [OK] 环境检查通过"
echo ""

# ---- 检查前端依赖 ----
echo "[2/5] 检查前端依赖..."
if [ ! -d "$SCRIPT_DIR/frontend/node_modules" ]; then
    echo "      未安装依赖，正在安装..."
    cd "$SCRIPT_DIR/frontend"
    npm install
    cd "$SCRIPT_DIR"
    echo "      [OK] 依赖安装完成"
else
    echo "      [OK] 依赖已安装"
fi
echo ""

# ---- 启动后端 ----
echo "[3/5] 启动后端服务 (端口 $BACKEND_PORT)..."
cd "$SCRIPT_DIR/backend"
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=$BACKEND_PORT" &
BACKEND_PID=$!
cd "$SCRIPT_DIR"
echo "      后端 PID: $BACKEND_PID"
echo ""

# ---- 等待后端就绪 ----
echo "[4/5] 等待后端服务就绪..."
WAIT_COUNT=0
while [ $WAIT_COUNT -lt 30 ]; do
    sleep 2
    WAIT_COUNT=$((WAIT_COUNT + 2))
    if curl -s -o /dev/null "http://localhost:$BACKEND_PORT/api/tools" 2>/dev/null; then
        echo "      [OK] 后端已就绪 (等待 ${WAIT_COUNT} 秒)"
        break
    fi
    if [ $WAIT_COUNT -ge 30 ]; then
        echo "      [警告] 后端可能尚未完全启动，继续启动前端..."
    fi
done
echo ""

# ---- 启动前端 ----
echo "[5/5] 启动前端服务 (端口 $FRONTEND_PORT)..."
cd "$SCRIPT_DIR/frontend"
npm run dev &
FRONTEND_PID=$!
cd "$SCRIPT_DIR"
echo "      前端 PID: $FRONTEND_PID"
sleep 3

# ---- 打开浏览器 ----
if [ "$AUTO_OPEN_BROWSER" = "true" ]; then
    echo ""
    echo "[完成] 正在打开浏览器..."
    if command -v xdg-open &> /dev/null; then
        xdg-open "$BROWSER_URL"
    elif command -v open &> /dev/null; then
        open "$BROWSER_URL"
    fi
fi

echo ""
echo "========================================"
echo "    启动完成！"
echo "========================================"
echo ""
echo "    后端地址: http://localhost:$BACKEND_PORT"
echo "    前端地址: http://localhost:$FRONTEND_PORT"
echo "    H2 控制台: http://localhost:$BACKEND_PORT/h2-console"
echo ""
echo "    后端 PID: $BACKEND_PID"
echo "    前端 PID: $FRONTEND_PID"
echo "    停止服务: kill $BACKEND_PID $FRONTEND_PID"
echo "========================================"
echo ""

# 捕获 Ctrl+C，停止所有服务
trap "echo '正在停止服务...'; kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; exit 0" INT TERM
wait
