package tesis.tesisventas.client.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.Map;


@Service
public class NotifyClientService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentClientService.class);

    @Value("${services.notify.url}")
    private String NOTIFY_SERVICE_URL;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;


    public void sendPurchaseConfirmation(String email, String orderCode, BigDecimal total) {
        try {
            String url = NOTIFY_SERVICE_URL + "/api/notifications/purchase-confirmation";

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url)
                    .queryParam("email", email)
                    .queryParam("orderCode", orderCode)
                    .queryParam("total", total);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    builder.toUriString(),
                    null,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("✅ Confirmación de compra enviada - Orden: {} a: {}", orderCode, email);
            } else {
                logger.error("❌ Error enviando confirmación de compra. Status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("❌ Error enviando confirmación de compra para orden {}: {}", orderCode, e.getMessage());
        }
    }

    public void sendPaymentApproved(String email, String orderCode) {
        try {
            String url = NOTIFY_SERVICE_URL + "/api/notifications/payment-approved";

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url)
                    .queryParam("email", email)
                    .queryParam("orderCode", orderCode);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    builder.toUriString(),
                    null,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("✅ Notificación de pago aprobado enviada - Orden: {} a: {}", orderCode, email);
            } else {
                logger.error("❌ Error enviando notificación de pago aprobado. Status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("❌ Error enviando notificación de pago para orden {}: {}", orderCode, e.getMessage());
        }
    }
}
