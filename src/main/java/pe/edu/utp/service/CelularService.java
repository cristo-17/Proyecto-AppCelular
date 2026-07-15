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

    public List<Celular> listarTodos() {
        return celularRepository.findAll();
    }

    public List<Celular> listarPorProveedor(Long proveedorId) {
        return celularRepository.findByProveedorId(proveedorId);
    }

    // Añade esto dentro de tu clase CelularService
    public List<Celular> buscarPorMarca(String marca) {
        return celularRepository.findByMarcaIgnoreCase(marca);
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