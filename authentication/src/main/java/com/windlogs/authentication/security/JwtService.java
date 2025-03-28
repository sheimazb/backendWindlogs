package com.windlogs.authentication.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);
    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;
    @Value("${application.security.jwt.secret-key}")
    private String secretKey;
    @Value("${application.security.jwt.service-token-expiration:2592000000}") // 30 days in milliseconds
    private long serviceTokenExpiration;

    public String extractUsername(String token){
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims,T> claimResolver){
        final Claims claims= extractAllClaims(token);
        return claimResolver.apply(claims);
    }
    private Claims extractAllClaims (String token){
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String generateToken(UserDetails userDetails) {
            return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> claims, UserDetails userDetails) {

        return buildToken(claims, userDetails, jwtExpiration);
    }
    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long jwtExpiration)
    {
        var authorities = userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        
        // Merge the authorities with existing claims
        extraClaims.put("authorities", authorities);
        
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey())
                .compact();
    }


    public boolean isTokenValid(String token, UserDetails userDetails){
        final String username = extractUsername(token);
        logger.debug("Validating token for user: {}", username);
        boolean isValid = (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
        logger.debug("Is token valid: {}", isValid);
        return isValid;
    }
    private boolean isTokenExpired(String token){
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        // will return a date from the token
        return extractClaim(token, Claims::getExpiration);
    }

    //here we generated, or we decoded our signing key, and we are using it to sign our token
    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateServiceToken(Map<String, Object> claims) {
        return Jwts
                .builder()
                .setClaims(claims)
                .setSubject("service-account")
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + serviceTokenExpiration))
                .signWith(getSignInKey())
                .compact();
    }
}
