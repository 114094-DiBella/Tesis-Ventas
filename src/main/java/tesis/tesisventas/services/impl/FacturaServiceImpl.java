package tesis.tesisventas.services.impl;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tesis.tesisventas.dtos.DetalleRequest;
import tesis.tesisventas.dtos.FacturaRequest;
import tesis.tesisventas.entities.DetalleFacturaEntity;
import tesis.tesisventas.entities.FacturaEntity;
import tesis.tesisventas.models.DetalleFactura;
import tesis.tesisventas.models.Factura;
import tesis.tesisventas.repositories.DetalleJpaRepository;
import tesis.tesisventas.repositories.FacturaJpaRepository;
import tesis.tesisventas.services.FacturaService;

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

        // Crear la entidad factura sin asignar ID (será generado por JPA)
        FacturaEntity facturaEntity = new FacturaEntity();
        facturaEntity.setUserId(request.getUserId());

        facturaEntity.setIdFormaPago(request.getIdFormaPago());
        facturaEntity.setCreatedAt(LocalDateTime.now());

        // Inicializar la lista de detalles
        facturaEntity.setDetalles(new ArrayList<>());

        // Guardar la factura para obtener su ID generado
        FacturaEntity savedFactura = facturaJpaRepository.save(facturaEntity);

        // Procesar los detalles si existen
        if (request.getDetalles() != null && !request.getDetalles().isEmpty()) {
            for (DetalleRequest detalleRequest : request.getDetalles()) {
                // Crear la entidad detalle
                DetalleFacturaEntity detalle = new DetalleFacturaEntity();
                // No asignamos ID (será generado por JPA)
                detalle.setIdFactura(savedFactura.getId());
                detalle.setIdProducto(detalleRequest.getIdProducto());
                detalle.setCantidad(detalleRequest.getCantidad());


                // Establecer la relación con la factura
                detalle.setFactura(savedFactura);

                // Guardar el detalle
                DetalleFacturaEntity savedDetalle = detalleJpaRepository.save(detalle);

                // Añadir el detalle guardado a la lista de detalles de la factura
                savedFactura.getDetalles().add(savedDetalle);
            }
        }

        // Mapear a modelo de dominio y retornar
            Factura factura =  modelMapper.map(savedFactura, Factura.class);
        return factura;
    }

    @Override
    @Transactional
    public Factura update(UUID id, FacturaRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        // Buscar la factura a actualizar
        Optional<FacturaEntity> optionalFactura = facturaJpaRepository.findById(id);
        if (optionalFactura.isEmpty()) {
            return null; // O lanzar excepción
        }

        FacturaEntity facturaEntity = optionalFactura.get();

        facturaEntity.setUserId(request.getUserId());

        facturaEntity.setIdFormaPago(request.getIdFormaPago());

        facturaEntity.getDetalles().clear();


        // Agregar los nuevos detalles
        if (request.getDetalles() != null && !request.getDetalles().isEmpty()) {
            for (DetalleRequest detalleRequest : request.getDetalles()) {
                DetalleFacturaEntity detalle = new DetalleFacturaEntity();
                detalle.setIdFactura(id);
                detalle.setIdProducto(detalleRequest.getIdProducto());
                detalle.setCantidad(detalleRequest.getCantidad());

                detalle.setFactura(facturaEntity);

                // Si usas cascada, solo añadirlo a la lista es suficiente
                facturaEntity.getDetalles().add(detalle);
            }
        }

        // Guardar la factura actualizada con sus detalles
        FacturaEntity updatedFactura = facturaJpaRepository.save(facturaEntity);

        return modelMapper.map(updatedFactura, Factura.class);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Optional<FacturaEntity> optionalFactura = facturaJpaRepository.findById(id);
        if (optionalFactura.isPresent()) {
            optionalFactura.get().setActive(false);
            return;
        }
        return;
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
}