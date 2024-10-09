package spring.cloud.ali.common.config;

import feign.Client;
import feign.RequestInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.cloud.openfeign.loadbalancer.FeignBlockingLoadBalancerClient;
import org.springframework.cloud.openfeign.loadbalancer.OnRetryNotEnabledCondition;
import org.springframework.cloud.openfeign.loadbalancer.RetryableFeignBlockingLoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import spring.cloud.ali.common.component.web.WrappedFeignClient;
import spring.cloud.ali.common.context.LoginContext;
import spring.cloud.ali.common.context.LoginUser;

import static spring.cloud.ali.common.context.LoginContext.HTTP_HEADER_LOGIN_USER_ID;

/**
 * FeignClient配置类，替换默认FeignClient
 * @see org.springframework.cloud.openfeign.loadbalancer.DefaultFeignLoadBalancerConfiguration
 */
public class FeignClientConfig {

    @Bean
    @Conditional(OnRetryNotEnabledCondition.class)
    public Client feignClient(LoadBalancerClient loadBalancerClient,
                              LoadBalancerClientFactory loadBalancerClientFactory,
                              WrappedFeignClient wrappedFeignClient) {
        return new FeignBlockingLoadBalancerClient(
                wrappedFeignClient,
                loadBalancerClient,
                loadBalancerClientFactory);
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.retry.support.RetryTemplate")
    @ConditionalOnBean(LoadBalancedRetryFactory.class)
    @ConditionalOnProperty(value = "spring.cloud.loadbalancer.retry.enabled", havingValue = "true",
            matchIfMissing = true)
    public Client feignRetryClient(LoadBalancerClient loadBalancerClient,
                                   LoadBalancedRetryFactory loadBalancedRetryFactory,
                                   LoadBalancerClientFactory loadBalancerClientFactory,
                                   WrappedFeignClient wrappedFeignClient) {
        return new RetryableFeignBlockingLoadBalancerClient(
                wrappedFeignClient,
                loadBalancerClient,
                loadBalancedRetryFactory,
                loadBalancerClientFactory);
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            // 用户上下文传递
            LoginUser loginUser = LoginContext.get();
            if (loginUser != null){
                template.header(HTTP_HEADER_LOGIN_USER_ID, String.valueOf(loginUser.getId()));
            }
        };
    }

}
