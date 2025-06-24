package tesis.tesisventas.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tesis.tesisventas.services.impl.FacturaServiceImpl;

@Component
@EnableScheduling
public class FacturaScheduler {

    private static final Logger logger = LoggerFactory.getLogger(FacturaScheduler.class);

    @Autowired
    private FacturaServiceImpl facturaService;

    // Ejecutar cada 3 minutos para verificar facturas vencidas
    @Scheduled(fixedRate = 180000) // 3 minutos en milisegundos
    public void verificarFacturasVencidas() {
        logger.debug("Ejecutando verificación automática de facturas vencidas");

        try {
            facturaService.verificarFacturasExpiradas();
        } catch (Exception e) {
            logger.error("Error en la verificación automática de facturas: {}", e.getMessage());
        }
    }
}