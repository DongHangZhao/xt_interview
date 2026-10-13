package com.xt.interview.security;

import com.xt.interview.enums.RoleLevel;

/**
 * 当前登录用户的权限上下文。
 * 由 JwtAuthenticationFilter 在校验通过后放入 SecurityContextHolder。
 *
 * 用 ThreadLocal 而非 SecurityContext 是因为本项目用自建 JWT 过滤器，
 * 未接入 Spring Security 的认证体系，ThreadLocal 更轻量直观。
 */
public class LoginUser {

    private Long userId;
    private String username;
    private Integer roleLevel;
    private Integer isSuper;

    public LoginUser() {}

    public LoginUser(Long userId, String username, Integer roleLevel, Integer isSuper) {
        this.userId = userId;
        this.username = username;
        this.roleLevel = roleLevel;
        this.isSuper = isSuper;
    }

    public boolean isAdminOrAbove() {
        return RoleLevel.isAdminOrAbove(roleLevel);
    }

    public boolean isSuperAdmin() {
        return roleLevel != null && roleLevel >= RoleLevel.SUPER_ADMIN.getCode();
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Integer getRoleLevel() { return roleLevel; }
    public void setRoleLevel(Integer roleLevel) { this.roleLevel = roleLevel; }

    public Integer getIsSuper() { return isSuper; }
    public void setIsSuper(Integer isSuper) { this.isSuper = isSuper; }
}
