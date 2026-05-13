package pe.edu.utp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.utp.model.FormaPago;
import java.util.List;

@Repository
public interface FormaPagoRepository extends JpaRepository<FormaPago, Long> {
    // Busca todas las formas de pago de un usuario
    List<FormaPago> findByUsuarioId(Long usuarioId);
}