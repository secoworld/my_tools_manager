# ⚡ Tools Manager — 开发者工具宇宙

> 一个可扩展的、支持多窗口并行的开发者工具管理框架。像搭积木一样组装你的工具箱，让效率触手可及。

---

## 📖 项目简介

日常开发中，JSON 格式化、Base64 编解码、时间戳转换等高频小工具散落在各个网站，缺乏统一入口，且无法同时打开多个工具对比结果。**Tools Manager** 将这些常用小工具整合到一个框架中，提供瀑布流仪表盘、多窗口并行、插件化扩展和命令库管理，打造一站式开发者工作台。

### 核心能力

- 🧩 **SPI 插件机制** — `@Tool` 注解 + Spring 自动扫描注册，新增工具零配置上线
- 🪟 **多窗口并行** — 同一工具支持多实例，Pinia + markRaw + 异步组件按需加载
- 📊 **瀑布流仪表盘** — CSS Grid 12 列网格 + dense 填充，拖拽排序 + 拖拽调整大小
- 💾 **状态缓存** — 仪表盘布局和工具输入内容自动持久化，刷新页面不丢失
- 📚 **命令库管理** — 团队常用命令集中管理，支持模块/命令的增删改查
- 🎨 **自定义工具页面** — Vue3 单文件组件开发，所见即所得

---

## 🛠️ 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Java + SpringBoot | 17 + 3.2.5 |
| 数据访问 | Spring Data JPA + H2 | 内嵌数据库 |
| 前端框架 | Vue3 + Pinia + Vue Router | 3 |
| UI 组件库 | Element Plus | 2.7+ |
| 构建工具 | Vite | 4.5 |
| 布局方案 | CSS Grid + HTML5 Drag API | — |

---

## 📂 项目结构

```
tools_manager/
├── backend/                            # SpringBoot 后端
│   ├── pom.xml
│   └── src/main/java/com/tools/manager/
│       ├── ToolManagerApplication.java # 启动类
│       ├── config/
│       │   └── CorsConfig.java         # 跨域配置
│       ├── controller/
│       │   ├── ToolController.java     # 工具执行 API
│       │   └── CommandController.java  # 命令库 CRUD API
│       ├── entity/
│       │   ├── Command.java            # 命令实体
│       │   └── CommandModule.java      # 命令模块实体
│       ├── repository/
│       │   ├── CommandRepository.java
│       │   └── CommandModuleRepository.java
│       ├── service/
│       │   └── CommandService.java     # 命令库业务逻辑
│       └── tool/                       # SPI 工具框架
│           ├── Tool.java               # @Tool 注解
│           ├── ToolContext.java        # 工具上下文
│           ├── ToolDefinition.java     # 工具定义接口
│           ├── ToolExecutor.java       # 工具执行接口
│           ├── ToolRegistry.java       # 工具注册中心（自动扫描）
│           ├── ToolResult.java         # 工具执行结果
│           └── tools/                  # 内置工具实现
│               ├── Base64Tool.java     # Base64 编解码
│               └── TimestampTool.java  # 时间戳转换
│
├── frontend/                           # Vue3 前端
│   ├── package.json
│   ├── vite.config.js
│   ├── index.html
│   └── src/
│       ├── App.vue
│       ├── main.js
│       ├── api/index.js               # 后端 API 调用封装
│       ├── components/
│       │   ├── TopBar.vue              # 顶部导航栏
│       │   └── Sidebar.vue             # 侧边栏
│       ├── composables/
│       │   └── useToolState.js         # 工具状态持久化 composable
│       ├── router/index.js             # 路由配置
│       ├── stores/
│       │   ├── windowManager.js        # 多窗口管理 (Pinia)
│       │   └── commandStore.js         # 命令库状态
│       ├── tools/                      # 工具组件
│       │   ├── registry.js             # 工具注册中心
│       │   ├── JsonFormatter.vue       # JSON 格式化
│       │   ├── Base64Converter.vue     # Base64 转换
│       │   ├── TimestampConverter.vue  # 时间戳转换
│       │   └── TextEditor.vue          # 文本编辑器
│       └── views/
│           ├── Workspace.vue           # 工作区（多窗口）
│           ├── Dashboard.vue           # 瀑布流仪表盘
│           └── CommandLibrary.vue      # 命令库管理
│
├── DESIGN.md                           # 框架设计文档
├── showcase.html                       # 创意展示页面（Trae 创意大赛）
├── .gitignore
└── README.md
```

---

## 🚀 快速开始

### 环境要求

- **JDK** 17+
- **Maven** 3.6+
- **Node.js** 16+
- **npm** 8+

### 1. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端启动于 `http://localhost:8080`，使用内嵌 H2 数据库，无需额外安装。

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端启动于 `http://localhost:5173`。

### 3. 访问应用

浏览器打开 `http://localhost:5173` 即可使用。

---

## 🎮 功能导览

### 仪表盘（瀑布流工作台）

- 点击右下角 **+** 添加工具到仪表盘
- **拖拽卡片标题栏** → 排序
- **拖拽卡片右下角** → 按格子单位调整大小
- **一键清除** → 清空所有工具和缓存
- 布局和输入内容**自动缓存**，刷新页面不丢失

### 工具箱（多窗口模式）

- 侧边栏选择工具，支持同时打开多个
- 同一工具可开多个实例，独立运行
- Tab 切换不同工具窗口

### 命令库

- 按模块分类管理常用命令
- 支持新增/编辑/删除模块和命令
- 团队协作知识库

---

## 🔧 内置工具

### 📄 JSON 格式化

| 功能 | 说明 |
|------|------|
| 格式化 | 2 空格缩进美化 |
| 压缩 | 移除所有空白 |
| 转义 | 特殊字符转义（`"` → `\"` 等） |
| 去除转义 | 还原转义字符 |
| 排序 | 按 key 字母排序 |
| 校验 | JSON 语法校验 |
| 复制 / 移到输入 | 快捷操作 |

### 🔐 Base64 转换

| 功能 | 说明 |
|------|------|
| 编码 | 文本 → Base64 |
| 解码 | Base64 → 文本 |
| 混合解码 | 智能识别文本中的 Base64 片段并解码，非 Base64 内容原样保留 |

### ⏰ 时间戳转换

| 功能 | 说明 |
|------|------|
| 日期 → 时间戳 | 同时输出秒(s)和毫秒(ms) |
| 时间戳 → 日期 | 自动识别 10 位(秒) / 13 位(毫秒) |
| 当前时间戳 | 一键获取当前时间戳 |

### 📝 文本编辑器

| 功能 | 说明 |
|------|------|
| 编辑 | 大文本编辑区，等宽字体 |
| 下载 .txt | 导出为 txt 文件 |
| 导入 .txt | 从本地加载 txt 文件 |
| 自动保存 | 内容随仪表盘缓存持久化 |
| 字符统计 | 实时显示字符数和行数 |

---

## 🧩 扩展开发指南

### 新增后端工具（SPI 机制）

1. **实现工具定义和执行器**

```java
@Tool(id = "my-tool", name = "我的工具", category = "自定义")
public class MyTool implements ToolDefinition, ToolExecutor {

    @Override
    public ToolResult execute(ToolContext context) {
        String input = context.getParameter("input");
        // 业务逻辑...
        return ToolResult.success("处理结果");
    }
}
```

2. **启动应用** — Spring 自动扫描 `@Tool` 注解并注册，无需任何配置

### 新增前端工具组件

1. **创建 Vue 组件** `frontend/src/tools/MyTool.vue`

```vue
<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { executeTool } from '../api'
import { useToolState } from '../composables/useToolState'

const props = defineProps({
  instanceId: { type: String, required: true }
})

const inputText = ref('')
const outputText = ref('')

// 状态持久化（可选）
useToolState(props.instanceId, { inputText, outputText })

const convert = async () => {
  const result = await executeTool('my-tool', { input: inputText.value })
  outputText.value = result.data
}
</script>

<template>
  <!-- 工具界面 -->
</template>
```

2. **注册到工具注册中心** `frontend/src/tools/registry.js`

```javascript
export const toolRegistry = {
  // ... 已有工具
  'my-tool': {
    id: 'my-tool',
    name: '我的工具',
    icon: 'Tools',
    category: '自定义',
    needBackend: true,
    component: defineAsyncComponent(() => import('./MyTool.vue'))
  }
}
```

3. **（可选）设置仪表盘默认尺寸** `frontend/src/views/Dashboard.vue`

```javascript
const defaultSizes = {
  // ... 已有工具
  'my-tool': { colSpan: 6, rowSpan: 3 }
}
```

完成！工具自动出现在工具箱和仪表盘的添加列表中。

---

## 🏗️ 架构设计

```
┌─────────────────────────────────────────┐
│         前端展示层 (Vue3)                │
│  仪表盘 · 工作区(多窗口) · 命令库        │
├──────────────┬──────────────────────────┤
│ 工具注册中心  │  窗口管理器 (Pinia)       │
│ registry.js  │  markRaw + 异步组件       │
├──────────────┴──────────────────────────┤
│        REST API (SpringBoot 3.2)        │
│      /api/tools/execute                 │
│      /api/commands (CRUD)               │
├─────────────────────────────────────────┤
│     SPI 扫描注册 (@Tool 注解)            │
│  Base64 · Timestamp · ...可扩展          │
├─────────────────────────────────────────┤
│        H2 数据库 (命令库存储)             │
└─────────────────────────────────────────┘
```

### 关键设计

- **SPI 插件机制**：借鉴 Java SPI 思想，`@Tool` 注解驱动，`ToolRegistry` 在启动时自动扫描注册
- **多窗口管理**：Pinia store + `markRaw` 避免组件被响应式 Proxy 包裹，`defineAsyncComponent` 按需懒加载
- **瀑布流布局**：CSS Grid 12 列 + `grid-auto-flow: dense`，HTML5 Drag API 排序 + mousedown/mouseup 调整大小
- **状态持久化**：`useToolState` composable 基于 localStorage，按 instanceId 隔离缓存

---

## 📡 API 接口

### 工具执行

```
POST /api/tools/execute
Content-Type: application/json

{
  "toolId": "base64-converter",
  "parameters": {
    "action": "encode",
    "text": "Hello World"
  }
}
```

### 工具列表

```
GET /api/tools
```

### 命令库 CRUD

```
GET    /api/modules              # 获取所有模块
POST   /api/modules              # 新增模块
DELETE /api/modules/{id}         # 删除模块
GET    /api/commands             # 获取所有命令
POST   /api/commands             # 新增命令
DELETE /api/commands/{id}        # 删除命令
```

---

## 📸 创意展示

项目附带一个交互式展示页面 `showcase.html`，包含：

- 赛博朋克风格视觉设计
- 三大工具的**真实可交互在线演示**
- 瀑布流仪表盘布局预览
- 系统架构流程图
- 技术栈展示与开发历程时间线

直接在浏览器中打开 `showcase.html` 即可体验。

---

## 📝 开发历程

1. **框架设计** — 定义 SPI 接口与注解，Spring 自动扫描注册
2. **多窗口管理** — Pinia + markRaw + defineAsyncComponent，解决异步组件响应式包裹问题
3. **命令库 CRUD** — JPA + H2，修复 Windows GBK 编码中文乱码
4. **三大工具实现** — JSON 格式化 / Base64 混合解码 / 时间戳双向转换
5. **瀑布流仪表盘** — CSS Grid + 拖拽排序 + 拖拽调整大小
6. **布局自适应** — 工具组件适配任意卡片尺寸
7. **状态缓存** — 仪表盘布局 + 工具输入内容持久化，一键清除
8. **文本编辑器** — 支持 txt 导入导出 + 自动保存

---

## 📄 License

MIT License

---

> **Tools Manager** — 让每一个小工具都有归处，让开发效率触手可及。⚡
