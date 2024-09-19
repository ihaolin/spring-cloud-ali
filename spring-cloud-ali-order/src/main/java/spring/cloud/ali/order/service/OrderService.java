package spring.cloud.ali.order.service;

import spring.cloud.ali.order.result.OrderDetailResult;

public interface OrderService {

    OrderDetailResult queryUserOrder(String userName, String orderNo);
}
