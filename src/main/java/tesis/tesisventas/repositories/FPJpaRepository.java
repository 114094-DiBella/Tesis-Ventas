package tesis.tesisventas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tesis.tesisventas.entities.FormaPagoEntity;

import java.util.UUID;
@Repository
public interface FPJpaRepository extends JpaRepository<FormaPagoEntity, UUID> {
}
