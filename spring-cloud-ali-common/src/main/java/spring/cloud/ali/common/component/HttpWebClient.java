package spring.cloud.ali.common.component;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import spring.cloud.ali.common.dto.HttpResult;

import javax.annotation.Resource;

import java.util.Collections;
import java.util.Map;

import static spring.cloud.ali.common.exception.BizException.SERVICE_NOT_UNAVAILABLE;

/**
 * HTTP负载调用客户端（内部服务使用）
 * 推荐使用@FeignClient
 */
public class HttpServiceClient {

    private static final String HTTP = "http:";

    @Resource
    private LoadBalancerClient client;

    @Resource
    private RestTemplate restTemplate;

    public <T> HttpResult<T> get(
            String appName, String uri, Map<String, String> headers, Map<String, String> queries){
        ServiceInstance srv = client.choose(appName);
        if (srv == null){
            throw SERVICE_NOT_UNAVAILABLE;
        }
        String targetUri = String.format(
                HTTP + "//%s:%s/%s", srv.getHost(), srv.getPort(), uri);

        StringBuilder urlWithParams = new StringBuilder(targetUri);
        urlWithParams.append("?");
        queries.forEach((key, value) -> urlWithParams.append(key).append("=").append(value).append("&"));

        // Remove the trailing "&"
        String finalUrl = urlWithParams.toString().replaceAll("&$", "");

        if (headers == null){
            headers = Collections.emptyMap();
        }
        HttpEntity<?> httpEntity = new HttpEntity<>(headers);

        return restTemplate.exchange(finalUrl,
                HttpMethod.GET,
                httpEntity,
                new ParameterizedTypeReference<HttpResult<T>>() {}).getBody();
    }
}
