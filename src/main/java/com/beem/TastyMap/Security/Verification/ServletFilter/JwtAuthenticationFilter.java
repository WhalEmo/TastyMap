package com.beem.TastyMap.Security.Verification.ServletFilter;

import com.beem.TastyMap.RegisterLogin.UserService;
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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String header=request.getHeader("Authorization");
        String token=null;

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        if(header != null && header.startsWith("Bearer ")){
            token=header.substring(7);
        }
        try {
            if (token != null && jwtUtill.validateAccessToken(token)) {
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
        }
        filterChain.doFilter(request, response);
    }
}
