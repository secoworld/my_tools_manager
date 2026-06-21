package com.tools.manager.tool;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 工具执行上下文，承载执行参数与状态。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ToolContext {

    private String instanceId;

    private Map<String, Object> params;

    private Map<String, Object> state;

    /**
     * 获取参数，不存在时返回 null。
     */
    public Object getParam(String key) {
        return params == null ? null : params.get(key);
    }

}
