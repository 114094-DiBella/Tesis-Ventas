package tesis.tesisventas.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tesis.tesisventas.dtos.FacturaRequest;
import tesis.tesisventas.models.Factura;
import tesis.tesisventas.services.FacturaService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/checkout")
@CrossOrigin(origins = "http://localhost:4200")
public class CheckoutController {

    private static final Logger logger = LoggerFactory.getLogger(CheckoutController.class);

    @Autowired
    private FacturaService facturaService;

    /**
     * Crea una factura y devuelve la URL de pago si es necesario
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createCheckout(@RequestBody FacturaRequest request) {
        try {
            logger.info("Creando checkout para usuario: {}", request.getUserId());

            Factura factura = facturaService.create(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("facturaId", factura.getId());
            response.put("total", factura.getTotal());
            response.put("status", factura.getStatus());

            // Si tiene URL de pago, incluirla
            if (factura.getPaymentUrl() != null) {
                response.put("paymentUrl", factura.getPaymentUrl());
                response.put("needsPayment", true);
            } else {
                response.put("needsPayment", false);
            }

            logger.info("Checkout creado exitosamente - Factura: {}", factura.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error en checkout: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}