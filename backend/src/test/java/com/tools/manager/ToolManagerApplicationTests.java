package com.tools.manager;

import com.tools.manager.service.CommandService;
import com.tools.manager.tool.ToolRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class ToolManagerApplicationTests {

    @Autowired
    private ToolRegistry toolRegistry;

    @Autowired
    private CommandService commandService;

    @Test
    void contextLoads() {
        assertNotNull(toolRegistry);
        assertNotNull(commandService);
    }

    @Test
    void listToolsShouldNotBeEmpty() {
        assertFalse(toolRegistry.listTools().isEmpty());
    }

    @Test
    void listModulesShouldWork() {
        assertNotNull(commandService.listModules());
    }

}
