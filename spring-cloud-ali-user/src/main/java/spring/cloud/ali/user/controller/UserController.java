package spring.cloud.ali.user.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import spring.cloud.ali.common.dto.HttpResult;
import spring.cloud.ali.user.result.UserDetailResult;
import spring.cloud.ali.user.service.UserService;

import javax.annotation.Resource;

@RestController
@RequestMapping("/users")
public class UserController {

    @Resource
    private UserService userService;

    @SentinelResource("queryUserByName")
    @GetMapping(value = "/detail")
    public HttpResult<UserDetailResult> queryUserByName(@RequestParam String userName) throws InterruptedException {
        Thread.sleep(10000000);
        return HttpResult.success(userService.queryUserByName(userName));
    }

    @GetMapping(value = "/{userId}")
    public HttpResult<UserDetailResult> queryUserById(@PathVariable Long userId) {
        return HttpResult.success(userService.queryUserById(userId));
    }
}
