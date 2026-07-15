package pe.edu.utp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.model.Celular;
import pe.edu.utp.model.Pedido;
import pe.edu.utp.service.CelularService;
import pe.edu.utp.service.PedidoService;
import pe.edu.utp.service.UsuarioService;
import jakarta.validation.Valid;

import java.util.List;

@Controller
public class CelularController {

    @Autowired
    private CelularService celularService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private PedidoService pedidoService; // <-- Nuevo servicio inyectado

    @GetMapping("/")
    public String mostrarCatalogoPrincipal(Model model) {
        List<Celular> celulares = celularService.listarTodos();
        model.addAttribute("celulares", celulares);
        return "catalogo";
    }

    @GetMapping("/catalogo")
    public String mostrarCatalogo(Model model) {
        List<Celular> celulares = celularService.listarTodos();
        model.addAttribute("celulares", celulares);
        return "catalogo";
    }

    @GetMapping("/catalogo/marca/{marca}")
    public String mostrarCatalogoPorMarca(@PathVariable String marca, Model model) {
        List<Celular> celularesFiltrados = celularService.buscarPorMarca(marca);
        model.addAttribute("celulares", celularesFiltrados);
        return "catalogo";
    }

    @GetMapping("/legal/politicas")
    public String mostrarPoliticas() {
        return "politicas";
    }

    @GetMapping("/legal/terminos")
    public String mostrarTerminos() {
        return "terminos";
    }

    @GetMapping("/proveedor/dashboard")
    public String dashboardProveedor(Model model, jakarta.servlet.http.HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");

        if (usuarioId == null || !"PROVEEDOR".equals(session.getAttribute("usuarioRol"))) {
            return "redirect:/login";
        }

        List<pe.edu.utp.model.Celular> misCelulares = celularService.listarPorProveedor(usuarioId);

        // 1. Lógica de calificaciones
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
        double promedio = (totalResenas > 0) ? (sumaEstrellas / totalResenas) : 0.0;

        // 2. Traer los pedidos reales de este proveedor
        List<Pedido> misPedidos = pedidoService.listarPorProveedor(usuarioId);

        // 3. Calcular ingresos matemáticamente sumando el total de pedidos
        // completados/en camino
        double ingresos = 0.0;
        for (Pedido p : misPedidos) {
            ingresos += p.getTotal();
        }

        model.addAttribute("celulares", misCelulares);
        model.addAttribute("promedioEstrellas", promedio);
        model.addAttribute("nuevoCelular", new pe.edu.utp.model.Celular());
        model.addAttribute("misPedidos", misPedidos); // Pasamos los pedidos reales a la vista
        model.addAttribute("ingresosMes", ingresos); // Pasamos la suma real de dinero

        return "proveedor_dashboard";
    }

    @PostMapping("/proveedor/guardar")
    public String guardarCelular(@Valid @ModelAttribute("nuevoCelular") pe.edu.utp.model.Celular celular,
            org.springframework.validation.BindingResult result,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes,
            jakarta.servlet.http.HttpSession session) {

        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null)
            return "redirect:/login";

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorValidacion",
                    "Error: No se pudo guardar. Verifica que el precio y stock sean mayores a cero.");
            return "redirect:/proveedor/dashboard";
        }

        pe.edu.utp.model.Usuario proveedor = usuarioService.buscarPorId(usuarioId).orElse(null);
        if (proveedor != null) {
            celular.setProveedor(proveedor);
            celularService.guardar(celular);
            redirectAttributes.addFlashAttribute("exito", "¡Celular publicado/actualizado correctamente!");
        }
        return "redirect:/proveedor/dashboard";
    }

    @GetMapping("/proveedor/eliminar/{id}")
    public String eliminarCelular(@PathVariable Long id) {
        celularService.eliminar(id);
        return "redirect:/proveedor/dashboard";
    }

    // NUEVO: Ruta para cambiar el estado logístico de un pedido
    @PostMapping("/proveedor/pedido/estado")
    public String actualizarEstadoPedido(@RequestParam Long pedidoId, @RequestParam String nuevoEstado,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        pedidoService.actualizarEstadoLogistico(pedidoId, nuevoEstado);
        redirectAttributes.addFlashAttribute("exito", "El estado del pedido ha sido actualizado a: " + nuevoEstado);
        return "redirect:/proveedor/dashboard";
    }
}