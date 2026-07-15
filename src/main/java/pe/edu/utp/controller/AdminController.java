package pe.edu.utp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.utp.model.Celular;
import pe.edu.utp.model.Usuario;
import pe.edu.utp.service.CelularService;
import pe.edu.utp.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private CelularService celularService;

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/dashboard")
    public String dashboardAdmin(HttpSession session, Model model) {
        // Validación de seguridad
        if (session.getAttribute("usuarioId") == null || !"ADMIN".equals(session.getAttribute("usuarioRol"))) {
            return "redirect:/login";
        }

        // Datos para la pestaña de Inventario
        List<Celular> inventarioGlobal = celularService.listarTodos();

        // Datos para la pestaña de Gestión de Usuarios
        List<Usuario> todosLosUsuarios = usuarioService.listarTodos();

        model.addAttribute("inventarioGlobal", inventarioGlobal);
        model.addAttribute("usuarios", todosLosUsuarios);

        return "admin_dashboard";
    }

    // ELIMINAR USUARIO DEFINITIVAMENTE
    @GetMapping("/usuario/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        usuarioService.eliminar(id);
        redirectAttributes.addFlashAttribute("exito", "Usuario eliminado del sistema correctamente.");
        return "redirect:/admin/dashboard";
    }

    // SUSPENDER O RESTAURAR USUARIO (Cambiando el Rol)
    @PostMapping("/usuario/estado")
    public String cambiarEstadoUsuario(@RequestParam Long usuarioId, @RequestParam String nuevoRol,
            RedirectAttributes redirectAttributes) {
        Usuario usuario = usuarioService.buscarPorId(usuarioId).orElse(null);

        if (usuario != null) {
            // Protección: Un admin no puede banear a otro admin por error
            if ("ADMIN".equals(usuario.getRol())) {
                redirectAttributes.addFlashAttribute("error",
                        "Operación denegada. No puedes modificar el estado de otro Administrador.");
                return "redirect:/admin/dashboard";
            }

            usuario.setRol(nuevoRol);
            usuarioService.guardar(usuario);

            String mensaje = nuevoRol.equals("BANEADO") ? "La cuenta ha sido suspendida."
                    : "La cuenta ha sido restaurada con éxito.";
            redirectAttributes.addFlashAttribute("exito", mensaje);
        }
        return "redirect:/admin/dashboard";
    }
}