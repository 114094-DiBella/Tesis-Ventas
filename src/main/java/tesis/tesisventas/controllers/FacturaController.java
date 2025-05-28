package tesis.tesisventas.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tesis.tesisventas.dtos.FacturaRequest;
import tesis.tesisventas.dtos.OrderResponse;
import tesis.tesisventas.models.Factura;
import tesis.tesisventas.models.Status;
import tesis.tesisventas.services.FacturaService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/facturas")
public class FacturaController {

    @Autowired
    private FacturaService facturaService;

    @GetMapping
    public ResponseEntity<List<Factura>> findAll() {
        return ResponseEntity.ok(facturaService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Factura> findById(@PathVariable UUID id) {
        Factura factura = facturaService.getById(id);
        if (factura != null) {
            return ResponseEntity.ok(factura);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Factura> create(@RequestBody FacturaRequest request) {
        try {
            Factura createdFactura = facturaService.create(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdFactura);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Factura> update(@PathVariable UUID id, @RequestBody FacturaRequest request) {
        try {
            Factura updatedFactura = facturaService.update(id, request);
            if (updatedFactura != null) {
                return ResponseEntity.ok(updatedFactura);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        facturaService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Endpoints adicionales útiles

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Factura>> findByUserId(@PathVariable UUID userId) {
        return ResponseEntity.ok(facturaService.getByUserId(userId));
    }

    @GetMapping("/forma-pago/{idFormaPago}")
    public ResponseEntity<List<Factura>> findByFormaPago(@PathVariable UUID idFormaPago) {
        return ResponseEntity.ok(facturaService.getByFormaPago(idFormaPago));
    }

    @GetMapping("orders")
    public ResponseEntity<List<OrderResponse>> findAllOrders() {
        return ResponseEntity.ok(facturaService.getAllOrders());
    }

    @PutMapping("/{id}/update-status")
    public ResponseEntity<?> updatePaymentStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> request) {

        try {
            String newStatus = request.get("status");
            facturaService.updateStatus(id, Status.valueOf(newStatus));
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

}