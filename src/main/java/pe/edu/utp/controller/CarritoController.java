package pe.edu.utp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.servlet.http.HttpSession;
import pe.edu.utp.model.Celular;
import pe.edu.utp.model.ItemCarrito;
import pe.edu.utp.service.CelularService;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/carrito")
public class CarritoController {

    @Autowired
    private CelularService celularService;

    // Aquí está el GetMapping que carga la página principal del carrito
    // 1. Ver el carrito
    @GetMapping
    public String verCarrito(HttpSession session, Model model) {
        // Validar que sea un comprador logueado
        if (session.getAttribute("usuarioId") == null || !"COMPRADOR".equals(session.getAttribute("usuarioRol"))) {
            return "redirect:/login";
        }

        // Obtener el carrito de la sesión
        List<ItemCarrito> carrito = obtenerCarritoDeSesion(session);

        double total = 0;
        for (ItemCarrito item : carrito) {
            // --- ¡LA CURA AL ERROR 500! ---
            // Refrescamos el celular desde la base de datos para reconectar sus relaciones
            // (como el proveedor)
            Celular celularFresco = celularService.buscarPorId(item.getCelular().getId()).orElse(null);
            if (celularFresco != null) {
                item.setCelular(celularFresco); // Reemplazamos el viejo por el reconectado
                total += item.getSubtotal();
            }
        }

        model.addAttribute("carrito", carrito);
        model.addAttribute("total", total);
        return "carrito";
    }

    @GetMapping("/agregar/{id}")
    public String agregarAlCarrito(@PathVariable Long id, HttpSession session) {
        if (session.getAttribute("usuarioId") == null || !"COMPRADOR".equals(session.getAttribute("usuarioRol"))) {
            return "redirect:/login";
        }

        Celular celular = celularService.buscarPorId(id).orElse(null);
        if (celular != null) {
            List<ItemCarrito> carrito = obtenerCarritoDeSesion(session);

            boolean existe = false;
            for (ItemCarrito item : carrito) {
                if (item.getCelular().getId().equals(celular.getId())) {
                    item.setCantidad(item.getCantidad() + 1);
                    existe = true;
                    break;
                }
            }
            if (!existe) {
                carrito.add(new ItemCarrito(celular, 1));
            }

            session.setAttribute("miCarrito", carrito);
        }
        return "redirect:/carrito";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarDelCarrito(@PathVariable Long id, HttpSession session) {
        List<ItemCarrito> carrito = obtenerCarritoDeSesion(session);
        carrito.removeIf(item -> item.getCelular().getId().equals(id));
        session.setAttribute("miCarrito", carrito);
        return "redirect:/carrito";
    }

    @SuppressWarnings("unchecked")
    private List<ItemCarrito> obtenerCarritoDeSesion(HttpSession session) {
        List<ItemCarrito> carrito = (List<ItemCarrito>) session.getAttribute("miCarrito");
        if (carrito == null) {
            carrito = new ArrayList<>();
        }
        return carrito;
    }
}