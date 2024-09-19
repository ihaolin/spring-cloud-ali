package spring.cloud.ali.order.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 应用业务配置
 */
@Data
@RefreshScope
@ConfigurationProperties(prefix = "order")
@Component
public class AppConfig {

    private String orderNoPrefix;
}
