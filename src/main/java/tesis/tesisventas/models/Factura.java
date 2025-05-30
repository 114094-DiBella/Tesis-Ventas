package tesis.tesisventas.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Factura {

    private UUID id;
    private UUID userId;
    private List<DetalleFactura> detalles;
    private BigDecimal total;
    private UUID idFormaPago;
    private String paymentUrl;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    private Status status;
}
