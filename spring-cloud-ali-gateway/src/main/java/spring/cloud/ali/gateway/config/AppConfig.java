package spring.cloud.ali.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;


/**
 * 网关业务配置
 */
@Data
@RefreshScope
@ConfigurationProperties(prefix = "gateway")
@Component
public class AppConfig {

}
