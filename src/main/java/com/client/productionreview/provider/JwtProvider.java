package com.client.productionreview.provider;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class JwtProvider {



    @Value("${security.token.secret}")
    private String secretKey;

    @Value("${security.token.expiration}")
    private Long expiration;

    @Value("${security.token.expiration.refresh}")
    private Long expirationRefresh;

    public DecodedJWT validateToken(String token) {
        token = token.replace("Bearer ", "");

        Algorithm algorithm = Algorithm.HMAC256(secretKey);

        try {
            var tokenDecoded = JWT.require(algorithm)
                    .build()
                    .verify(token);

            return tokenDecoded;
        } catch (JWTVerificationException ex) {
            ex.printStackTrace();
            return null;
        }
    }


    public String generateToken(Long userId) {

        Algorithm algorithm = Algorithm.HMAC256(secretKey);

        return JWT.create()
                .withIssuer("Production Review API")
                .withExpiresAt(Instant.now().plusSeconds(expiration))
                .withSubject(userId.toString())
                .sign(algorithm);
    }

public String generateRefreshToken(Long userId) {

        Algorithm algorithm = Algorithm.HMAC256(secretKey);

        return JWT.create()
                .withIssuer("Production Review API")
                .withSubject(userId.toString())
                .withExpiresAt(
                        Instant.now().plusSeconds(expirationRefresh)
                )
                .sign(algorithm);
    }

    public Long getUserId(String token) {
        DecodedJWT decodedJWT = validateToken(token);
        return Long.parseLong(decodedJWT.getSubject());
    }

    public Long getUserIdFromRefreshToken(String token) {
        DecodedJWT decodedJWT = validateToken(token);
        return Long.parseLong(decodedJWT.getSubject());
    }

    public boolean isValid(String token) {
        return validateToken(token) != null;
    }

    public boolean isValidRefreshToken(String token) {
        return validateToken(token) != null;
    }

}
