package com.tibet.tourism.security;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtils {

    @Value("${jwt.secret:tibetTourismSecretKey12345678901234567890}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private int jwtExpirationMs;

    public String generateJwtToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();

        return Jwts.builder()
                .setSubject((userPrincipal.getUsername()))
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody().getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
            System.out.println("=== [JwtUtils] Token验证成功");
            return true;
        } catch (SignatureException e) {
            System.err.println("=== [JwtUtils] Invalid JWT signature: " + e.getMessage());
            System.err.println("=== [JwtUtils] 使用的密钥长度: " + (jwtSecret != null ? jwtSecret.length() : 0));
        } catch (MalformedJwtException e) {
            System.err.println("=== [JwtUtils] Invalid JWT token: " + e.getMessage());
        } catch (ExpiredJwtException e) {
            System.err.println("=== [JwtUtils] JWT token is expired: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.err.println("=== [JwtUtils] JWT token is unsupported: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("=== [JwtUtils] JWT claims string is empty: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("=== [JwtUtils] JWT验证异常: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
}
