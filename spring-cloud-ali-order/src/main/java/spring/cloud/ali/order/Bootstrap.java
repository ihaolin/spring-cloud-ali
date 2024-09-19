package spring.cloud.ali.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(
    basePackages =  { // 确保能扫描到依赖api的FeignClient，如user-api
        "spring.cloud.ali.user"
    }
)
@ComponentScan({ // 需要扫描的包，应明确指出，避免扫描到不必要的组件
    "spring.cloud.ali.order",
    "spring.cloud.ali.common"
})
public class Bootstrap {

    public static void main(String[] args) {
        SpringApplication.run(Bootstrap.class, args);
    }
}
