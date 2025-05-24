package tesis.tesisventas.client.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tesis.tesisventas.dtos.ProductResponse;
import tesis.tesisventas.dtos.UserResponse;

import java.util.UUID;

@Service
public class UserClientServiceImpl {

    private static final Logger logger = LoggerFactory.getLogger(UserClientServiceImpl.class);
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${services.user.url}")
    private String userServiceUrl;

    public UserResponse getUserById (UUID userID){
        try {
            String url = userServiceUrl + "/" + userID;
            logger.info("URL: " + url);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return objectMapper.readValue(response.getBody(), UserResponse.class);
            } else {
                logger.error("Error al obtener producto. Status: {}", response.getStatusCode());
                return null;
            }
        } catch (RestClientException e) {
            logger.error("Error de conexión al servicio de Usuarios: {}", e.getMessage());
            return null;
        } catch (JsonProcessingException e) {
            logger.error("Error al procesar JSON de respuesta: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Error inesperado al obtener el user: {}", e.getMessage());
            return null;
        }

    }
}
