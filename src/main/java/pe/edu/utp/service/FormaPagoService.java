package pe.edu.utp.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import pe.edu.utp.model.FormaPago;
import pe.edu.utp.repository.FormaPagoRepository;

@Service
public class FormaPagoService {

    @Autowired
    private FormaPagoRepository formaPagoRepository;

    public void guardar(FormaPago formaPago) {
        formaPagoRepository.save(formaPago);
    }

    public List<FormaPago> listarPorUsuario(Long usuarioId) {
        return formaPagoRepository.findByUsuarioId(usuarioId);
    }

    public void eliminar(Long id) {
        formaPagoRepository.deleteById(id);
    }
}