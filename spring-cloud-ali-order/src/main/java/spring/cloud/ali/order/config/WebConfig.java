package spring.cloud.ali.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import spring.cloud.ali.common.interceptor.ServiceSentinelInterceptor;


@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public ServiceSentinelInterceptor serviceSentinelInterceptor(){
        return new ServiceSentinelInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(serviceSentinelInterceptor()).addPathPatterns("/**");
    }
}
