package tesis.tesisventas.client.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tesis.tesisventas.dtos.ProductResponse;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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


    public boolean updateProductStock(UUID productId, BigInteger quantityToReduce) {
        try {
            ProductResponse producto = getProductById(productId);
            if (producto == null) {
                logger.error("No se encontró el producto para actualizar stock: {}", productId);
                return false;
            }

            BigInteger nuevoStock = producto.getStock().subtract(quantityToReduce);
            if (nuevoStock.compareTo(BigInteger.ZERO) < 0) {
                nuevoStock = BigInteger.ZERO;
            }

            String url = this.productServiceUrl + "/" + productId;

            Map<String, Object> updateRequest = new HashMap<>();
            updateRequest.put("name", producto.getName());
            updateRequest.put("marcaId", producto.getMarca().getId());
            updateRequest.put("size", producto.getSize());
            updateRequest.put("color", producto.getColor());
            updateRequest.put("categoryId", producto.getCategory().getId());
            updateRequest.put("stock", nuevoStock);
            updateRequest.put("price", producto.getPrice());
            updateRequest.put("active", producto.getActive());
            updateRequest.put("imageIds", new ArrayList<>());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(updateRequest, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    String.class
            );

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.error("Error al actualizar el stock del producto {}: {}", productId, e.getMessage());
            return false;
        }
    }

    public boolean hasEnoughStock(UUID productId, int quantity) {
        ProductResponse product = getProductById(productId);
        return product != null && product.getActive();
    }
}
