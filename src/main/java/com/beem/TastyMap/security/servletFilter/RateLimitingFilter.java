package com.beem.TastyMap.security.servletFilter;

import com.beem.TastyMap.redis.RedisRateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RedisRateLimitService rateLimitService;
    private static final String AUTH_COOKIE_NAME = "access_token";

    public RateLimitingFilter(RedisRateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String rateLimitKey = null;
        boolean isAuthenticated = false;

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            rateLimitKey = authHeader.substring(7);
            isAuthenticated = true;
        }


        if (!isAuthenticated && request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (AUTH_COOKIE_NAME.equals(cookie.getName())) {
                    rateLimitKey = cookie.getValue();
                    isAuthenticated = true;
                    break; // Cookie'yi bulduk, döngüden çık
                }
            }
        }

        // 3. İkisi de yoksa Ziyaretçidir (IP'ye düş)
        if (!isAuthenticated) {
            rateLimitKey = request.getHeader("X-Forwarded-For");
            if (rateLimitKey == null || rateLimitKey.isEmpty()) {
                rateLimitKey = request.getRemoteAddr();
            }
        }

        if (!rateLimitService.tryConsume(rateLimitKey, isAuthenticated)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"status\": 429, \"error\": \"Too Many Requests\", \"message\": \"Cok fazla istek attiniz. Lutfen bekleyin.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}