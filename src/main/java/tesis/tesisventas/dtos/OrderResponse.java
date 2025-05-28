package tesis.tesisventas.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tesis.tesisventas.models.DetalleFactura;
import tesis.tesisventas.models.Status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private String codOrder;
    private UserResponse user;
    private List<OrderDetailResponse> details;
    private BigDecimal total;
    private UUID idFormaPago;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    private Status status;
}
