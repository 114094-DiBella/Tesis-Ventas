package ar.edu.utn.frc.tup.lciii.config;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class SwaggerConfig {

    @Value("${app.url}")
    private String url;

    @Value("${app.dev-name}")
    private String devName;

    @Value("${app.dev-email}")
    private String devEmail;
    @Bean
    public OpenAPI openApi(
            @Value("${app.name}") String appName,
            @Value("${app.desc}") String appDescription,
            @Value("${app.version}") String appVersion) {

        // Información de la API
        Info info = new Info()
                .title(appName)
                .version(appVersion)
                .description(appDescription)
                .contact(new Contact()
                        .name(devName)
                        .email(devEmail));

        // Información del servidor
        Server server = new Server()
                .url(url)
                .description(appDescription);

        // Configuración de OpenAPI
        return new OpenAPI()
                .components(new Components())
                .info(info)
                .addServersItem(server);
    }
}
