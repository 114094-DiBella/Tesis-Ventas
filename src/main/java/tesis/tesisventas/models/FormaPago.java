package tesis.tesisventas.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FormaPago {
    private UUID id;
    private String name;
    private boolean active;
}
