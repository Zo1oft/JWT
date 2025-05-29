package com.example.SpringBootFarm.components;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class JwtTokenUtil {
    // Секретный ключ для подписи токена (генерируется автоматически)
    private static final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private static final long EXPIRATION_TIME = 86400000; // 24 часа


    /**
     * Генерация JWT токена для пользователя.
     * Аналогично выдаче паспорта:
     * - Указываем владельца (subject)
     * - Добавляем права (roles)
     * - Устанавливаем срок действия
     * - Подписываем документ (signWith)
     */

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toList()));
        return Jwts.builder()
                .setClaims(claims) // Данные пользователя
                .setSubject(userDetails.getUsername()) // Идентификатор
                .setIssuedAt(new Date()) // Дата выдачи
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))  // Срок действия
                .signWith(SECRET_KEY) // Подпись
                .compact(); // Сборка токена
    }

    /**
     * Проверка токена:
     * 1. Совпадает ли username в токене и БД?
     * 2. Не истек ли срок действия?
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    // Извлечение username из токена
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Проверка срока действия
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Извлечение даты истечения
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Общий метод для извлечения данных из токена
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Парсинг токена
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    //Валидация токена по API
    public boolean validateToken(String token, String username) {
        final String tokenUsername = extractUsername(token);
        return (username.equals(tokenUsername) && !isTokenExpired(token));
    }

    //Получение информации из токена по API
    public Map<String, Object> getTokenInfo(String token) {
        Claims claims = extractAllClaims(token);
        Map<String, Object> tokenInfo = new HashMap<>();
        tokenInfo.put("username", claims.getSubject());
        tokenInfo.put("issuedAt", claims.getIssuedAt());
        tokenInfo.put("expiration", claims.getExpiration());
        tokenInfo.put("roles", claims.get("roles"));
        return tokenInfo;
    }
}
