package spring.cloud.ali.gateway.filter;

import com.google.common.base.Strings;
import io.jsonwebtoken.Claims;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import spring.cloud.ali.common.util.JwtTokenUtil;
import spring.cloud.ali.gateway.config.AppConfig;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;

import static spring.cloud.ali.common.context.LoginContext.HTTP_HEADER_LOGIN_TOKEN;
import static spring.cloud.ali.common.context.LoginContext.HTTP_HEADER_LOGIN_USER_ID;
import static spring.cloud.ali.common.enums.HttpRespStatus.HTTP_NOT_AUTH;

@Slf4j
@Component
public class RouteLoginFilter extends AbstractGatewayFilterFactory<RouteLoginFilter.Config> {

    @Resource
    private AppConfig appConfig;

    public RouteLoginFilter() {
        super(Config.class);
    }

    @Data
    public static class Config {
        /**
         * 需要排除的url：GET#/users/login，POST#/users/register
         */
        private Set<String> excludes;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {

            ServerHttpRequest req = exchange.getRequest();
            assert req.getMethod() != null;

            if (config.getExcludes() != null &&
                    config.getExcludes().contains(req.getMethod().name() + "#" + req.getURI().getPath())) {
                return chain.filter(exchange);
            }

            String loginToken = req.getHeaders().getFirst(HTTP_HEADER_LOGIN_TOKEN);
            if (Strings.isNullOrEmpty(loginToken)){
                return notLogin(exchange);
            }

            Claims claims = JwtTokenUtil.parse(appConfig.getLoginSignKey(), loginToken);
            if (claims == null || claims.getExpiration().before(new Date())){
                return notLogin(exchange);
            }

            String loginUserId = claims.get(HTTP_HEADER_LOGIN_USER_ID, String.class);
            if (Strings.isNullOrEmpty(loginUserId)){
                return notLogin(exchange);
            }

            // 埋入请求Header
            ServerHttpRequest request = exchange.getRequest().mutate()
                    .header(HTTP_HEADER_LOGIN_USER_ID, loginUserId)
                    .build();
            return chain.filter(exchange.mutate().request(request).build());
        };
    }

    private Mono<Void> notLogin(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HTTP_NOT_AUTH.getStatus());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        return response.writeWith(Mono.just(response.bufferFactory().wrap(
                HTTP_NOT_AUTH.getMsg().getBytes(StandardCharsets.UTF_8))));
    }
}
