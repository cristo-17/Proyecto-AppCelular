package pe.edu.utp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.model.Celular;
import pe.edu.utp.model.Usuario;
import pe.edu.utp.service.CelularService;
import pe.edu.utp.service.UsuarioService;

import java.util.List;

@Controller
public class CelularController {

    @Autowired
    private CelularService celularService;

    @Autowired
    private UsuarioService usuarioService;

    // Muestra el Catálogo a todos
    @GetMapping("/")
    public String mostrarCatalogoPrincipal(Model model) {
        List<Celular> celulares = celularService.listarTodos();
        model.addAttribute("celulares", celulares);
        return "catalogo";
    }

    // Muestra el Catálogo a los compradores
    @GetMapping("/catalogo")
    public String mostrarCatalogo(Model model) {
        // Trae todos los celulares de todos los proveedores
        List<Celular> celulares = celularService.listarTodos();
        model.addAttribute("celulares", celulares);
        return "catalogo";
    }

    // Muestra el Dashboard del Proveedor
    @GetMapping("/proveedor/dashboard")
    public String dashboardProveedor(Model model, jakarta.servlet.http.HttpSession session) {
        // Obtiene el ID del usuario directamente de la sesión activa
        Long usuarioId = (Long) session.getAttribute("usuarioId");

        // Si no inicia sesión o no es proveedor, lo botamos al login
        if (usuarioId == null || !"PROVEEDOR".equals(session.getAttribute("usuarioRol"))) {
            return "redirect:/login";
        }

        // Trae solo los celulares de este proveedor
        List<Celular> misCelulares = celularService.listarPorProveedor(usuarioId);

        model.addAttribute("celulares", misCelulares);
        model.addAttribute("nuevoCelular", new Celular());
        return "proveedor_dashboard";
    }

    // Guarda un nuevo celular
    @PostMapping("/proveedor/guardar")
    public String guardarCelular(@ModelAttribute Celular celular, jakarta.servlet.http.HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");

        if (usuarioId != null) {
            Usuario proveedor = usuarioService.buscarPorId(usuarioId).orElse(null);
            if (proveedor != null) {
                celular.setProveedor(proveedor); // Vincula el celular con el proveedor logueado
                celularService.guardar(celular); // Inserta en MySQL
            }
        }
        return "redirect:/proveedor/dashboard";
    }

    // Elimina un celular de su inventario
    @GetMapping("/proveedor/eliminar/{id}")
    public String eliminarCelular(@PathVariable Long id) {
        celularService.eliminar(id);
        return "redirect:/proveedor/dashboard";
    }
}