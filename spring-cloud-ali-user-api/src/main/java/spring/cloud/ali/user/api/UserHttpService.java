package spring.cloud.ali.user.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import spring.cloud.ali.common.config.FeignClientConfig;
import spring.cloud.ali.common.dto.HttpResult;
import spring.cloud.ali.user.result.UserDetailResult;
import spring.cloud.ali.user.result.VerifyTokenResult;

@FeignClient(name = "ali-user", configuration = FeignClientConfig.class)
public interface UserHttpService {

    @GetMapping("/users/verify-token")
    HttpResult<VerifyTokenResult> verifyToken(@RequestParam String token);

    @GetMapping("/users/detail")
    HttpResult<UserDetailResult> queryUserByName(@RequestParam String userName);

    @GetMapping("/users/{userId}")
    HttpResult<UserDetailResult> queryUserById(@PathVariable Long userId);
}
