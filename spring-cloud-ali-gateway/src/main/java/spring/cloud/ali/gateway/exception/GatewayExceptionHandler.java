package spring.cloud.ali.gateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;
import spring.cloud.ali.common.dto.HttpResult;
import spring.cloud.ali.common.enums.HttpRespStatus;
import spring.cloud.ali.common.exception.BizException;
import spring.cloud.ali.common.util.JsonUtil;

import java.nio.charset.StandardCharsets;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;
import static spring.cloud.ali.common.enums.HttpRespStatus.HTTP_NOT_FOUND;

/**
 * 全局错误处理（针对后端服务的错误响应）
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class GatewayExceptionHandler implements WebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable e) {
        ServerHttpResponse resp = exchange.getResponse();
        if (resp.isCommitted()) {
            return Mono.error(e);
        }

        // 当前路由信息
        String msg = HttpRespStatus.DEFAULT.getMsg();
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        if(route == null){
            // 没有路由404
            return respError(resp, HTTP_NOT_FOUND.getMsg());
        }

        if (e instanceof BizException){
            // 业务异常
            return resp.writeWith(Mono.just(resp.bufferFactory().wrap(
                    JsonUtil.toJson(HttpResult.fail((BizException) e)).getBytes(StandardCharsets.UTF_8))));
        }

        if (e instanceof ResponseStatusException){
            ResponseStatusException respExp = (ResponseStatusException) e;
            if(HttpRespStatus.isServiceUnAvailable(respExp.getStatus())){
                // 服务不可用异常，向上抛出
                return Mono.error(e);
            }
            HttpRespStatus respStatus = HttpRespStatus.get(respExp.getStatus());
            resp.setStatusCode(respStatus.getStatus());
            msg = respStatus.getMsg();
        }

        return respError(resp, msg);
    }

    private Mono<Void> respError(ServerHttpResponse resp, String msg){
        resp.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return resp.writeWith(
                Mono.just(resp.bufferFactory().wrap(msg.getBytes(StandardCharsets.UTF_8)))
        );
    }
}