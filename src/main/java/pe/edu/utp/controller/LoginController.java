package pe.edu.utp.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.utp.model.Usuario;
import pe.edu.utp.security.JwtUtil;
import pe.edu.utp.service.UsuarioService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class LoginController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    // --- SISTEMA DE BLOQUEO EN MEMORIA ---
    // Usamos ConcurrentHashMap para que soporte múltiples usuarios intentando al
    // mismo tiempo
    private final Map<String, Integer> intentosFallidos = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> tiempoBloqueo = new ConcurrentHashMap<>();
    private final int MAX_INTENTOS = 3;
    private final int TIEMPO_BLOQUEO_MINUTOS = 10;

    @GetMapping("/login")
    public String mostrarLogin(HttpSession session) {
        // Si el usuario ya inició sesión, lo redirigimos automáticamente
        if (session.getAttribute("usuarioId") != null) {
            return "redirect:/catalogo";
        }
        return "index"; // Muestra la vista index.html
    }

    @PostMapping("/login")
    public String procesarLogin(@RequestParam String correo,
            @RequestParam String contrasena,
            HttpSession session,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {

        // 1. VERIFICAR SI LA CUENTA ESTÁ BLOQUEADA ACTUALMENTE
        if (tiempoBloqueo.containsKey(correo)) {
            LocalDateTime tiempoDesbloqueo = tiempoBloqueo.get(correo).plusMinutes(TIEMPO_BLOQUEO_MINUTOS);

            if (LocalDateTime.now().isBefore(tiempoDesbloqueo)) {
                // Aún no ha pasado el tiempo de castigo
                long minutosRestantes = ChronoUnit.MINUTES.between(LocalDateTime.now(), tiempoDesbloqueo) + 1;
                redirectAttributes.addFlashAttribute("error",
                        "Cuenta bloqueada por múltiples intentos. Intente de nuevo en " + minutosRestantes
                                + " minutos.");
                return "redirect:/login";
            } else {
                // Ya pasaron los 10 minutos, perdonamos al usuario y limpiamos su registro
                tiempoBloqueo.remove(correo);
                intentosFallidos.remove(correo);
            }
        }

        // 2. INTENTAR BUSCAR AL USUARIO EN LA BASE DE DATOS
        Usuario usuario = usuarioService.buscarPorCorreo(correo);

        boolean credencialesValidas = false;

        if (usuario != null) {
            // BCrypt siempre genera cadenas que empiezan con "$2a$". Verificamos si ya está
            // encriptada.
            if (usuario.getContrasena().startsWith("$2a$")) {
                // Compara la contraseña escrita (texto plano) con el hash de la BD
                credencialesValidas = passwordEncoder.matches(contrasena, usuario.getContrasena());
            } else {
                // Es un usuario antiguo con contraseña en texto plano (ej. "1234")
                credencialesValidas = usuario.getContrasena().equals(contrasena);

                // MIGRACIÓN SILENCIOSA: Si logró entrar con su clave vieja, la encriptamos de
                // inmediato y la actualizamos en la BD
                if (credencialesValidas) {
                    usuario.setContrasena(passwordEncoder.encode(contrasena));
                    usuarioService.guardar(usuario); // Esto actualizará el registro en MySQL
                }
            }
        }

        // Verificamos el resultado de la autenticación haciendo feliz al IDE
        if (usuario != null && credencialesValidas) {
            // ==========================================
            // --- LOGIN EXITOSO ---
            // ==========================================

            // Limpiamos el historial de errores si entró bien
            intentosFallidos.remove(correo);
            tiempoBloqueo.remove(correo);

            // A) Generar el Token JWT con la lógica que creamos
            String token = jwtUtil.generateToken(usuario.getCorreo(), usuario.getRol(), usuario.getId(),
                    usuario.getNombres());

            // B) Guardar el Token en una Cookie HTTP-Only para máxima seguridad
            Cookie jwtCookie = new Cookie("jwt", token);
            jwtCookie.setHttpOnly(true); // Evita que código JavaScript malicioso lea el token
            jwtCookie.setPath("/"); // Disponible en toda la aplicación
            jwtCookie.setMaxAge(60 * 60 * 10); // Expira en 10 horas
            response.addCookie(jwtCookie);

            // C) Mantener las variables de sesión clásicas para que Thymeleaf siga
            // funcionando
            session.setAttribute("usuarioId", usuario.getId());
            session.setAttribute("usuarioNombre", usuario.getNombres());
            session.setAttribute("usuarioRol", usuario.getRol());

            // Redirecciones estratégicas según el rol
            if ("ADMIN".equals(usuario.getRol())) {
                return "redirect:/admin/dashboard";
            } else if ("PROVEEDOR".equals(usuario.getRol())) {
                return "redirect:/proveedor/dashboard";
            }
            return "redirect:/catalogo";

        } else {
            // ==========================================
            // --- LOGIN FALLIDO ---
            // ==========================================
            int intentosActuales = intentosFallidos.getOrDefault(correo, 0) + 1;
            intentosFallidos.put(correo, intentosActuales);

            if (intentosActuales >= MAX_INTENTOS) {
                // Alcanzó el límite, aplicamos el bloqueo y registramos la hora actual
                tiempoBloqueo.put(correo, LocalDateTime.now());
                redirectAttributes.addFlashAttribute("error",
                        "Cuenta bloqueada por seguridad tras 3 intentos fallidos. Intente de nuevo en "
                                + TIEMPO_BLOQUEO_MINUTOS + " minutos.");
            } else {
                // Aún le quedan intentos
                int intentosRestantes = MAX_INTENTOS - intentosActuales;
                redirectAttributes.addFlashAttribute("error",
                        "Credenciales incorrectas. Te quedan " + intentosRestantes + " intento(s).");
            }
            return "redirect:/login";
        }
    }

    @GetMapping("/logout")
    public String cerrarSesion(HttpSession session, HttpServletResponse response) {
        // 1. Destruir la memoria tradicional de Thymeleaf
        session.invalidate();

        // 2. Destruir la Cookie del Token JWT enviando una expirada
        Cookie cookie = new Cookie("jwt", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0); // El valor 0 obliga al navegador a borrarla inmediatamente
        response.addCookie(cookie);

        return "redirect:/";
    }
}