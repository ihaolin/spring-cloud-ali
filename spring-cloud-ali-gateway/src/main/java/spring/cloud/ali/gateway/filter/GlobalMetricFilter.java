package spring.cloud.ali.gateway.filter;

import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import io.micrometer.core.instrument.MeterRegistry;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class GlobalMetricFilter implements GlobalFilter {

    private static final String METRIC_PREFIX = "ali_gateway_requests";

    private final MeterRegistry meterRegistry;

    @Autowired
    public GlobalMetricFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        if(route == null){
            return chain.filter(exchange);
        }

        String service = route.getId();
        long startTime = System.currentTimeMillis();

        return chain.filter(exchange).doOnTerminate(() -> {
            long duration = System.currentTimeMillis() - startTime;
            String uri = exchange.getRequest().getURI().getPath();
            String method = exchange.getRequest().getMethodValue();

            // 计数
            meterRegistry.counter(
                    METRIC_PREFIX + "_counter",
                    "service", service,
                    "uri", uri,
                    "method", method)
                    .increment();

            // 耗时
            Timer.builder(METRIC_PREFIX + "_timer")
                    .tags("service", service, "uri", uri, "method", method)
                    .publishPercentileHistogram()
                    .publishPercentiles(0.5, 0.90, 0.95, 0.99)
                    .serviceLevelObjectives(
                        Duration.ofMillis(50),
                        Duration.ofMillis(100),
                        Duration.ofMillis(200),
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(5))
                    .minimumExpectedValue(Duration.ofMillis(10))
                    .maximumExpectedValue(Duration.ofSeconds(5))
                    .distributionStatisticExpiry(Duration.ofMinutes(5))
                    .register(meterRegistry)
                    .record(duration, TimeUnit.MILLISECONDS);
        });
    }
}
