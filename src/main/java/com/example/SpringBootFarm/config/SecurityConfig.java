package com.example.SpringBootFarm.config;

import com.example.SpringBootFarm.components.JwtAuthenticationFilter;
import com.example.SpringBootFarm.components.JwtAuthorizationFilter;
import com.example.SpringBootFarm.components.JwtCookieUtil;
import com.example.SpringBootFarm.components.JwtTokenUtil;
import jakarta.servlet.http.Cookie;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtTokenUtil jwtTokenUtil;
    private final JwtCookieUtil jwtCookieUtil;
    private final UserDetailsService userDetailsService;
    private final AuthenticationConfiguration authConfig;

    public SecurityConfig(JwtTokenUtil jwtTokenUtil, JwtCookieUtil jwtCookieUtil,
                          UserDetailsService userDetailsService,
                          AuthenticationConfiguration authConfig) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.jwtCookieUtil = jwtCookieUtil;
        this.userDetailsService = userDetailsService;
        this.authConfig = authConfig;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Создаем AuthenticationManager правильно
        AuthenticationManager authManager = authenticationManager(authConfig);

        JwtAuthenticationFilter jwtAuthFilter = new JwtAuthenticationFilter(authManager, jwtTokenUtil);
        jwtAuthFilter.setFilterProcessesUrl("/api/auth/login");

        http // Разрешаем CORS (для взаимодействия с фронтендом)
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(List.of("http://localhost:8080"));
                    config.setAllowCredentials(true);
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
                    return config;
                }))
                // Отключаем CSRF (т.к. используем JWT)
                .csrf(AbstractHttpConfigurer::disable)
                // Настройка доступа:
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/login", "/static/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(login -> login
                        .loginPage("/login")
                        .successHandler((request, response, authentication) -> {
                            response.addCookie(jwtCookieUtil.createJwtCookie(
                                    (UserDetails) authentication.getPrincipal()
                            ));
                            response.sendRedirect("/farm");
                        })
                        .permitAll()
                )
                .addFilter(jwtAuthFilter)
                .addFilterAfter(new JwtAuthorizationFilter(jwtTokenUtil, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class)
                // Отключаем сессии (используем JWT)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}