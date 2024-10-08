package spring.cloud.ali.user.mq;

import org.springframework.stereotype.Component;
import spring.cloud.ali.common.component.mq.RocketMQConsumer;
import spring.cloud.ali.order.mq.OrderMessage;

@Component
public class OrderConsumer extends RocketMQConsumer<OrderMessage> {

    @Override
    protected String topic() {
        return "ali-order-topic";
    }

    @Override
    protected boolean consume(OrderMessage message) {

        return true;
    }
}
