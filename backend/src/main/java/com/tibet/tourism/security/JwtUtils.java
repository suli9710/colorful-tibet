package com.tibet.tourism.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;

@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);
    private static final int MIN_SECRET_LENGTH = 64;
    private static final int MIN_PROD_SECRET_LENGTH = 64;
    private static final int MIN_EXPIRATION_MS = 5 * 60 * 1000;
    private static final int MAX_EXPIRATION_MS = 7 * 24 * 60 * 60 * 1000;
    private static final String DEV_SECRET_MARKER = "dev-only";
    private static final String CHANGE_ME_MARKER = "change-me";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private int jwtExpirationMs;

    @Value("${app.security.require-strong-secrets:false}")
    private boolean requireStrongSecrets;

    @Autowired
    private Environment environment;

    @PostConstruct
    public void validateJwtConfiguration() {
        String secret = jwtSecret == null ? "" : jwtSecret.trim();
        boolean prodProfile = environment != null
                && Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equalsIgnoreCase);
        boolean strictMode = requireStrongSecrets || prodProfile;

        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("JWT secret must be configured");
        }
        jwtSecret = secret;
        if (jwtExpirationMs < MIN_EXPIRATION_MS || jwtExpirationMs > MAX_EXPIRATION_MS) {
            throw new IllegalStateException("JWT expiration must be between 5 minutes and 7 days");
        }
        if (secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("JWT secret must be at least " + MIN_SECRET_LENGTH + " characters");
        }

        String lowerSecret = secret.toLowerCase();
        boolean looksLikeDevSecret = lowerSecret.contains(DEV_SECRET_MARKER) || lowerSecret.contains(CHANGE_ME_MARKER);
        if (strictMode) {
            if (secret.length() < MIN_PROD_SECRET_LENGTH) {
                throw new IllegalStateException("Production JWT secret must be at least "
                        + MIN_PROD_SECRET_LENGTH + " characters");
            }
            if (looksLikeDevSecret) {
                throw new IllegalStateException("Production JWT secret cannot use the development placeholder");
            }
        } else if (looksLikeDevSecret) {
            logger.warn("Using development JWT secret placeholder; configure JWT_SECRET before shared deployment");
        }
    }

    public String generateJwtToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        return parseClaims(token).getPayload().getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            parseClaims(authToken);
            return true;
        } catch (SignatureException e) {
            logger.warn("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.warn("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.debug("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.warn("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.warn("JWT claims string is empty: {}", e.getMessage());
        } catch (Exception e) {
            logger.warn("JWT validation failed: {}", e.getMessage());
        }
        return false;
    }

    private Jws<Claims> parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
