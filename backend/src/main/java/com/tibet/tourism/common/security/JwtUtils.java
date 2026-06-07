package com.tibet.tourism.common.security;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);
    private static final int MIN_SECRET_LENGTH = 64;
    private static final int MIN_PROD_SECRET_LENGTH = 64;
    private static final int MIN_RANDOM_SECRET_BYTES = 64;
    private static final double MIN_PROD_SECRET_ENTROPY_BITS_PER_CHAR = 4.0;
    private static final int MIN_EXPIRATION_MS = 5 * 60 * 1000;
    private static final int MAX_EXPIRATION_MS = 7 * 24 * 60 * 60 * 1000;
    private static final String PUBLISHED_DEVELOPMENT_SECRET =
            "dev-only-jwt-secret-change-me-before-any-shared-deployment-2026";
    private static final String DEV_SECRET_MARKER = "dev-only";
    private static final String CHANGE_ME_MARKER = "change-me";
    private static final String SESSION_VERSION_CLAIM = "sv";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private int jwtExpirationMs;

    @Value("${jwt.issuer:colorful-tibet}")
    private String jwtIssuer = "colorful-tibet";

    @Value("${jwt.audience:colorful-tibet-web}")
    private String jwtAudience = "colorful-tibet-web";

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
        jwtIssuer = requireText(jwtIssuer, "JWT issuer must be configured");
        jwtAudience = requireText(jwtAudience, "JWT audience must be configured");
        if (jwtExpirationMs < MIN_EXPIRATION_MS || jwtExpirationMs > MAX_EXPIRATION_MS) {
            throw new IllegalStateException("JWT expiration must be between 5 minutes and 7 days");
        }
        if (PUBLISHED_DEVELOPMENT_SECRET.equals(secret)) {
            throw new IllegalStateException("JWT secret cannot use the published development placeholder");
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
            if (!hasStrongRandomMaterial(secret)) {
                throw new IllegalStateException("Production JWT secret must be high-entropy random material");
            }
        } else if (looksLikeDevSecret) {
            logger.warn("Using development JWT secret placeholder; configure JWT_SECRET before shared deployment");
        }
    }

    public String generateJwtToken(Authentication authentication) {
        return generateJwtToken(authentication, 0L);
    }

    public String generateJwtToken(Authentication authentication, long sessionVersion) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();

        return Jwts.builder()
                .issuer(jwtIssuer)
                .audience().add(jwtAudience).and()
                .subject(userPrincipal.getUsername())
                .claim(SESSION_VERSION_CLAIM, Math.max(0L, sessionVersion))
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        return parseClaims(token).getPayload().getSubject();
    }

    public Date getExpirationDateFromJwtToken(String token) {
        return parseClaims(token).getPayload().getExpiration();
    }

    public long getSessionVersionFromJwtToken(String token) {
        Object value = parseClaims(token).getPayload().get(SESSION_VERSION_CLAIM);
        if (value instanceof Number number) {
            return Math.max(0L, number.longValue());
        }
        if (value instanceof String text) {
            try {
                return Math.max(0L, Long.parseLong(text));
            } catch (NumberFormatException ex) {
                return 0L;
            }
        }
        return 0L;
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
                .requireIssuer(jwtIssuer)
                .requireAudience(jwtAudience)
                .build()
                .parseSignedClaims(token);
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    private String requireText(String value, String message) {
        String normalized = value == null ? "" : value.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalStateException(message);
        }
        return normalized;
    }

    private boolean hasStrongRandomMaterial(String secret) {
        byte[] decoded = decodeBase64Secret(secret);
        if (decoded != null) {
            return decoded.length >= MIN_RANDOM_SECRET_BYTES && hasByteVariety(decoded);
        }
        return shannonEntropy(secret) >= MIN_PROD_SECRET_ENTROPY_BITS_PER_CHAR && distinctCharacterCount(secret) >= 16;
    }

    private byte[] decodeBase64Secret(String secret) {
        for (Base64.Decoder decoder : new Base64.Decoder[]{
                Base64.getDecoder(),
                Base64.getUrlDecoder()
        }) {
            try {
                byte[] decoded = decoder.decode(secret);
                if (decoded.length > 0) {
                    return decoded;
                }
            } catch (IllegalArgumentException ignored) {
                // Try the next supported Base64 alphabet, then entropy fallback.
            }
        }
        return null;
    }

    private boolean hasByteVariety(byte[] bytes) {
        Set<Byte> unique = new HashSet<>();
        for (byte value : bytes) {
            unique.add(value);
        }
        return unique.size() >= 16;
    }

    private int distinctCharacterCount(String value) {
        return (int) value.chars().distinct().count();
    }

    private double shannonEntropy(String value) {
        int[] counts = new int[Character.MAX_VALUE + 1];
        for (int i = 0; i < value.length(); i++) {
            counts[value.charAt(i)]++;
        }
        double entropy = 0.0;
        for (int count : counts) {
            if (count == 0) {
                continue;
            }
            double probability = (double) count / value.length();
            entropy -= probability * (Math.log(probability) / Math.log(2));
        }
        return entropy;
    }
}
