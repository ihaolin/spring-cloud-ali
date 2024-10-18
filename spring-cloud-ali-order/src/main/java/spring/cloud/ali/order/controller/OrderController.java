package spring.cloud.ali.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import spring.cloud.ali.common.context.LoginContext;
import spring.cloud.ali.common.dto.HttpResult;
import spring.cloud.ali.order.request.CreateOrderRequest;
import spring.cloud.ali.order.result.OrderDetailResult;
import spring.cloud.ali.order.service.OrderService;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/create")
    public HttpResult<Boolean> createOrder(@RequestBody CreateOrderRequest req){
        return HttpResult.success(orderService.createOrder(LoginContext.get(), req));
    }

    @GetMapping(value = "/detail")
    public HttpResult<OrderDetailResult> queryOrderDetail(@RequestParam String orderNo) {
        return HttpResult.success(orderService.queryUserOrder(LoginContext.get(), orderNo));
    }

    @GetMapping(value = "/paging")
    public HttpResult<IPage<OrderDetailResult>> pagingOrders(@RequestParam Integer pageNo) {
        return HttpResult.success(orderService.pagingUserOrders(LoginContext.get(), pageNo));
    }
}
