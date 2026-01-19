package com.beem.TastyMap.Security.Verification.ServletFilter;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JWTUtill {
    @Value("${jwt.access-secret}")
    private String secret;

    @Value("${jwt.access-exp-ms:900000}")
    private Long expirationMs;


    @Value("${jwt.refresh-secret}")
    private String refreshSecret;

    @Value("${jwt.refresh-exp-ms}")
    private long refreshExp;

    public JWTUtill(){}


    public String generateAccessToken(Long userId, String role) {

        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("role", role)
                .claim("type", "access")
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(
                        Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256
                )
                .compact();
    }

    public String generateRefreshToken(Long userId, String deviceId) {

        Date now = new Date();
        Date exp = new Date(now.getTime() + refreshExp);

        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("deviceId", deviceId)
                .claim("type", "refresh")
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(
                        Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256
                )
                .compact();
    }


    public boolean validateAccessToken(String token){
        try{
            Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseClaimsJws(token);
            return true;

        }catch (JwtException e){
            return false;
        }
    }

    public boolean validateRefreshToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        return Long.parseLong(
                getClaims(token).getSubject()
        );
    }

    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
