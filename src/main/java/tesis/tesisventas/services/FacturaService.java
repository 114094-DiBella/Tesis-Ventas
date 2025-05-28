package tesis.tesisventas.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tesis.tesisventas.dtos.FacturaRequest;
import tesis.tesisventas.dtos.OrderResponse;
import tesis.tesisventas.models.DetalleFactura;
import tesis.tesisventas.models.Factura;

import java.util.List;
import java.util.UUID;

@Service
public interface FacturaService {

    List<Factura> getAll();
    List<OrderResponse> getAllOrders();
    Factura getById(UUID id);
    Factura create(FacturaRequest request);
    Factura update(UUID id, FacturaRequest request);
    void delete(UUID id);

    @Transactional(readOnly = true)
    List<Factura> getByUserId(UUID userId);

    @Transactional(readOnly = true)
    List<Factura> getByFormaPago(UUID idFormaPago);

    @Transactional(readOnly = true)
    List<DetalleFactura> getDetallesByProducto(UUID idProducto);

    @Transactional(readOnly = true)
    List<DetalleFactura> getDetallesByFactura(UUID idFactura);
}
