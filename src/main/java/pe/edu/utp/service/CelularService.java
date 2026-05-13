package pe.edu.utp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pe.edu.utp.model.Celular;
import pe.edu.utp.repository.CelularRepository;
import java.util.List;
import java.util.Optional;

@Service
public class CelularService {

    @Autowired
    private CelularRepository celularRepository;

    // Para mostrar todos los celulares en la página principal
    public List<Celular> listarTodos() {
        return celularRepository.findAll();
    }

    // Para mostrar solo los celulares de un proveedor específico en su dashboard
    public List<Celular> listarPorProveedor(Long proveedorId) {
        return celularRepository.findByProveedorId(proveedorId);
    }

    public Optional<Celular> buscarPorId(Long id) {
        return celularRepository.findById(id);
    }

    public void guardar(Celular celular) {
        celularRepository.save(celular);
    }

    public void eliminar(Long id) {
        celularRepository.deleteById(id);
    }
}