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

    @GetMapping("/login")
    public String index() {
        return "index";
    }

    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "registro";
    }

    @PostMapping("/registro")
    public String registrarUsuario(@Valid @ModelAttribute("usuario") pe.edu.utp.model.Usuario usuario,
            BindingResult result,
            Model model) {

        // 1. Si Spring Validator detecta errores (campos vacíos, mal correo, etc.)
        if (result.hasErrors()) {
            // Retornamos a la misma vista de registro para mostrar los mensajes en rojo
            return "registro";
        }

        // 2. Opcional pero recomendado: Verificar si el correo ya existe en la BD
        if (usuarioService.existeCorreo(usuario.getCorreo())) {
            model.addAttribute("errorGlobal", "Este correo ya está registrado.");
            return "registro";
        }

        // 3. Si todo está perfecto, guardamos el usuario
        usuarioService.guardar(usuario);

        // Redirigimos al login con un parámetro de éxito
        return "redirect:/login?exito=true";
    }

    // Muestra el Dashboard del Administrador
    @GetMapping("/admin/dashboard")
    public String dashboardAdmin(Model model, jakarta.servlet.http.HttpSession session) {

        Long usuarioId = (Long) session.getAttribute("usuarioId");
        // Si no inicia sesión o no es administrador, lo botamos al login
        if (usuarioId == null || !"ADMIN".equals(session.getAttribute("usuarioRol"))) {
            return "redirect:/login";
        }

        // Traem Todo el inventario de la base de datos
        List<pe.edu.utp.model.Celular> inventarioGlobal = celularService.listarTodos();
        model.addAttribute("inventarioGlobal", inventarioGlobal);

        return "admin_dashboard";
    }

    @GetMapping("/admin/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Long id) {
        // Usa el método eliminar de nuestro Service
        usuarioService.eliminar(id);
        return "redirect:/admin/dashboard";
    }

    // Procesa el formulario de Login
    @PostMapping("/login")
    public String procesarLogin(@RequestParam String correo,
            @RequestParam String contrasena,
            HttpSession session,
            Model model) {

        // Busca al usuario en la BD por su correo
        Usuario usuario = usuarioService.buscarPorCorreo(correo);

        // Valida si existe y si la contraseña coincide
        if (usuario != null && usuario.getContrasena().equals(contrasena)) {

            // Si el login es exitoso, guarda sus datos en la "Sesión"
            session.setAttribute("usuarioId", usuario.getId());
            session.setAttribute("usuarioRol", usuario.getRol());
            session.setAttribute("usuarioNombre", usuario.getNombres());

            // Redirige a la vista correcta según su ROL
            if ("ADMIN".equalsIgnoreCase(usuario.getRol())) {
                return "redirect:/admin/dashboard";
            } else if ("PROVEEDOR".equalsIgnoreCase(usuario.getRol())) {
                return "redirect:/proveedor/dashboard";
            } else {
                return "redirect:/catalogo";
            }

        } else {
            // Si el login es fallido recarga el index y mandamos un mensaje de error
            model.addAttribute("error", "Correo o contraseña incorrectos.");
            return "index";
        }
    }

    // Cerrar sesión
    @GetMapping("/logout")
    public String cerrarSesion(HttpSession session) {
        session.invalidate(); // Cierra la sesión, borra todos los datos guardados
        return "redirect:/"; // Redirige al inicio
    }

    // Muestra la vista del Perfil
    @GetMapping("/perfil")
    public String verPerfil(Model model, jakarta.servlet.http.HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return "redirect:/login"; // Si no hay sesión, al login
        }

        // Trae los datos del usuario actual
        Usuario usuario = usuarioService.buscarPorId(usuarioId).orElse(null);
        model.addAttribute("usuario", usuario);

        // Trae sus tarjetas/métodos de pago guardados
        List<pe.edu.utp.model.FormaPago> pagos = formaPagoService.listarPorUsuario(usuarioId);
        model.addAttribute("formasPago", pagos);
        model.addAttribute("nuevaFormaPago", new pe.edu.utp.model.FormaPago()); // Objeto vacío para el modal

        return "perfil";
    }

    // Guarda un nuevo método de pago
    @PostMapping("/perfil/pago/guardar")
    public String guardarFormaPago(@ModelAttribute pe.edu.utp.model.FormaPago nuevaFormaPago,
            jakarta.servlet.http.HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId != null) {
            Usuario usuario = usuarioService.buscarPorId(usuarioId).orElse(null);
            nuevaFormaPago.setUsuario(usuario); // Vinculamos la tarjeta a este usuario

            if (nuevaFormaPago.getFechaExpiracion() == null || nuevaFormaPago.getFechaExpiracion().isEmpty()) {
                nuevaFormaPago.setFechaExpiracion("N/A");
            }

            formaPagoService.guardar(nuevaFormaPago);
        }
        return "redirect:/perfil"; // Recarga la página para mostrar la nueva tarjeta
    }

    // Elimina un método de pago de la lista
    @GetMapping("/perfil/pago/eliminar/{id}")
    public String eliminarPago(@PathVariable Long id) {
        formaPagoService.eliminar(id);
        return "redirect:/perfil";
    }

    // Elimina la cuenta completa
    @GetMapping("/perfil/eliminarCuenta")
    public String eliminarMiCuenta(jakarta.servlet.http.HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId != null) {
            usuarioService.eliminar(usuarioId);
            session.invalidate(); // Cierra la sesión
        }
        return "redirect:/"; // Lo devuelve al inicio
    }
}