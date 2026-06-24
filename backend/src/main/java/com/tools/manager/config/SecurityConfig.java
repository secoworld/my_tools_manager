package com.tools.manager.config;

import com.tools.manager.auth.SessionManager;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final SessionManager sessionManager;

    private static final Set<String> PUBLIC_EXACT_PATHS = Set.of(
            "/api/admin/login",
            "/api/plugins",
            "/api/plugins/submit",
            "/",
            "/index.html",
            "/favicon.ico"
    );

    private static final Set<String> PUBLIC_PREFIXES = Set.of(
            "/api/tools/",
            "/api/commands/",
            "/assets/",
            "/vendor/",
            "/h2-console/"
    );

    public SecurityConfig(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/admin/login",
                                "/api/tools/**",
                                "/api/commands/**",
                                "/api/plugins",
                                "/api/plugins/submit",
                                "/api/plugins/*/content",
                                "/",
                                "/index.html",
                                "/favicon.ico",
                                "/assets/**",
                                "/vendor/**",
                                "/h2-console/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .addFilterBefore(tokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public Filter tokenAuthenticationFilter() {
        return new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                HttpServletRequest httpRequest = (HttpServletRequest) request;
                HttpServletResponse httpResponse = (HttpServletResponse) response;

                String path = httpRequest.getRequestURI();
                String method = httpRequest.getMethod();

                // 放行 OPTIONS 预检请求
                if ("OPTIONS".equalsIgnoreCase(method)) {
                    chain.doFilter(request, response);
                    return;
                }

                if (isPublicPath(path)) {
                    chain.doFilter(request, response);
                    return;
                }

                String authHeader = httpRequest.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    if (sessionManager.validate(token)) {
                        // 设置 Spring Security 认证对象
                        String username = sessionManager.getUsername(token);
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        username, null,
                                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        chain.doFilter(request, response);
                        return;
                    }
                }

                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.setContentType("application/json;charset=UTF-8");
                httpResponse.getWriter().write("{\"success\":false,\"message\":\"未登录或登录已过期\"}");
            }
        };
    }

    private boolean isPublicPath(String path) {
        if (path == null) {
            return false;
        }
        // 精确匹配
        if (PUBLIC_EXACT_PATHS.contains(path)) {
            return true;
        }
        // 前缀匹配
        for (String prefix : PUBLIC_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        // 插件内容路径: /api/plugins/{pluginId}/content
        if (path.matches("^/api/plugins/[^/]+/content$")) {
            return true;
        }
        return false;
    }
}
