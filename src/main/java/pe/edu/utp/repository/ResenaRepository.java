package pe.edu.utp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.utp.model.Resena;
import java.util.List;

@Repository
public interface ResenaRepository extends JpaRepository<Resena, Long> {
    // Busca todas las reseñas de un celular específico
    List<Resena> findByCelularId(Long celularId);
}