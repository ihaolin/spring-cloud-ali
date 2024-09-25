package spring.cloud.ali.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import spring.cloud.ali.common.interceptor.WebInterceptor;

public class WebConfig implements WebMvcConfigurer {

    @Bean
    public WebInterceptor webInterceptor(){
        return new WebInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(webInterceptor()).addPathPatterns("/**");
    }
}
