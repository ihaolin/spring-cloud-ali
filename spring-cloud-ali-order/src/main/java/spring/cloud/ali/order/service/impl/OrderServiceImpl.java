package spring.cloud.ali.order.service.impl;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import spring.cloud.ali.common.component.mq.RocketMQProducer;
import spring.cloud.ali.common.context.LoginUser;
import spring.cloud.ali.common.dto.HttpResult;
import spring.cloud.ali.common.vo.Page;
import spring.cloud.ali.order.config.AppConfig;
import spring.cloud.ali.order.mapper.OrderMapper;
import spring.cloud.ali.order.model.Order;
import spring.cloud.ali.order.mq.OrderMessage;
import spring.cloud.ali.order.request.CreateOrderRequest;
import spring.cloud.ali.order.result.OrderDetailResult;
import spring.cloud.ali.order.service.OrderService;
import spring.cloud.ali.user.api.UserHttpService;
import spring.cloud.ali.user.result.UserDetailResult;

import java.util.List;

import static spring.cloud.ali.common.exception.BizException.throwDataNotFound;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserHttpService userHttpService;

    @Autowired
    private RocketMQProducer producer;

    @Autowired
    private AppConfig appConfig;

    @Override
    public OrderDetailResult queryUserOrder(LoginUser login, String orderNo) {

        // 通过FeignClient调用远端
        HttpResult<UserDetailResult> userRes = userHttpService.queryUserById(login.getId());

        UserDetailResult user = userRes.getData();

        Order order = orderMapper.queryByUserIdAndOrderNo(user.getId(), orderNo);
        if (order == null){
            return null;
        }
        OrderDetailResult odr = new OrderDetailResult();
        BeanUtils.copyProperties(order, odr);

        odr.setUserName(user.getUsername());

        return odr;
    }

    @Override
    public Page<OrderDetailResult> pagingUserOrders(LoginUser login, Integer pageNo) {

        // 通过FeignClient调用远端
        HttpResult<UserDetailResult> userRes = userHttpService.queryUserById(login.getId());
        if (userRes.getData() == null){
            throwDataNotFound();
        }

        List<Order> orders = orderMapper.queryByUserId(userRes.getData().getId());
        List<OrderDetailResult> orderDetailResults = orders.stream().map(order -> {
            OrderDetailResult odr = new OrderDetailResult();
            BeanUtils.copyProperties(order, odr);
            return odr;
        }).toList();

        return new Page<>(1, orderDetailResults);
    }

    @Override
    public Boolean createOrder(LoginUser login, CreateOrderRequest req) {

        String orderNo = appConfig.getOrderNoPrefix() + System.currentTimeMillis();

        producer.send("ali-order-topic", orderNo, new OrderMessage(login.getId(), orderNo));

        return Boolean.TRUE;
    }
}
