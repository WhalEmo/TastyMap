package com.beem.TastyMap.security.servletFilter;

import com.beem.TastyMap.redis.RedisRateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);
    private final RedisRateLimitService rateLimitService;

    public RateLimitingFilter(RedisRateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }


    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();

        return path.startsWith("/ws/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);
        String rateLimitKey;
        boolean isAuthenticated = false;

        if (token != null && token.length() > 20) {
            rateLimitKey = "auth:" + hashString(token);
            isAuthenticated = true;
        } else {
            rateLimitKey = "ip:" + getSafeSocketIp(request);
        }

        boolean allowed = rateLimitService.tryConsume(rateLimitKey, isAuthenticated);

        if (!allowed) {
            log.warn("[RateLimit BLOCKED] IP/Token kilitlendi -> URI: {} | Method: {} | Key: {} | Auth: {}",
                    request.getRequestURI(), request.getMethod(), rateLimitKey, isAuthenticated);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader("Retry-After", "30");
            response.getWriter().write("""
                {
                  "status": 429,
                  "error": "Too Many Requests",
                  "message": "Çok fazla istek attınız. Lütfen kısa bir süre bekleyin."
                }
                """);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        // A. Mobil için Header kontrolü
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }

        // B. Web için Cookie kontrolü
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                String validCookieName = "access_token";
                if (validCookieName.equalsIgnoreCase(cookie.getName())) {
                    String value = cookie.getValue();
                    if (value != null && !value.isBlank()) {
                        return value.trim();
                    }
                }
            }
        }

        return null;
    }

    private String getSafeSocketIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        if (remoteAddr == null || remoteAddr.isBlank()) {
            return "unknown";
        }

        // IPv6 Localhost adreslerini IPv4 standardına normalize et
        if ("0:0:0:0:0:0:0:1".equals(remoteAddr) || "::1".equals(remoteAddr)) {
            return "127.0.0.1";
        }

        return remoteAddr;
    }

    private String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }
}