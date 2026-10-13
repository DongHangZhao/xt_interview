package com.xt.interview.security;

/**
 * ThreadLocal 存储当前登录用户。
 * 请求进入时由 JwtAuthenticationFilter 设置，请求结束时由过滤器清理，避免线程复用导致的信息泄漏。
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {}

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
