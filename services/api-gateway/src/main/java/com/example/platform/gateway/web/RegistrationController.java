package com.example.platform.gateway.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class RegistrationController {

    private final WebClient webClient;
    private final String keycloakUrl;

    public RegistrationController(@Value("${services.keycloak.url:http://keycloak:8080}") String keycloakUrl) {
        this.keycloakUrl = keycloakUrl;
        this.webClient = WebClient.builder().build();
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<Map<String, String>>> register(@RequestBody Map<String, String> userData) {
        // Получаем admin token
        return webClient.post()
                .uri(keycloakUrl + "/realms/master/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("client_id", "admin-cli")
                        .with("username", "admin")
                        .with("password", "admin")
                        .with("grant_type", "password"))
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(tokenResponse -> {
                    String adminToken = (String) tokenResponse.get("access_token");
                    
                    // Создаем пользователя
                    Map<String, Object> user = new HashMap<>();
                    user.put("username", userData.get("username"));
                    user.put("email", userData.get("email"));
                    user.put("firstName", userData.getOrDefault("firstName", ""));
                    user.put("lastName", userData.getOrDefault("lastName", ""));
                    user.put("enabled", true);
                    user.put("emailVerified", true);
                    
                    Map<String, Object> credential = new HashMap<>();
                    credential.put("type", "password");
                    credential.put("value", userData.get("password"));
                    credential.put("temporary", false);
                    user.put("credentials", List.of(credential));
                    user.put("requiredActions", List.of());

                    return webClient.post()
                            .uri(keycloakUrl + "/admin/realms/app/users")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(user)
                            .retrieve()
                            .toBodilessEntity()
                            .map(response -> {
                                if (response.getStatusCode().is2xxSuccessful()) {
                                    return ResponseEntity.ok(Map.of("message", "User registered successfully"));
                                } else {
                                    return ResponseEntity.status(response.getStatusCode())
                                            .body(Map.of("error", "Registration failed"));
                                }
                            })
                            .onErrorResume(error -> {
                                return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body(Map.of("error", "User already exists or invalid data")));
                            });
                })
                .onErrorResume(error -> {
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", error.getMessage() != null ? error.getMessage() : "Registration failed")));
                });
    }
}
