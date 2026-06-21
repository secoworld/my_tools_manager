package com.tools.manager.tool.tools;

import com.tools.manager.tool.Tool;
import com.tools.manager.tool.ToolContext;
import com.tools.manager.tool.ToolDefinition;
import com.tools.manager.tool.ToolExecutor;
import com.tools.manager.tool.ToolResult;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * 时间戳转换工具。
 */
@Tool(
        id = "timestamp-converter",
        name = "时间戳转换",
        description = "日期字符串与时间戳之间的相互转换",
        icon = "timestamp",
        category = "转换",
        needBackend = true
)
public class TimestampTool implements ToolDefinition, ToolExecutor {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String getToolId() {
        return getId();
    }

    @Override
    public String getId() {
        return "timestamp-converter";
    }

    @Override
    public String getName() {
        return "时间戳转换";
    }

    @Override
    public String getDescription() {
        return "日期字符串与时间戳之间的相互转换";
    }

    @Override
    public String getIcon() {
        return "timestamp";
    }

    @Override
    public String getCategory() {
        return "转换";
    }

    @Override
    public boolean needBackend() {
        return true;
    }

    @Override
    public ToolResult execute(ToolContext context) {
        Object actionObj = context.getParam("action");
        Object inputObj = context.getParam("value");
        if (actionObj == null || inputObj == null) {
            return ToolResult.fail("缺少参数 action 或 value");
        }
        String action = String.valueOf(actionObj);
        String input = String.valueOf(inputObj).trim();
        try {
            switch (action) {
                case "date-to-timestamp": {
                    // 支持 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss
                    LocalDateTime dateTime;
                    if (input.length() <= 10) {
                        dateTime = LocalDate.parse(input, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
                    } else {
                        dateTime = LocalDateTime.parse(input, DATE_FORMATTER);
                    }
                    long millis = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    long seconds = millis / 1000;
                    Map<String, Object> result = new HashMap<>();
                    result.put("seconds", seconds);
                    result.put("milliseconds", millis);
                    return ToolResult.success(result);
                }
                case "timestamp-to-date": {
                    long ts = Long.parseLong(input);
                    // 10 位及以下视为秒级时间戳，自动转换为毫秒
                    if (input.length() <= 10) {
                        ts = ts * 1000;
                    }
                    LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault());
                    return ToolResult.success(DATE_FORMATTER.format(date));
                }
                default:
                    return ToolResult.fail("不支持的 action: " + action + "，仅支持 date-to-timestamp/timestamp-to-date");
            }
        } catch (DateTimeParseException e) {
            return ToolResult.fail("日期解析失败，请使用格式 yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd");
        } catch (NumberFormatException e) {
            return ToolResult.fail("时间戳解析失败，请输入合法的数字时间戳");
        }
    }

}
