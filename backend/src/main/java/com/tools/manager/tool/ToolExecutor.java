package com.tools.manager.tool;

/**
 * 工具执行器接口，负责执行工具逻辑。
 */
public interface ToolExecutor {

    String getToolId();

    ToolResult execute(ToolContext context);

}
