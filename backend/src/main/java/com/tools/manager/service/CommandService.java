package com.tools.manager.service;

import com.tools.manager.entity.Command;
import com.tools.manager.entity.CommandModule;
import com.tools.manager.repository.CommandModuleRepository;
import com.tools.manager.repository.CommandRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommandService {

    private final CommandModuleRepository moduleRepository;

    private final CommandRepository commandRepository;

    public CommandService(CommandModuleRepository moduleRepository, CommandRepository commandRepository) {
        this.moduleRepository = moduleRepository;
        this.commandRepository = commandRepository;
    }

    public List<CommandModule> listModules() {
        return moduleRepository.findAll();
    }

    @Transactional
    public CommandModule createModule(CommandModule module) {
        if (module.getCreatedAt() == null) {
            module.setCreatedAt(LocalDateTime.now());
        }
        return moduleRepository.save(module);
    }

    @Transactional
    public void deleteModule(Long id) {
        List<Command> commands = commandRepository.findByModuleId(id);
        commandRepository.deleteAll(commands);
        moduleRepository.deleteById(id);
    }

    @Transactional
    public CommandModule updateModule(Long id, CommandModule module) {
        CommandModule existing = moduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("模块不存在: " + id));
        existing.setName(module.getName());
        existing.setDescription(module.getDescription());
        existing.setIcon(module.getIcon());
        existing.setSortOrder(module.getSortOrder());
        if (module.getFormat() != null) {
            existing.setFormat(module.getFormat());
        }
        return moduleRepository.save(existing);
    }

    public List<Command> listCommandsByModule(Long moduleId) {
        return commandRepository.findByModuleIdOrderBySortOrderAsc(moduleId);
    }

    @Transactional
    public Command createCommand(Command command) {
        if (command.getCreatedAt() == null) {
            command.setCreatedAt(LocalDateTime.now());
        }
        return commandRepository.save(command);
    }

    @Transactional
    public void deleteCommand(Long id) {
        commandRepository.deleteById(id);
    }

    @Transactional
    public Command updateCommand(Long id, Command command) {
        Command existing = commandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("命令不存在: " + id));
        existing.setModuleId(command.getModuleId());
        existing.setName(command.getName());
        existing.setCommand(command.getCommand());
        existing.setDescription(command.getDescription());
        existing.setCategory(command.getCategory());
        existing.setSortOrder(command.getSortOrder());
        existing.setContent(command.getContent());
        return commandRepository.save(existing);
    }

}
