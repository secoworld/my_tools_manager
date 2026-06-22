package com.tools.manager.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class PluginService {

    private static final Logger log = LoggerFactory.getLogger(PluginService.class);

    private final PluginRepository pluginRepository;

    private final PluginValidator pluginValidator;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.plugins.dir}")
    private String pluginsDir;

    @Value("${app.plugins.max-size}")
    private long maxSize;

    @Value("${app.plugins.max-files}")
    private int maxFiles;

    public PluginService(PluginRepository pluginRepository, PluginValidator pluginValidator) {
        this.pluginRepository = pluginRepository;
        this.pluginValidator = pluginValidator;
    }

    @PostConstruct
    public void loadPluginsFromDisk() {
        try {
            Path pluginsPath = Paths.get(pluginsDir);
            if (!Files.exists(pluginsPath)) {
                Files.createDirectories(pluginsPath);
                log.info("插件目录已创建: {}", pluginsPath.toAbsolutePath());
                return;
            }

            int created = 0;
            int updated = 0;
            int disabled = 0;

            List<String> diskPluginIds = new ArrayList<>();
            try (Stream<Path> stream = Files.list(pluginsPath)) {
                List<Path> subDirs = new ArrayList<>();
                stream.filter(Files::isDirectory).forEach(subDirs::add);

                for (Path subDir : subDirs) {
                    Path manifestPath = subDir.resolve("manifest.json");
                    if (!Files.exists(manifestPath)) {
                        continue;
                    }
                    String manifestJson;
                    try {
                        manifestJson = Files.readString(manifestPath, StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        log.warn("读取 manifest 失败: {}", manifestPath, e);
                        continue;
                    }

                    JsonNode manifest;
                    try {
                        manifest = objectMapper.readTree(manifestJson);
                    } catch (Exception e) {
                        log.warn("解析 manifest 失败: {}", manifestPath, e);
                        continue;
                    }

                    JsonNode idNode = manifest.get("id");
                    if (idNode == null || idNode.asText().isBlank()) {
                        continue;
                    }
                    String pluginId = idNode.asText();
                    diskPluginIds.add(pluginId);

                    Optional<PluginEntity> existingOpt = pluginRepository.findByPluginId(pluginId);
                    if (existingOpt.isEmpty()) {
                        PluginEntity entity = parseManifestToEntity(manifest);
                        pluginRepository.save(entity);
                        created++;
                    } else {
                        PluginEntity existing = existingOpt.get();
                        existing.setName(textOrDefault(manifest, "name", existing.getName()));
                        existing.setVersion(textOrDefault(manifest, "version", existing.getVersion()));
                        existing.setDescription(textOrDefault(manifest, "description", existing.getDescription()));
                        existing.setIcon(textOrDefault(manifest, "icon", existing.getIcon()));
                        existing.setCategory(textOrDefault(manifest, "category", existing.getCategory()));
                        existing.setAuthor(textOrDefault(manifest, "author", existing.getAuthor()));
                        existing.setEntryFile(textOrDefault(manifest, "entryFile", existing.getEntryFile()));
                        JsonNode needBackendNode = manifest.get("needBackend");
                        if (needBackendNode != null && !needBackendNode.isNull()) {
                            existing.setNeedBackend(needBackendNode.asBoolean());
                        }
                        pluginRepository.save(existing);
                        updated++;
                    }
                }
            }

            // DB has, disk doesn't: disable
            List<PluginEntity> all = pluginRepository.findAll();
            for (PluginEntity entity : all) {
                if (!diskPluginIds.contains(entity.getPluginId()) && Boolean.TRUE.equals(entity.getEnabled())) {
                    entity.setEnabled(false);
                    pluginRepository.save(entity);
                    disabled++;
                }
            }

            log.info("插件加载完成: 新增={}, 更新={}, 禁用={}", created, updated, disabled);
        } catch (IOException e) {
            log.error("加载插件目录失败", e);
        }
    }

    @Transactional
    public PluginEntity uploadPlugin(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".zip")) {
            throw new RuntimeException("仅支持 ZIP 文件");
        }

        List<String> entryNames = new ArrayList<>();
        String manifestJson = null;

        try (InputStream is = file.getInputStream();
             ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name == null) {
                    continue;
                }
                if (name.contains("..")) {
                    throw new RuntimeException("ZIP 条目包含路径穿越字符: " + name);
                }
                entryNames.add(name);
                if (entryNames.size() > maxFiles) {
                    throw new RuntimeException("ZIP 文件数量超过上限: " + maxFiles);
                }
                if ("manifest.json".equals(name) || name.endsWith("/manifest.json")) {
                    manifestJson = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException("读取 ZIP 文件失败: " + e.getMessage(), e);
        }

        if (manifestJson == null) {
            throw new RuntimeException("ZIP 中未找到 manifest.json");
        }

        PluginValidator.ValidationResult validationResult = pluginValidator.validateManifest(manifestJson, entryNames);
        if (validationResult.isHasErrors()) {
            StringBuilder sb = new StringBuilder("插件校验失败: ");
            for (PluginValidator.ValidationItem item : validationResult.getItems()) {
                if ("ERROR".equals(item.getLevel())) {
                    sb.append(item.getMessage()).append("; ");
                }
            }
            throw new RuntimeException(sb.toString());
        }

        String pluginId;
        try {
            JsonNode manifest = objectMapper.readTree(manifestJson);
            pluginId = manifest.get("id").asText();
        } catch (Exception e) {
            throw new RuntimeException("解析 manifest 失败: " + e.getMessage(), e);
        }

        if (pluginRepository.findByPluginId(pluginId).isPresent()) {
            throw new RuntimeException("插件 ID 已存在: " + pluginId);
        }

        Path pluginDir = Paths.get(pluginsDir, pluginId);
        try {
            Files.createDirectories(pluginDir);
            try (InputStream is = file.getInputStream();
                 ZipInputStream zis = new ZipInputStream(is)) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        Path dir = pluginDir.resolve(entry.getName());
                        Files.createDirectories(dir);
                    } else {
                        Path target = pluginDir.resolve(entry.getName());
                        Files.createDirectories(target.getParent());
                        Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                    zis.closeEntry();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("解压 ZIP 文件失败: " + e.getMessage(), e);
        }

        try {
            JsonNode manifest = objectMapper.readTree(manifestJson);
            PluginEntity entity = parseManifestToEntity(manifest);
            return pluginRepository.save(entity);
        } catch (Exception e) {
            throw new RuntimeException("解析 manifest 并保存失败: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deletePlugin(String pluginId) {
        PluginEntity entity = pluginRepository.findByPluginId(pluginId)
                .orElseThrow(() -> new RuntimeException("插件不存在: " + pluginId));

        Path pluginDir = Paths.get(pluginsDir, pluginId);
        try {
            if (Files.exists(pluginDir)) {
                deleteRecursively(pluginDir);
            }
        } catch (IOException e) {
            log.warn("删除插件目录失败: {}", pluginDir, e);
        }

        pluginRepository.delete(entity);
    }

    @Transactional
    public PluginEntity togglePlugin(String pluginId, boolean enabled) {
        PluginEntity entity = pluginRepository.findByPluginId(pluginId)
                .orElseThrow(() -> new RuntimeException("插件不存在: " + pluginId));
        entity.setEnabled(enabled);
        return pluginRepository.save(entity);
    }

    @Transactional
    public PluginEntity updateVisibility(String pluginId, PluginVisibility visibility) {
        PluginEntity entity = pluginRepository.findByPluginId(pluginId)
                .orElseThrow(() -> new RuntimeException("插件不存在: " + pluginId));
        entity.setVisibility(visibility);
        return pluginRepository.save(entity);
    }

    public List<PluginEntity> getEnabledPlugins() {
        return pluginRepository.findByEnabledTrue();
    }

    public List<PluginEntity> getAllPlugins() {
        return pluginRepository.findAll();
    }

    public String getPluginContent(String pluginId) {
        PluginEntity entity = pluginRepository.findByPluginId(pluginId)
                .orElseThrow(() -> new RuntimeException("插件不存在: " + pluginId));
        Path entryPath = Paths.get(pluginsDir, pluginId, entity.getEntryFile());
        try {
            return Files.readString(entryPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("读取插件内容失败: " + e.getMessage(), e);
        }
    }

    private PluginEntity parseManifestToEntity(JsonNode manifest) {
        PluginEntity entity = new PluginEntity();
        entity.setPluginId(textOrEmpty(manifest, "id"));
        entity.setName(textOrEmpty(manifest, "name"));
        entity.setVersion(textOrEmpty(manifest, "version"));
        entity.setDescription(textOrEmpty(manifest, "description"));
        entity.setIcon(textOrEmpty(manifest, "icon"));
        entity.setCategory(textOrEmpty(manifest, "category"));
        entity.setAuthor(textOrEmpty(manifest, "author"));
        entity.setEntryFile(textOrEmpty(manifest, "entryFile"));
        JsonNode needBackendNode = manifest.get("needBackend");
        entity.setNeedBackend(needBackendNode != null && !needBackendNode.isNull() && needBackendNode.asBoolean());
        return entity;
    }

    private String textOrEmpty(JsonNode node, String field) {
        JsonNode child = node.get(field);
        if (child == null || child.isNull()) {
            return null;
        }
        return child.asText();
    }

    private String textOrDefault(JsonNode node, String field, String defaultValue) {
        JsonNode child = node.get(field);
        if (child == null || child.isNull() || child.asText().isBlank()) {
            return defaultValue;
        }
        return child.asText();
    }

    private void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> stream = Files.list(path)) {
                List<Path> children = new ArrayList<>();
                stream.forEach(children::add);
                for (Path child : children) {
                    deleteRecursively(child);
                }
            }
        }
        Files.deleteIfExists(path);
    }
}
