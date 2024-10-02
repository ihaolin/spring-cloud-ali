package spring.cloud.ali.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import spring.cloud.ali.common.context.LoginUser;
import spring.cloud.ali.order.request.CreateOrderRequest;
import spring.cloud.ali.order.result.OrderDetailResult;

public interface OrderService {

    OrderDetailResult queryUserOrder(String userName, String orderNo);

    IPage<OrderDetailResult> pagingUserOrders(Long userId, Integer pageNo);

    Boolean createOrder(LoginUser login, CreateOrderRequest req);
}
