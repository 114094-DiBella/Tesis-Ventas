package tesis.tesisventas.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FacturaRequest {
    private List<DetalleRequest> detalles; // Cambio aquí: lista de DetalleRequest en lugar de UUIDs
    private UUID userId; // Cambio aquí: userId en lugar de idUser para consistencia
    private UUID idFormaPago;
}