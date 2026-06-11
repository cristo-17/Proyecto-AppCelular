package pe.edu.utp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.model.Usuario;
import pe.edu.utp.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;

import java.util.List;

@Controller
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private pe.edu.utp.service.FormaPagoService formaPagoService;

    @Autowired
    private pe.edu.utp.service.CelularService celularService;

    // Muestra la página de login
    @GetMapping("/login")
    public String index() {
        return "index";
    }

    // Muestra la página de registro
    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "registro";
    }

    // Procesa el formulario de registro
    @PostMapping("/registro")
    public String registrarUsuario(@Valid @ModelAttribute("usuario") pe.edu.utp.model.Usuario usuario,
            BindingResult result,
            Model model) {

        // Verfica que no haya errores de validación
        if (result.hasErrors()) {
            return "registro";
        }

        // Verifica que el correo no esté ya registrado
        if (usuarioService.existeCorreo(usuario.getCorreo())) {
            model.addAttribute("errorGlobal", "Este correo ya está registrado.");
            return "registro";
        }

        usuarioService.guardar(usuario);

        return "redirect:/login?exito=true";
    }

    // Muestra el Dashboard del Administrador
    @GetMapping("/admin/dashboard")
    public String dashboardAdmin(Model model, jakarta.servlet.http.HttpSession session) {

        Long usuarioId = (Long) session.getAttribute("usuarioId");

        if (usuarioId == null || !"ADMIN".equals(session.getAttribute("usuarioRol"))) {
            return "redirect:/login";
        }

        List<pe.edu.utp.model.Celular> inventarioGlobal = celularService.listarTodos();
        model.addAttribute("inventarioGlobal", inventarioGlobal);

        return "admin_dashboard";
    }

    // Elimina un usuario (solo para Admin)
    @GetMapping("/admin/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Long id) {
        usuarioService.eliminar(id);
        return "redirect:/admin/dashboard";
    }

    // Procesa el formulario de Login
    @PostMapping("/login")
    public String procesarLogin(@RequestParam String correo,
            @RequestParam String contrasena,
            HttpSession session,
            Model model) {

        Usuario usuario = usuarioService.buscarPorCorreo(correo);

        if (usuario != null && usuario.getContrasena().equals(contrasena)) {

            // Si el login es exitoso, guarda sus datos en la "Sesión"
            session.setAttribute("usuarioId", usuario.getId());
            session.setAttribute("usuarioRol", usuario.getRol());
            session.setAttribute("usuarioNombre", usuario.getNombres());

            // Redirige según el rol del usuario
            if ("ADMIN".equalsIgnoreCase(usuario.getRol())) {
                return "redirect:/admin/dashboard";
            } else if ("PROVEEDOR".equalsIgnoreCase(usuario.getRol())) {
                return "redirect:/proveedor/dashboard";
            } else {
                return "redirect:/catalogo";
            }

        } else {
            // Si el login falla, muestra un mensaje de error
            model.addAttribute("error", "Correo o contraseña incorrectos.");
            return "index";
        }
    }

    // Cierra la sesión del usuario
    @GetMapping("/logout")
    public String cerrarSesion(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    // Muestra el perfil del usuario con sus datos y métodos de pago
    @GetMapping("/perfil")
    public String verPerfil(Model model, jakarta.servlet.http.HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return "redirect:/login";
        }

        Usuario usuario = usuarioService.buscarPorId(usuarioId).orElse(null);
        model.addAttribute("usuario", usuario);

        List<pe.edu.utp.model.FormaPago> pagos = formaPagoService.listarPorUsuario(usuarioId);
        model.addAttribute("formasPago", pagos);
        model.addAttribute("nuevaFormaPago", new pe.edu.utp.model.FormaPago());

        return "perfil";
    }

    // Guarda una nueva forma de pago para el usuario
    @PostMapping("/perfil/pago/guardar")
    public String guardarFormaPago(@ModelAttribute pe.edu.utp.model.FormaPago nuevaFormaPago,
            jakarta.servlet.http.HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId != null) {
            Usuario usuario = usuarioService.buscarPorId(usuarioId).orElse(null);
            nuevaFormaPago.setUsuario(usuario);

            if (nuevaFormaPago.getFechaExpiracion() == null || nuevaFormaPago.getFechaExpiracion().isEmpty()) {
                nuevaFormaPago.setFechaExpiracion("N/A");
            }

            formaPagoService.guardar(nuevaFormaPago);
        }
        return "redirect:/perfil";
    }

    // Elimina un método de pago de la lista
    @GetMapping("/perfil/pago/eliminar/{id}")
    public String eliminarPago(@PathVariable Long id) {
        formaPagoService.eliminar(id);
        return "redirect:/perfil";
    }

    // Elimina la cuenta del usuario logueado
    @GetMapping("/perfil/eliminarCuenta")
    public String eliminarMiCuenta(jakarta.servlet.http.HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId != null) {
            usuarioService.eliminar(usuarioId);
            session.invalidate();
        }
        return "redirect:/";
    }
}