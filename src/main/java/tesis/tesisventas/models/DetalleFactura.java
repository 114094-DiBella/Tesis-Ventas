package tesis.tesisventas.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DetalleFactura {

    private UUID idDetalle;
    private UUID idFactura;
    private UUID idProducto;
    private BigInteger cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
}
