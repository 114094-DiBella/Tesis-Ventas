package tesis.tesisventas.services.impl;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tesis.tesisventas.client.impl.ProductClientServiceImpl;
import tesis.tesisventas.dtos.DetalleRequest;
import tesis.tesisventas.dtos.FacturaRequest;
import tesis.tesisventas.dtos.ProductResponse;
import tesis.tesisventas.entities.DetalleFacturaEntity;
import tesis.tesisventas.entities.FacturaEntity;
import tesis.tesisventas.models.DetalleFactura;
import tesis.tesisventas.models.Factura;
import tesis.tesisventas.models.Status;
import tesis.tesisventas.repositories.DetalleJpaRepository;
import tesis.tesisventas.repositories.FacturaJpaRepository;
import tesis.tesisventas.services.FacturaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FacturaServiceImpl implements FacturaService {

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private FacturaJpaRepository facturaJpaRepository;

    @Autowired
    private DetalleJpaRepository detalleJpaRepository;

    private static final Logger logger = LoggerFactory.getLogger(FacturaServiceImpl.class);

    private static final BigDecimal descuentoEfec = BigDecimal.valueOf(0.15);
    @Autowired
    private ProductClientServiceImpl productClient;

    @Override
    @Transactional(readOnly = true)
    public List<Factura> getAll() {
        List<FacturaEntity> facturaList = facturaJpaRepository.findAll();
        return facturaList.stream()
                .map(entity -> modelMapper.map(entity, Factura.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Factura getById(UUID id) {
        Optional<FacturaEntity> entity = facturaJpaRepository.findById(id);
        return entity.map(facturaEntity -> modelMapper.map(facturaEntity, Factura.class)).orElse(null);
    }

    @Override
    @Transactional
    public Factura create(FacturaRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        FacturaEntity facturaEntity = new FacturaEntity();
        facturaEntity.setUserId(request.getUserId());
        facturaEntity.setIdFormaPago(request.getIdFormaPago());
        facturaEntity.setCreatedAt(LocalDateTime.now());
        facturaEntity.setDetalles(new ArrayList<>());
        facturaEntity.setStatus(String.valueOf(Status.PENDIENTE));
        FacturaEntity savedFactura = facturaJpaRepository.save(facturaEntity);

        if (request.getDetalles() != null && !request.getDetalles().isEmpty()) {
            for (DetalleRequest detalleRequest : request.getDetalles()) {
                ProductResponse producto = productClient.getProductById(detalleRequest.getIdProducto());

                if (producto == null) {
                    throw new IllegalArgumentException("Producto no encontrado: " + detalleRequest.getIdProducto());
                }

                if (producto.getStock().compareTo(detalleRequest.getCantidad()) < 0) {
                    throw new IllegalArgumentException("Stock insuficiente para el producto: " + producto.getName());
                }

                DetalleFacturaEntity detalle = new DetalleFacturaEntity();
                detalle.setIdFactura(savedFactura.getId());
                detalle.setIdProducto(detalleRequest.getIdProducto());
                detalle.setCantidad(detalleRequest.getCantidad());
                detalle.setPrecioUnitario(producto.getPrice());
                detalle.setSubtotal(producto.getPrice().multiply(new BigDecimal(detalleRequest.getCantidad())));
                detalle.setFactura(savedFactura);


                DetalleFacturaEntity savedDetalle = detalleJpaRepository.save(detalle);
                savedFactura.getDetalles().add(savedDetalle);
            }
            facturaEntity.calcularTotal();
            facturaJpaRepository.save(facturaEntity);
            //if(!facturaEntity.getFormaPago().getName().equals("Efectivo")){
                //TODO: Llamar al web client de medio de pago para que procese el pago
                //TODO: Actualizar el estado de la factura a PAGADA o RECHAZADA segun corresponda
            //}

            BigDecimal descuento = facturaEntity.getTotal().multiply(descuentoEfec);

            facturaEntity.setTotal(facturaEntity.getTotal().subtract(descuento));
            actualizarProducto(request);
        }
      return modelMapper.map(savedFactura, Factura.class);
    }

    @Override
    @Transactional
    public Factura update(UUID id, FacturaRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        Optional<FacturaEntity> optionalFactura = facturaJpaRepository.findById(id);
        if (optionalFactura.isEmpty()) {
            logger.error("Factura no encontrada: " + id);
            return null;
        }

        FacturaEntity facturaEntity = optionalFactura.get();
        facturaEntity.setUserId(request.getUserId());
        facturaEntity.setIdFormaPago(request.getIdFormaPago());
        facturaEntity.getDetalles().clear();

        if (request.getDetalles() != null && !request.getDetalles().isEmpty()) {
            for (DetalleRequest detalleRequest : request.getDetalles()) {
                ProductResponse producto = productClient.getProductById(detalleRequest.getIdProducto());

                if (producto == null) {
                    throw new IllegalArgumentException("Producto no encontrado: " + detalleRequest.getIdProducto());
                }

                if (producto.getStock().compareTo(detalleRequest.getCantidad()) < 0) {
                    throw new IllegalArgumentException("Stock insuficiente para el producto: " + producto.getName());
                }

                DetalleFacturaEntity detalle = new DetalleFacturaEntity();
                detalle.setIdFactura(id);
                detalle.setIdProducto(detalleRequest.getIdProducto());
                detalle.setCantidad(detalleRequest.getCantidad());
                detalle.setPrecioUnitario(producto.getPrice());
                detalle.setSubtotal(producto.getPrice().multiply(new BigDecimal(detalleRequest.getCantidad())));
                detalle.setFactura(facturaEntity);

                facturaEntity.getDetalles().add(detalle);
            }
        }

        facturaEntity.calcularTotal();
        FacturaEntity updatedFactura = facturaJpaRepository.save(facturaEntity);
        actualizarProducto(request); //Metodo privado

        return modelMapper.map(updatedFactura, Factura.class);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Optional<FacturaEntity> optionalFactura = facturaJpaRepository.findById(id);
        if (optionalFactura.isPresent()) {
            optionalFactura.get().setStatus(String.valueOf(Status.RECHAZADA));
            facturaJpaRepository.save(optionalFactura.get());
            logger.info("Factura " + id + " eliminada");
        }
    }

    @Transactional(readOnly = true)
    @Override
    public List<Factura> getByUserId(UUID userId) {
        List<FacturaEntity> facturas = facturaJpaRepository.findByUserId(userId);
        return facturas.stream()
                .map(entity -> modelMapper.map(entity, Factura.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public List<Factura> getByFormaPago(UUID idFormaPago) {
        List<FacturaEntity> facturas = facturaJpaRepository.findByIdFormaPago(idFormaPago);
        return facturas.stream()
                .map(entity -> modelMapper.map(entity, Factura.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public List<DetalleFactura> getDetallesByProducto(UUID idProducto) {
        List<DetalleFacturaEntity> detalles = detalleJpaRepository.findByIdProducto(idProducto);
        return detalles.stream()
                .map(entity -> modelMapper.map(entity, DetalleFactura.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public List<DetalleFactura> getDetallesByFactura(UUID idFactura) {
        List<DetalleFacturaEntity> detalles = detalleJpaRepository.findByIdFactura(idFactura);
        return detalles.stream()
                .map(entity -> modelMapper.map(entity, DetalleFactura.class))
                .collect(Collectors.toList());
    }


    private void actualizarProducto(FacturaRequest request) {
        for (DetalleRequest detalleRequest : request.getDetalles()) {
            boolean stockUpdated = productClient.updateProductStock(
                    detalleRequest.getIdProducto(),
                    detalleRequest.getCantidad()
            );
            if (!stockUpdated) {
                logger.warn("No se pudo actualizar el stock del producto: {}", detalleRequest.getIdProducto());
            }
        }
    }
}