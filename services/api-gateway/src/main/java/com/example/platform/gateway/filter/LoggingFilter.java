package com.example.platform.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().toString();
        HttpHeaders headers = request.getHeaders();
        String authHeader = headers.getFirst("Authorization");
        
        logger.info("=== REQUEST DEBUG ===");
        logger.info("Method: {}, Path: {}", method, path);
        logger.info("All headers: {}", headers);
        
        if (authHeader != null) {
            String tokenPreview = authHeader.length() > 50 
                ? authHeader.substring(0, 50) + "..." 
                : authHeader;
            logger.info("Authorization header present: {}", tokenPreview);
            logger.info("Authorization header length: {}", authHeader.length());
        } else {
            logger.error("!!! NO Authorization header in request !!!");
            logger.error("Available headers: {}", headers.keySet());
        }
        logger.info("====================");
        
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -100; // Выполняется очень первым, до Security
    }
}

