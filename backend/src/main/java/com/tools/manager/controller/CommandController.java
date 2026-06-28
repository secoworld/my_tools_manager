package com.tools.manager.controller;

import com.tools.manager.entity.Command;
import com.tools.manager.entity.CommandModule;
import com.tools.manager.service.CommandService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/commands")
public class CommandController {

    private final CommandService commandService;

    public CommandController(CommandService commandService) {
        this.commandService = commandService;
    }

    @GetMapping("/modules")
    public List<CommandModule> listModules() {
        return commandService.listModules();
    }

    @PostMapping("/modules")
    public CommandModule createModule(@RequestBody CommandModule module) {
        return commandService.createModule(module);
    }

    @DeleteMapping("/modules/{id}")
    public void deleteModule(@PathVariable Long id) {
        commandService.deleteModule(id);
    }

    @PutMapping("/modules/{id}")
    public CommandModule updateModule(@PathVariable Long id, @RequestBody CommandModule module) {
        return commandService.updateModule(id, module);
    }

    @GetMapping("/module/{moduleId}")
    public List<Command> listCommandsByModule(@PathVariable Long moduleId) {
        return commandService.listCommandsByModule(moduleId);
    }

    @PostMapping
    public Command createCommand(@RequestBody Command command) {
        return commandService.createCommand(command);
    }

    @PutMapping("/{id}")
    public Command updateCommand(@PathVariable Long id, @RequestBody Command command) {
        return commandService.updateCommand(id, command);
    }

    @DeleteMapping("/{id}")
    public void deleteCommand(@PathVariable Long id) {
        commandService.deleteCommand(id);
    }

}
