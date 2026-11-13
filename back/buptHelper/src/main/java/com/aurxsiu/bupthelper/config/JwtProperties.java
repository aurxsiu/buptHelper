package com.aurxsiu.bupthelper.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置项，前缀 bupthelper.jwt。
 */
@ConfigurationProperties(prefix = "bupthelper.jwt")
public class JwtProperties {

    /** HMAC-SHA256 签名密钥，长度不少于 32 字节 */
    private String secret;

    private String issuer = "bupt-helper";

    /** 未勾选“记住我”时的有效期（秒） */
    private long expireSeconds = 7200;

    /** 勾选“记住我”时的有效期（秒） */
    private long rememberMeExpireSeconds = 604800;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public long getExpireSeconds() {
        return expireSeconds;
    }

    public void setExpireSeconds(long expireSeconds) {
        this.expireSeconds = expireSeconds;
    }

    public long getRememberMeExpireSeconds() {
        return rememberMeExpireSeconds;
    }

    public void setRememberMeExpireSeconds(long rememberMeExpireSeconds) {
        this.rememberMeExpireSeconds = rememberMeExpireSeconds;
    }
}
