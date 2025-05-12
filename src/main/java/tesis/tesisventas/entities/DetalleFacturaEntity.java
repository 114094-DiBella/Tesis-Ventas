package tesis.tesisventas.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@ToString(exclude = {"factura"})
@Table(name = "detalle_factura")
public class DetalleFacturaEntity {
    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id_detalle")
    private UUID idDetalle;

    @Column(name = "id_factura")
    private UUID idFactura;

    @Column(name = "id_producto")
    private UUID idProducto;

    @Column(name = "cantidad")
    private BigInteger cantidad;

    @Column(name = "precio_unitario")
    private BigDecimal precioUnitario;

    @Column(name = "subtotal")
    private BigDecimal subtotal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_factura", insertable = false, updatable = false)
    private FacturaEntity factura;
}