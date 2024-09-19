
package spring.cloud.ali.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ComponentConfig {

    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }
}
