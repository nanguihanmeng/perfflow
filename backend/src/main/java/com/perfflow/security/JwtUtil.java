package com.perfflow.security;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：使用 HS256 算法签发与解析访问/刷新令牌（基于 jjwt API）。
 * 密钥与有效期由配置注入，令牌携带用户身份、主角色与令牌版本号等声明。
 */
@Slf4j
@Component
public class JwtUtil {

    /** HS256 签名要求的最小密钥长度（字节）。 */
    private static final int HS256_KEY_BYTES = 32;

    /** 秒转毫秒系数，用于把 TTL 秒数换算为过期时间戳。 */
    private static final long MS_PER_SECOND = 1000L;

    private final SecretKey key;
    private final long accessTtl;
    private final long refreshTtl;
    private final String issuer;
    // 构造器，从配置初始化密钥与有效期。
    public JwtUtil(@Value("${perfflow.jwt.secret}") String secret,
                   @Value("${perfflow.jwt.access-token-ttl-seconds}") long accessTtl,
                   @Value("${perfflow.jwt.refresh-token-ttl-seconds}") long refreshTtl,
                   @Value("${perfflow.jwt.issuer}") String issuer) {
        // 优先按 Base64 解码；解码失败再当字节使用，并保证长度 ≥ 32
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(secret);
            if (bytes.length < HS256_KEY_BYTES) {
                bytes = secret.getBytes(StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        if (bytes.length < HS256_KEY_BYTES) {
            // 字节不足时补零到最小长度，避免 HS256 因密钥过短而签名失败。
            byte[] ext = new byte[HS256_KEY_BYTES];
            System.arraycopy(bytes, 0, ext, 0, bytes.length);
            bytes = ext;
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
        this.issuer = issuer;
    }

    /**
     * 生成访问令牌：携带用户身份、主角色、部门与令牌版本号（用于多端互踢）。
     */
    public String generateAccess(Long userId, String username, String role, Long deptId, Boolean deptLead,

                                 Boolean mustChangePwd, Integer tokenVersion) {

        return generate(userId, username, role, deptId, deptLead, mustChangePwd, "access", accessTtl, tokenVersion);
    }

    /**
     * 生成刷新令牌：仅含用户标识与版本号，用于续期换取新的访问令牌。
     */
    public String generateRefresh(Long userId, String username, String role, Integer tokenVersion) {

        return generate(userId, username, role, null, false, false, "refresh", refreshTtl, tokenVersion);
    }

    private String generate(Long userId, String username, String role, Long deptId, Boolean deptLead,

                            Boolean mustChangePwd, String type, long ttl, Integer tokenVersion) {

        long now = System.currentTimeMillis();
        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .claim("deptId", deptId)
                .claim("deptLead", deptLead != null && deptLead)
                .claim("mustChangePwd", mustChangePwd != null && mustChangePwd)
                .claim("type", type)
                .claim("tokenVersion", tokenVersion == null ? 0 : tokenVersion)
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttl * MS_PER_SECOND))
                .signWith(key)
                .compact();
    }

    /**
     * 解析并校验令牌签名与签发方（issuer），返回其中的声明集合。
     * @param token 待校验的 JWT 字符串
     */
    public Claims parse(String token) {

        Jws<Claims> jws = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token);
        return jws.getPayload();
    }

    public long getAccessTtl() { return accessTtl; }

    public long getRefreshTtl() { return refreshTtl; }
}
