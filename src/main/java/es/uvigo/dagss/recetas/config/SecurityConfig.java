package es.uvigo.dagss.recetas.config;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import es.uvigo.dagss.recetas.entities.UserType;
import es.uvigo.dagss.recetas.security.AuthenticatedUser;
import es.uvigo.dagss.recetas.security.JwtService;
import es.uvigo.dagss.recetas.security.RestSecurityErrorHandler;

/**
 * Stateless JWT authentication, BCrypt hashing and role-based access to every endpoint;
 * anything not explicitly allowed is denied. Rules that depend on which record is accessed
 * live in security.AccessPolicy; the complete matrix is in doc/seguridad.md.
 */
@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private static final String ADMIN = "ADMINISTRATOR";
    private static final String DOCTOR = "DOCTOR";
    private static final String PATIENT = "PATIENT";
    private static final String PHARMACY = "PHARMACY";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * HMAC key used to sign and verify the tokens. If no secret is configured a
     * random one is generated on every start (so tokens do not survive a restart).
     */
    @Bean
    public SecretKey jwtSigningKey(@Value("${app.security.jwt-secret:}") String secret) {
        byte[] bytes;
        if (secret == null || secret.isBlank()) {
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
            log.warn("app.security.jwt-secret (variable JWT_SECRET) no está configurado: se usa una clave aleatoria "
                    + "y las sesiones se perderán al reiniciar la aplicación");
        } else {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < 32) {
                throw new IllegalStateException("app.security.jwt-secret debe tener al menos 32 caracteres");
            }
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSigningKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSigningKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    /** Turns a verified token into the authentication of the request: principal = id + role. */
    public static AbstractAuthenticationToken toAuthentication(Jwt jwt) {
        try {
            Long id = Long.valueOf(jwt.getClaimAsString(JwtService.CLAIM_USER_ID));
            UserType role = UserType.valueOf(jwt.getClaimAsString(JwtService.CLAIM_ROLE));
            return new UsernamePasswordAuthenticationToken(new AuthenticatedUser(id, role), jwt,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
        } catch (RuntimeException e) {
            throw new BadCredentialsException("El token no contiene un usuario y un rol válidos", e);
        }
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:*}") String allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Arrays.stream(allowedOrigins.split(",")).map(String::trim)
                .filter(origin -> !origin.isEmpty()).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, RestSecurityErrorHandler errorHandler)
            throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // stateless API, no cookies: nothing for CSRF to abuse
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(handling -> handling
                    .authenticationEntryPoint(errorHandler)
                    .accessDeniedHandler(errorHandler))
            .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(SecurityConfig::toAuthentication))
                    .authenticationEntryPoint(errorHandler)
                    .accessDeniedHandler(errorHandler))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(antMatcher(HttpMethod.OPTIONS, "/**")).permitAll()
                .requestMatchers(antMatcher("/error")).permitAll()
                .requestMatchers(antMatcher(HttpMethod.POST, "/api/auth/login")).permitAll()
                .requestMatchers(antMatcher("/swagger-ui.html"), antMatcher("/swagger-ui/**"),
                        antMatcher("/v3/api-docs/**")).permitAll()

                .requestMatchers(antMatcher("/api/administrators/**")).hasRole(ADMIN)

                .requestMatchers(antMatcher(HttpMethod.GET, "/api/health-centers/**"),
                        antMatcher(HttpMethod.GET, "/api/medications/**")).authenticated()
                .requestMatchers(antMatcher("/api/health-centers/**"),
                        antMatcher("/api/medications/**")).hasRole(ADMIN)

                .requestMatchers(antMatcher(HttpMethod.PATCH, "/api/doctors/*/profile"),
                        antMatcher(HttpMethod.PATCH, "/api/doctors/*/password")).hasRole(DOCTOR)
                .requestMatchers(antMatcher(HttpMethod.GET, "/api/doctors/*"),
                        antMatcher(HttpMethod.GET, "/api/doctors/*/schedule")).hasAnyRole(ADMIN, DOCTOR)
                .requestMatchers(antMatcher("/api/doctors/**")).hasRole(ADMIN)

                .requestMatchers(antMatcher(HttpMethod.PATCH, "/api/patients/*/profile"),
                        antMatcher(HttpMethod.PATCH, "/api/patients/*/password"),
                        antMatcher(HttpMethod.GET, "/api/patients/*/available-slots"),
                        antMatcher(HttpMethod.GET, "/api/patients/*/refills")).hasRole(PATIENT)
                .requestMatchers(antMatcher(HttpMethod.GET, "/api/patients/*/appointments")).hasAnyRole(ADMIN, PATIENT)
                .requestMatchers(antMatcher(HttpMethod.GET, "/api/patients/*/prescriptions")).hasAnyRole(PATIENT, DOCTOR)
                .requestMatchers(antMatcher(HttpMethod.GET, "/api/patients/*")).hasAnyRole(ADMIN, PATIENT, DOCTOR)
                .requestMatchers(antMatcher("/api/patients/**")).hasRole(ADMIN)

                .requestMatchers(antMatcher(HttpMethod.PATCH, "/api/pharmacies/*/profile"),
                        antMatcher(HttpMethod.PATCH, "/api/pharmacies/*/password")).hasRole(PHARMACY)
                .requestMatchers(antMatcher(HttpMethod.GET, "/api/pharmacies/*")).hasAnyRole(ADMIN, PHARMACY)
                .requestMatchers(antMatcher("/api/pharmacies/**")).hasRole(ADMIN)

                .requestMatchers(antMatcher(HttpMethod.GET, "/api/appointments")).hasRole(ADMIN)
                .requestMatchers(antMatcher(HttpMethod.POST, "/api/appointments")).hasRole(PATIENT)
                .requestMatchers(antMatcher(HttpMethod.POST, "/api/appointments/*/cancel"))
                        .hasAnyRole(ADMIN, PATIENT, DOCTOR)
                .requestMatchers(antMatcher(HttpMethod.POST, "/api/appointments/*/no-show"),
                        antMatcher(HttpMethod.POST, "/api/appointments/*/complete")).hasRole(DOCTOR)

                .requestMatchers(antMatcher(HttpMethod.POST, "/api/prescriptions"),
                        antMatcher(HttpMethod.POST, "/api/prescriptions/*/cancel")).hasRole(DOCTOR)
                .requestMatchers(antMatcher(HttpMethod.GET, "/api/refills"),
                        antMatcher(HttpMethod.POST, "/api/refills/*/fill")).hasRole(PHARMACY)

                .anyRequest().denyAll());
        return http.build();
    }
}
