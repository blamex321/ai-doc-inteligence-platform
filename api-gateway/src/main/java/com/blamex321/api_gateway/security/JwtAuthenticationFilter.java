package com.blamex321.api_gateway.security;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtUtil jwtUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        // 1. Strip any client-supplied spoofed internal headers
        ServerWebExchange cleanedExchange = exchange.mutate()
                .request(r -> r.headers(h -> {
                    h.remove("X-User-Email");
                    h.remove("X-User-Role");
                }))
                .build();

        // 2. Allow public endpoints
        if (isPublicPath(path)) {
            return chain.filter(cleanedExchange);
        }

        // 3. Check Authorization header
        String authHeader = cleanedExchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorizedResponse(cleanedExchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7).trim();

        if (!jwtUtil.validateToken(token)) {
            return unauthorizedResponse(cleanedExchange, "JWT token is expired or invalid");
        }

        String email = jwtUtil.extractEmail(token);
        String role = jwtUtil.extractRole(token);

        // 4. Inject verified user context headers for downstream microservices
        ServerWebExchange mutatedExchange = cleanedExchange.mutate()
                .request(builder -> builder
                        .header("X-User-Email", email)
                        .header("X-User-Role", role)
                )
                .build();

        return chain.filter(mutatedExchange);
    }

    private boolean isPublicPath(String path) {
        return path.equals("/auth/login") || path.equals("/auth/register")
                || path.startsWith("/actuator") || path.equals("/actuator")
                || path.equals("/") || path.equals("/health") || path.startsWith("/health");
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String json = String.format(
                "{\"timestamp\":\"%s\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"%s\",\"path\":\"%s\"}",
                LocalDateTime.now(), message, exchange.getRequest().getURI().getPath()
        );

        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(json.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}