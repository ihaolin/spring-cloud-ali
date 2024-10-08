package spring.cloud.ali.common.config;

import org.springframework.context.annotation.Bean;
import spring.cloud.ali.common.component.mq.RocketMQProducer;

public class RocketMQConfig {

    public static final String USER_PROP_BRAVE_SAMPLED = "Brave-Sampled";
    public static final String USER_PROP_BRAVE_TRACE_ID = "Brave-Trace-Id";
    public static final String USER_PROP_BRAVE_SPAN_ID = "Brave-Span-Id";

    @Bean
    public RocketMQProducer rocketMQProducer(){
        return new RocketMQProducer();
    }
}
