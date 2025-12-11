package com.example.platform.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Value("${services.resource-service.url:http://resource-service:8081}")
    private String resourceServiceUrl;

    @Value("${services.booking-service.url:http://booking-service:8082}")
    private String bookingServiceUrl;

    @Value("${services.notification-service.url:http://notification-service:8083}")
    private String notificationServiceUrl;

    @Value("${services.keycloak.url:http://keycloak:8080}")
    private String keycloakUrl;

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("resource-service", r -> r.path("/resources", "/resources/**").uri(resourceServiceUrl))
                .route("file-service", r -> r.path("/files", "/files/**").uri(resourceServiceUrl))
                .route("booking-service", r -> r.path("/bookings", "/bookings/**").uri(bookingServiceUrl))
                .route("notification-service", r -> r.path("/notifications/**").uri(notificationServiceUrl))
                .route("keycloak-auth", r -> r.path("/auth/**")
                        .filters(f -> f.rewritePath("/auth/(?<segment>.*)", "/${segment}"))
                        .uri(keycloakUrl))
                .build();
    }
}

