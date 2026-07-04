package com.beem.TastyMap.security.verification.servletFilter;

import com.beem.TastyMap.redis.RedisRateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RedisRateLimitService rateLimitService;

    public RateLimitingFilter(RedisRateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String rateLimitKey;
        boolean isAuthenticated = false;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            rateLimitKey = authHeader.substring(7);
            isAuthenticated = true;
        } else {
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