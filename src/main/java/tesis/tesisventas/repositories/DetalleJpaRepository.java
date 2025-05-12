package tesis.tesisventas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tesis.tesisventas.entities.DetalleFacturaEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface DetalleJpaRepository extends JpaRepository<DetalleFacturaEntity, UUID> {
    List<DetalleFacturaEntity> findByIdFactura(UUID idFactura);

    List<DetalleFacturaEntity> findByIdProducto(UUID idProducto);
}