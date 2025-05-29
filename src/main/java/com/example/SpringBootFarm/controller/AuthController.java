package com.example.SpringBootFarm.controller;

import com.example.SpringBootFarm.components.JwtTokenUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final JwtTokenUtil jwtTokenUtil;

    public AuthController(JwtTokenUtil jwtTokenUtil) {
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token: No Authorization header");
        }

        String token = header.substring(7);
        try {
            if (jwtTokenUtil.validateToken(token, jwtTokenUtil.extractUsername(token))) {
                return ResponseEntity.ok().body("Token is valid");
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token: Validation failed");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid token: " + e.getMessage());
        }
    }

    @GetMapping("/token-info")
    public ResponseEntity<?> getTokenInfo(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Invalid Authorization header");
        }

        String token = authHeader.substring(7);
        try {
            Map<String, Object> tokenInfo = jwtTokenUtil.getTokenInfo(token);
            return ResponseEntity.ok(tokenInfo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid token: " + e.getMessage());
        }
    }
}
