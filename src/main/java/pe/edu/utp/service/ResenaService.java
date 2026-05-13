package pe.edu.utp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pe.edu.utp.model.Resena;
import pe.edu.utp.repository.ResenaRepository;
import java.util.List;

@Service
public class ResenaService {

    @Autowired
    private ResenaRepository resenaRepository;

    public void guardar(Resena resena) {
        resenaRepository.save(resena);
    }

    // Para mostrar los comentarios debajo de cada celular
    public List<Resena> listarPorCelular(Long celularId) {
        return resenaRepository.findByCelularId(celularId);
    }

    // Para buscar una reseña antes de editarla
    public java.util.Optional<Resena> buscarPorId(Long id) {
        return resenaRepository.findById(id);
    }

    // Para eliminar una reseña
    public void eliminar(Long id) {
        resenaRepository.deleteById(id);
    }
}