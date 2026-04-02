package io.github.eendroroy.fuelquota.security;

import io.github.eendroroy.fuelquota.config.AppProperties;
import io.github.eendroroy.fuelquota.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final AppProperties appProperties;
    private final SecretKey secretKey;

    public JwtTokenProvider(AppProperties appProperties) {
        this.appProperties = appProperties;
        this.secretKey = Keys.hmacShaKeyFor(appProperties.getJwt().getSecret().getBytes());
    }

    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + appProperties.getJwt().getExpirationMs());

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("name", user.getName())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public String generateQrToken(UUID vehicleId, String registrationNumber) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + appProperties.getJwt().getQrExpirationMs());

        return Jwts.builder()
                .subject(vehicleId.toString())
                .claim("type", "QR_TOKEN")
                .claim("registrationNumber", registrationNumber)
                .claim("nonce", UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID getUserIdFromToken(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public String getNameFromToken(String token) {
        return parseClaims(token).get("name", String.class);
    }

    public String getEmailFromToken(String token) {
        return parseClaims(token).get("email", String.class);
    }

    public String getRoleFromToken(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public UUID getVehicleIdFromQrToken(String qrToken) {
        Claims claims = parseClaims(qrToken);
        String type = claims.get("type", String.class);
        if (!"QR_TOKEN".equals(type)) {
            throw new IllegalArgumentException("Invalid QR token type");
        }
        return UUID.fromString(claims.getSubject());
    }

    public String getRegistrationNumberFromQrToken(String qrToken) {
        return parseClaims(qrToken).get("registrationNumber", String.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (SecurityException ex) {
            logger.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty");
        }
        return false;
    }

    public Claims getClaimsFromToken(String token) {
        return parseClaims(token);
    }

    public boolean isTokenExpired(String token) {
        try {
            return parseClaims(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public Map<String, Object> parseToken(String token) {
        Claims claims = parseClaims(token);
        return Map.of(
                "userId", claims.getSubject(),
                "email", claims.get("email", String.class),
                "name", claims.get("name", String.class),
                "role", claims.get("role", String.class),
                "issuedAt", claims.getIssuedAt(),
                "expiresAt", claims.getExpiration()
        );
    }
}
