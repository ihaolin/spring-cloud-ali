
package spring.cloud.ali.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import spring.cloud.ali.common.component.SentinelConfigService;

@Configuration
public class ComponentConfig {

    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }

    @Bean
    public SentinelConfigService sentinelConfigService(){
        return new SentinelConfigService();
    }
}
