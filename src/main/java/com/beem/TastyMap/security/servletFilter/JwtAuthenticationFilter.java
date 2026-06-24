package com.beem.TastyMap.security.servletFilter;

import com.beem.TastyMap.registerLogin.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JWTUtill jwtUtill;

    public JwtAuthenticationFilter(JWTUtill jwtUtill, UserService userService) {
        this.jwtUtill = jwtUtill;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        return path.startsWith("/api/users")
                || path.startsWith("/auth")
                || path.startsWith("/ws/auth");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // 1. ÖNEMLİ: Tarayıcının attığı gizli OPTIONS isteğini en başta yakala ve hemen 200 OK dön.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            filterChain.doFilter(request, response);
            return; // Metottan tamamen çık, aşağıdaki kodlar çalışmasın!
        }

        String header = request.getHeader("Authorization");
        String token = null;

        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
        } else if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }


        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {

            if (jwtUtill != null && jwtUtill.validateAccessToken(token)) {
                Long userId = jwtUtill.getUserId(token);
                String role = jwtUtill.getRole(token);

                var authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + role)
                );

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                authorities
                        );

                auth.setDetails(userId);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (Exception e) {
            logger.error("JWT Authentication hatası: ", e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Geçersiz Token");
            return;
        }

        filterChain.doFilter(request, response);
    }
}