package com.anno.ERP_SpringBoot_Experiment.authenticated;

import com.anno.ERP_SpringBoot_Experiment.service.JwtService;
import com.anno.ERP_SpringBoot_Experiment.service.RedisService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final RedisService redisService;
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        final String jwt;
        final String userName;

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(BEARER_PREFIX.length());

        try {
            userName = jwtService.extractUsername(jwt);
            if (userName != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userName);

                String accessToken = "access_token:" + jwt;
                boolean isTokenActiveInRedis = redisService.hasKey(accessToken);

                if (jwtService.isTokenValid(jwt, userDetails) && isTokenActiveInRedis) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.debug("Người dùng '{}' đã xác thực thành công qua JWT và Redis.", userName);
                } else {
                    if (!isTokenActiveInRedis) {
                        logger.warn("Mã thông báo JWT hợp lệ nhưng không tìm thấy trong Redis (có thể đã đăng xuất): {}. Mã thông báo sẽ bị bỏ qua.", userName);
                    } else {
                        logger.warn("Mã thông báo JWT không hợp lệ đối với người dùng: {}. Mã thông báo sẽ bị bỏ qua.", userName);
                    }
                }
            }
            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            logger.warn("Mã thông báo JWT đã hết hạn: {}", e.getMessage());
            handleJwtException(response, HttpServletResponse.SC_UNAUTHORIZED, "Mã thông báo đã hết hạn: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.warn("Mã thông báo JWT không được hỗ trợ: {}", e.getMessage());
            handleJwtException(response, HttpServletResponse.SC_UNAUTHORIZED, "Mã thông báo JWT không được hỗ trợ: " + e.getMessage());
        } catch (MalformedJwtException e) {
            logger.warn("Mã thông báo JWT không hợp lệ (bị lỗi): {}", e.getMessage());
            handleJwtException(response, HttpServletResponse.SC_UNAUTHORIZED, "Mã thông báo JWT không đúng định dạng: " + e.getMessage());
        } catch (SignatureException e) {
            logger.warn("Chữ ký JWT không hợp lệ: {}", e.getMessage());
            handleJwtException(response, HttpServletResponse.SC_UNAUTHORIZED, "Chữ ký JWT không hợp lệ: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.warn("JWT tuyên bố chuỗi trống hoặc đối số không hợp lệ: {}", e.getMessage());
            handleJwtException(response, HttpServletResponse.SC_BAD_REQUEST, "JWT không hợp lệ (khiếu nại hoặc lập luận): " + e.getMessage());
        }
    }

    private void handleJwtException(HttpServletResponse response, int status, String message) throws IOException {
        if (!response.isCommitted()) {
            SecurityContextHolder.clearContext();
            response.setStatus(status);
            response.setContentType("application/json;charset=UTF-8");
            String jsonErrorResponse = String.format("{\"timestamp\": %d, \"status\": %d, \"error\": \"%s\", \"message\": \"%s\"}", System.currentTimeMillis(), status, HttpStatus.valueOf(status).getReasonPhrase().replace("\"", "\\\""), message.replace("\"", "\\\""));
            response.getWriter().write(jsonErrorResponse);
            response.getWriter().flush();
        } else {
            logger.warn("Phản hồi đã được cam kết. Không thể gửi lỗi JWT: {}", message);
        }
    }
}