package com.example.SpringBootFarm.components;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthorizationFilter extends OncePerRequestFilter {
    private final JwtTokenUtil jwtTokenUtil;
    private final UserDetailsService userDetailsService;


    public JwtAuthorizationFilter(JwtTokenUtil jwtTokenUtil,
                                  UserDetailsService userDetailsService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Основной метод фильтра:
     * 1. Пропускаем публичные пути (/login, /static)
     * 2. Извлекаем токен из запроса
     * 3. Проверяем его валидность
     * 4. Если все ок - пропускаем запрос дальше
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {

        // Пропускаем публичные пути
        if (shouldSkipRequest(request)) {
            chain.doFilter(request, response);
            return;
        }

        // Получаем токен из запроса
        String token = extractToken(request);

        if (token == null) {
            handleMissingToken(request, response, chain);
            return;
        }

        // Проверяем и устанавливаем аутентификацию
        if (!processTokenAuthentication(request, response, token)) {
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean shouldSkipRequest(HttpServletRequest request) {
        return request.getRequestURI().equals("/login") ||
                request.getRequestURI().startsWith("/static/") ||
                request.getRequestURI().equals("/api/auth/login");
    }

    private String extractToken(HttpServletRequest request) {
        // Проверяем заголовок Authorization
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // Проверяем куки
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("JWT".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void handleMissingToken(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {
        if (isApiRequest(request)) {
            sendJsonError(response, "No JWT token provided");
        } else {
            // Для UI запросов разрешаем продолжить цепочку фильтров
            chain.doFilter(request, response);
        }
    }

    private boolean processTokenAuthentication(HttpServletRequest request,
                                               HttpServletResponse response,
                                               String token) throws IOException {
        try {
            String username = jwtTokenUtil.extractUsername(token);
            if (username == null) {
                throw new Exception("Invalid token");
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (!jwtTokenUtil.validateToken(token, userDetails)) {
                throw new Exception("Invalid token");
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return true;
        } catch (Exception e) {
            if (isApiRequest(request)) {
                sendJsonError(response, "Invalid JWT token");
            } else {
                // Для UI запросов очищаем невалидный токен
                clearInvalidToken(response);
                response.sendRedirect("/login");
            }
            return false;
        }
    }

    private boolean isApiRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api");
    }

    private void sendJsonError(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(
                String.format("{\"error\":\"Unauthorized\", \"message\":\"%s\"}", message)
        );
    }

    private void clearInvalidToken(HttpServletResponse response) {
        Cookie cookie = new Cookie("JWT", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
