package spring.cloud.ali.common.component;

import com.alibaba.nacos.api.config.ConfigFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;

import java.util.Properties;

/**
 * 基于Nacos存储的Sentinel服务
 * 涉及到Sentinel规则动态变更的场景
 * 如，不同场景feign熔断，api限流，redis熔断等，都需要监听相关规则
 *    一个应用实例保证有一个ConfigService实例
 */
@Slf4j
public class SentinelConfigService implements InitializingBean {

    /**
     * nacos配置中心地址
     */
    @Value("${spring.cloud.nacos.config.server-addr}")
    private String nacosServer;

    /**
     * nacos中，用于存放sentinel规则的命名空间（id）
     */
    @Value("${spring.cloud.sentinel.nacos.namespace}")
    private String nacosNamespace;

    private ConfigService configService;

    @Override
    public void afterPropertiesSet() {
        initConfigService();
    }

    private void initConfigService() {

        if (Strings.isNullOrEmpty(nacosNamespace)){
            log.warn("nacosNamespace is empty, won't be initialized");
            return;
        }

        Properties properties = new Properties();
        properties.setProperty("serverAddr", nacosServer);
        properties.setProperty("namespace", nacosNamespace);

        try {
            configService = ConfigFactory.createConfigService(properties);
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    public ConfigService get(){

        if (configService == null){
            throw new RuntimeException("configService isn't initialized, " +
                    "you must configure ${spring.cloud.sentinel.nacos.namespace} item in your application");
        }

        return configService;
    }
}
