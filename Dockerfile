# ============================================================
#   Tools Manager Dockerfile (多阶段构建)
#   构建: docker build -t tools-manager .
#   运行: docker run -p 8080:8080 tools-manager
# ============================================================

# ---- 阶段 1: 构建前端 ----
FROM node:18-alpine AS frontend-builder
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build

# ---- 阶段 2: 构建后端 (含前端静态资源) ----
FROM maven:3.9-eclipse-temurin-17 AS backend-builder
WORKDIR /app/backend
# 先复制 pom.xml 加速依赖缓存
COPY backend/pom.xml ./
RUN mvn dependency:resolve -B
# 复制源码
COPY backend/src ./src
# 将前端构建产物复制到 SpringBoot 静态资源目录
COPY --from=frontend-builder /app/frontend/dist ./src/main/resources/static
# 打包 (跳过测试)
RUN mvn package -DskipTests -B

# ---- 阶段 3: 运行时 ----
FROM eclipse-temurin:17-jre
WORKDIR /app
LABEL maintainer="Tools Manager"
LABEL description="开发者工具管理框架"
# 复制构建好的 JAR
COPY --from=backend-builder /app/backend/target/*.jar app.jar
# 暴露端口
EXPOSE 8080
# JVM 参数 (可通过环境变量调整)
ENV JAVA_OPTS="-Xms256m -Xmx512m"
# 启动
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
