package spring.cloud.ali.order.service;

import spring.cloud.ali.common.context.LoginUser;
import spring.cloud.ali.common.vo.Page;
import spring.cloud.ali.order.request.CreateOrderRequest;
import spring.cloud.ali.order.result.OrderDetailResult;

public interface OrderService {

    OrderDetailResult queryUserOrder(LoginUser login, String orderNo);

    Page<OrderDetailResult> pagingUserOrders(LoginUser login, Integer pageNo);

    Boolean createOrder(LoginUser login, CreateOrderRequest req);
}
