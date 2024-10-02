package spring.cloud.ali.common.component;

import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Value;
import spring.cloud.ali.common.util.JsonUtil;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;

@Slf4j
public class RocketMQProducer {

    @Value("${spring.rocketmq.name-server}")
    private String nameServer;

    @Value("${spring.rocketmq.producer.group}")
    private String producerGroup;

    private DefaultMQProducer delegate;

    @PostConstruct
    public void onInit() throws MQClientException {
        DefaultMQProducer producer = new DefaultMQProducer(producerGroup, true);
        producer.setNamesrvAddr(nameServer);
        producer.start();
        delegate = producer;
    }

    /**
     * 发送MQ消息
     * @param topic     消息主题
     * @param key       消息Key（用于去重、定位消息）
     * @param message   消息内容
     */
    public boolean send(String topic, String key, Object message) {

        Message msg = new Message();
        msg.setTopic(topic);
        msg.setKeys(key);
        msg.setBody(JsonUtil.toJson(message).getBytes(StandardCharsets.UTF_8));

        try {
            SendResult res = delegate.send(msg);
            log.info("message send: topic={}, key={}, message={}, result={}", topic, key, message, res);
            // TODO 发送可靠性保证
        } catch (Throwable e) {
            log.error("message send failed: topic={}, key={}, message={}, error={}",
                    topic, key, message, Throwables.getStackTraceAsString(e));
            return false;
        }

        return true;
    }
}
