package spring.cloud.ali.user.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import spring.cloud.ali.common.config.FeignClientConfig;
import spring.cloud.ali.common.dto.HttpResult;
import spring.cloud.ali.user.result.UserDetailResult;

@FeignClient(name = "ali-user", configuration = FeignClientConfig.class)
public interface UserHttpService {

    @GetMapping("/users/{userId}")
    HttpResult<UserDetailResult> queryUserById(@PathVariable Long userId);
}
