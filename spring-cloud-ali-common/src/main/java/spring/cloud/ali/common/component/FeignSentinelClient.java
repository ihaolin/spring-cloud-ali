package spring.cloud.ali.common.component;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.nacos.shaded.com.google.common.collect.ImmutableMap;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import feign.Client;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.pattern.PathPatternParser;
import spring.cloud.ali.common.dto.HttpResult;
import spring.cloud.ali.common.enums.HttpRespStatus;
import spring.cloud.ali.common.exception.BizException;
import spring.cloud.ali.common.exception.ServiceException;
import spring.cloud.ali.common.exception.SystemException;
import spring.cloud.ali.common.util.JsonUtil;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 自定义FeignClient
 */
@Slf4j
public class FeignSentinelClient implements Client {

    private static final String SERVER_INTERNAL_ERROR = JsonUtil.toJson(HttpResult.fail(BizException.SERVER_INTERNAL_ERROR));

    /**
     * 请求路径解析器
     */
    private static final PathPatternParser PATH_PARSER = new PathPatternParser();

    /**
     * 降级规则文件
     */
    private static final String DEGRADE_RULES = "degrade-rules-feign.json";

    /**
     * 资源前缀（用于隔离其他类资源，如mysql，redis等）
     */
    private static final String RESOURCE_PREFIX = "feign";

    /**
     * 资源间隔符号
     */
    private static final String RESOURCE_SPLITTER = "#";

    /**
     * 降级规则更新锁
     */
    private final Lock degradeRulesRefreshLock = new ReentrantLock();

    private final Client delegate;

    public FeignSentinelClient(Client delegate) {
        this.delegate = delegate;
    }

    @Override
    public Response execute(Request request, Request.Options options) {

        RequestTemplate rt = request.requestTemplate();
        String appName = rt.feignTarget().name();
        String reqMethod = rt.method();
        String reqUri = rt.path().replace(rt.feignTarget().url(), "");

        /*
         * 内存中的规则：
         *  feign#ali-user#GET#/users/detail
         * 配置文件中：
         *  {
         *      "resource": "GET#/users/detail",
         *      "limitApp": "ali-user",
         *      "..."
         *  }
         */
        String resource = RESOURCE_PREFIX + RESOURCE_SPLITTER +
                            appName + RESOURCE_SPLITTER +
                                reqMethod + RESOURCE_SPLITTER + reqUri;

        Entry sentinelEntry = null;
        Response response = null;
        try {
            sentinelEntry = SphU.entry(resource);
            response = delegate.execute(request, options);
            if (Objects.equals(HttpStatus.OK.value(), response.status())){
                return response;
            }

            if (HttpRespStatus.isServiceUnAvailable(response.status())){
                // 熔断异常拦截埋点
                Tracer.traceEntry(new ServiceException(response.status(), response.reason()), sentinelEntry);
            }

        } catch (BlockException e) {
            // 熔断了
            log.error("feign sentinel blocked: entry={}", sentinelEntry);
            return serverInternalError(request);
        } catch (SocketTimeoutException e) {
            // 接口响应超时
            log.error("feign response timeout: resource={}, request={}", resource, request);
            Tracer.traceEntry(new SystemException(e), sentinelEntry);
            return serverInternalError(request);
        } catch (Throwable e){
            // 未知异常
            log.error("feign unknown exception: resource={}, error={}", resource, Throwables.getStackTraceAsString(e));
            Tracer.traceEntry(new SystemException(e), sentinelEntry);
            return serverInternalError(request);
        } finally {
            if(sentinelEntry != null){
                sentinelEntry.exit();
            }
        }

        return response;
    }

    private static Response serverInternalError(Request request) {
        return Response.builder()
                .status(HttpStatus.OK.value())  // 设置状态码为200
                .body(SERVER_INTERNAL_ERROR, StandardCharsets.UTF_8)
                .headers(ImmutableMap.of(
                        "Content-Type", Lists.newArrayList("application/json")
                ))
                .request(request)
                .build();
    }
}
