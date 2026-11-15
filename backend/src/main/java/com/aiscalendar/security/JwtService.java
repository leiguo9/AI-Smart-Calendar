package com.aiscalendar.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Create and verify JWTs (JJWT library).
 *
 * <p>We put the user id in {@code sub} and email as a custom claim. The HMAC secret comes from
 * {@code app.jwt.secret} / {@code APP_JWT_SECRET} — must be long enough for the algorithm.
 */
@Service
public class JwtService {

  private final JwtProperties properties;
  private final SecretKey key;

  public JwtService(JwtProperties properties) {
    this.properties = properties;
    this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
  }

  public String generateToken(UUID userId, String email) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + properties.getExpirationMs());
    return Jwts.builder()
        .subject(userId.toString())
        .claim("email", email)
        .issuedAt(now)
        .expiration(expiry)
        .signWith(key)
        .compact();
  }

  public UUID extractUserId(String token) {
    return UUID.fromString(parseClaims(token).getSubject());
  }

  public boolean isValid(String token) {
    try {
      Claims claims = parseClaims(token);
      return claims.getExpiration().after(new Date());
    } catch (Exception ex) {
      // Bad signature, malformed token, etc. → treat as unauthenticated.
      return false;
    }
  }

  private Claims parseClaims(String token) {
    // verifyWith checks the signature; parseSignedClaims fails if tampered.
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
