package com.tools.manager.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SPA 路由控制器
 * 将前端路由路径转发到 index.html，支持 Vue Router history 模式
 * （仅在生产环境打包部署时生效，开发环境由 Vite 代理处理）
 */
@Controller
public class SpaController {

    /**
     * 前端路由路径转发到 index.html
     * /api/** 路径不受影响，由 REST Controller 处理
     */
    @GetMapping({
        "/dashboard",
        "/dashboard/**",
        "/commands",
        "/commands/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
