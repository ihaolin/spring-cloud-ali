package spring.cloud.ali.common.component;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import spring.cloud.ali.common.util.JsonUtil;

import javax.annotation.Resource;

@Slf4j
public class RocketMQProducer {

    @Resource
    private RocketMQTemplate template;

    /**
     * 发送MQ消息
     * @param topic     消息主题
     * @param key       消息Key（用于去重、定位消息）
     * @param message   消息内容
     */
    public boolean send(String topic, String key, Object message) {
        template.send(topic, MessageBuilder.withPayload(JsonUtil.toJson(message))
                .setHeader("KEYS", key)
                .build());
        return true;
    }
}
