package com.tools.manager.tool;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具执行结果。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ToolResult {

    private boolean success;

    private Object data;

    private String message;

    public static ToolResult success(Object data) {
        return new ToolResult(true, data, null);
    }

    public static ToolResult fail(String message) {
        return new ToolResult(false, null, message);
    }

}
