package pe.edu.utp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pe.edu.utp.model.Pedido;
import pe.edu.utp.repository.PedidoRepository;
import java.util.List;

@Service
public class PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;

    public List<Pedido> listarPorProveedor(Long proveedorId) {
        return pedidoRepository.findByProveedorIdOrderByFechaPedidoDesc(proveedorId);
    }

    public void actualizarEstadoLogistico(Long pedidoId, String nuevoEstado) {
        Pedido pedido = pedidoRepository.findById(pedidoId).orElse(null);
        if (pedido != null) {
            pedido.setEstadoLogistico(nuevoEstado);
            pedidoRepository.save(pedido);
        }
    }
}