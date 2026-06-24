package com.tools.manager.plugin;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tools.manager.auth.SessionManager;

import java.util.HashMap;
import java.util.Map;

/**
 * 插件审核日志控制器
 *
 * - GET /api/plugins/audit-logs：分页查询所有审核日志（admin）
 * - GET /api/plugins/audit-logs/{pluginId}：根据 pluginId 分页查询日志（admin）
 */
@RestController
@RequestMapping("/api/plugins/audit-logs")
public class PluginAuditLogController {

    private static final Logger log = LoggerFactory.getLogger(PluginAuditLogController.class);

    private final PluginAuditLogService auditLogService;
    private final SessionManager sessionManager;

    public PluginAuditLogController(PluginAuditLogService auditLogService,
                                     SessionManager sessionManager) {
        this.auditLogService = auditLogService;
        this.sessionManager = sessionManager;
    }

    /**
     * 分页查询所有审核日志（admin）
     *
     * @param page 页码（从 1 开始，默认 1）
     * @param size 每页大小（默认 100）
     */
    @GetMapping
    public ResponseEntity<?> getAuditLogs(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "100") int size,
                                           HttpServletRequest request) {
        if (!requireAdmin(request)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "需要管理员权限"));
        }
        if (page < 1) page = 1;
        if (size < 1 || size > 500) size = 100;
        Page<PluginAuditLogEntity> logs = auditLogService.getLogs(page - 1, size);
        return ResponseEntity.ok(buildPageResponse(logs, page));
    }

    /**
     * 根据 pluginId 分页查询日志（admin）
     */
    @GetMapping("/{pluginId}")
    public ResponseEntity<?> getAuditLogsByPluginId(@PathVariable String pluginId,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "100") int size,
                                                     HttpServletRequest request) {
        if (!requireAdmin(request)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "需要管理员权限"));
        }
        if (page < 1) page = 1;
        if (size < 1 || size > 500) size = 100;
        Page<PluginAuditLogEntity> logs = auditLogService.getLogsByPluginId(pluginId, page - 1, size);
        return ResponseEntity.ok(buildPageResponse(logs, page));
    }

    private Map<String, Object> buildPageResponse(Page<PluginAuditLogEntity> logs, int page) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", logs.getContent());
        result.put("total", logs.getTotalElements());
        result.put("totalPages", logs.getTotalPages());
        result.put("page", page);
        result.put("size", logs.getSize());
        return result;
    }

    /** 检查是否为 admin 用户 */
    private boolean requireAdmin(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null || !sessionManager.validate(token)) {
            return false;
        }
        String username = sessionManager.getUsername(token);
        return "admin".equals(username);
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
