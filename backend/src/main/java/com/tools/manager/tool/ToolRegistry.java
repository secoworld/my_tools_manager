package com.tools.manager.tool;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具注册中心。
 * 通过构造器注入所有 ToolDefinition 和 ToolExecutor 实现，
 * 实现类同时实现两个接口时会被注入到对应的 List 中。
 */
@Component
public class ToolRegistry {

    private final Map<String, ToolDefinition> tools = new ConcurrentHashMap<>();

    private final Map<String, ToolExecutor> executors = new ConcurrentHashMap<>();

    public ToolRegistry(List<ToolDefinition> definitions, List<ToolExecutor> executors) {
        if (definitions != null) {
            for (ToolDefinition definition : definitions) {
                this.tools.put(definition.getId(), definition);
            }
        }
        if (executors != null) {
            for (ToolExecutor executor : executors) {
                this.executors.put(executor.getToolId(), executor);
            }
        }
    }

    public List<ToolDefinition> listTools() {
        return List.copyOf(tools.values());
    }

    public ToolDefinition getTool(String id) {
        return tools.get(id);
    }

    public ToolResult execute(String toolId, Map<String, Object> params) {
        ToolExecutor executor = executors.get(toolId);
        if (executor == null) {
            return ToolResult.fail("未找到工具执行器: " + toolId);
        }
        ToolContext context = new ToolContext(null, params, Collections.emptyMap());
        return executor.execute(context);
    }

}
