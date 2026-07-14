package pe.edu.utp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String jwt = null;
        String username = null;

        // 1. Buscar el token JWT en las Cookies del navegador
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt".equals(cookie.getName())) {
                    jwt = cookie.getValue();
                    try {
                        username = jwtUtil.extractUsername(jwt);
                    } catch (Exception e) {
                        // El token caducó o fue alterado
                        System.out.println("Token inválido o expirado");
                    }
                }
            }
        }

        // 2. Si hay usuario y aún no está logueado en el contexto de Spring Security
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Validamos que el token sea legítimo
            if (jwtUtil.validateToken(jwt, username)) {

                // Extraemos el rol que guardamos dentro del JWT
                String rol = jwtUtil.extractClaim(jwt, claims -> claims.get("rol", String.class));

                // Spring Security requiere el prefijo "ROLE_" para los permisos
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + rol);

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        username, null, Collections.singletonList(authority));

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // ¡Aprobado! Lo registramos oficialmente en Spring Security
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // Dejamos que la petición continúe su camino
        filterChain.doFilter(request, response);
    }
}