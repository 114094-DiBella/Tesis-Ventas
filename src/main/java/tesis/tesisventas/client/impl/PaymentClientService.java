package tesis.tesisventas.client.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tesis.tesisventas.dtos.PaymentRequest;
import tesis.tesisventas.services.impl.FacturaServiceImpl;

import java.util.Map;

@Service
public class PaymentClientService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentClientService.class);

    @Value("${services.payment.url}")
    private String PAYMENT_SERVICE_URL;

    @Autowired
    private RestTemplate restTemplate;

    public String createPaymentPreference(PaymentRequest request) {
        logger.info("Create payment preference");
        String url = PAYMENT_SERVICE_URL + "/api/payments/create-preference";
        logger.info("URL: {}", url);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            logger.info("Response: {}", response.getBody());
            return (String) response.getBody().get("preferenceId");
        } catch (Exception e) {
            logger.error("Error creating payment preference", e);
            throw new RuntimeException("Error al crear preferencia de pago", e);
        }
    }
}