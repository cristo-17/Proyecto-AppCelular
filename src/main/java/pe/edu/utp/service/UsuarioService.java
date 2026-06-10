package pe.edu.utp.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import pe.edu.utp.model.Usuario;
import pe.edu.utp.repository.UsuarioRepository;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    // Lista todos los usuarios
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    // Busca un usuario específico por su ID
    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    // Busca un usuario por su correo
    public Usuario buscarPorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo);
    }

    // Guarda o actualiza un usuario
    public void guardar(Usuario usuario) {
        usuarioRepository.save(usuario);
    }

    // Elimina un usuario por su ID
    public void eliminar(Long id) {
        usuarioRepository.deleteById(id);
    }

    // Método para verificar si un correo ya existe en la base de datos
    public boolean existeCorreo(String correo) {
        // reutilizamos el método buscarPorCorreo para verificar si el correo ya está
        // registrado
        Usuario usuarioEncontrado = buscarPorCorreo(correo);
        // si usuarioEncontrado es diferente de null, significa que el correo ya existe,
        // por lo tanto retornamos true, de lo contrario retornamos false
        return usuarioEncontrado != null;
    }
}