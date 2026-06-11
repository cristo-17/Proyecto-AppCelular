package pe.edu.utp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import pe.edu.utp.model.Celular;
import pe.edu.utp.model.Resena;
import pe.edu.utp.model.Usuario;
import pe.edu.utp.service.CelularService;
import pe.edu.utp.service.ResenaService;
import pe.edu.utp.service.UsuarioService;

@Controller
public class ResenaController {

    @Autowired
    private ResenaService resenaService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private CelularService celularService;

    // Guarda una nueva reseña
    @PostMapping("/resenas/guardar")
    public String guardarResena(@RequestParam String comentario,
            @RequestParam Integer estrellas,
            @RequestParam Long celularId,
            HttpSession session) {

        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null)
            return "redirect:/login";

        Usuario comprador = usuarioService.buscarPorId(usuarioId).orElse(null);
        Celular celular = celularService.buscarPorId(celularId).orElse(null);

        if (comprador != null && celular != null) {
            Resena nuevaResena = new Resena();
            nuevaResena.setComentario(comentario);
            nuevaResena.setEstrellas(estrellas);
            nuevaResena.setComprador(comprador);
            nuevaResena.setCelular(celular);
            resenaService.guardar(nuevaResena);
        }
        return "redirect:/catalogo";
    }

    // Edita una reseña existente
    @PostMapping("/resenas/editar")
    public String editarResena(@RequestParam Long id,
            @RequestParam String comentario,
            @RequestParam Integer estrellas,
            HttpSession session) {

        Long usuarioId = (Long) session.getAttribute("usuarioId");
        Resena resena = resenaService.buscarPorId(id).orElse(null);

        // Solo edita si es el dueño de la reseña
        if (resena != null && usuarioId != null && resena.getComprador().getId().equals(usuarioId)) {
            resena.setComentario(comentario);
            resena.setEstrellas(estrellas);
            resenaService.guardar(resena);
        }
        return "redirect:/catalogo";
    }

    // Elimina una reseña
    @GetMapping("/resenas/eliminar/{id}")
    public String eliminarResena(@PathVariable Long id, HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        Resena resena = resenaService.buscarPorId(id).orElse(null);

        // Solo borra si es el dueño
        if (resena != null && usuarioId != null && resena.getComprador().getId().equals(usuarioId)) {
            resenaService.eliminar(id);
        }
        return "redirect:/catalogo";
    }
}