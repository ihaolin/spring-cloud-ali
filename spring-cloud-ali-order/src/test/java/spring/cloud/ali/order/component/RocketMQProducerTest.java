package spring.cloud.ali.order.component;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import spring.cloud.ali.common.component.mq.RocketMQProducer;
import spring.cloud.ali.order.mq.OrderMessage;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class RocketMQProducerTest {

    @Resource
    private RocketMQProducer producer;

    @Test
    public void testSend(){
        String orderNo = String.valueOf(System.currentTimeMillis());
        assertTrue(producer.send("ali-order-topic", orderNo, new OrderMessage(1L, orderNo)));
    }
}
