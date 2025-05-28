package tesis.tesisventas.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class OrderDetailResponse {
    private ProductResponse product;
    private BigInteger quantity;
    private BigDecimal priceUnit;
    private BigDecimal subTotal;
}
