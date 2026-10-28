package es.uvigo.dagss.recetas.security;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import es.uvigo.dagss.recetas.entities.UserType;

/**
 * Issues the signed access tokens (JWT, HS256) returned by the login. A token
 * carries the username (subject), the user id and the role, and expires after
 * {@code app.security.jwt-ttl-minutes}. Verification is done by Spring
 * Security's resource server on every request.
 */
@Service
public class JwtService {

    public static final String CLAIM_USER_ID = "uid";
    public static final String CLAIM_ROLE = "role";

    public record IssuedToken(String value, Instant expiresAt) {
    }

    private final JwtEncoder encoder;
    private final Duration timeToLive;

    public JwtService(JwtEncoder encoder, @Value("${app.security.jwt-ttl-minutes:120}") long ttlMinutes) {
        this.encoder = encoder;
        this.timeToLive = Duration.ofMinutes(ttlMinutes);
    }

    public IssuedToken issue(Long userId, String username, UserType role) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(timeToLive);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(username)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(CLAIM_USER_ID, String.valueOf(userId))
                .claim(CLAIM_ROLE, role.name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }
}
