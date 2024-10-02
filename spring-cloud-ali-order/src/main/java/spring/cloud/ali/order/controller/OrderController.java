package spring.cloud.ali.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
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

import javax.annotation.Resource;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Resource
    private OrderService orderService;

    @PostMapping("/create")
    public HttpResult<Boolean> createOrder(@RequestBody CreateOrderRequest req){
        return HttpResult.success(orderService.createOrder(LoginContext.get(), req));
    }

    @GetMapping(value = "/detail")
    public HttpResult<OrderDetailResult> queryOrderDetail(
            @RequestParam String userName, @RequestParam String orderNo) throws InterruptedException {
        // Thread.sleep(1000000);
        return HttpResult.success(orderService.queryUserOrder(userName, orderNo));
    }

    @GetMapping(value = "/paging")
    public HttpResult<IPage<OrderDetailResult>> pagingOrders(
            @RequestParam Long userId, @RequestParam Integer pageNo) throws InterruptedException {
        // Thread.sleep(1000000);
        return HttpResult.success(orderService.pagingUserOrders(userId, pageNo));
    }
}
