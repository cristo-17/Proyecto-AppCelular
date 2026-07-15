package pe.edu.utp.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Autowired
        private JwtRequestFilter jwtRequestFilter;

        // Herramienta para encriptar contraseñas en el futuro
        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                // Desactivamos CSRF temporalmente para facilitar la migración a API/React
                                .csrf(csrf -> csrf.disable())

                                // Reglas de autorización de rutas
                                .authorizeHttpRequests(auth -> auth
                                                // Rutas públicas (No requieren JWT)
                                                .requestMatchers("/", "/login", "/registro", "/catalogo",
                                                                "/catalogo/marca/**", "/legal/**", "/css/**", "/js/**",
                                                                "/images/**",
                                                                "/error")
                                                .permitAll()

                                                // Rutas protegidas por Rol
                                                .requestMatchers("/carrito/**", "/perfil/**", "/resenas/guardar",
                                                                "/resenas/editar",
                                                                "/resenas/eliminar/**")
                                                .hasRole("COMPRADOR")
                                                .requestMatchers("/proveedor/**").hasRole("PROVEEDOR")
                                                .requestMatchers("/admin/**").hasRole("ADMIN")

                                                // Cualquier otra ruta requiere usuario autenticado
                                                .anyRequest().authenticated())

                                // Política Stateless: Apagamos las sesiones en memoria del servidor
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                // Le decimos a Spring: "Usa mi filtro JWT antes de intentar tu lógica por
                                // defecto"
                                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}