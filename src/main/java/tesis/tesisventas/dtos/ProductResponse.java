package tesis.tesisventas.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ProductResponse {

    private UUID id;
    private String name;
    private MarcaResponse marca;
    private String size;
    private CategoryResponse category;
    private String color;
    private BigDecimal price;
    private BigInteger stock;
    private Boolean active;
    private List<String> imageUrls;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}