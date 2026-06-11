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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/carrito")
public class CarritoController {

    @Autowired
    private CelularService celularService;

    // Ver el carrito de compras
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
            // Busca la infomación más reciente del celular en la base de datos
            Celular celularFresco = celularService.buscarPorId(item.getCelular().getId()).orElse(null);
            // Si el celular existe, actualizamos su información en el carrito y calculamos
            // el subtotal
            if (celularFresco != null) {
                item.setCelular(celularFresco);
                total += item.getSubtotal();
            }
        }

        model.addAttribute("carrito", carrito);
        model.addAttribute("total", total);
        return "carrito";
    }

    // Agregar un celular al carrito
    @GetMapping("/agregar/{id}")
    public String agregarAlCarrito(@PathVariable Long id, HttpSession session) {
        if (session.getAttribute("usuarioId") == null || !"COMPRADOR".equals(session.getAttribute("usuarioRol"))) {
            return "redirect:/login";
        }

        Celular celular = celularService.buscarPorId(id).orElse(null);
        if (celular != null) {
            List<ItemCarrito> carrito = obtenerCarritoDeSesion(session);

            boolean existe = false;
            // Verificar si el celular ya está en el carrito para sumar la cantidad
            for (ItemCarrito item : carrito) {
                if (item.getCelular().getId().equals(celular.getId())) {
                    item.setCantidad(item.getCantidad() + 1);
                    existe = true;
                    break;
                }
            }
            // Si no existe, lo agregamos como un nuevo item al carrito
            if (!existe) {
                carrito.add(new ItemCarrito(celular, 1));
            }

            session.setAttribute("miCarrito", carrito);
        }
        return "redirect:/carrito";
    }

    // Eliminar un celular del carrito
    @GetMapping("/eliminar/{id}")
    public String eliminarDelCarrito(@PathVariable Long id, HttpSession session) {
        List<ItemCarrito> carrito = obtenerCarritoDeSesion(session);
        carrito.removeIf(item -> item.getCelular().getId().equals(id));
        session.setAttribute("miCarrito", carrito);
        return "redirect:/carrito";
    }

    // Procesar el pago y actualizar el stock de los celulares comprados
    @PostMapping("/pagar")
    public String procesarPago(HttpSession session, RedirectAttributes redirectAttributes) {

        List<ItemCarrito> carrito = obtenerCarritoDeSesion(session);

        if (carrito.isEmpty()) {
            return "redirect:/carrito";
        }

        for (ItemCarrito item : carrito) {
            Celular celularBD = celularService.buscarPorId(item.getCelular().getId()).orElse(null);

            if (celularBD != null) {
                // Calculamos el nuevo stock restando la cantidad comprada al stock actual
                int nuevoStock = celularBD.getStock() - item.getCantidad();
                // Evitamos que el stock quede en números negativos por seguridad
                if (nuevoStock < 0) {
                    nuevoStock = 0;
                }
                celularBD.setStock(nuevoStock);
                celularService.guardar(celularBD);
            }
        }

        session.removeAttribute("miCarrito");

        redirectAttributes.addFlashAttribute("exitoCompra", "¡Pago realizado con éxito! Tu orden ha sido procesada.");
        return "redirect:/catalogo";
    }

    // Método auxiliar para obtener el carrito de la sesión, con manejo de tipo
    // seguro
    @SuppressWarnings("unchecked")
    private List<ItemCarrito> obtenerCarritoDeSesion(HttpSession session) {
        List<ItemCarrito> carrito = (List<ItemCarrito>) session.getAttribute("miCarrito");
        if (carrito == null) {
            carrito = new ArrayList<>();
        }
        return carrito;
    }
}