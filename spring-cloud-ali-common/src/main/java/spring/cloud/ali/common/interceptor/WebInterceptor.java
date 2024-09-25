package spring.cloud.ali.common.interceptor;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.shaded.com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.pattern.PathPattern;
import spring.cloud.ali.common.component.SentinelConfigService;
import spring.cloud.ali.common.util.WebUtil;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static spring.cloud.ali.common.enums.HttpRespStatus.HTTP_REQUEST_TOO_MANY;

/**
 * 基于Sentinel实现的微服务API限流拦截器
 * TODO 增加默认限流策略
 */
@Slf4j
public class WebInterceptor implements HandlerInterceptor {

    private static final String FLOW_RULES = "flow-rules.json";

    private static final String RESOURCE_SPLITTER = "#";

    @Value("${spring.application.name}")
    private String appName;

    @Resource
    private SentinelConfigService sentinelConfigService;

    /**
     * 用于路径资源匹配
     */
    private volatile Map<String, FlowRule> flowRuleMap = Collections.emptyMap();

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        try {
            sentinelConfigService.initFlowRules(FLOW_RULES, appName, new SentinelConfigService.RuleListener<FlowRule>() {
                @Override
                public void postRefresh(List<FlowRule> refreshed) {
                    flowRuleMap = refreshed.stream().collect(Collectors.toMap(FlowRule::getResource, f -> f));
                }
            });
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String resource = resolveResource(request);
        if(Strings.isNullOrEmpty(resource)){
            return true;
        }

        Entry entry = null;
        try {
            // 通过SphU.entry来检查该资源是否通过限流
            entry = SphU.entry(resource, EntryType.IN);
        } catch (BlockException ex) {
            if (ex instanceof FlowException){
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(HTTP_REQUEST_TOO_MANY.getMsg());
            }
            return false;
        } finally {
            // 释放资源
            if (entry != null) {
                entry.exit();
            }
        }

        return true;
    }

    private String resolveResource(HttpServletRequest request) {
        // GET#/api/v1/users
        String resourcePrefix = request.getMethod() + RESOURCE_SPLITTER;
        String resource = resourcePrefix + request.getRequestURI();

        // 精确匹配
        if (flowRuleMap.containsKey(resource)){
            return resource;
        }

        // 尝试模式匹配，如/users/123 -> /users/{id}
        for (String flowResource : flowRuleMap.keySet()){
            if (flowResource.contains("{")){
                PathPattern.PathMatchInfo matched =
                        WebUtil.matchUri(flowResource.replace(resourcePrefix, ""), request.getRequestURI());
                if (matched != null){
                    // 匹配到了
                    return flowResource;
                }
            }
        }

        return null;
    }
}
