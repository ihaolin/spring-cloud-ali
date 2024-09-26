package spring.cloud.ali.common.config;

import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import spring.cloud.ali.common.interceptor.WebInterceptor;

import javax.annotation.Resource;

public class WebConfig implements WebMvcConfigurer {

    @Resource
    private WebInterceptor webInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(webInterceptor).addPathPatterns("/**");
    }
}
