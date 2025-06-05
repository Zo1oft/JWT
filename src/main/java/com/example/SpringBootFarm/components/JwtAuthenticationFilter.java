package com.example.SpringBootFarm.components;

import com.example.SpringBootFarm.dto.LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.BufferedReader;
import java.io.IOException;

public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    private final JwtTokenUtil jwtTokenUtil;
    private final JwtCookieUtil jwtCookieUtil;

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager, JwtTokenUtil jwtTokenUtil, JwtCookieUtil jwtCookieUtil) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.jwtCookieUtil = jwtCookieUtil;
        setAuthenticationManager(authenticationManager);
        setFilterProcessesUrl("/api/auth/login");
    }

    /**
     * Попытка аутентификации:
     * 1. Читаем JSON из запроса (логин/пароль)
     * 2. Передаем данные в Spring Security для проверки
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response) {
        try {
            // Чтение JSON из тела запроса
            BufferedReader reader = request.getReader();
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            // Парсинг JSON
            ObjectMapper mapper = new ObjectMapper();
            LoginRequest loginRequest = mapper.readValue(sb.toString(), LoginRequest.class);

            return getAuthenticationManager().authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Действия после успешной аутентификации:
     * 1. Генерируем токен
     * 2. Возвращаем его клиенту (в JSON или куки)
     */
    @Override
    protected void successfulAuthentication(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain,
            Authentication authResult) throws IOException {

        String token = jwtTokenUtil.generateToken((UserDetails) authResult.getPrincipal());
        Cookie cookie = jwtCookieUtil.createJwtCookie((UserDetails) authResult.getPrincipal());

        // Явно устанавливаем тип контента и статус
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);

        // Возвращаем JSON с токеном для API запросов
        response.getWriter().write(
                String.format("{\"token\":\"%s\", \"username\":\"%s\", \"roles\":\"%s\"}",
                        token,
                        ((UserDetails) authResult.getPrincipal()).getUsername(),
                        authResult.getAuthorities())
        );

        //Добавляем токен в Cookie, чтобы видеть его в DevTools в Header

        cookie.setHttpOnly(true);
        cookie.setSecure(true); // Для HTTPS
        cookie.setPath("/");
        response.addCookie(cookie);
        response.getWriter().flush();
    }
}