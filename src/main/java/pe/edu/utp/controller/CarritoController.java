package pe.edu.utp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional; // <-- IMPORTANTE
import jakarta.servlet.http.HttpSession;

import pe.edu.utp.model.Celular;
import pe.edu.utp.model.ItemCarrito;
import pe.edu.utp.model.Pedido;
import pe.edu.utp.model.DetallePedido;
import pe.edu.utp.model.Usuario;
import pe.edu.utp.service.CelularService;
import pe.edu.utp.service.UsuarioService;
import pe.edu.utp.repository.PedidoRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/carrito")
public class CarritoController {

    @Autowired
    private CelularService celularService;

    @Autowired
    private pe.edu.utp.service.FormaPagoService formaPagoService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private PedidoRepository pedidoRepository;

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

    // ============================================================
    // LA ETIQUETA @Transactional HACE LA MAGIA DE GUARDADO
    // ============================================================
    @PostMapping("/pagar")
    @Transactional
    public String procesarPago(@RequestParam("direccionEnvio") String direccionEnvio, HttpSession session,
            RedirectAttributes redirectAttributes) {
        List<ItemCarrito> carrito = obtenerCarritoDeSesion(session);
        if (carrito.isEmpty()) {
            return "redirect:/carrito";
        }

        Long usuarioId = (Long) session.getAttribute("usuarioId");
        Usuario comprador = usuarioService.buscarPorId(usuarioId).orElse(null);

        if (comprador == null)
            return "redirect:/login";

        // Agrupamos por ID del Proveedor
        Map<Long, List<ItemCarrito>> itemsPorProveedor = new HashMap<>();
        for (ItemCarrito item : carrito) {
            Celular celularFresco = celularService.buscarPorId(item.getCelular().getId()).orElse(null);
            if (celularFresco != null) {
                item.setCelular(celularFresco);
                Long idProv = celularFresco.getProveedor().getId();
                itemsPorProveedor.computeIfAbsent(idProv, k -> new ArrayList<>()).add(item);
            }
        }

        for (Map.Entry<Long, List<ItemCarrito>> entry : itemsPorProveedor.entrySet()) {
            Usuario proveedor = usuarioService.buscarPorId(entry.getKey()).orElse(null);
            List<ItemCarrito> items = entry.getValue();

            if (proveedor == null)
                continue;

            Pedido pedido = new Pedido();
            pedido.setNumeroOrden("ORD-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            pedido.setFechaPedido(LocalDateTime.now());
            pedido.setEstadoLogistico("Pendiente");
            pedido.setDireccionEnvio(direccionEnvio);
            pedido.setComprador(comprador);
            pedido.setProveedor(proveedor);

            double totalPedido = 0;
            List<DetallePedido> detalles = new ArrayList<>();

            for (ItemCarrito item : items) {
                Celular celularBD = item.getCelular();

                // Reducción de Stock 100% segura
                int stockActual = celularBD.getStock() != null ? celularBD.getStock() : 0;
                int nuevoStock = stockActual - item.getCantidad();
                celularBD.setStock(Math.max(nuevoStock, 0));
                celularService.guardar(celularBD);

                DetallePedido detalle = new DetallePedido();
                detalle.setCantidad(item.getCantidad());
                detalle.setPrecioUnitario(celularBD.getPrecio());
                detalle.setSubtotal(item.getCantidad() * celularBD.getPrecio());
                detalle.setCelular(celularBD);
                detalle.setPedido(pedido);

                detalles.add(detalle);
                totalPedido += detalle.getSubtotal();
            }

            if (totalPedido < 5000) {
                totalPedido += 50.0;
            }

            pedido.setTotal(totalPedido);
            pedido.setDetalles(detalles);

            pedidoRepository.save(pedido);
        }

        session.removeAttribute("miCarrito");
        redirectAttributes.addFlashAttribute("exitoCompra",
                "¡Pago realizado con éxito! Tu orden ha sido enviada a los proveedores.");
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