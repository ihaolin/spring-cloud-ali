package spring.cloud.ali.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan({ // 需要扫描的包，应明确指出，避免扫描到不必要的组件
    "spring.cloud.ali.user",
    "spring.cloud.ali.common"
})
@MapperScan(basePackages = {
    "spring.cloud.ali.user.mapper"
})
public class Bootstrap {

    public static void main(String[] args) {
        SpringApplication.run(Bootstrap.class, args);
    }
}
