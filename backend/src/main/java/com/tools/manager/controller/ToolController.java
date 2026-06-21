package com.tools.manager.controller;

import com.tools.manager.tool.ToolDefinition;
import com.tools.manager.tool.ToolRegistry;
import com.tools.manager.tool.ToolResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ToolController {

    private final ToolRegistry toolRegistry;

    public ToolController(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @GetMapping("/tools")
    public List<ToolDefinition> listTools() {
        return toolRegistry.listTools();
    }

    @PostMapping("/tools/{toolId}/execute")
    public ToolResult execute(@PathVariable String toolId, @RequestBody Map<String, Object> params) {
        return toolRegistry.execute(toolId, params);
    }

}
