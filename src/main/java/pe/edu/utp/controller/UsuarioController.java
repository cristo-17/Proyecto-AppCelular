package pe.edu.utp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import pe.edu.utp.model.Usuario;
import pe.edu.utp.service.UsuarioService;
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
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    // Muestra la página de registro
    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "registro";
    }

    // Procesa el formulario de registro
    @PostMapping("/registro")
    public String registrarUsuario(@Valid @ModelAttribute("usuario") pe.edu.utp.model.Usuario usuario,
            BindingResult result,
            Model model) {

        // Verifica que no haya errores de validación
        if (result.hasErrors()) {
            return "registro";
        }

        // Verifica que el correo no esté ya registrado
        if (usuarioService.existeCorreo(usuario.getCorreo())) {
            model.addAttribute("errorGlobal", "Este correo ya está registrado.");
            return "registro";
        }

        // ENCRIPTACIÓN AQUÍ: Hasheamos la contraseña antes de mandarla a la BD
        usuario.setContrasena(passwordEncoder.encode(usuario.getContrasena()));

        usuarioService.guardar(usuario);

        return "redirect:/login?exito=true";
    }

    // Muestra el perfil del usuario con sus datos y métodos de pago
    @GetMapping("/perfil")
    public String verPerfil(Model model, jakarta.servlet.http.HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return "redirect:/login";
        }

        Usuario usuario = usuarioService.buscarPorId(usuarioId).orElse(null);
        model.addAttribute("usuario", usuario);

        List<pe.edu.utp.model.FormaPago> pagos = formaPagoService.listarPorUsuario(usuarioId);
        model.addAttribute("formasPago", pagos);
        model.addAttribute("nuevaFormaPago", new pe.edu.utp.model.FormaPago());

        return "perfil";
    }

    // Guarda una nueva forma de pago para el usuario
    @PostMapping("/perfil/pago/guardar")
    public String guardarFormaPago(@ModelAttribute pe.edu.utp.model.FormaPago nuevaFormaPago,
            jakarta.servlet.http.HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId != null) {
            Usuario usuario = usuarioService.buscarPorId(usuarioId).orElse(null);
            nuevaFormaPago.setUsuario(usuario);

            if (nuevaFormaPago.getFechaExpiracion() == null || nuevaFormaPago.getFechaExpiracion().isEmpty()) {
                nuevaFormaPago.setFechaExpiracion("N/A");
            }

            formaPagoService.guardar(nuevaFormaPago);
        }
        return "redirect:/perfil";
    }

    // Elimina un método de pago de la lista
    @GetMapping("/perfil/pago/eliminar/{id}")
    public String eliminarPago(@PathVariable Long id) {
        formaPagoService.eliminar(id);
        return "redirect:/perfil";
    }

    // Elimina la cuenta del usuario logueado
    @GetMapping("/perfil/eliminarCuenta")
    public String eliminarMiCuenta(jakarta.servlet.http.HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId != null) {
            usuarioService.eliminar(usuarioId);
            session.invalidate();
        }
        return "redirect:/";
    }
}