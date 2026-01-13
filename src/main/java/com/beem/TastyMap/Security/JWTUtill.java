package com.beem.TastyMap.Security;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

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


    public String generateAccessToken(UserDetails user){
        Date now=new Date();
        Date exp=new Date(now.getTime()+expirationMs);

        String role = user.getAuthorities()
                .iterator()
                .next()
                .getAuthority()
                .replace("ROLE_", "");


        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + refreshExp);

        return Jwts.builder()
                .setSubject(username)
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

    public String getUsername(String token) {
        return getClaims(token).getSubject();
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
