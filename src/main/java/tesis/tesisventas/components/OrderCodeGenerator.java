package tesis.tesisventas.components;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tesis.tesisventas.repositories.FacturaJpaRepository;

import java.time.LocalDate;

@Component
public class OrderCodeGenerator {

    @Autowired
    private FacturaJpaRepository facturaRepository;
    /**
     * Versión más simple - solo números consecutivos
     */
    public String generateSimpleCode() {
        long totalCount = facturaRepository.count();
        return String.format("COD-%06d", totalCount + 1); // ORD-000001, ORD-000002...
    }
}