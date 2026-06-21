# 工具管理器 (Tools Manager) 框架设计文档

## 一、项目概述

提供工作中常用小功能的工具管理平台，支持工具多开、自定义工具页面、命令库管理。

### 技术栈

- **后端**: Java 17 + SpringBoot 3 + Spring Data JPA + H2 数据库
- **前端**: Vue3 + Vite + Pinia + Vue Router + Element Plus + Axios

### 核心需求

1. 框架可扩展，新增工具简单快捷
2. 工具窗口可多开，同一工具支持多实例
3. 支持自定义工具接口页面
4. 提供命令库页面，支持模块/命令的增删改查

---

## 二、整体架构

```
┌─────────────────────────────────────────────────────────┐
│                    前端 (Vue3 + Pinia)                   │
│  ┌──────────┐  ┌────────────────────────────────────┐  │
│  │ 侧边栏    │  │        工作区 (多窗口管理)          │  │
│  │ - 工具列表│  │  ┌──────┐ ┌──────┐ ┌──────┐       │  │
│  │ - 命令库  │  │  │工具A │ │工具B │ │工具A'│       │  │
│  │ - 设置    │  │  │Tab1  │ │Tab2  │ │Tab3  │       │  │
│  └──────────┘  │  └──────┘ └──────┘ └──────┘       │  │
│                └────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
                         │ REST API
┌─────────────────────────────────────────────────────────┐
│                后端 (SpringBoot)                         │
│  ┌────────────┐ ┌────────────┐ ┌────────────────────┐  │
│  │工具注册中心 │ │工具执行器   │ │ 命令管理 (CRUD)    │  │
│  └────────────┘ └────────────┘ └────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │        工具插件层 (SPI 自动扫描注册)              │  │
│  │  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐        │  │
│  │  │工具1│ │工具2│ │工具3│ │工具4│ │...  │        │  │
│  │  └─────┘ └─────┘ └─────┘ └─────┘ └─────┘        │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
                         │
┌─────────────────────────────────────────────────────────┐
│              数据库 (H2/MySQL/SQLite)                    │
│  命令模块表、命令表、工具配置表                           │
└─────────────────────────────────────────────────────────┘
```

---

## 三、后端设计 (Java + SpringBoot)

### 3.1 工具 SPI 接口

所有工具的统一规范：

```java
// 工具元数据定义
public interface ToolDefinition {
    String getId();           // 唯一标识
    String getName();         // 显示名称
    String getDescription();  // 描述
    String getIcon();         // 图标
    String getCategory();     // 分类
    boolean needBackend();    // 是否需要后端执行
}

// 工具执行器（需要后端逻辑的工具实现此接口）
public interface ToolExecutor {
    String getToolId();
    ToolResult execute(ToolContext context);
}

// 执行上下文
public class ToolContext {
    private String instanceId;          // 实例ID（支持多开）
    private Map<String, Object> params; // 输入参数
    private Map<String, Object> state;  // 实例状态
}

// 执行结果
public class ToolResult {
    private boolean success;
    private Object data;
    private String message;
}
```

### 3.2 工具注解（一键注册）

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface Tool {
    String id();
    String name();
    String description() default "";
    String icon() default "";
    String category() default "default";
    boolean needBackend() default false;
}
```

### 3.3 工具注册中心（自动扫描）

```java
@Component
public class ToolRegistry {
    private final Map<String, ToolDefinition> tools = new ConcurrentHashMap<>();
    private final Map<String, ToolExecutor> executors = new ConcurrentHashMap<>();

    public ToolRegistry(List<ToolDefinition> definitions, List<ToolExecutor> executors) {
        definitions.forEach(d -> tools.put(d.getId(), d));
        executors.forEach(e -> this.executors.put(e.getToolId(), e));
    }

    public List<ToolDefinition> listTools() { ... }
    public ToolResult execute(String toolId, ToolContext context) { ... }
}
```

### 3.4 REST 控制器

```java
@RestController
@RequestMapping("/api")
public class ToolController {
    @GetMapping("/tools")
    public List<ToolDefinition> listTools() { ... }

    @PostMapping("/tools/{toolId}/execute")
    public ToolResult execute(@PathVariable String toolId,
                              @RequestBody Map<String, Object> params) { ... }
}
```

### 3.5 命令管理（CRUD）

```java
@Entity
public class CommandModule {
    @Id @GeneratedValue(strategy = IDENTITY)
    private Long id;
    private String name;
    private String description;
    private String icon;
    private Integer sortOrder;
}

@Entity
public class Command {
    @Id @GeneratedValue(strategy = IDENTITY)
    private Long id;
    private Long moduleId;
    private String name;
    @Column(columnDefinition = "TEXT") private String command;
    private String description;
    private String category;
    private Integer sortOrder;
}
```

### 3.6 数据库表结构

```sql
CREATE TABLE command_module (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    icon VARCHAR(100),
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE command (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    module_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    command TEXT NOT NULL,
    description TEXT,
    category VARCHAR(50),
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (module_id) REFERENCES command_module(id) ON DELETE CASCADE
);
```

---

## 四、前端设计 (Vue3 + Pinia)

### 4.1 工具注册表

```javascript
// src/tools/registry.js
export const toolRegistry = {
  'json-formatter': {
    id: 'json-formatter',
    name: 'JSON格式化',
    icon: 'Code',
    category: '格式化',
    needBackend: false,
    component: () => import('./JsonFormatter.vue')
  }
  // 新增工具只需在此注册一行
}
```

### 4.2 窗口管理器 (Pinia Store)

```javascript
// src/stores/windowManager.js
export const useWindowManager = defineStore('windowManager', {
  state: () => ({
    windows: [],
    activeWindowId: null
  }),
  actions: {
    openTool(toolId) {
      // 每次生成新实例，支持同工具多开
      const instanceId = `${toolId}-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`
      this.windows.push({ id: instanceId, toolId, ... })
      this.activeWindowId = instanceId
    },
    closeWindow(id) { ... },
    setActive(id) { ... }
  }
})
```

### 4.3 主布局组件

使用 Tab 标签页 + 动态组件渲染，每个窗口实例状态独立。

---

## 五、新增一个小工具的完整步骤

### 纯前端工具（最简，2步）

1. 创建 Vue 组件 `src/tools/XxxTool.vue`
2. 在 `src/tools/registry.js` 中注册一行

### 需要后端的工具（3步）

1. 后端：创建带 `@Tool` 注解的类（自动注册）
2. 前端：创建 Vue 组件
3. 前端：在 `registry.js` 中注册

---

## 六、项目目录结构

```
tools_manager/
├── DESIGN.md
├── backend/                          # SpringBoot 后端
│   ├── pom.xml
│   └── src/main/java/com/tools/manager/
│       ├── ToolManagerApplication.java
│       ├── config/CorsConfig.java
│       ├── controller/
│       │   ├── ToolController.java
│       │   └── CommandController.java
│       ├── entity/
│       │   ├── CommandModule.java
│       │   └── Command.java
│       ├── repository/
│       ├── service/
│       ├── tool/                     # 工具核心
│       │   ├── Tool.java
│       │   ├── ToolDefinition.java
│       │   ├── ToolExecutor.java
│       │   ├── ToolContext.java
│       │   ├── ToolResult.java
│       │   ├── ToolRegistry.java
│       │   └── tools/                # 具体工具实现
│       └── resources/
│           ├── application.yml
│           └── data.sql
│
└── frontend/                         # Vue3 前端
    ├── package.json
    ├── vite.config.js
    ├── index.html
    └── src/
        ├── App.vue
        ├── main.js
        ├── router/
        ├── stores/
        │   ├── windowManager.js
        │   └── commandStore.js
        ├── views/
        │   ├── Workspace.vue
        │   └── CommandLibrary.vue
        ├── components/
        │   └── Sidebar.vue
        ├── tools/
        │   ├── registry.js
        │   ├── JsonFormatter.vue
        │   ├── Base64Converter.vue
        │   └── TimestampConverter.vue
        └── api/index.js
```

---

## 七、设计要点总结

| 需求 | 实现方案 |
|------|----------|
| **可扩展性** | 后端 `@Tool` 注解 + Spring 自动扫描；前端 `registry.js` 注册表 |
| **窗口多开** | 前端 `windowManager` store 为每个窗口生成唯一 `instanceId`，动态组件渲染，状态隔离 |
| **同工具多开** | `openTool()` 每次创建新实例，不检查是否已存在 |
| **自定义工具页面** | 每个工具是独立 Vue 组件，通过 `component: () => import()` 动态加载 |
| **命令库管理** | 后端 JPA 实体 + REST CRUD API，前端模块化展示 |
| **技术栈匹配** | Java + SpringBoot（后端），Vue3 + Pinia + Element Plus（前端） |
