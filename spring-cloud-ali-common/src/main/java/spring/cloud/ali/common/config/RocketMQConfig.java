package spring.cloud.ali.common.config;

import org.springframework.context.annotation.Bean;
import spring.cloud.ali.common.component.RocketMQProducer;

public class RocketMQConfig {

    public static final String USER_PROP_TRACE_ID = "Sleuth-Trace-Id";
    public static final String USER_PROP_SPAN_ID = "Sleuth-Span-Id";

    @Bean
    public RocketMQProducer rocketMQProducer(){
        return new RocketMQProducer();
    }
}
