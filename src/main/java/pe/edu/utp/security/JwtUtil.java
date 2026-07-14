package pe.edu.utp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    // Llave secreta encriptada (¡En un proyecto real esto va en variables de
    // entorno!)
    private final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // Tiempo de vida del token (Ejemplo: 10 horas)
    private final long JWT_EXPIRATION = 1000 * 60 * 60 * 10;

    // 1. Extraer el correo (username) del token
    public String extractUsername(String token) {
        return extractClaim(token, claims -> claims.getSubject());
    }

    // 2. Extraer la fecha de expiración
    public Date extractExpiration(String token) {
        return extractClaim(token, claims -> claims.getExpiration());
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // 3. Generar el Token (Aquí podemos inyectar el Rol y el ID del usuario)
    public String generateToken(String email, String rol, Long id, String nombre) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("rol", rol);
        claims.put("id", id);
        claims.put("nombre", nombre);
        return createToken(claims, email);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject) // Generalmente el correo
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION))
                .signWith(SECRET_KEY)
                .compact();
    }

    // 4. Validar que el token pertenezca al usuario y no esté expirado
    public Boolean validateToken(String token, String email) {
        final String username = extractUsername(token);
        return (username.equals(email) && !isTokenExpired(token));
    }
}