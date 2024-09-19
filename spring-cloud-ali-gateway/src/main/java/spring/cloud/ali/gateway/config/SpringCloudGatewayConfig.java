package spring.cloud.ali.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Spring网关标准配置
 */
@Data
@RefreshScope
@ConfigurationProperties(prefix = "spring.cloud.gateway")
@Component
public class SpringCloudGatewayConfig {

    private List<RouteDefinition> routes;
}
