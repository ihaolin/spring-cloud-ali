package spring.cloud.ali.common.config;

import feign.Client;
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
import spring.cloud.ali.common.component.FeignSentinelClient;

/**
 * FeignClient配置类，替换默认FeignClient
 * @see org.springframework.cloud.openfeign.loadbalancer.DefaultFeignLoadBalancerConfiguration
 */
public class FeignClientConfig {

    @Bean
    @Conditional(OnRetryNotEnabledCondition.class)
    public Client feignClient(LoadBalancerClient loadBalancerClient,
                              LoadBalancerClientFactory loadBalancerClientFactory) {
        return new FeignBlockingLoadBalancerClient(
                new FeignSentinelClient(new Client.Default(null, null)),
                loadBalancerClient,
                loadBalancerClientFactory);
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.retry.support.RetryTemplate")
    @ConditionalOnBean(LoadBalancedRetryFactory.class)
    @ConditionalOnProperty(value = "spring.cloud.loadbalancer.retry.enabled", havingValue = "true",
            matchIfMissing = true)
    public Client feignRetryClient(LoadBalancerClient loadBalancerClient,
                                   LoadBalancedRetryFactory loadBalancedRetryFactory, LoadBalancerClientFactory loadBalancerClientFactory) {
        return new RetryableFeignBlockingLoadBalancerClient(
                new FeignSentinelClient(new Client.Default(null, null)),
                loadBalancerClient,
                loadBalancedRetryFactory,
                loadBalancerClientFactory);
    }
}
