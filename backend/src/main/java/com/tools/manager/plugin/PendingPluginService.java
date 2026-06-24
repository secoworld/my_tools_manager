package com.tools.manager.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 待审核插件服务
 *
 * 处理非 admin 用户提交的插件：
 * - 提交：保存 ZIP 到 pending 目录，记录到 pending_plugin 表
 * - 相同 pluginId 的待审核插件会被覆盖（只保留最新）
 * - 待审核列表最多 100 个
 * - 审核通过：将插件解压到正式插件目录，保存到 custom_plugin 表
 * - 审核拒绝：删除待审核记录和 ZIP 文件
 */
@Service
public class PendingPluginService {

    private static final Logger log = LoggerFactory.getLogger(PendingPluginService.class);

    private static final int MAX_PENDING = 100;
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    private final PendingPluginRepository pendingPluginRepository;
    private final PluginRepository pluginRepository;
    private final PluginValidator pluginValidator;
    private final PluginService pluginService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.plugins.dir}")
    private String pluginsDir;

    /** 待审核插件 ZIP 存储目录 */
    @Value("${app.plugins.pending-dir:pending}")
    private String pendingDirName;

    public PendingPluginService(PendingPluginRepository pendingPluginRepository,
                                PluginRepository pluginRepository,
                                PluginValidator pluginValidator,
                                PluginService pluginService) {
        this.pendingPluginRepository = pendingPluginRepository;
        this.pluginRepository = pluginRepository;
        this.pluginValidator = pluginValidator;
        this.pluginService = pluginService;
    }

    private Path getPendingDir() {
        Path pendingDir = Paths.get(pluginsDir, pendingDirName);
        if (!Files.exists(pendingDir)) {
            try {
                Files.createDirectories(pendingDir);
            } catch (IOException e) {
                log.error("创建待审核插件目录失败: {}", pendingDir, e);
            }
        }
        return pendingDir;
    }

    /**
     * 提交插件（非 admin 用户）
     * 相同 pluginId 的待审核插件会被覆盖
     */
    @Transactional
    public PendingPluginEntity submitPlugin(MultipartFile file, String submitter) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".zip")) {
            throw new RuntimeException("仅支持 ZIP 文件");
        }

        // 解析 ZIP
        List<String> entryNames = new java.util.ArrayList<>();
        String manifestJson = null;

        try (InputStream is = file.getInputStream();
             ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name == null) continue;
                if (name.contains("..")) {
                    throw new RuntimeException("ZIP 条目包含路径穿越字符: " + name);
                }
                entryNames.add(name);
                if (entryNames.size() > 100) {
                    throw new RuntimeException("ZIP 文件数量超过上限");
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

        // 校验 manifest
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

        // 解析 manifest
        JsonNode manifest;
        String pluginId;
        try {
            manifest = objectMapper.readTree(manifestJson);
            pluginId = manifest.get("id").asText();
        } catch (Exception e) {
            throw new RuntimeException("解析 manifest 失败: " + e.getMessage(), e);
        }

        // 检查待审核数量上限（排除相同 id 的待审核插件，因为会被覆盖）
        long pendingCount = pendingPluginRepository.countByStatus(STATUS_PENDING);
        Optional<PendingPluginEntity> existingPending = pendingPluginRepository
                .findByPluginIdAndStatus(pluginId, STATUS_PENDING);
        if (existingPending.isEmpty() && pendingCount >= MAX_PENDING) {
            throw new RuntimeException("待审核插件数量已达上限 (" + MAX_PENDING + ")，请联系管理员及时审核");
        }

        // 保存 ZIP 文件到 pending 目录
        Path pendingDir = getPendingDir();
        String zipFileName = pluginId + "-" + System.currentTimeMillis() + ".zip";
        Path zipPath = pendingDir.resolve(zipFileName);
        try {
            Files.copy(file.getInputStream(), zipPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("保存 ZIP 文件失败: " + e.getMessage(), e);
        }

        // 如果存在相同 pluginId 的待审核插件，先删除旧的 ZIP 文件
        if (existingPending.isPresent()) {
            PendingPluginEntity old = existingPending.get();
            // 删除旧 ZIP 文件
            try {
                Path oldZip = pendingDir.resolve(old.getZipPath()).getFileName();
                Path oldZipPath = pendingDir.resolve(oldZip);
                Files.deleteIfExists(oldZipPath);
            } catch (IOException e) {
                log.warn("删除旧 ZIP 文件失败: {}", old.getZipPath(), e);
            }
            // 更新记录
            old.setName(textOrDefault(manifest, "name", old.getName()));
            old.setVersion(textOrDefault(manifest, "version", old.getVersion()));
            old.setDescription(textOrDefault(manifest, "description", old.getDescription()));
            old.setIcon(textOrDefault(manifest, "icon", old.getIcon()));
            old.setCategory(textOrDefault(manifest, "category", old.getCategory()));
            old.setAuthor(textOrDefault(manifest, "author", old.getAuthor()));
            old.setEntryFile(textOrDefault(manifest, "entryFile", old.getEntryFile()));
            JsonNode needBackendNode = manifest.get("needBackend");
            if (needBackendNode != null && !needBackendNode.isNull()) {
                old.setNeedBackend(needBackendNode.asBoolean());
            }
            old.setSubmitter(submitter);
            old.setZipPath(zipFileName);
            old.setStatus(STATUS_PENDING);
            old.setReviewComment(null);
            log.info("更新待审核插件: {} (提交者: {})", pluginId, submitter);
            return pendingPluginRepository.save(old);
        } else {
            // 新建待审核记录
            PendingPluginEntity entity = new PendingPluginEntity();
            entity.setPluginId(pluginId);
            entity.setName(textOrDefault(manifest, "name", pluginId));
            entity.setVersion(textOrDefault(manifest, "version", "1.0.0"));
            entity.setDescription(textOrDefault(manifest, "description", ""));
            entity.setIcon(textOrDefault(manifest, "icon", "Tools"));
            entity.setCategory(textOrDefault(manifest, "category", "自定义"));
            entity.setAuthor(textOrDefault(manifest, "author", ""));
            entity.setEntryFile(textOrDefault(manifest, "entryFile", "index.html"));
            JsonNode needBackendNode = manifest.get("needBackend");
            entity.setNeedBackend(needBackendNode != null && !needBackendNode.isNull() && needBackendNode.asBoolean());
            entity.setSubmitter(submitter);
            entity.setZipPath(zipFileName);
            entity.setStatus(STATUS_PENDING);
            log.info("新提交待审核插件: {} (提交者: {})", pluginId, submitter);
            return pendingPluginRepository.save(entity);
        }
    }

    /**
     * 获取所有待审核插件
     */
    public List<PendingPluginEntity> getPendingPlugins() {
        return pendingPluginRepository.findByStatusOrderBySubmittedAtDesc(STATUS_PENDING);
    }

    /**
     * 获取待审核插件的 ZIP 文件内容（用于预览）
     */
    public String getPendingPluginContent(String pluginId) {
        Optional<PendingPluginEntity> opt = pendingPluginRepository
                .findByPluginIdAndStatus(pluginId, STATUS_PENDING);
        if (opt.isEmpty()) {
            throw new RuntimeException("待审核插件不存在: " + pluginId);
        }
        PendingPluginEntity entity = opt.get();
        Path pendingDir = getPendingDir();
        Path zipPath = pendingDir.resolve(entity.getZipPath());
        if (!Files.exists(zipPath)) {
            throw new RuntimeException("待审核插件 ZIP 文件不存在");
        }

        String entryFile = entity.getEntryFile();
        // 从 ZIP 中读取 entryFile 内容
        try (InputStream is = Files.newInputStream(zipPath);
             ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.equals(entryFile) || name.endsWith("/" + entryFile)) {
                    return new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException("读取 ZIP 文件失败: " + e.getMessage(), e);
        }
        throw new RuntimeException("ZIP 中未找到入口文件: " + entryFile);
    }

    /**
     * 审核通过：将插件安装到正式插件目录
     */
    @Transactional
    public PluginEntity approvePlugin(String pluginId, String reviewComment) {
        Optional<PendingPluginEntity> opt = pendingPluginRepository
                .findByPluginIdAndStatus(pluginId, STATUS_PENDING);
        if (opt.isEmpty()) {
            throw new RuntimeException("待审核插件不存在: " + pluginId);
        }
        PendingPluginEntity pending = opt.get();

        Path pendingDir = getPendingDir();
        Path zipPath = pendingDir.resolve(pending.getZipPath());
        if (!Files.exists(zipPath)) {
            throw new RuntimeException("待审核插件 ZIP 文件不存在");
        }

        // 读取 ZIP 中的 manifest
        String manifestJson = null;
        try (InputStream is = Files.newInputStream(zipPath);
             ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("manifest.json".equals(entry.getName()) || entry.getName().endsWith("/manifest.json")) {
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

        // 解压到正式插件目录
        Path pluginDir = Paths.get(pluginsDir, pluginId);
        try {
            Files.createDirectories(pluginDir);
            try (InputStream is = Files.newInputStream(zipPath);
                 ZipInputStream zis = new ZipInputStream(is)) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        Files.createDirectories(pluginDir.resolve(entry.getName()));
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

        // 保存到 custom_plugin 表（如果已存在则更新，保留 enabled 和 visibility）
        JsonNode manifest;
        try {
            manifest = objectMapper.readTree(manifestJson);
        } catch (Exception e) {
            throw new RuntimeException("解析 manifest 失败: " + e.getMessage(), e);
        }

        PluginEntity pluginEntity;
        Optional<PluginEntity> existingOpt = pluginRepository.findByPluginId(pluginId);
        if (existingOpt.isPresent()) {
            // 更新已有插件：保留 enabled 和 visibility
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
            pluginEntity = pluginRepository.save(existing);
            log.info("审核通过，更新插件: {} v{}", pluginId, pluginEntity.getVersion());
        } else {
            // 新插件
            pluginEntity = new PluginEntity();
            pluginEntity.setPluginId(pluginId);
            pluginEntity.setName(textOrDefault(manifest, "name", pluginId));
            pluginEntity.setVersion(textOrDefault(manifest, "version", "1.0.0"));
            pluginEntity.setDescription(textOrDefault(manifest, "description", ""));
            pluginEntity.setIcon(textOrDefault(manifest, "icon", "Tools"));
            pluginEntity.setCategory(textOrDefault(manifest, "category", "自定义"));
            pluginEntity.setAuthor(textOrDefault(manifest, "author", ""));
            pluginEntity.setEntryFile(textOrDefault(manifest, "entryFile", "index.html"));
            pluginEntity.setEnabled(true);
            pluginEntity.setVisibility(PluginVisibility.ALL);
            JsonNode needBackendNode = manifest.get("needBackend");
            pluginEntity.setNeedBackend(needBackendNode != null && !needBackendNode.isNull() && needBackendNode.asBoolean());
            pluginEntity = pluginRepository.save(pluginEntity);
            log.info("审核通过，新增插件: {} v{}", pluginId, pluginEntity.getVersion());
        }

        // 更新待审核记录状态
        pending.setStatus(STATUS_APPROVED);
        pending.setReviewComment(reviewComment);
        pendingPluginRepository.save(pending);

        // 删除 ZIP 文件（已解压到正式目录）
        try {
            Files.deleteIfExists(zipPath);
        } catch (IOException e) {
            log.warn("删除待审核 ZIP 文件失败: {}", zipPath, e);
        }

        return pluginEntity;
    }

    /**
     * 审核拒绝：删除待审核记录和 ZIP 文件
     */
    @Transactional
    public void rejectPlugin(String pluginId, String reviewComment) {
        Optional<PendingPluginEntity> opt = pendingPluginRepository
                .findByPluginIdAndStatus(pluginId, STATUS_PENDING);
        if (opt.isEmpty()) {
            throw new RuntimeException("待审核插件不存在: " + pluginId);
        }
        PendingPluginEntity pending = opt.get();

        // 删除 ZIP 文件
        Path pendingDir = getPendingDir();
        Path zipPath = pendingDir.resolve(pending.getZipPath());
        try {
            Files.deleteIfExists(zipPath);
        } catch (IOException e) {
            log.warn("删除待审核 ZIP 文件失败: {}", zipPath, e);
        }

        // 更新状态为已拒绝
        pending.setStatus(STATUS_REJECTED);
        pending.setReviewComment(reviewComment);
        pendingPluginRepository.save(pending);

        log.info("拒绝插件: {} (原因: {})", pluginId, reviewComment);
    }

    /**
     * 删除待审核插件记录
     */
    @Transactional
    public void deletePendingPlugin(String pluginId) {
        Optional<PendingPluginEntity> opt = pendingPluginRepository
                .findByPluginIdAndStatus(pluginId, STATUS_PENDING);
        if (opt.isPresent()) {
            PendingPluginEntity pending = opt.get();
            // 删除 ZIP 文件
            Path pendingDir = getPendingDir();
            Path zipPath = pendingDir.resolve(pending.getZipPath());
            try {
                Files.deleteIfExists(zipPath);
            } catch (IOException e) {
                log.warn("删除待审核 ZIP 文件失败: {}", zipPath, e);
            }
            pendingPluginRepository.delete(pending);
        }
    }

    private String textOrDefault(JsonNode node, String field, String defaultValue) {
        JsonNode n = node.get(field);
        if (n == null || n.isNull() || n.asText().isBlank()) {
            return defaultValue;
        }
        return n.asText();
    }
}
