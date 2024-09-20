
package spring.cloud.ali.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import spring.cloud.ali.common.component.SentinelConfigService;

@Configuration
public class ComponentConfig {

    @Bean
    public SentinelConfigService sentinelConfigService(){
        return new SentinelConfigService();
    }
}
