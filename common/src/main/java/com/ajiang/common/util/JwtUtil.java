package com.ajiang.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * AI
 * @description:
 * @author: ajiang
 * @date: 2025/6/22 16:59
 * @param:
 * @return:
 **/
@Slf4j
@Component
public class JwtUtil {

    // 直接在类中设置 JWT 密钥和过期时间（单位：秒）
    private final String secret = "AJiang";   // 签名密钥
    private final Long expiration = 1800L;             // 半小时

    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return Long.parseLong(claims.getSubject());
    }

    public String getRoleCodeFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("roleCode", String.class);
    }

    public String generateToken(Long userId, String roleCode) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roleCode", roleCode);
        return doGenerateToken(claims, userId.toString());
    }

    public Boolean isTokenExpired(String token) {
        Claims claims = getClaimsFromToken(token);
        Date expiration = claims.getExpiration();
        return expiration.before(new Date());
    }

    public Boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.error("JWT验证失败: {}", e.getMessage());
            return false;
        }
    }

    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }

    private String doGenerateToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration * 1000);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }
}
