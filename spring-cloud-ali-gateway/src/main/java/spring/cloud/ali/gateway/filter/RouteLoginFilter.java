package spring.cloud.ali.gateway.filter;

import com.alibaba.nacos.shaded.com.google.common.collect.ImmutableMap;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import spring.cloud.ali.common.component.HttpWebFluxClient;
import spring.cloud.ali.common.dto.HttpResult;
import spring.cloud.ali.common.enums.HttpRespStatus;
import spring.cloud.ali.user.result.VerifyTokenResult;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static spring.cloud.ali.common.context.LoginContext.HTTP_HEADER_LOGIN_TOKEN;
import static spring.cloud.ali.common.context.LoginContext.HTTP_HEADER_LOGIN_USER_ID;
import static spring.cloud.ali.common.enums.HttpRespStatus.HTTP_NOT_AUTH;

@Slf4j
@Component
public class RouteLoginFilter extends AbstractGatewayFilterFactory<RouteLoginFilter.Config> {

    @Resource
    private HttpWebFluxClient httpWebFluxClient;

    public RouteLoginFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {

            ServerHttpRequest req = exchange.getRequest();
            assert req.getMethod() != null;

            String reqUri = req.getMethod().name() + "#" + req.getURI().getPath();
            if (config.getExcludes() != null && config.getExcludes().contains(reqUri)) {
                return chain.filter(exchange);
            }

            String loginToken = req.getHeaders().getFirst(HTTP_HEADER_LOGIN_TOKEN);
            if (Strings.isNullOrEmpty(loginToken)) {
                return respError(exchange, HTTP_NOT_AUTH);
            }

            // 调用 WebClient 的异步请求来校验 token
            return httpWebFluxClient.get(
                    "ali-user", "/users/verify-token", null, ImmutableMap.of("token", loginToken),
                            new ParameterizedTypeReference<HttpResult<VerifyTokenResult>>() {})
                    .flatMap(verifyResult -> {

                        if (verifyResult == null ||
                                verifyResult.getData() == null ||
                                    !verifyResult.getData().isPass()) {
                            return respError(exchange, HTTP_NOT_AUTH);
                        }

                        // 验证通过用户ID埋入请求头
                        ServerHttpRequest request = exchange.getRequest().mutate()
                                .header(HTTP_HEADER_LOGIN_USER_ID, verifyResult.getData().getUserId().toString())
                                .build();

                        return chain.filter(exchange.mutate().request(request).build());
                    })
                    .onErrorResume(e -> {
                        log.error("unknown exception: reqUri={}, error={}", reqUri, Throwables.getStackTraceAsString(e));

                        if (e instanceof WebClientResponseException){
                            // TODO 用户响应异常， 降级处理

                        }

                        return respError(exchange, HttpRespStatus.DEFAULT);
                    });
        };
    }

    private Mono<Void> respError(ServerWebExchange exchange, HttpRespStatus s) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(s.getStatus());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        return response.writeWith(Mono.just(response.bufferFactory().wrap(
                s.getMsg().getBytes(StandardCharsets.UTF_8))));
    }

    @Data
    public static class Config {
        /**
         * 需要排除的url：GET#/users/login，POST#/users/register
         */
        private Set<String> excludes;
    }
}
