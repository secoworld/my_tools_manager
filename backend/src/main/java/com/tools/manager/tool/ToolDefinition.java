package com.tools.manager.tool;

/**
 * 工具定义接口，描述工具的元信息。
 */
public interface ToolDefinition {

    String getId();

    String getName();

    String getDescription();

    String getIcon();

    String getCategory();

    boolean needBackend();

}
