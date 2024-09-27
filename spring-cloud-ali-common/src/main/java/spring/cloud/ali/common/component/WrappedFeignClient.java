package spring.cloud.ali.common.component;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.shaded.com.google.common.base.Strings;
import com.alibaba.nacos.shaded.com.google.common.collect.ImmutableMap;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import feign.Client;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.pattern.PathPattern;
import spring.cloud.ali.common.dto.HttpResult;
import spring.cloud.ali.common.enums.HttpRespStatus;
import spring.cloud.ali.common.exception.BizException;
import spring.cloud.ali.common.exception.ServiceException;
import spring.cloud.ali.common.exception.SystemException;
import spring.cloud.ali.common.util.JsonUtil;
import spring.cloud.ali.common.util.WebUtil;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * FeignClient包装类
 */
@Slf4j
public class WrappedFeignClient implements Client {

    private static final String SERVER_INTERNAL_ERROR = JsonUtil.toJson(HttpResult.fail(BizException.SERVER_INTERNAL_ERROR));

    private static final String DEGRADE_RULES = "degrade-rules-feign.json";

    private static final String RESOURCE_PREFIX = "feign";

    private static final String RESOURCE_SPLITTER = "#";

    @Value("${spring.application.name}")
    private String appName;

    @Resource
    private SentinelConfigService sentinelConfigService;

    private volatile Map<String, DegradeRule> degradeRuleMap = Collections.emptyMap();

    private final Client delegate;

    public WrappedFeignClient(Client delegate) {
        this.delegate = delegate;
    }

    @PostConstruct
    public void onInit() {
        try {
            sentinelConfigService.initDegradeRules(DEGRADE_RULES, appName, new SentinelConfigService.RuleListener<DegradeRule>() {

                @Override
                public void prevRefresh(List<DegradeRule> refreshing) {
                    // 增加前缀feign和limitApp，避免和其他应用规则冲突
                    refreshing.forEach((r) -> r.setResource(
                            RESOURCE_PREFIX + RESOURCE_SPLITTER + r.getLimitApp() + RESOURCE_SPLITTER + r.getResource()));
                }

                @Override
                public void postRefresh(List<DegradeRule> refreshed) {
                    degradeRuleMap = refreshed.stream().collect(Collectors.toMap(DegradeRule::getResource, f -> f));
                }
            });
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Response execute(Request request, Request.Options options) {

        Entry sentinelEntry = null;
        Response response;
        String resource = resolveResource(request);
        try {

            if(Strings.isNullOrEmpty(resource)){
                // 未匹配到降级规则
                return delegate.execute(request, options);
            }

            sentinelEntry = SphU.entry(resource);
            response = delegate.execute(request, options);
            if (Objects.equals(HttpStatus.OK.value(), response.status())){
                // TODO 检查业务结果？
                return response;
            }

            if (HttpRespStatus.isServiceUnAvailable(response.status())){
                // 熔断异常拦截埋点
                Tracer.traceEntry(new ServiceException(response.status(), response.reason()), sentinelEntry);
                log.warn("resource {} isn't available, trace entry: {}", resource, sentinelEntry);
            }

        } catch (BlockException e) {
            // 熔断了
            log.error("feign request blocked: resource={}, rule={}", resource, e.getRule());
            return serverInternalError(request);
        } catch (SocketTimeoutException e) {
            // 接口响应超时
            log.error("feign response timeout: resource={}, request={}", resource, request);
            Tracer.traceEntry(new SystemException(e), sentinelEntry);
            return serverInternalError(request);
        } catch (Throwable e){
            // 未知异常
            log.error("feign request exception: resource={}, error={}", resource, Throwables.getStackTraceAsString(e));
            Tracer.traceEntry(new SystemException(e), sentinelEntry);
            return serverInternalError(request);
        } finally {
            if(sentinelEntry != null){
                sentinelEntry.exit();
            }
        }

        return response;
    }

    private String resolveResource(Request request) {
        // GET#/api/users
        RequestTemplate rt = request.requestTemplate();
        String targetName = rt.feignTarget().name();    // 应用名称, @FeignClient(name)
        String reqMethod = rt.method();
        String reqUri = rt.path().replace(rt.feignTarget().url(), "");

        /*
         * 内存中的规则：
         *  feign#ali-user#GET#/users/detail
         * 配置文件中：
         *  {
         *      "resource": "GET#/users/detail",
         *      "limitApp": "ali-user", （和targetName保持一致）
         *      "..."
         *  }
         */
        String resourcePrefix = RESOURCE_PREFIX + RESOURCE_SPLITTER + targetName + RESOURCE_SPLITTER;
        String resource = resourcePrefix + reqMethod + RESOURCE_SPLITTER + reqUri;

        // 精确匹配
        if (degradeRuleMap.containsKey(resource)){
            return resource;
        }

        // 尝试模式匹配，如/users/123 -> /users/{id}
        for (String degradeResource : degradeRuleMap.keySet()){
            if (degradeResource.contains("{")){
                // feign#ali-user#GET#/users/{userId} -> /users/{userId}
                String fmtDegradeResource = degradeResource.replace(resourcePrefix + reqMethod + RESOURCE_SPLITTER, "");
                PathPattern.PathMatchInfo matched = WebUtil.matchUri(fmtDegradeResource, reqUri);
                if (matched != null){
                    // 匹配到了
                    return degradeResource;
                }
            }
        }

        return null;
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
