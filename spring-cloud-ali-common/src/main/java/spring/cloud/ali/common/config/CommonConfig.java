package spring.cloud.ali.common.config;

import feign.Client;
import org.springframework.context.annotation.Bean;
import spring.cloud.ali.common.component.WrappedFeignClient;
import spring.cloud.ali.common.component.SentinelConfigService;
import spring.cloud.ali.common.interceptor.LoginInterceptor;
import spring.cloud.ali.common.interceptor.WebInterceptor;

public class CommonConfig {

    @Bean
    public SentinelConfigService sentinelConfigService(){
        return new SentinelConfigService();
    }

    @Bean
    public WrappedFeignClient wrappedFeignClient(){
        return new WrappedFeignClient(new Client.Default(null, null));
    }

    @Bean
    public FeignClientConfig feignClientConfig(){
        return new FeignClientConfig();
    }

    @Bean
    public WebInterceptor webInterceptor() {
        return new WebInterceptor();
    }

    @Bean
    public LoginInterceptor loginInterceptor(){
        return new LoginInterceptor();
    }
}
