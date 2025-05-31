package tesis.tesisventas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tesis.tesisventas.entities.FacturaEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FacturaJpaRepository extends JpaRepository<FacturaEntity, UUID> {

    List<FacturaEntity> findByUserId(UUID userId);

    List<FacturaEntity> findByIdFormaPago(UUID idFormaPago);

    @Query("SELECT f FROM FacturaEntity f LEFT JOIN FETCH f.detalles WHERE f.codFactura = :codFactura")
    Optional<FacturaEntity> findByCodFacturaWithDetails(@Param("codFactura") String codFactura);
}
