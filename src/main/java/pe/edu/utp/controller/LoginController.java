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
    private final Map<String, Integer> intentosFallidos = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> tiempoBloqueo = new ConcurrentHashMap<>();
    private final int MAX_INTENTOS = 3;
    private final int TIEMPO_BLOQUEO_MINUTOS = 10;

    @GetMapping("/login")
    public String mostrarLogin(HttpSession session) {
        if (session.getAttribute("usuarioId") != null) {
            return "redirect:/catalogo";
        }
        return "index";
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
                long minutosRestantes = ChronoUnit.MINUTES.between(LocalDateTime.now(), tiempoDesbloqueo) + 1;
                redirectAttributes.addFlashAttribute("error",
                        "Cuenta bloqueada por múltiples intentos. Intente de nuevo en " + minutosRestantes
                                + " minutos.");
                return "redirect:/login";
            } else {
                tiempoBloqueo.remove(correo);
                intentosFallidos.remove(correo);
            }
        }

        // 2. BUSCAR AL USUARIO EN LA BASE DE DATOS
        Usuario usuario = usuarioService.buscarPorCorreo(correo);

        // ==========================================
        // CASO A: EL CORREO NO EXISTE (No restamos intentos)
        // ==========================================
        if (usuario == null) {
            redirectAttributes.addFlashAttribute("errorCorreo",
                    "Lo sentimos, no coincide con nuestros registros. Comprueba que lo has escrito bien y vuelve a intentarlo.");
            return "redirect:/login";
        }

        // ==========================================
        // CASO B: EL CORREO EXISTE, VERIFICAMOS CONTRASEÑA
        // ==========================================
        boolean credencialesValidas = false;

        if (usuario.getContrasena().startsWith("$2a$")) {
            credencialesValidas = passwordEncoder.matches(contrasena, usuario.getContrasena());
        } else {
            credencialesValidas = usuario.getContrasena().equals(contrasena);
            if (credencialesValidas) {
                usuario.setContrasena(passwordEncoder.encode(contrasena));
                usuarioService.guardar(usuario);
            }
        }

        if (credencialesValidas) {
            // --- LOGIN EXITOSO ---
            intentosFallidos.remove(correo);
            tiempoBloqueo.remove(correo);

            String token = jwtUtil.generateToken(usuario.getCorreo(), usuario.getRol(), usuario.getId(),
                    usuario.getNombres());

            Cookie jwtCookie = new Cookie("jwt", token);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(60 * 60 * 10);
            response.addCookie(jwtCookie);

            session.setAttribute("usuarioId", usuario.getId());
            session.setAttribute("usuarioNombre", usuario.getNombres());
            session.setAttribute("usuarioRol", usuario.getRol());

            if ("ADMIN".equals(usuario.getRol())) {
                return "redirect:/admin/dashboard";
            } else if ("PROVEEDOR".equals(usuario.getRol())) {
                return "redirect:/proveedor/dashboard";
            }
            return "redirect:/catalogo";

        } else {
            // --- LOGIN FALLIDO POR CONTRASEÑA (Restamos intentos) ---
            int intentosActuales = intentosFallidos.getOrDefault(correo, 0) + 1;
            intentosFallidos.put(correo, intentosActuales);

            if (intentosActuales >= MAX_INTENTOS) {
                tiempoBloqueo.put(correo, LocalDateTime.now());
                // El bloqueo es un error general, se muestra arriba en la alerta
                redirectAttributes.addFlashAttribute("error",
                        "Cuenta bloqueada por seguridad tras 3 intentos fallidos. Intente de nuevo en "
                                + TIEMPO_BLOQUEO_MINUTOS + " minutos.");
            } else {
                int intentosRestantes = MAX_INTENTOS - intentosActuales;
                // El error de contraseña se muestra justo debajo del input correspondiente
                redirectAttributes.addFlashAttribute("errorContrasena",
                        "Contraseña incorrecta. Te quedan " + intentosRestantes + " intento(s).");
            }
            return "redirect:/login";
        }
    }

    @GetMapping("/logout")
    public String cerrarSesion(HttpSession session, HttpServletResponse response) {
        session.invalidate();
        Cookie cookie = new Cookie("jwt", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return "redirect:/";
    }
}