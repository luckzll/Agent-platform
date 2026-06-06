package com.luckzll.demo.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtils {

    /**
     * 密钥（至少256位）
     */
    private static final String SECRET = System.getenv().getOrDefault(
            "JWT_SECRET",
            "change-me-before-production-use-32bytes"
    );

    /**
     * 过期时间（毫秒）- 7天
     */
    private static final long EXPIRATION = 7 * 24 * 60 * 60 * 1000;

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成JWT Token
     *
     * @param userId   用户ID
     * @param userName 用户名
     * @return JWT Token
     */
    public String generateToken(Long userId, String userName) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("userName", userName);

        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION);

        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 从Token中获取用户ID
     *
     * @param token JWT Token
     * @return 用户ID
     */
    public static Long getUserIdFromToken(String token) {
        JwtUtils jwtUtils = new JwtUtils();
        Claims claims = jwtUtils.parseToken(token);
        if (claims != null) {
            return Long.valueOf(claims.get("userId").toString());
        }
        return null;
    }

    /**
     * 从Token中获取用户名
     *
     * @param token JWT Token
     * @return 用户名
     */
    public String getUserNameFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims != null) {
            return claims.get("userName").toString();
        }
        return null;
    }

    /**
     * 解析Token
     *
     * @param token JWT Token
     * @return Claims
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            // Token过期
            return null;
        } catch (UnsupportedJwtException e) {
            // 不支持的Token
            return null;
        } catch (MalformedJwtException e) {
            // Token格式错误
            return null;
        } catch (SecurityException e) {
            // 签名验证失败
            return null;
        } catch (IllegalArgumentException e) {
            // Token为空或非法
            return null;
        }
    }

    /**
     * 验证Token是否有效
     *
     * @param token JWT Token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        Claims claims = parseToken(token);
        return claims != null && !claims.getExpiration().before(new Date());
    }

    /**
     * 获取Token过期时间
     *
     * @param token JWT Token
     * @return 过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims != null) {
            return claims.getExpiration();
        }
        return null;
    }
}
