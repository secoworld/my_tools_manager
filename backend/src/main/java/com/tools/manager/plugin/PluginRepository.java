package com.tools.manager.plugin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PluginRepository extends JpaRepository<PluginEntity, Long> {

    Optional<PluginEntity> findByPluginId(String pluginId);

    List<PluginEntity> findByEnabledTrue();

    List<PluginEntity> findByEnabledTrueAndVisibilityIn(List<PluginVisibility> visibilities);

}
