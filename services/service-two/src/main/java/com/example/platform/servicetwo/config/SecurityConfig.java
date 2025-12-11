package com.example.platform.servicetwo.config;

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
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    /**
     * Кастомный JwtDecoder, который принимает issuer как keycloak:8080, localhost:8088, localhost:8080
     * и любой issuer, оканчивающийся на /realms/app. Это устраняет 401 из-за несоответствия iss.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        String jwkSetUri = issuerUri + "/protocol/openid-connect/certs";
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        // Проверяем только срок действия (без строгой проверки issuer)
        OAuth2TokenValidator<Jwt> baseValidator = new JwtTimestampValidator();

        OAuth2TokenValidator<Jwt> issuerValidator = jwt -> {
            String tokenIssuer = jwt.getIssuer().toString();
            String expected = issuerUri;
            String alt1 = issuerUri.replace("keycloak:8080", "localhost:8088");
            String alt2 = issuerUri.replace("keycloak:8080", "localhost:8080");

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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(registry -> registry
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())))
                .csrf(csrf -> csrf.disable());
        return http.build();
    }
}


