package spring.cloud.ali.common.config;

import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import spring.cloud.ali.common.component.web.LoginInterceptor;
import spring.cloud.ali.common.component.web.WebInterceptor;

import javax.annotation.Resource;

public class WebConfig implements WebMvcConfigurer {

    @Resource
    private WebInterceptor webInterceptor;

    @Resource
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(webInterceptor).addPathPatterns("/**");
        registry.addInterceptor(loginInterceptor).addPathPatterns("/**");
    }
}
