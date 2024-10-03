
package spring.cloud.ali.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import spring.cloud.ali.common.component.SentinelConfigService;
import spring.cloud.ali.common.config.WebFluxConfig;


@Import({
        WebFluxConfig.class
})
@Configuration
public class ComponentConfig {

    @Bean
    public SentinelConfigService sentinelConfigService(){
        return new SentinelConfigService();
    }
}
