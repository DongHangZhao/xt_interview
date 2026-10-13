package com.xt.interview.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 配置。
 *
 * 设计取舍说明：
 * 本项目的认证链路是「自建JwtAuthenticationFilter 解析 token → 写入 UserContext
 * → PermissionChecker 按业务规则判权」，这套链路已经完整可用。
 * 若再让 Spring Security 接管认证，会出现两层过滤器同时鉴权、
 * 规则重复且难以排查的问题。
 *
 * 因此这里的 SecurityFilterChain 采取「放行 + 保留基础设施」的策略：
 *   1. 放行所有请求，认证交给既有的 JWT 过滤器
 *   2. 保留 @EnableMethodSecurity，使 @PreAuthorize 注解可用，
 *      后续如需更细粒度的注解式鉴权可直接启用
 *   3. 关闭默认的 formLogin / httpBasic，避免弹出登录框
 *   4. 关闭 CSRF：项目为纯 token 鉴权的 REST API，无 Cookie 会话，CSRF 不适用
 *   5. 提供 PasswordEncoder Bean，供后续密码加密升级使用
 *      （历史数据为 MD5，贸然切换会导致老账号无法登录）
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 纯 token 鉴权的 REST API，不使用 Cookie 会话，关闭 CSRF
            .csrf(csrf -> csrf.disable())
            // 认证交给自建 JWT 过滤器，这里全部放行
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            // 关闭默认登录方式，避免出现框架自带的登录页
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout.disable());

        return http.build();
    }

    /**
     * 密码编码器。
     * 当前注册流程仍沿用历史 MD5 存储，此 Bean 预留给后续密码加密升级使用。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
