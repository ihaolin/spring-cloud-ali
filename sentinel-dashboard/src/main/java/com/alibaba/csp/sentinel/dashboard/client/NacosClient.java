package com.alibaba.csp.sentinel.dashboard.client;

import com.alibaba.csp.sentinel.dashboard.controller.FlowControllerV1;
import com.alibaba.nacos.api.config.ConfigFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Properties;


@Component
public class NacosClient {

    private final Logger logger = LoggerFactory.getLogger(NacosClient.class);

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

    @PostConstruct
    public void onInit(){
        if (Strings.isNullOrEmpty(nacosNamespace)){
            logger.warn("nacosNamespace is empty, won't be initialized");
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
        return configService;
    }
}
