
package spring.cloud.ali.gateway.config;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerClientRequestTransformer;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import spring.cloud.ali.common.component.HttpWebFluxClient;
import spring.cloud.ali.common.component.SentinelConfigService;

import java.util.List;

@Configuration
public class ComponentConfig {

    @Bean
    public SentinelConfigService sentinelConfigService(){
        return new SentinelConfigService();
    }

    @Bean
    public WebClient webClient(ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory,
                                           List<LoadBalancerClientRequestTransformer> transformers) {
        ReactorLoadBalancerExchangeFilterFunction lbFunction =
                new ReactorLoadBalancerExchangeFilterFunction(loadBalancerFactory, transformers);
        return WebClient.builder().filter(lbFunction).build();
    }

    @Bean
    public HttpWebFluxClient httpWebFluxClient(WebClient webClient){
        return new HttpWebFluxClient(webClient);
    }
}
