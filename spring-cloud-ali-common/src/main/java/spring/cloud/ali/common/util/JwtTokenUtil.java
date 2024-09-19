package spring.cloud.ali.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecureDigestAlgorithm;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * 基于JWT的token生成/校验工具
 */
public class JwtTokenUtil {

    private final static SecureDigestAlgorithm<SecretKey, SecretKey> ALGORITHM = Jwts.SIG.HS256;

    private JwtTokenUtil(){}

    /**
     * 生成token
     * @param signKey 签名key
     * @param claims 业务数据
     * @param expireSecs 过期时间（秒）
     * @return token
     */
    public static String generate(String signKey, Map<String, ?> claims, long expireSecs) {

        return Jwts.builder()
                .header()
                .add("typ", "JWT")
                .add("alg", "HS256")
                .and()
                .claims(claims)
                .id(UUID.randomUUID().toString())
                .expiration(Date.from(Instant.now().plusSeconds(expireSecs)))
                .issuedAt(new Date())
                .signWith(Keys.hmacShaKeyFor(signKey.getBytes()), ALGORITHM)
                .compact();
    }

    /**
     * 解析token
     * @param signKey 签名key
     * @param token token
     * @return 业务数据
     */
    public static Claims parse(String signKey, String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(signKey.getBytes()))
                .build()
                .parseSignedClaims(token).getPayload();
    }
}
