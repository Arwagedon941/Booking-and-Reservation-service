package com.example.platform.fileservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    /**
     * Кастомный JwtDecoder, который принимает токены с issuer как внутри Docker (keycloak:8080),
     * так и снаружи (localhost:8080 / localhost:8088). Без этого Keycloak возвращает issuer
     * по публичному URL, и сервисы отклоняют токен.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        String jwkSetUri = issuerUri + "/protocol/openid-connect/certs";
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        // Проверяем только срок действия (без строгой проверки issuer)
        OAuth2TokenValidator<Jwt> baseValidator = new JwtTimestampValidator();

        OAuth2TokenValidator<Jwt> issuerValidator = jwt -> {
            String tokenIssuer = jwt.getIssuer().toString();
            String expected = issuerUri; // http://keycloak:8080/realms/app
            String alt1 = issuerUri.replace("keycloak:8080", "localhost:8088"); // внешние обращения
            String alt2 = issuerUri.replace("keycloak:8080", "localhost:8080"); // порт 8080

            boolean ok = tokenIssuer.equals(expected)
                    || tokenIssuer.equals(alt1)
                    || tokenIssuer.equals(alt2)
                    || tokenIssuer.endsWith("/realms/app");

            if (ok) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Invalid issuer: " + tokenIssuer, null)
            );
        };

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(baseValidator, issuerValidator));
        return decoder;
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            java.util.List<GrantedAuthority> authorities = new java.util.ArrayList<>();

            Object realmAccess = jwt.getClaim("realm_access");
            if (realmAccess instanceof java.util.Map<?, ?> map) {
                Object roles = map.get("roles");
                if (roles instanceof java.util.Collection<?> col) {
                    for (Object r : col) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + r));
                    }
                }
            }

            Object resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess instanceof java.util.Map<?, ?> map) {
                for (Object val : map.values()) {
                    if (val instanceof java.util.Map<?, ?> rm) {
                        Object roles = rm.get("roles");
                        if (roles instanceof java.util.Collection<?> col) {
                            for (Object r : col) {
                                authorities.add(new SimpleGrantedAuthority("ROLE_" + r));
                            }
                        }
                    }
                }
            }

            return authorities;
        });
        return converter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(registry -> registry
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        ))
                .csrf(csrf -> csrf.disable());
        return http.build();
    }
}


