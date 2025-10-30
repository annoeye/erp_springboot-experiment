package com.anno.ERP_SpringBoot_Experiment.component;

import com.anno.ERP_SpringBoot_Experiment.service.JwtService;
import com.anno.ERP_SpringBoot_Experiment.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpSessionHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final RedisService redisService;

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) throws Exception {
        if (request instanceof ServletServerHttpRequest serverHttpRequest) {
            String token = serverHttpRequest.getServletRequest().getParameter("token");

            if (token == null || token.isEmpty()) {
                String authHeader = serverHttpRequest.getServletRequest().getHeader(AUTH_HEADER);
                if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                    token = authHeader.substring(BEARER_PREFIX.length());
                }
            }

            if (token != null && !token.isEmpty()) {
                try {
                    String username = jwtService.extractUsername(token);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    String accessTokenKey = "access_token:" + token;
                    boolean isTokenActiveInRedis = redisService.hasKey(accessTokenKey);

                    if (jwtService.isTokenValid(token, userDetails) && isTokenActiveInRedis) {
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                        attributes.put("AUTHENTICATED_USER", authentication);
                        attributes.put("USERNAME", username);
                        attributes.put("EMAIL", userDetails.getUsername());

                        log.info("WebSocket Handshake - Người dùng '{}' đã được xác thực thành công", username);
                        return true;
                    } else {
                        log.info("WebSocket Handshake - Mã thông báo không hợp lệ hoặc đã hết hạn");
                        return false;
                    }
                } catch (Exception e) {
                    log.info("WebSocket Handshake - Lỗi xác thực: {}", e.getMessage());
                    return false;
                }
            } else {
                log.warn("WebSocket Handshake - Không cung cấp mã thông báo, cho phép kết nối ẩn danh");
                attributes.put("ANONYMOUS", true);
                return true;
            }
        }
        return false;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        if (exception != null) {
            log.error("webSocket Handshake không thành công: {}", exception.getMessage());
        } else {
            log.info("webSocket Handshake đã hoàn tất thành công");
        }
    }
}
