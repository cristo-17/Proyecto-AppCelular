package pe.edu.utp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.model.Celular;
import pe.edu.utp.service.CelularService;
import pe.edu.utp.service.UsuarioService;
import jakarta.validation.Valid;

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
        List<Celular> celulares = celularService.listarTodos();
        model.addAttribute("celulares", celulares);
        return "catalogo";
    }

    // Muestra el Dashboard del Proveedor
    @GetMapping("/proveedor/dashboard")
    public String dashboardProveedor(Model model, jakarta.servlet.http.HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");

        if (usuarioId == null || !"PROVEEDOR".equals(session.getAttribute("usuarioRol"))) {
            return "redirect:/login";
        }

        // Trae solo los celulares de este proveedor
        List<pe.edu.utp.model.Celular> misCelulares = celularService.listarPorProveedor(usuarioId);

        // Calculamos el promedio de estrellas para mostrarlo en el dashboard del
        // proveedor
        double sumaEstrellas = 0;
        int totalResenas = 0;

        for (pe.edu.utp.model.Celular celular : misCelulares) {
            if (celular.getResenas() != null) {
                for (pe.edu.utp.model.Resena resena : celular.getResenas()) {
                    sumaEstrellas += resena.getEstrellas();
                    totalResenas++;
                }
            }
        }

        // Calculamos el promedio, evitando división por cero
        double promedio = (totalResenas > 0) ? (sumaEstrellas / totalResenas) : 0.0;

        model.addAttribute("celulares", misCelulares);
        model.addAttribute("promedioEstrellas", promedio);
        model.addAttribute("nuevoCelular", new pe.edu.utp.model.Celular());
        return "proveedor_dashboard";
    }

    // Guarda un nuevo celular o actualiza uno existente
    @PostMapping("/proveedor/guardar")
    public String guardarCelular(@Valid @ModelAttribute("nuevoCelular") pe.edu.utp.model.Celular celular,
            org.springframework.validation.BindingResult result,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes,
            jakarta.servlet.http.HttpSession session) {

        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null)
            return "redirect:/login";

        // Si hay errores de validación, redirige de vuelta al dashboard con un mensaje
        // de error
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorValidacion",
                    "Error: No se pudo guardar. Verifica que el precio y stock sean mayores a cero.");
            return "redirect:/proveedor/dashboard";
        }

        // Si todo está correcto, guarda normalmente
        pe.edu.utp.model.Usuario proveedor = usuarioService.buscarPorId(usuarioId).orElse(null);
        if (proveedor != null) {
            // Asignamos el proveedor al celular antes de guardarlo
            celular.setProveedor(proveedor);
            celularService.guardar(celular);
            redirectAttributes.addFlashAttribute("exito", "¡Celular publicado/actualizado correctamente!");
        }
        return "redirect:/proveedor/dashboard";
    }

    // Elimina un celular del catálogo
    @GetMapping("/proveedor/eliminar/{id}")
    public String eliminarCelular(@PathVariable Long id) {
        celularService.eliminar(id);
        return "redirect:/proveedor/dashboard";
    }
}