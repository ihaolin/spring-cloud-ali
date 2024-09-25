package spring.cloud.ali.common.config;

import feign.Client;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import spring.cloud.ali.common.component.CustomFeignClient;
import spring.cloud.ali.common.component.SentinelConfigService;

public class CommonConfig {

    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }

    @Bean
    public SentinelConfigService sentinelConfigService(){
        return new SentinelConfigService();
    }

    @Bean
    public CustomFeignClient customFeignClient(){
        return new CustomFeignClient(new Client.Default(null, null));
    }

    @Bean
    public FeignClientConfig feignClientConfig(){
        return new FeignClientConfig();
    }
}
