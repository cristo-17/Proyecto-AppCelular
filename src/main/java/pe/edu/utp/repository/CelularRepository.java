package pe.edu.utp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.utp.model.Celular;
import java.util.List;

@Repository
public interface CelularRepository extends JpaRepository<Celular, Long> {
    // Busca todos los celulares de un proveedor
    List<Celular> findByProveedorId(Long proveedorId);
}