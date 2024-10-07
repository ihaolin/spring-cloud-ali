
package spring.cloud.ali.order.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import spring.cloud.ali.common.config.CommonConfig;
import spring.cloud.ali.common.config.MybatisConfig;
import spring.cloud.ali.common.config.RedisConfig;
import spring.cloud.ali.common.config.RocketMQConfig;
import spring.cloud.ali.common.config.WebConfig;


@Import({
        CommonConfig.class,
        WebConfig.class,
        MybatisConfig.class,
        RedisConfig.class,
        RocketMQConfig.class
})
@Configuration
public class ComponentConfig {

}
