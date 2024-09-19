package spring.cloud.ali.order.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import spring.cloud.ali.common.dto.HttpResult;
import spring.cloud.ali.order.result.OrderDetailResult;
import spring.cloud.ali.order.service.OrderService;

import javax.annotation.Resource;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Resource
    private OrderService orderService;

    @GetMapping(value = "/detail")
    public HttpResult<OrderDetailResult> queryOrderDetail(
            @RequestParam String userName, @RequestParam String orderNo) throws InterruptedException {
        // Thread.sleep(1000000);
        return HttpResult.success(orderService.queryUserOrder(userName, orderNo));
    }
}
