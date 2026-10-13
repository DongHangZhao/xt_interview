package com.xt.interview.filter;

import com.xt.interview.entity.Account;
import com.xt.interview.repository.AccountRepository;
import com.xt.interview.security.LoginUser;
import com.xt.interview.security.UserContext;
import com.xt.interview.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AccountRepository accountRepository;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final Integer NOT_DELETED = 0;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return pathMatcher.match("/api/auth/**", path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String servletPath = request.getServletPath();
        String authorization = request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            sendErrorResponse(response, 401, "未授权访问，请先登录");
            return;
        }

        String token = authorization.substring(7);

        try {
            boolean valid = jwtUtil.isTokenValid(token);
            if (!valid) {
                sendErrorResponse(response, 401, "token已过期，请重新登录");
                return;
            }

            String username = jwtUtil.getUsernameFromToken(token);
            Long userId = jwtUtil.getUserIdFromToken(token);
            Integer roleLevel = jwtUtil.getRoleLevelFromToken(token);

            request.setAttribute("userId", userId);
            request.setAttribute("username", username);
            request.setAttribute("roleLevel", roleLevel);

            /**
             * 🔴 权限不能只信 JWT 里的 claim。
             * JWT 签发后到过期前，用户权限可能被管理员调整（或账号被软删除），
             * 若直接用 token 里的旧 roleLevel 判权，改权限后旧 token 仍持有高权限。
             * 因此每次请求都回库查一次实时权限，成本是一次主键查询，可接受。
             */
            Account account = accountRepository.findByIdAndDeleted(userId, NOT_DELETED)
                    .orElse(null);
            if (account == null) {
                sendErrorResponse(response, 401, "账号不存在或已删除，请重新登录");
                return;
            }
            if (account.getStatus() != null && account.getStatus() != 1) {
                sendErrorResponse(response, 403, "账号已被禁用");
                return;
            }

            UserContext.set(new LoginUser(userId, username,
                    account.getRoleLevel(), account.getIsSuper()));

        } catch (Exception e) {
            sendErrorResponse(response, 401, "token无效，请重新登录");
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // 必须在请求结束时清理：Tomcat 线程池会复用线程，
            // 不清理会导致下一个请求拿到上一个用户的权限（严重的越权风险）
            UserContext.clear();
        }
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");

        String escapedMessage = escapeJson(message);
        String jsonResponse = String.format(
            "{\"success\":false,\"message\":\"%s\",\"code\":%d}",
            escapedMessage, status
        );

        response.getWriter().write(jsonResponse);
    }

    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"': sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}
