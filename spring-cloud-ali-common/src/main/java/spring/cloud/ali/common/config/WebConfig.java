package spring.cloud.ali.common.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import spring.cloud.ali.common.component.web.LoginInterceptor;
import spring.cloud.ali.common.component.web.WebInterceptor;

public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private WebInterceptor webInterceptor;

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(webInterceptor).addPathPatterns("/**");
        registry.addInterceptor(loginInterceptor).addPathPatterns("/**");
    }
}
