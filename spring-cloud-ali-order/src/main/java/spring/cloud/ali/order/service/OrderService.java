package spring.cloud.ali.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import spring.cloud.ali.common.context.LoginUser;
import spring.cloud.ali.order.request.CreateOrderRequest;
import spring.cloud.ali.order.result.OrderDetailResult;

public interface OrderService {

    OrderDetailResult queryUserOrder(LoginUser login, String orderNo);

    IPage<OrderDetailResult> pagingUserOrders(LoginUser login, Integer pageNo);

    Boolean createOrder(LoginUser login, CreateOrderRequest req);
}
