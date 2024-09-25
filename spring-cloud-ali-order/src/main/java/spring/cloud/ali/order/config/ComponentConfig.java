
package spring.cloud.ali.order.config;

import feign.Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import spring.cloud.ali.common.component.FeignSentinelClient;
import spring.cloud.ali.common.component.SentinelConfigService;
import spring.cloud.ali.common.config.FeignClientConfig;

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

    @Bean
    public FeignSentinelClient feignSentinelClient(){
        return new FeignSentinelClient(new Client.Default(null, null));
    }

    @Bean
    public FeignClientConfig feignClientConfig(){
        return new FeignClientConfig();
    }
}
