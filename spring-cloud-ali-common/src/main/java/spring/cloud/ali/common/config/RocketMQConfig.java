package spring.cloud.ali.common.config;

import org.springframework.context.annotation.Bean;
import spring.cloud.ali.common.component.RocketMQProducer;

public class RocketMQConfig {

    @Bean
    public RocketMQProducer rocketMQProducer(){
        return new RocketMQProducer();
    }
}
