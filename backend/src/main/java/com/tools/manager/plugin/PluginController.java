package com.tools.manager.plugin;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plugins")
public class PluginController {

    private final PluginService pluginService;

    public PluginController(PluginService pluginService) {
        this.pluginService = pluginService;
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
                                          @RequestParam(value = "force", defaultValue = "false") boolean force) {
        try {
            PluginEntity entity = pluginService.uploadPlugin(file, force);
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
    public ResponseEntity<Map<String, Object>> deletePlugin(@PathVariable String pluginId) {
        try {
            pluginService.deletePlugin(pluginId);
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
