package spring.cloud.ali.common.config;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import spring.cloud.ali.common.component.MessageProducer;

public class RocketMQConfig {

    @Value("${spring.rocketmq.name-server}")
    private String nameServer;

    @Value("${spring.rocketmq.producer.group}")
    private String producerGroup;

    @Bean
    public MessageProducer messageProducer(){
        return new MessageProducer();
    }

    @Bean
    public RocketMQTemplate rocketTemplate(){
        DefaultMQProducer producer = new DefaultMQProducer(producerGroup);
        producer.setNamesrvAddr(nameServer);

        RocketMQTemplate template = new RocketMQTemplate();
        template.setProducer(producer);
        return template;
    }
}
