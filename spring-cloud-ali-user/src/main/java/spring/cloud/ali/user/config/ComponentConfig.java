
package spring.cloud.ali.user.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import spring.cloud.ali.common.config.CommonConfig;
import spring.cloud.ali.common.config.RedisConfig;
import spring.cloud.ali.common.config.WebConfig;

@Import({
        CommonConfig.class,
        WebConfig.class,
        RedisConfig.class
})
@Configuration
public class ComponentConfig {

}
