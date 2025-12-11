package com.example.platform.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class AuthErrorFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(AuthErrorFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange)
                .onErrorResume(throwable -> {
                    logger.error("=== AUTHENTICATION ERROR ===");
                    logger.error("Path: {}", exchange.getRequest().getURI().getPath());
                    logger.error("Method: {}", exchange.getRequest().getMethod());
                    logger.error("Error: {}", throwable.getMessage());
                    logger.error("Error class: {}", throwable.getClass().getName());
                    throwable.printStackTrace();
                    logger.error("=============================");
                    
                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    response.getHeaders().add("Content-Type", "application/json");
                    
                    String errorBody = "{\"error\":\"authentication_failed\",\"message\":\"" + 
                        throwable.getMessage() + "\"}";
                    DataBuffer buffer = response.bufferFactory()
                        .wrap(errorBody.getBytes(StandardCharsets.UTF_8));
                    return response.writeWith(Mono.just(buffer));
                });
    }

    @Override
    public int getOrder() {
        return 100; // Выполняется после Security
    }
}




