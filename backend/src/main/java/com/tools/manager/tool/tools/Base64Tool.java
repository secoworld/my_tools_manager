package com.tools.manager.tool.tools;

import com.tools.manager.tool.Tool;
import com.tools.manager.tool.ToolContext;
import com.tools.manager.tool.ToolDefinition;
import com.tools.manager.tool.ToolExecutor;
import com.tools.manager.tool.ToolResult;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base64 编解码工具。
 */
@Tool(
        id = "base64-converter",
        name = "Base64转换",
        description = "对文本进行 Base64 编码或解码",
        icon = "base64",
        category = "编码",
        needBackend = true
)
public class Base64Tool implements ToolDefinition, ToolExecutor {

    /** 匹配可能的 Base64 片段：至少 4 个 Base64 字符，末尾可有 0-2 个 = 填充 */
    private static final Pattern BASE64_PATTERN = Pattern.compile("[A-Za-z0-9+/]{4,}={0,2}");

    @Override
    public String getToolId() {
        return getId();
    }

    @Override
    public String getId() {
        return "base64-converter";
    }

    @Override
    public String getName() {
        return "Base64转换";
    }

    @Override
    public String getDescription() {
        return "对文本进行 Base64 编码或解码";
    }

    @Override
    public String getIcon() {
        return "base64";
    }

    @Override
    public String getCategory() {
        return "编码";
    }

    @Override
    public boolean needBackend() {
        return true;
    }

    @Override
    public ToolResult execute(ToolContext context) {
        Object actionObj = context.getParam("action");
        Object textObj = context.getParam("text");
        if (actionObj == null || textObj == null) {
            return ToolResult.fail("缺少参数 action 或 text");
        }
        String action = String.valueOf(actionObj);
        String text = String.valueOf(textObj);
        try {
            Base64.Encoder encoder = Base64.getEncoder();
            Base64.Decoder decoder = Base64.getDecoder();
            switch (action) {
                case "encode":
                    return ToolResult.success(encoder.encodeToString(text.getBytes(StandardCharsets.UTF_8)));
                case "decode":
                    return ToolResult.success(new String(decoder.decode(text), StandardCharsets.UTF_8));
                case "mixed-decode": {
                    // 混合解码：用正则匹配 Base64 片段，逐个尝试解码，非 Base64 部分保留原样
                    Matcher matcher = BASE64_PATTERN.matcher(text);
                    StringBuilder result = new StringBuilder();
                    while (matcher.find()) {
                        String base64Str = matcher.group();
                        try {
                            byte[] decoded = decoder.decode(base64Str);
                            String decodedStr = new String(decoded, StandardCharsets.UTF_8);
                            // 只替换解码后为可打印文本的片段，否则保留原文
                            if (isPrintable(decodedStr)) {
                                matcher.appendReplacement(result, Matcher.quoteReplacement(decodedStr));
                            } else {
                                matcher.appendReplacement(result, Matcher.quoteReplacement(base64Str));
                            }
                        } catch (IllegalArgumentException e) {
                            // 解码失败，保留原文
                            matcher.appendReplacement(result, Matcher.quoteReplacement(base64Str));
                        }
                    }
                    matcher.appendTail(result);
                    return ToolResult.success(result.toString());
                }
                default:
                    return ToolResult.fail("不支持的 action: " + action + "，仅支持 encode/decode/mixed-decode");
            }
        } catch (IllegalArgumentException e) {
            return ToolResult.fail("解码失败，输入不是合法的 Base64 字符串: " + e.getMessage());
        }
    }

    /**
     * 检查字符串是否为可打印文本（允许换行、回车、制表符、ASCII 可打印字符、Unicode 字符）。
     */
    private boolean isPrintable(String str) {
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\n' || c == '\r' || c == '\t') {
                continue;
            }
            if (c >= 32 && c <= 126) {
                continue;
            }
            if (c >= 128) {
                // Unicode 字符（如中文）
                continue;
            }
            return false;
        }
        return true;
    }

}
