package spring.cloud.ali.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import spring.cloud.ali.common.component.RocketMQProducer;
import spring.cloud.ali.common.context.LoginUser;
import spring.cloud.ali.common.dto.HttpResult;
import spring.cloud.ali.order.config.AppConfig;
import spring.cloud.ali.order.mapper.OrderMapper;
import spring.cloud.ali.order.model.Order;
import spring.cloud.ali.order.mq.OrderMessage;
import spring.cloud.ali.order.request.CreateOrderRequest;
import spring.cloud.ali.order.result.OrderDetailResult;
import spring.cloud.ali.order.service.OrderService;
import spring.cloud.ali.user.api.UserHttpService;
import spring.cloud.ali.user.result.UserDetailResult;

import javax.annotation.Resource;

import static spring.cloud.ali.common.exception.BizException.throwBizException;
import static spring.cloud.ali.common.exception.BizException.throwDataNotFound;
import static spring.cloud.ali.common.exception.BizException.throwRespDataError;

@Service
public class OrderServiceImpl implements OrderService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private UserHttpService userHttpService;

    @Resource
    private RocketMQProducer producer;

    @Resource
    private AppConfig appConfig;

    @Override
    public OrderDetailResult queryUserOrder(LoginUser login, String orderNo) {

        // 通过FeignClient调用远端
        HttpResult<UserDetailResult> userRes = userHttpService.queryUserById(login.getId());
        if(userRes == null){
            throwRespDataError();
        }
        if (!userRes.isOk()){
            throwBizException(userRes.getCode(), userRes.getMsg());
        }
        if (userRes.getData() == null){
            throwDataNotFound();
        }

        UserDetailResult user = userRes.getData();

        QueryWrapper<Order> q = new QueryWrapper<>();
        q.eq("user_id", user.getId());
        q.eq("order_no", orderNo);
        Order order = orderMapper.selectOne(q);
        if (order == null){
            return null;
        }
        OrderDetailResult odr = new OrderDetailResult();
        BeanUtils.copyProperties(order, odr);

        odr.setUserName(user.getUsername());

        return odr;
    }

    @Override
    public IPage<OrderDetailResult> pagingUserOrders(LoginUser login, Integer pageNo) {

        // 通过FeignClient调用远端
        HttpResult<UserDetailResult> userRes = userHttpService.queryUserById(login.getId());
        if(userRes == null){
            throwRespDataError();
        }
        if (!userRes.isOk()){
            throwBizException(userRes.getCode(), userRes.getMsg());
        }
        if (userRes.getData() == null){
            throwDataNotFound();
        }

        QueryWrapper<Order> q = new QueryWrapper<>();
        q.eq("user_id", login.getId());
        Page<Order> paging = new Page<>();
        paging.setCurrent(pageNo);
        paging.setSize(10);
        IPage<Order> pagingResult = orderMapper.selectPage(paging, q);

        return pagingResult.convert(order -> {
            OrderDetailResult odr = new OrderDetailResult();
            BeanUtils.copyProperties(order, odr);
            return odr;
        });
    }

    @Override
    public Boolean createOrder(LoginUser login, CreateOrderRequest req) {

        String orderNo = appConfig.getOrderNoPrefix() + System.currentTimeMillis();

        producer.send("ali-order-topic", orderNo, new OrderMessage(login.getId(), orderNo));

        return Boolean.TRUE;
    }
}
