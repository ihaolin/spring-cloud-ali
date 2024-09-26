
package spring.cloud.ali.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import spring.cloud.ali.common.config.CommonConfig;
import spring.cloud.ali.common.config.RedisConfig;
import spring.cloud.ali.common.config.WebConfig;
import spring.cloud.ali.common.interceptor.WebInterceptor;

@Import({CommonConfig.class, WebConfig.class, RedisConfig.class})
@Configuration
public class ComponentConfig {

    @Bean
    public WebInterceptor webInterceptor() {
        return new WebInterceptor();
    }
}
