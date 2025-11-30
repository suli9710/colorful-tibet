package com.tibet.tourism.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AuthTokenFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            String path = request.getServletPath();
            
            if (path.startsWith("/api/admin/")) {
                System.out.println("=== [AuthTokenFilter] 处理管理员请求: " + path);
                System.out.println("=== [AuthTokenFilter] JWT token存在: " + (jwt != null));
                if (jwt != null) {
                    System.out.println("=== [AuthTokenFilter] JWT token长度: " + jwt.length());
                    System.out.println("=== [AuthTokenFilter] JWT token前20字符: " + jwt.substring(0, Math.min(20, jwt.length())));
                }
            }
            
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                String username = jwtUtils.getUserNameFromJwtToken(jwt);
                System.out.println("=== [AuthTokenFilter] JWT验证成功，用户名: " + username);
                
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                if (path.startsWith("/api/admin/")) {
                    System.out.println("=== [AuthTokenFilter] 认证已设置，权限: " + userDetails.getAuthorities());
                }
            } else {
                if (path.startsWith("/api/admin/")) {
                    if (jwt == null) {
                        System.out.println("=== [AuthTokenFilter] 错误: JWT token为空");
                    } else {
                        System.out.println("=== [AuthTokenFilter] 错误: JWT token验证失败");
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e);
            System.err.println("=== [AuthTokenFilter] 异常: " + e.getMessage());
            e.printStackTrace();
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String servletPath = request.getServletPath();
        
        // 跳过公开API路径，不执行JWT验证
        // 注意：对于需要同时支持匿名和认证访问的接口（如查看评论），不应在此处跳过
        // 这里只跳过完全不需要用户上下文的公开接口，或者会导致401问题的特殊接口
        // /api/auth/me 和 /api/auth/me/** 需要认证，所以不跳过
        boolean skip = (servletPath.startsWith("/api/auth/") && 
                       !servletPath.startsWith("/api/auth/me")) ||
               servletPath.startsWith("/api/spots/") ||
               servletPath.startsWith("/api/news/") ||
               servletPath.startsWith("/api/heritage/") ||
               servletPath.equals("/api/routes/generate") || // 只跳过AI生成接口
               servletPath.startsWith("/api/routes/shared") || // 分享列表保持匿名访问
               servletPath.startsWith("/api/test/") ||
               servletPath.startsWith("/h2-console/");
        
        return skip;
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        
        // 调试日志：检查Authorization头
        if (request.getServletPath().startsWith("/api/admin/")) {
            System.out.println("=== [AuthTokenFilter] Authorization头: " + (headerAuth != null ? headerAuth.substring(0, Math.min(50, headerAuth.length())) + "..." : "null"));
        }

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            String token = headerAuth.substring(7);
            if (request.getServletPath().startsWith("/api/admin/")) {
                System.out.println("=== [AuthTokenFilter] 解析出的token长度: " + token.length());
            }
            return token;
        }

        return null;
    }
}
