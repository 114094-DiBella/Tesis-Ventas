package tesis.tesisventas.services.impl;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tesis.tesisventas.client.impl.NotifyClientService;
import tesis.tesisventas.client.impl.PaymentClientService;
import tesis.tesisventas.client.impl.ProductClientServiceImpl;
import tesis.tesisventas.client.impl.UserClientServiceImpl;
import tesis.tesisventas.components.OrderCodeGenerator;
import tesis.tesisventas.dtos.*;
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
import java.math.BigInteger;
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

    @Autowired
    private OrderCodeGenerator orderCodeGenerator;

    private static final Logger logger = LoggerFactory.getLogger(FacturaServiceImpl.class);

    private static final BigDecimal descuentoEfec = BigDecimal.valueOf(0.15);

    private static final int EXPIRATION_MINUTES = 5;

    @Autowired
    private ProductClientServiceImpl productClient;

    @Autowired
    private UserClientServiceImpl userClient;

    @Autowired
    private PaymentClientService paymentClientService;

    @Autowired
    private NotifyClientService notifyClientService;


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

        String orderCode = orderCodeGenerator.generateSimpleCode();
        facturaEntity.setCodFactura(orderCode);

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

            savedFactura.calcularTotal();

            savedFactura = facturaJpaRepository.save(savedFactura);
            actualizarProducto(request);

        }
        Factura facturaResponse = modelMapper.map(savedFactura, Factura.class);

            try {
                PaymentRequest paymentReq = new PaymentRequest();
                paymentReq.setOrderCode(savedFactura.getId().toString());
                paymentReq.setAmount(savedFactura.getTotal());
                paymentReq.setDescription("Pago de factura " + savedFactura.getCodFactura());
                paymentReq.setQuantity(1);
                paymentReq.setPaymentMethodId(savedFactura.getIdFormaPago().toString());
                paymentReq.setProductName("Productos de la tienda");

                String paymentUrl = paymentClientService.createPaymentPreference(paymentReq);
                logger.info("URL de pago creada: {}", paymentUrl);

                facturaResponse.setPaymentUrl(paymentUrl);


            } catch (Exception e) {
                logger.error("Error creando preferencia de pago: {}", e.getMessage());
                // La factura se creó bien, solo falló el pago
            }

          UserResponse user = userClient.getUserById(request.getUserId());
          notifyClientService.sendPurchaseConfirmation(user.getEmail(), facturaEntity.getCodFactura(), facturaEntity.getTotal());
        return facturaResponse;
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


    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        List<FacturaEntity> facturaList = facturaJpaRepository.findAll();
        List<OrderResponse> orderResponses = new ArrayList<>();

        for (FacturaEntity facturaEntity : facturaList) {
            OrderResponse orderResponse = new OrderResponse();
            orderResponse.setDetails(new ArrayList<>());

            orderResponse.setCodOrder(facturaEntity.getCodFactura());
            orderResponse.setUser(userClient.getUserById(facturaEntity.getUserId()));

            // Process each detail
            for (DetalleFacturaEntity detalleFacturaEntity : facturaEntity.getDetalles()) {
                // Create NEW instance for each detail
                OrderDetailResponse orderDetailResponse = new OrderDetailResponse();
                orderDetailResponse.setProduct(productClient.getProductById(detalleFacturaEntity.getIdProducto()));
                orderDetailResponse.setQuantity(detalleFacturaEntity.getCantidad());
                orderDetailResponse.setPriceUnit(detalleFacturaEntity.getPrecioUnitario());
                orderDetailResponse.setSubTotal(detalleFacturaEntity.getSubtotal());

                orderResponse.getDetails().add(orderDetailResponse);
            }

            orderResponse.setIdFormaPago(facturaEntity.getIdFormaPago());
            orderResponse.setStatus(Status.valueOf(facturaEntity.getStatus()));
            orderResponse.setTotal(facturaEntity.getTotal());
            orderResponse.setCreatedAt(facturaEntity.getCreatedAt());

            orderResponses.add(orderResponse);
        }

        return orderResponses;
    }

    @Transactional
    @Override
    public void updateStatus(UUID id, Status newStatus) {
        Optional<FacturaEntity> optionalFactura = facturaJpaRepository.findById(id);
        if (optionalFactura.isPresent()) {
            FacturaEntity factura = optionalFactura.get();
            factura.setStatus(newStatus.toString());
            facturaJpaRepository.save(factura);
            UserResponse userResponse = userClient.getUserById(factura.getUserId());
            notifyClientService.sendPaymentApproved(userResponse.getEmail(), factura.getCodFactura());
            logger.info("Factura {} actualizada a estado: {}", id, newStatus);
        }
    }

    @Transactional
    public void procesarFacturasVencidas() {
        logger.info("Iniciando verificación de facturas vencidas");

        try {
            // Buscar todas las facturas (usamos el método que ya existe)
            List<FacturaEntity> todasLasFacturas = facturaJpaRepository.findAll();

            LocalDateTime ahora = LocalDateTime.now();
            int facturasRechazadas = 0;

            for (FacturaEntity factura : todasLasFacturas) {
                // Solo procesar facturas PENDIENTES
                if (Status.PENDIENTE.toString().equals(factura.getStatus())) {

                    // Verificar si han pasado más de 5 minutos
                    LocalDateTime tiempoExpiracion = factura.getCreatedAt().plusMinutes(EXPIRATION_MINUTES);

                    if (ahora.isAfter(tiempoExpiracion)) {
                        // 1. Cambiar estado a RECHAZADA
                        factura.setStatus(String.valueOf(Status.RECHAZADA));
                        facturaJpaRepository.save(factura);

                        // 2. Restaurar stock
                        restaurarStockFactura(factura);

                        // 3. Notificar al cliente
                        notificarCancelacionAutomatica(factura);

                        facturasRechazadas++;

                        logger.info("Factura {} rechazada automáticamente por expiración. Creada: {}",
                                factura.getCodFactura(), factura.getCreatedAt());
                    }
                }
            }

            if (facturasRechazadas > 0) {
                logger.info("Total de facturas rechazadas automáticamente: {}", facturasRechazadas);
            } else {
                logger.debug("No se encontraron facturas vencidas");
            }

        } catch (Exception e) {
            logger.error("Error procesando facturas vencidas: {}", e.getMessage(), e);
        }
    }

    // Método para restaurar el stock usando la función existente
    private void restaurarStockFactura(FacturaEntity factura) {
        try {
            for (DetalleFacturaEntity detalle : factura.getDetalles()) {
                // Obtener el producto actual para verificar su stock
                ProductResponse producto = productClient.getProductById(detalle.getIdProducto());

                if (producto != null) {
                    // Sumar la cantidad de vuelta al stock (usar BigInteger para suma)
                    BigInteger stockActual = producto.getStock();
                    BigInteger cantidadARestaurar = detalle.getCantidad();
                    BigInteger nuevoStock = stockActual.add(cantidadARestaurar);

                    // Crear request para actualizar con el nuevo stock
                    boolean stockRestaurado = actualizarStockDirecto(detalle.getIdProducto(), nuevoStock, producto);

                    if (stockRestaurado) {
                        logger.info("Stock restaurado para producto {}: {} unidades",
                                detalle.getIdProducto(), detalle.getCantidad());
                    } else {
                        logger.warn("No se pudo restaurar el stock del producto: {}", detalle.getIdProducto());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error restaurando stock para factura {}: {}", factura.getCodFactura(), e.getMessage());
        }
    }

    // Método auxiliar para actualizar stock directamente
    private boolean actualizarStockDirecto(UUID productId, java.math.BigInteger nuevoStock, ProductResponse producto) {
        try {
            String url = productClient.getClass().getDeclaredField("productServiceUrl").get(productClient) + "/" + productId;

            java.util.Map<String, Object> updateRequest = new java.util.HashMap<>();
            updateRequest.put("name", producto.getName());
            updateRequest.put("marcaId", producto.getMarca().getId());
            updateRequest.put("size", producto.getSize());
            updateRequest.put("color", producto.getColor());
            updateRequest.put("categoryId", producto.getCategory().getId());
            updateRequest.put("stock", nuevoStock);
            updateRequest.put("price", producto.getPrice());
            updateRequest.put("active", producto.getActive());
            updateRequest.put("imageIds", new java.util.ArrayList<>());

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            org.springframework.http.HttpEntity<java.util.Map<String, Object>> entity =
                    new org.springframework.http.HttpEntity<>(updateRequest, headers);

            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.PUT,
                    entity,
                    String.class
            );

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.error("Error actualizando stock directamente: {}", e.getMessage());
            return false;
        }
    }

    // Método para notificar cancelación automática
    private void notificarCancelacionAutomatica(FacturaEntity factura) {
        try {
            UserResponse user = userClient.getUserById(factura.getUserId());

            // Usar el método que agregaste al NotifyClientService
            notifyClientService.sendOrderCancellation(
                    user.getEmail(),
                    factura.getCodFactura(),
                    "Su pedido ha sido cancelado automáticamente por falta de pago dentro de " + EXPIRATION_MINUTES + " minutos."
            );

            logger.info("Notificación de cancelación enviada a {} para factura {}",
                    user.getEmail(), factura.getCodFactura());

        } catch (Exception e) {
            logger.error("Error notificando cancelación automática para factura {}: {}",
                    factura.getCodFactura(), e.getMessage());
        }
    }

    // Método público simplificado para ser llamado por scheduler
    @Transactional
    public void verificarFacturasExpiradas() {
        procesarFacturasVencidas();
    }
}