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
    private static final String[] PUBLIC_PATHS = {"/login", "/static/", "/api/auth/login"};
    private final JwtTokenUtil jwtTokenUtil;
    private final UserDetailsService userDetailsService;

    public JwtAuthorizationFilter(JwtTokenUtil jwtTokenUtil,
                                  UserDetailsService userDetailsService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {

        // Пропускаем публичные пути
        if (isPublicPath(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        try {
            // Извлекаем токен из заголовка или куки
            String token = getTokenFromRequest(request);
            if (token != null && processToken(request, token)) {
                chain.doFilter(request, response);
                return;
            }
        } catch (Exception e) {
            handleAuthError(response, request, e);
            return;
        }

        sendUnauthorizedError(response, "Missing or invalid JWT token");
    }

    //Проверяет, является ли запрошенный URI публичным (не требует аутентификации)
    private boolean isPublicPath(String requestURI) {
        for (String path : PUBLIC_PATHS) {
            if (requestURI.startsWith(path)) {
                return true;
            }
        }
        return false;
    }

    //Извлекает JWT-токен из запроса
    private String getTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // Для фронтенда проверяем куку
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

    //Проверяет токен и аутентифицирует пользователя
    private boolean processToken(HttpServletRequest request, String token) {
        String username = jwtTokenUtil.extractUsername(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (jwtTokenUtil.validateToken(token, userDetails)) {
            var authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return true;
        }
        return false;
    }

    //Обрабатывает ошибки аутентификации
    private void handleAuthError(HttpServletResponse response,
                                 HttpServletRequest request,
                                 Exception e) throws IOException {
        if (request.getRequestURI().startsWith("/api")) {
            sendUnauthorizedError(response, "Invalid JWT token: " + e.getMessage());
        } else {
            response.sendRedirect("/login");
        }
    }

    //Отправляет стандартизированную JSON-ошибку для API
    private void sendUnauthorizedError(HttpServletResponse response,
                                       String message) throws IOException {
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("{\"error\":\"Unauthorized\", \"message\":\"" + message + "\"}");
    }
}
