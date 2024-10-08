package spring.cloud.ali.common.component.mq;


import brave.Span;
import brave.Tracer;
import brave.propagation.TraceContext;
import com.google.common.base.Strings;
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
import javax.annotation.Resource;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;

import static org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
import static spring.cloud.ali.common.config.RocketMQConfig.USER_PROP_BRAVE_SAMPLED;
import static spring.cloud.ali.common.config.RocketMQConfig.USER_PROP_BRAVE_SPAN_ID;
import static spring.cloud.ali.common.config.RocketMQConfig.USER_PROP_BRAVE_TRACE_ID;

@Slf4j
public abstract class RocketMQConsumer<T> {

    @SuppressWarnings("unchecked")
    private final Class<T> msgType =
            (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];

    @Value("${spring.application.name}")
    private String appName;

    @Value("${spring.rocketmq.name-server}")
    private String nameServer;

    @Resource
    private Tracer tracer;

    @PostConstruct
    public void onInit() throws MQClientException {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(group(), true);
        consumer.setNamesrvAddr(nameServer);

        consumer.subscribe(topic(), "");
        consumer.registerMessageListener((MessageListenerConcurrently) (messages, context) -> {

            if (CollectionUtils.isEmpty(messages)){
                return CONSUME_SUCCESS;
            }

            String sampled = messages.get(0).getUserProperty(USER_PROP_BRAVE_SAMPLED);
            String traceId = messages.get(0).getUserProperty(USER_PROP_BRAVE_TRACE_ID);
            String spanId = messages.get(0).getUserProperty(USER_PROP_BRAVE_SPAN_ID);

            Span consumeSpan = null;
            if (!Strings.isNullOrEmpty(traceId) && !Strings.isNullOrEmpty(spanId)){
                consumeSpan = tracer.newChild(
                        TraceContext.newBuilder()
                                .traceId(Long.parseLong(traceId))
                                .spanId(Long.parseLong(spanId))
                                .sampled(Boolean.parseBoolean(sampled))
                                .build()
                ).start().name("consumer").remoteServiceName("rocketmq");
            }

            for (MessageExt message : messages){
                if (message == null || message.getBody() == null || message.getBody().length == 0){
                    log.warn("message body is empty and ignored, message={}", message);
                    continue;
                }
                try {

                    if (consumeSpan != null){
                        consumeSpan.tag("topic", message.getTopic());
                        consumeSpan.tag("msgId", message.getMsgId());
                    }

                    if (consumeSpan != null){
                        consumeSpan.annotate("Prepare consuming message start");
                    }
                    String msgBody = new String(message.getBody(), StandardCharsets.UTF_8);
                    T msg = JsonUtil.toObject(msgBody, msgType);
                    if (consumeSpan != null){
                        consumeSpan.annotate("Prepare consuming message end");
                    }

                    log.info("message consume start: {}", msg);

                    if (consumeSpan != null){
                        consumeSpan.annotate("Consume message start");
                    }
                    boolean result = consume(msg);
                    if (!result){
                        log.warn("message consume failed: {}", msg);
                        // TODO 重试 死信
                    }
                    if (consumeSpan != null){
                        consumeSpan.tag("result", String.valueOf(result));
                        consumeSpan.annotate("Consume message end");
                    }

                    log.info("message consume end: {} {}", msg, result);
                } catch (Throwable e){
                    log.error("message consume exception: message={}, error={}",
                            message, Throwables.getStackTraceAsString(e));
                    // TODO 重试 死信
                } finally {
                    if (consumeSpan != null){
                        consumeSpan.finish();
                    }
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
