package tesis.tesisventas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TesisVentasApplication {

    public static void main(String[] args) {
        SpringApplication.run(TesisVentasApplication.class, args);
    }

}
