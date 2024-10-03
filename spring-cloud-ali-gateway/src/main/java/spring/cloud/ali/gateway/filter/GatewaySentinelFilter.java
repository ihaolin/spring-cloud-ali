package spring.cloud.ali.gateway.filter;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.context.Context;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.shaded.com.google.common.base.Strings;
import com.alibaba.nacos.shaded.com.google.common.base.Throwables;
import com.alibaba.nacos.shaded.com.google.common.collect.Maps;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import reactor.core.publisher.Mono;
import spring.cloud.ali.common.component.SentinelConfigService;
import spring.cloud.ali.common.enums.HttpRespStatus;
import spring.cloud.ali.common.util.WebUtil;
import spring.cloud.ali.gateway.config.SpringCloudGatewayConfig;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;
import static spring.cloud.ali.common.enums.HttpRespStatus.DEFAULT;
import static spring.cloud.ali.common.enums.HttpRespStatus.HTTP_REQUEST_TOO_MANY;

/**
 * 基于Sentinel实现的流控熔断拦截器：
 *   1. API维度（Sentinel本身只支持route和API分组维度的治理）
 *   2. 支持nacos动态更新（业务应用，需提前在nacos配置规则，因为网关无法预知有哪些具体路径）
 *   3. TODO 可考虑为业务应用，进行自动化配置（拉取应用所有api写入默认规则文件）
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewaySentinelFilter implements GlobalFilter {

    private static final String SENTINEL_CTX = "_sentinel_gateway_ctx_";

    private static final String SENTINEL_FLOW_RULES = "flow-rules";
    private static final String SENTINEL_FLOW_RULES_PREFIX = SENTINEL_FLOW_RULES + "-";

    private static final String SENTINEL_DEGRADE_RULES = "degrade-rules";
    private static final String SENTINEL_DEGRADE_RULES_PREFIX = SENTINEL_DEGRADE_RULES + "-";

    private static final String SENTINEL_RULES_SUFFIX = ".json";

    private static final String SENTINEL_RULE_APP_SPLITTER = "#";

    @Value("${spring.application.name}")
    private String appName;

    @Resource
    private SpringCloudGatewayConfig gatewayConfig;

    @Resource
    private SentinelConfigService sentinelConfigService;

    private final Map<String, RouteAppRules> allAppRules = Maps.newConcurrentMap();

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        try {
            initAllAppRules();
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    @EventListener
    public void onApplicationEnvChanged(EnvironmentChangeEvent event) {
        for (String key : event.getKeys()){
            if (key.startsWith("spring.cloud.gateway.routes")){
                // 路由信息发生变化
                log.info("routes updated: {}", gatewayConfig.getRoutes());
                refreshAppRules();
                break;
            }
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();
        ServerHttpResponse resp = exchange.getResponse();
        resp.getHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");

        // 当前路由信息
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        if(route == null){
            return chain.filter(exchange);
        }

        String resource = resolveResource(route, req);
        try {

            if (Strings.isNullOrEmpty(resource)){
                // 该路径，没有配置规则
                return chain.filter(exchange);
            }

            Context sentinelCtx = ContextUtil.enter(SENTINEL_CTX);
            Entry entry = SphU.entry(resource);
            return chain.filter(exchange)
                    .onErrorResume(e -> {

                        if (e instanceof ResponseStatusException){
                            ResponseStatusException rse = (ResponseStatusException) e;
                            if (HttpRespStatus.isServiceUnAvailable(rse.getStatus())){
                                // 异常埋点统计
                                ContextUtil.runOnContext(sentinelCtx, () -> Tracer.traceEntry(e, entry));
                                log.warn("resource {} isn't available, trace entry: {}", resource, entry);
                            }
                        }

                        return resp.writeWith(Mono.just(
                                resp.bufferFactory().wrap(DEFAULT.getMsg().getBytes(StandardCharsets.UTF_8))));
                    }).then(Mono.defer(() -> {
                        if (entry != null) {
                            entry.exit();
                        }
                        ContextUtil.exit();
                        return Mono.empty();
                    }));
        } catch (BlockException e){
            HttpRespStatus rs = e instanceof FlowException ? HTTP_REQUEST_TOO_MANY : DEFAULT;
            log.error("api request blocked: resource={}, rule={}", resource, e.getRule());
            return resp.writeWith(Mono.just(resp.bufferFactory().wrap(
                    rs.getMsg().getBytes(StandardCharsets.UTF_8))));
        } catch (Throwable e){
            log.error("unknown exception: resource={}, error={}", resource, Throwables.getStackTraceAsString(e));
        }

        return resp.writeWith(Mono.just(
                resp.bufferFactory().wrap(DEFAULT.getMsg().getBytes(StandardCharsets.UTF_8))));
    }

    public String resolveResource(Route route, ServerHttpRequest req) {

        // 内存中规则：ali-user#GET#/users/detail，规则配置文件里是GET#/users/detail
        String requestPath = req.getPath().value();
        String resourcePrefix = route.getId() + SENTINEL_RULE_APP_SPLITTER + req.getMethod().name() + SENTINEL_RULE_APP_SPLITTER;
        String resource = resourcePrefix + requestPath;

        RouteAppRules appRules = allAppRules.get(route.getId());
        if (appRules.contain(resource)){
            // 精确匹配到
            return resource;
        }

        // 尝试模式匹配，如：/users/123 -> /users/{id}
        return doPatternMatch(requestPath, resourcePrefix, appRules.getDegrades().keySet());
    }

    private static String doPatternMatch(String requestPath, String resourcePrefix, Set<String> resources) {
        for(String resource : resources){
            if (resource.contains("{")){
                PathPattern.PathMatchInfo matched =
                        WebUtil.matchPatten(resource.replace(resourcePrefix, ""), requestPath);
                if (matched != null){
                    // 匹配到了
                    return resource;
                }
            }
        }
        return null;
    }

    /**
     * 启动时，初始化应用规则
     */
    private void initAllAppRules() throws NacosException {
        // 拉取各应用的规则
        List<RouteDefinition> routes = gatewayConfig.getRoutes();
        if(CollectionUtils.isEmpty(routes)){
            return;
        }

        for (RouteDefinition route : routes){
            initAppRules(route.getId());
        }
    }

    /**
     * 刷新正在监听的规则文件列表（至少能按应用隔离配置文件）
     *  1. 一个route表示一个应用
     *  2. 一个应用有多个规则文件，如应用ali-user，则会监听：
     *     命名空间：_sentinel_
     *     配置组：ali-gateway
     *     规则文件列表：
     *        flow-rules-ali-user.json
     *        degrade-rules-ali-user.json
     *        ...
     */
    private void refreshAppRules() {

        List<RouteDefinition> latestRoutes = gatewayConfig.getRoutes();
        if (!CollectionUtils.isEmpty(latestRoutes)){

            Set<String> currentRouteIds = allAppRules.keySet();
            Set<String> latestRouteIds = latestRoutes.stream().map(RouteDefinition::getId).collect(Collectors.toSet());

            // 新增的Route，需添加监听
            Set<String> addedRouteIds = new HashSet<>(latestRouteIds);
            addedRouteIds.removeAll(currentRouteIds);
            if(!CollectionUtils.isEmpty(addedRouteIds)){
                for (String routeId : addedRouteIds){
                    try {
                        initAppRules(routeId);
                    } catch (NacosException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            // 删除的Route，需移除监听和卸载规则
            Set<String> removedRouteIds = new HashSet<>(currentRouteIds);
            removedRouteIds.removeAll(latestRouteIds);
            if(!CollectionUtils.isEmpty(removedRouteIds)){
                for (String routeId : removedRouteIds){
                    destroyAppRules(routeId);
                }
            }
        }
    }

    private void initAppRules(String routeId) throws NacosException {

        // 初始化流控规则
        sentinelConfigService.initFlowRules(buildFlowRulesFileName(routeId), appName, new SentinelConfigService.RuleListener<FlowRule>() {
            @Override
            public void prevRefresh(List<FlowRule> refreshing) {
                // 网关内存用routeId作前缀，避免不同应用冲突
                refreshing.forEach((r) -> r.setResource(routeId + SENTINEL_RULE_APP_SPLITTER + r.getResource()));
            }

            @Override
            public void postRefresh(List<FlowRule> refreshed) {
                RouteAppRules routeRules = allAppRules.get(routeId);
                if (routeRules == null){
                    routeRules = new RouteAppRules();
                    routeRules.setRouteId(routeId);
                    allAppRules.put(routeId, routeRules);
                }
                routeRules.setFlows(refreshed.stream().collect(Collectors.toMap(FlowRule::getResource, r -> r)));
                log.info("sentinel flow rules refreshed: routeId={}, rules={}", routeId, routeRules.getFlows());
            }
        });

        // 初始化降级规则
        sentinelConfigService.initDegradeRules(buildDegradeRulesFileName(routeId), appName, new SentinelConfigService.RuleListener<DegradeRule>() {
            @Override
            public void prevRefresh(List<DegradeRule> refreshing) {
                // 网关内存用routeId作前缀，避免不同应用冲突
                refreshing.forEach((r) -> r.setResource(routeId + SENTINEL_RULE_APP_SPLITTER + r.getResource()));
            }

            @Override
            public void postRefresh(List<DegradeRule> refreshed) {
                RouteAppRules routeRules = allAppRules.get(routeId);
                if (routeRules == null){
                    routeRules = new RouteAppRules();
                    routeRules.setRouteId(routeId);
                    allAppRules.put(routeId, routeRules);
                }
                routeRules.setDegrades(refreshed.stream().collect(Collectors.toMap(DegradeRule::getResource, r -> r)));
                log.info("sentinel degrade rules refreshed: routeId={}, rules={}", routeId, routeRules.getDegrades());
            }
        });

    }

    /**
     * 卸载应用规则（当该路由不存在后）
     * @param routeId 路由ID
     */
    private void destroyAppRules(String routeId) {

        // 销毁流控规则
        sentinelConfigService.destroyFlowRules(buildFlowRulesFileName(routeId), appName);

        // 销毁熔断规则
        sentinelConfigService.destroyDegradeRules(buildDegradeRulesFileName(routeId), appName);

        // 移除该应用
        allAppRules.remove(routeId);
    }

    private String buildFlowRulesFileName(String routeId) {
        return SENTINEL_FLOW_RULES_PREFIX + routeId + SENTINEL_RULES_SUFFIX;
    }

    private String buildDegradeRulesFileName(String routeId) {
        return SENTINEL_DEGRADE_RULES_PREFIX + routeId + SENTINEL_RULES_SUFFIX;
    }

    @Data
    private static class RouteAppRules {

        private String routeId;

        private Map<String, FlowRule> flows = Collections.emptyMap();

        private Map<String, DegradeRule> degrades = Collections.emptyMap();

        public boolean contain(String resource){
            return flows.containsKey(resource) || degrades.containsKey(resource);
        }
    }
}
