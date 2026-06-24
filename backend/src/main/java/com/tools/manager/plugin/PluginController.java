package com.tools.manager.plugin;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tools.manager.auth.SessionManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plugins")
public class PluginController {

    private final PluginService pluginService;
    private final SessionManager sessionManager;
    private final PluginAuditLogService auditLogService;

    public PluginController(PluginService pluginService,
                            SessionManager sessionManager,
                            PluginAuditLogService auditLogService) {
        this.pluginService = pluginService;
        this.sessionManager = sessionManager;
        this.auditLogService = auditLogService;
    }

    @GetMapping("")
    public List<PluginEntity> getEnabledPlugins() {
        return pluginService.getEnabledPlugins();
    }

    @GetMapping("/all")
    public List<PluginEntity> getAllPlugins() {
        return pluginService.getAllPlugins();
    }

    @GetMapping("/{pluginId}/content")
    public ResponseEntity<String> getPluginContent(@PathVariable String pluginId) {
        try {
            String content = pluginService.getPluginContent(pluginId);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(content);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("获取插件内容失败: " + e.getMessage());
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadPlugin(@RequestParam("file") MultipartFile file,
                                          @RequestParam(value = "force", defaultValue = "false") boolean force,
                                          HttpServletRequest request) {
        try {
            PluginEntity entity = pluginService.uploadPlugin(file, force);
            // 记录审核日志
            String operator = getOperator(request);
            String clientIp = getClientIp(request);
            auditLogService.log(
                    entity.getPluginId(),
                    entity.getName(),
                    entity.getVersion(),
                    force ? "UPDATE" : "UPLOAD",
                    operator,
                    clientIp,
                    force ? "更新插件，版本: " + entity.getVersion() : "上传插件，版本: " + entity.getVersion()
            );
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", force ? "插件更新成功" : "插件上传成功");
            result.put("plugin", entity);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            // 检查是否是插件已存在的特殊情况
            if (e.getMessage() != null && e.getMessage().startsWith("PLUGIN_EXISTS:")) {
                String pluginId = e.getMessage().substring("PLUGIN_EXISTS:".length());
                error.put("code", "PLUGIN_EXISTS");
                error.put("pluginId", pluginId);
                error.put("message", "插件 ID 已存在: " + pluginId + "，是否更新？");
            } else {
                error.put("message", e.getMessage());
            }
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/{pluginId}")
    public ResponseEntity<Map<String, Object>> deletePlugin(@PathVariable String pluginId,
                                                             HttpServletRequest request) {
        try {
            // 先获取插件信息用于日志记录
            PluginEntity entity = pluginService.getAllPlugins().stream()
                    .filter(p -> pluginId.equals(p.getPluginId()))
                    .findFirst()
                    .orElse(null);
            pluginService.deletePlugin(pluginId);
            // 记录审核日志
            String operator = getOperator(request);
            String clientIp = getClientIp(request);
            auditLogService.log(
                    pluginId,
                    entity != null ? entity.getName() : pluginId,
                    entity != null ? entity.getVersion() : "",
                    "DELETE",
                    operator,
                    clientIp,
                    "删除插件"
            );
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "插件已删除: " + pluginId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /** 从请求中获取操作者用户名 */
    private String getOperator(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null && sessionManager.validate(token)) {
            return sessionManager.getUsername(token);
        }
        return "anonymous";
    }

    /** 获取客户端 IP 地址 */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    @PutMapping("/{pluginId}/enable")
    public ResponseEntity<?> enablePlugin(@PathVariable String pluginId) {
        try {
            PluginEntity entity = pluginService.togglePlugin(pluginId, true);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("plugin", entity);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/{pluginId}/disable")
    public ResponseEntity<?> disablePlugin(@PathVariable String pluginId) {
        try {
            PluginEntity entity = pluginService.togglePlugin(pluginId, false);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("plugin", entity);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/{pluginId}/visibility")
    public ResponseEntity<?> updateVisibility(@PathVariable String pluginId,
                                              @RequestBody Map<String, String> body) {
        try {
            String visibilityStr = body.get("visibility");
            if (visibilityStr == null || visibilityStr.isBlank()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "visibility 参数不能为空");
                return ResponseEntity.badRequest().body(error);
            }
            PluginVisibility visibility;
            try {
                visibility = PluginVisibility.valueOf(visibilityStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "无效的 visibility 值: " + visibilityStr);
                return ResponseEntity.badRequest().body(error);
            }
            PluginEntity entity = pluginService.updateVisibility(pluginId, visibility);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("plugin", entity);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
