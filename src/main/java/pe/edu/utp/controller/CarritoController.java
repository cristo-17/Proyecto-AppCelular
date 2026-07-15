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

    @Autowired
    private pe.edu.utp.service.FormaPagoService formaPagoService;

    @GetMapping
    public String verCarrito(HttpSession session, Model model) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");

        if (usuarioId == null || !"COMPRADOR".equals(session.getAttribute("usuarioRol"))) {
            return "redirect:/login";
        }

        List<ItemCarrito> carrito = obtenerCarritoDeSesion(session);
        double subtotal = 0;

        for (ItemCarrito item : carrito) {
            Celular celularFresco = celularService.buscarPorId(item.getCelular().getId()).orElse(null);
            if (celularFresco != null) {
                item.setCelular(celularFresco);

                // PROGRAMACIÓN DEFENSIVA: Evitamos NullPointerExceptions si el stock está vacío
                // en la BD
                int stockReal = (celularFresco.getStock() != null) ? celularFresco.getStock() : 0;

                if (item.getCantidad() > stockReal) {
                    item.setCantidad(stockReal);
                }
                subtotal += item.getSubtotal();
            }
        }

        double costoEnvio = (subtotal >= 5000 || subtotal == 0) ? 0.0 : 50.0;
        double totalFinal = subtotal + costoEnvio;

        List<pe.edu.utp.model.FormaPago> tarjetas = formaPagoService.listarPorUsuario(usuarioId);
        boolean tieneTarjeta = (tarjetas != null && !tarjetas.isEmpty());

        model.addAttribute("carrito", carrito);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("costoEnvio", costoEnvio);
        model.addAttribute("totalFinal", totalFinal);
        model.addAttribute("tieneTarjeta", tieneTarjeta);

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

            // Verificamos de forma segura el stock
            int stockDisponible = (celular.getStock() != null) ? celular.getStock() : 0;

            for (ItemCarrito item : carrito) {
                if (item.getCelular().getId().equals(celular.getId())) {
                    if (item.getCantidad() < stockDisponible) {
                        item.setCantidad(item.getCantidad() + 1);
                    }
                    existe = true;
                    break;
                }
            }
            if (!existe && stockDisponible > 0) {
                carrito.add(new ItemCarrito(celular, 1));
            }
            session.setAttribute("miCarrito", carrito);
        }
        return "redirect:/carrito";
    }

    @GetMapping("/restar/{id}")
    public String restarDelCarrito(@PathVariable Long id, HttpSession session) {
        List<ItemCarrito> carrito = obtenerCarritoDeSesion(session);
        for (ItemCarrito item : carrito) {
            if (item.getCelular().getId().equals(id)) {
                if (item.getCantidad() > 1) {
                    item.setCantidad(item.getCantidad() - 1);
                }
                break;
            }
        }
        session.setAttribute("miCarrito", carrito);
        return "redirect:/carrito";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarDelCarrito(@PathVariable Long id, HttpSession session) {
        List<ItemCarrito> carrito = obtenerCarritoDeSesion(session);
        carrito.removeIf(item -> item.getCelular().getId().equals(id));
        session.setAttribute("miCarrito", carrito);
        return "redirect:/carrito";
    }

    @PostMapping("/pagar")
    public String procesarPago(HttpSession session, RedirectAttributes redirectAttributes) {
        List<ItemCarrito> carrito = obtenerCarritoDeSesion(session);
        if (carrito.isEmpty()) {
            return "redirect:/carrito";
        }

        for (ItemCarrito item : carrito) {
            Celular celularBD = celularService.buscarPorId(item.getCelular().getId()).orElse(null);
            if (celularBD != null) {
                int stockActual = (celularBD.getStock() != null) ? celularBD.getStock() : 0;
                int nuevoStock = stockActual - item.getCantidad();
                if (nuevoStock < 0)
                    nuevoStock = 0;
                celularBD.setStock(nuevoStock);
                celularService.guardar(celularBD);
            }
        }

        session.removeAttribute("miCarrito");
        redirectAttributes.addFlashAttribute("exitoCompra", "¡Pago realizado con éxito! Tu orden ha sido procesada.");
        return "redirect:/catalogo";
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