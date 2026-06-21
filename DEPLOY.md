# 🚀 CI/CD 自动部署指南

本文档说明如何配置 GitHub Actions 自动部署到 1Panel 服务器。

---

## 架构流程

```
开发者 push 代码到 GitHub main 分支
         │
         ▼
GitHub Actions 触发
         │
         ├─ 1. 构建 Docker 镜像
         ├─ 2. 推送到 GitHub Container Registry (ghcr.io)
         │
         ▼
SSH 连接服务器
         │
         ├─ 3. 登录 GHCR 拉取最新镜像
         ├─ 4. docker compose 重启容器
         └─ 5. 清理旧镜像
         │
         ▼
部署完成，1Panel 面板自动显示新版本
```

---

## 一、GitHub 仓库配置

### 1. 配置 Variables（变量）

进入 GitHub 仓库 → **Settings → Secrets and variables → Actions → Variables**

| 变量名 | 说明 | 示例值 |
|--------|------|--------|
| `SERVER_HOST` | 服务器 IP 地址 | `123.45.67.89` |
| `SERVER_USER` | SSH 用户名 | `root` |
| `SERVER_PORT` | SSH 端口（1Panel 默认可能改过） | `22` |

### 2. 配置 Secrets（密钥）

进入 GitHub 仓库 → **Settings → Secrets and variables → Actions → Secrets**

| 密钥名 | 说明 |
|--------|------|
| `SERVER_SSH_KEY` | 服务器 SSH 私钥（完整内容） |

> `GITHUB_TOKEN` 由 GitHub 自动提供，无需手动配置。

### 3. 获取 SSH 私钥

在服务器上执行：

```bash
# 生成 SSH 密钥对（如果还没有）
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ~/.ssh/github_actions

# 将公钥添加到 authorized_keys
cat ~/.ssh/github_actions.pub >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys

# 查看私钥内容（复制到 GitHub Secrets）
cat ~/.ssh/github_actions
```

将 `-----BEGIN OPENSSH PRIVATE KEY-----` 到 `-----END OPENSSH PRIVATE KEY-----` 的完整内容粘贴到 `SERVER_SSH_KEY`。

---

## 二、服务器配置

### 1. 安装 Docker

如果服务器还没有 Docker，通过 1Panel 安装：
- 1Panel → **容器 → 设置** → 确认 Docker 已安装

### 2. 创建部署目录

```bash
mkdir -p /opt/tools-manager
cd /opt/tools-manager
```

### 3. 下载生产环境配置

```bash
# 方式 1：从 GitHub 直接下载（国外服务器推荐）
curl -fsSL "https://raw.githubusercontent.com/secoworld/my_tools_manager/main/docker-compose.prod.yml" -o docker-compose.prod.yml

# 方式 2：国内服务器使用代理下载（如果方式 1 失败）
curl -fsSL "https://ghproxy.com/https://raw.githubusercontent.com/secoworld/my_tools_manager/main/docker-compose.prod.yml" -o docker-compose.prod.yml

# 方式 3：使用 jsDelivr CDN
curl -fsSL "https://cdn.jsdelivr.net/gh/secoworld/my_tools_manager@main/docker-compose.prod.yml" -o docker-compose.prod.yml
```

### 4. 登录 GHCR（首次）

GitHub Container Registry 需要认证才能拉取私有镜像：

```bash
# 创建 Personal Access Token (GitHub → Settings → Developer settings → Personal access tokens)
# 权限: read:packages

echo "你的GitHub_Token" | docker login ghcr.io -u secoworld --password-stdin
```

> 如果仓库是 public，镜像也是 public，则无需登录。

### 5. 首次启动

```bash
docker compose -f docker-compose.prod.yml up -d
```

---

## 三、1Panel 配置

### 方式 A：通过 1Panel 管理容器（推荐）

1. 登录 1Panel 面板
2. 进入 **容器 → 容器**
3. 可以看到 `tools-manager` 容器正在运行
4. 后续 GitHub Actions 自动更新后，1Panel 面板会自动刷新显示新状态

### 方式 B：通过 1Panel 编排管理

1. 进入 **容器 → 编排**
2. 点击 **创建编排**
3. 名称：`tools-manager`
4. 选择 **已有编排文件**，路径：`/opt/tools-manager/docker-compose.prod.yml`
5. 确认后 1Panel 会接管该编排的管理

### 方式 C：配置反向代理（域名访问）

1. 进入 **网站 → 网站 → 创建网站**
2. 选择 **反向代理**
3. 主域名：`tools.yourdomain.com`
4. 代理地址：`http://127.0.0.1:8085`
5. 保存

### 方式 D：配置 SSL 证书

1. 网站设置 → **HTTPS**
2. 申请 Let's Encrypt 证书
3. 开启强制 HTTPS

---

## 四、自动更新流程

配置完成后，每次 push 到 `main` 分支：

1. GitHub Actions 自动构建新镜像
2. 推送到 GHCR
3. SSH 连接服务器，拉取新镜像并重启
4. 1Panel 面板自动反映新状态

### 手动触发部署

GitHub 仓库 → **Actions** → 选择 `Build and Deploy` workflow → **Run workflow**

### 查看部署日志

GitHub 仓库 → **Actions** → 点击对应的 workflow run → 查看详细日志

---

## 五、故障排查

### 镜像拉取失败

```bash
# 检查 GHCR 登录状态
cat ~/.docker/config.json

# 重新登录
echo "你的Token" | docker login ghcr.io -u secoworld --password-stdin

# 手动拉取测试
docker pull ghcr.io/secoworld/tools-manager:latest
```

### 国内服务器镜像加速（重要）

国内服务器直接拉取 `ghcr.io` 镜像经常超时失败，可通过以下方式加速：

#### 方式 1：使用 GHCR 代理（推荐）

通过国内镜像代理拉取，然后重新打标签：

```bash
# 使用南京大学镜像代理
docker pull ghcr.nju.edu.cn/secoworld/tools-manager:latest
docker tag ghcr.nju.edu.cn/secoworld/tools-manager:latest ghcr.io/secoworld/tools-manager:latest

# 或使用 dockerproxy
docker pull ghcr.dockerproxy.com/secoworld/tools-manager:latest
docker tag ghcr.dockerproxy.com/secoworld/tools-manager:latest ghcr.io/secoworld/tools-manager:latest
```

#### 方式 2：通过环境变量指定镜像源启动

`docker-compose.prod.yml` 支持通过 `IMAGE` 环境变量覆盖镜像地址：

```bash
# 使用代理镜像启动
IMAGE=ghcr.nju.edu.cn/secoworld/tools-manager:latest \
  docker compose -f docker-compose.prod.yml up -d
```

#### 方式 3：配置 GitHub Actions 自动使用代理

在 GitHub 仓库 → **Settings → Variables and secrets → Actions → Variables** 中添加：

| Variable 名称 | 值 | 说明 |
|---------------|-----|------|
| `GHCR_PROXY` | `ghcr.nju.edu.cn` | GHCR 镜像加速地址 |

配置后，GitHub Actions 部署时会：
1. 优先直接从 GHCR 拉取
2. 失败后自动尝试 `GHCR_PROXY` 指定的代理地址
3. 同时也会自动尝试 `ghcr.nju.edu.cn`、`ghcr.dockerproxy.com` 等默认代理

> **常用 GHCR 代理列表**（可用性可能变化，建议测试后选择）：
> - `ghcr.nju.edu.cn`（南京大学）
> - `ghcr.dockerproxy.com`
> - 自建 Cloudflare Workers 代理

### SSH 连接失败

```bash
# 检查 SSH 端口
ss -tlnp | grep ssh

# 检查防火墙
ufw status
# 如果端口不是 22，更新 GitHub Variables 中的 SERVER_PORT
```

### 容器启动失败

```bash
# 查看容器日志
docker logs tools-manager

# 检查端口占用
ss -tlnp | grep 8085

# 手动重启
cd /opt/tools-manager
docker compose -f docker-compose.prod.yml restart
```

### 1Panel 中看不到容器

确保 1Panel 管理的 Docker 与命令行使用的是同一个 Docker 实例：
```bash
# 检查 Docker socket
ls -la /var/run/docker.sock
docker info
```

---

## 六、回滚

```bash
cd /opt/tools-manager

# 查看历史镜像版本
docker images ghcr.io/secoworld/tools-manager

# 回滚到指定版本（用 commit SHA 前 7 位）
# 编辑 docker-compose.prod.yml，将 image 改为指定版本
# image: ghcr.io/secoworld/tools-manager:abc1234

# 重启
docker compose -f docker-compose.prod.yml up -d
```
