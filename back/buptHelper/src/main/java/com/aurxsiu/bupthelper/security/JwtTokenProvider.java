package com.aurxsiu.bupthelper.security;

import com.aurxsiu.bupthelper.common.ApiCode;
import com.aurxsiu.bupthelper.common.BusinessException;
import com.aurxsiu.bupthelper.config.JwtProperties;
import com.aurxsiu.bupthelper.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * HS256 令牌的签发与解析。
 */
@Component
public class JwtTokenProvider {

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_USER_TYPE = "type";
    private static final String CLAIM_TOKEN_VERSION = "ver";

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(toKeyBytes(properties.getSecret()));
    }

    public String createToken(User user, long expireSeconds, long tokenVersion) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getUserId()))
                .claim(CLAIM_USERNAME, user.getUsername())
                .claim(CLAIM_USER_TYPE, user.getUserType().name())
                .claim(CLAIM_TOKEN_VERSION, tokenVersion)
                .id(UUID.randomUUID().toString())
                .issuer(properties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expireSeconds)))
                .signWith(key)
                .compact();
    }

    /**
     * 校验签名与有效期，失败时抛出 40102 业务异常。
     */
    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, "登录已过期，请重新登录");
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ApiCode.UNAUTHORIZED, "请先登录");
        }
    }

    /**
     * 密钥不足 32 字节时用 SHA-256 派生 32 字节，避免启动失败。
     */
    private static byte[] toKeyBytes(String secret) {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException("缺少配置项 bupthelper.jwt.secret");
        }
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length >= 32) {
            return raw;
        }
        try {
            return MessageDigest.getInstance("SHA-256").digest(raw);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("无法初始化 JWT 密钥", e);
        }
    }
}
