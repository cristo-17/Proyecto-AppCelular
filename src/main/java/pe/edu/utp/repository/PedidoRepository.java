package pe.edu.utp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.utp.model.Pedido;
import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    // Busca los pedidos de un proveedor específico ordenados por los más recientes
    List<Pedido> findByProveedorIdOrderByFechaPedidoDesc(Long proveedorId);
}