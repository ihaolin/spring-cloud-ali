package spring.cloud.ali.common.component;

import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import spring.cloud.ali.common.util.JsonUtil;

import javax.annotation.PostConstruct;
import java.lang.reflect.ParameterizedType;

import static org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus.CONSUME_SUCCESS;

@Slf4j
public abstract class RocketMQConsumer<T> {

    @SuppressWarnings("unchecked")
    private final Class<T> msgType =
            (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];

    @Value("${spring.application.name}")
    private String appName;

    @Value("${spring.rocketmq.name-server}")
    private String nameServer;

    @PostConstruct
    public void onInit() throws MQClientException {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(group());
        consumer.setNamesrvAddr(nameServer);

        consumer.subscribe(topic(), "");
        consumer.registerMessageListener((MessageListenerConcurrently) (messages, context) -> {

            if (CollectionUtils.isEmpty(messages)){
                return CONSUME_SUCCESS;
            }

            for (MessageExt message : messages){
                if (message == null || message.getBody() == null || message.getBody().length == 0){
                    log.warn("message body is empty and ignored, message={}", message);
                    continue;
                }
                try {
                    String msgBody = new String(message.getBody());
                    T msg = JsonUtil.toObject(msgBody, msgType);
                    log.info("message consume start: {}", msg);
                    boolean result = consume(msg);
                    if (!result){
                        log.warn("message consume failed: {}", msg);
                        // TODO 重试 死信
                    }
                } catch (Throwable e){
                    log.error("message consume exception: message={}, error={}",
                            message, Throwables.getStackTraceAsString(e));
                    // TODO 重试 死信
                }
            }

            return CONSUME_SUCCESS;
        });

        consumer.start();
    }

    /**
     * 主题topic
     */
    protected abstract String topic();

    /**
     * 消费者组名
     * @return {appName}-{topicName}-consumer-group
     */
    public String group(){
        return appName + "-" + topic() + "-consumer-group";
    }

    /**
     * 消费消息
     * @param message 消息对象
     * @return 消费成功返回true，失败false
     */
    protected abstract boolean consume(T message);
}
