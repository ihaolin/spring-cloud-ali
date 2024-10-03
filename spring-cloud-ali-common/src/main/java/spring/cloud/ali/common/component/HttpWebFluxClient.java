package spring.cloud.ali.common.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import spring.cloud.ali.common.dto.HttpResult;

import java.util.Map;

/**
 * Http WebFlux Client（gateway等应用强制使用）
 */
@Slf4j
public class HttpWebClient {

    private final WebClient webClient;

    public HttpWebClient(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * 发送 GET 请求
     *
     * @param serviceName 服务名称
     * @param path        请求路径
     * @param headers     请求头
     * @param queries     query参数
     * @param responseType 响应类型
     * @param <T>        响应类型
     * @return 响应结果
     */
    public <T> Mono<HttpResult<T>> get(
            String serviceName, String path, Map<String, String> headers, Map<String, ?> queries, ParameterizedTypeReference<HttpResult<T>> responseType) {
        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(path);
                    if (!CollectionUtils.isEmpty(queries)) {
                        queries.forEach(uriBuilder::queryParam);
                    }
                    return uriBuilder.scheme("http")
                            .host(serviceName)
                            .build();
                })
                .headers((reqHeaders) -> {
                    if (!CollectionUtils.isEmpty(headers)){
                        headers.forEach((reqHeaders::add));
                    }
                })
                .retrieve()
                .bodyToMono(responseType);
    }

    /**
     * 发送 POST 请求
     *
     * @param serviceName 服务名称
     * @param path        请求路径
     * @param headers     请求头
     * @param queries     query参数
     * @param body        请求体
     * @param responseType  响应类型
     * @param <T>           响应类型
     * @return 响应结果
     */
    public <T> Mono<HttpResult<T>> post(
            String serviceName, String path, Map<String, String> headers, Map<String, ?> queries, Object body, ParameterizedTypeReference<HttpResult<T>> responseType) {
        return webClient.post()
                .uri(uriBuilder -> {
                    uriBuilder.path(path);
                    if (!CollectionUtils.isEmpty(queries)) {
                        queries.forEach(uriBuilder::queryParam);
                    }
                    return uriBuilder.scheme("http")
                            .host(serviceName)
                            .build();
                })
                .headers((reqHeaders) -> {
                    if (!CollectionUtils.isEmpty(headers)){
                        headers.forEach((reqHeaders::add));
                    }
                })
                .bodyValue(body)
                .retrieve()
                .bodyToMono(responseType);
    }
}
