package de.uol.dummydssp;

import de.uol.dummydssp.model.DataSetLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class TestClient {

    private static final Logger log = LoggerFactory.getLogger(TestClient.class);

    public static void main(String[] args) {
        //SpringApplication.run(TestClient.class, args);
    }

   // @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    //@Bean
    public CommandLineRunner run(RestTemplate restTemplate) throws Exception {
        return args -> {
            Map<String, String> vars = new HashMap<>();
            vars.put("urn", "urn:mrn:mcp:file:lol124");
            DataSetLocation test = restTemplate.getForObject(
                    "http://localhost:8080/data?urn={urn}", DataSetLocation.class, vars);
            log.info(test.toString());
        };
    }
}