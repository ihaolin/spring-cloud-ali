
package spring.cloud.ali.user.config;

import brave.Tracer;
import brave.Tracing;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import spring.cloud.ali.common.config.CommonConfig;
import spring.cloud.ali.common.config.FeignClientConfig;
import spring.cloud.ali.common.config.MybatisConfig;
import spring.cloud.ali.common.config.RedisConfig;
import spring.cloud.ali.common.config.RocketMQConfig;
import spring.cloud.ali.common.config.WebConfig;

import javax.sql.DataSource;

@Import({
        CommonConfig.class,
        WebConfig.class,
        FeignClientConfig.class,
        MybatisConfig.class,
        RedisConfig.class,
        RocketMQConfig.class
})
@Configuration
public class ComponentConfig {

}
