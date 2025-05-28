package tesis.tesisventas.client.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tesis.tesisventas.dtos.PaymentRequest;

import java.util.Map;

@Service
public class PaymentClientService {

    private final String PAYMENT_SERVICE_URL = "http://localhost:8082";

    @Autowired
    private RestTemplate restTemplate;

    public String createPaymentPreference(PaymentRequest request) {
        String url = PAYMENT_SERVICE_URL + "/api/payments/create-preference";

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            return (String) response.getBody().get("preferenceId");
        } catch (Exception e) {
            throw new RuntimeException("Error al crear preferencia de pago", e);
        }
    }
}