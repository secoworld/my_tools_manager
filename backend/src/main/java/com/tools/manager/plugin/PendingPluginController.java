package com.tools.manager.plugin;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tools.manager.auth.SessionManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 待审核插件控制器
 *
 * - POST /api/plugins/submit：非 admin 用户提交插件
 * - GET /api/plugins/pending：获取待审核列表（admin）
 * - GET /api/plugins/pending/{pluginId}/content：预览待审核插件内容（admin）
 * - POST /api/plugins/pending/{pluginId}/approve：审核通过（admin）
 * - POST /api/plugins/pending/{pluginId}/reject：审核拒绝（admin）
 * - DELETE /api/plugins/pending/{pluginId}：删除待审核插件（admin）
 */
@RestController
@RequestMapping("/api/plugins")
public class PendingPluginController {

    private static final Logger log = LoggerFactory.getLogger(PendingPluginController.class);

    private final PendingPluginService pendingPluginService;
    private final SessionManager sessionManager;

    public PendingPluginController(PendingPluginService pendingPluginService,
                                   SessionManager sessionManager) {
        this.pendingPluginService = pendingPluginService;
        this.sessionManager = sessionManager;
    }

    /**
     * 提交插件（非 admin 用户）
     * 需要登录 token，但不要求 admin 权限
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitPlugin(@RequestParam("file") MultipartFile file,
                                          HttpServletRequest request) {
        try {
            String submitter = getSubmitter(request);
            PendingPluginEntity entity = pendingPluginService.submitPlugin(file, submitter);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "插件提交成功，等待管理员审核");
            result.put("plugin", entity);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 获取待审核插件列表（admin）
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingPlugins(HttpServletRequest request) {
        if (!requireAdmin(request)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "需要管理员权限"));
        }
        List<PendingPluginEntity> list = pendingPluginService.getPendingPlugins();
        return ResponseEntity.ok(list);
    }

    /**
     * 预览待审核插件内容（admin）
     */
    @GetMapping("/pending/{pluginId}/content")
    public ResponseEntity<String> getPendingPluginContent(@PathVariable String pluginId,
                                                          HttpServletRequest request) {
        if (!requireAdmin(request)) {
            return ResponseEntity.status(403).body("需要管理员权限");
        }
        try {
            String content = pendingPluginService.getPendingPluginContent(pluginId);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(content);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("获取插件内容失败: " + e.getMessage());
        }
    }

    /**
     * 审核通过（admin）
     */
    @PostMapping("/pending/{pluginId}/approve")
    public ResponseEntity<?> approvePlugin(@PathVariable String pluginId,
                                           @RequestBody(required = false) Map<String, String> body,
                                           HttpServletRequest request) {
        if (!requireAdmin(request)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "需要管理员权限"));
        }
        try {
            String reviewComment = body != null ? body.get("reviewComment") : null;
            PluginEntity entity = pendingPluginService.approvePlugin(pluginId, reviewComment);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "插件审核通过，已安装");
            result.put("plugin", entity);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 审核拒绝（admin）
     */
    @PostMapping("/pending/{pluginId}/reject")
    public ResponseEntity<?> rejectPlugin(@PathVariable String pluginId,
                                          @RequestBody(required = false) Map<String, String> body,
                                          HttpServletRequest request) {
        if (!requireAdmin(request)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "需要管理员权限"));
        }
        try {
            String reviewComment = body != null ? body.get("reviewComment") : null;
            pendingPluginService.rejectPlugin(pluginId, reviewComment);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "插件已拒绝");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 删除待审核插件（admin）
     */
    @DeleteMapping("/pending/{pluginId}")
    public ResponseEntity<?> deletePendingPlugin(@PathVariable String pluginId,
                                                 HttpServletRequest request) {
        if (!requireAdmin(request)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "需要管理员权限"));
        }
        try {
            pendingPluginService.deletePendingPlugin(pluginId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "待审核插件已删除");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /** 从请求中获取提交者用户名 */
    private String getSubmitter(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null && sessionManager.validate(token)) {
            return sessionManager.getUsername(token);
        }
        return "anonymous";
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
