package tesis.tesisventas.client.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tesis.tesisventas.dtos.ProductResponse;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ProductClientServiceImpl{

    private static final Logger logger = LoggerFactory.getLogger(ProductClientServiceImpl.class);
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${services.product.url}")
    private String productServiceUrl;


    public ProductResponse getProductById(UUID id) {
        try {
            String url = this.productServiceUrl +"/" + id;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return objectMapper.readValue(response.getBody(), ProductResponse.class);
            } else {
                logger.error("Error al obtener producto. Status: {}", response.getStatusCode());
                return null;
            }
        } catch (RestClientException e) {
            logger.error("Error de conexión al servicio de productos: {}", e.getMessage());
            return null;
        } catch (JsonProcessingException e) {
            logger.error("Error al procesar JSON de respuesta: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Error inesperado al obtener producto: {}", e.getMessage());
            return null;
        }
    }


    public boolean hasEnoughStock(UUID productId, int quantity) {
        ProductResponse product = getProductById(productId);
        return product != null && product.getActive();
    }
}
