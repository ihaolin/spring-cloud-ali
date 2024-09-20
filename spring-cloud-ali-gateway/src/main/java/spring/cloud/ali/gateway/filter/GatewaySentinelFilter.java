package spring.cloud.ali.gateway.filter;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.context.Context;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.nacos.api.config.listener.AbstractListener;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.shaded.com.google.common.base.Strings;
import com.alibaba.nacos.shaded.com.google.common.base.Throwables;
import com.alibaba.nacos.shaded.com.google.common.collect.Maps;
import com.fasterxml.jackson.core.type.TypeReference;
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
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;
import spring.cloud.ali.common.component.SentinelConfigService;
import spring.cloud.ali.common.enums.HttpRespStatus;
import spring.cloud.ali.common.util.JsonUtil;
import spring.cloud.ali.gateway.config.SpringCloudGatewayConfig;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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

    /**
     * 请求路径解析器
     */
    private static final PathPatternParser PATH_PARSER = new PathPatternParser();

    /**
     * Sentinel上下文
     */
    private static final String SENTINEL_CTX = "_sentinel_gateway_ctx_";

    /**
     * 流控规则文件前缀
     */
    private static final String SENTINEL_FLOW_RULES = "flow-rules";
    private static final String SENTINEL_FLOW_RULES_PREFIX = SENTINEL_FLOW_RULES + "-";

    /**
     * 降级规则文件前缀
     */
    private static final String SENTINEL_DEGRADE_RULES = "degrade-rules";
    private static final String SENTINEL_DEGRADE_RULES_PREFIX = SENTINEL_DEGRADE_RULES + "-";

    /**
     * 规则文件后缀
     */
    private static final String SENTINEL_RULES_SUFFIX = ".json";

    /**
     * Sentinel Manager是共享实例，避免应用间的规则重名，加一个分隔符
     */
    private static final String SENTINEL_RULE_APP_SPLITTER = "#";

    /**
     * 流控规则更新锁
     */
    private final Lock flowRulesRefreshLock = new ReentrantLock();

    /**
     * 降级规则更新锁
     */
    private final Lock degradeRulesRefreshLock = new ReentrantLock();

    /**
     * 应用名称
     */
    @Value("${spring.application.name}")
    private String appName;

    @Resource
    private SpringCloudGatewayConfig gatewayConfig;

    @Resource
    private SentinelConfigService sentinelConfigService;

    /**
     * <routeId, RouteAppRules>
     */
    private final Map<String, RouteAppRules> allAppRules = Maps.newConcurrentMap();

    /**
     * <规则文件名, 监听对象>
     */
    private final Map<String, Listener> allRuleListeners = Maps.newConcurrentMap();

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        try {
            initAppRules();
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
        if (Strings.isNullOrEmpty(resource)){
            // 该路径，没有配置规则
            return chain.filter(exchange);
        }

        try {
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
            log.warn("resource {} is blocked: rule={}", resource, e.getRule());
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
                        PATH_PARSER.parse(resource.replace(resourcePrefix, ""))
                                .matchAndExtract(PathContainer.parsePath(requestPath));
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
    private void initAppRules() throws NacosException {
        // 拉取各应用的规则
        List<RouteDefinition> routes = gatewayConfig.getRoutes();
        if(CollectionUtils.isEmpty(routes)){
            return;
        }

        for (RouteDefinition route : routes){
            fetchAndListenRules(route.getId());
        }

        log.info("initAppRules finished: {}", allAppRules);
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
                        fetchAndListenRules(routeId);
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
                    endListenRules(routeId);
                }
            }
        }
    }

    private void fetchAndListenRules(String routeId) throws NacosException {

        // 拉取流控规则
        String flowRules = sentinelConfigService.get().getConfig(buildFlowRulesFileName(routeId), appName, 5000);
        doRefreshAppFlowRules(routeId, flowRules);

        // 拉取降级规则
        String degradeRules = sentinelConfigService.get().getConfig(buildDegradeRulesFileName(routeId), appName, 5000);
        doRefreshAppDegradeRules(routeId, degradeRules);

        startListenRules(routeId);
    }

    private void startListenRules(String routeId) throws NacosException {

        // 监听流控规则
        String flowRulesFile = buildFlowRulesFileName(routeId);
        if (!allRuleListeners.containsKey(flowRulesFile)){
            allRuleListeners.put(flowRulesFile, new AbstractListener() {
                @Override
                public void receiveConfigInfo(String rules) {
                    doRefreshAppFlowRules(routeId, rules);
                }
            });
            sentinelConfigService.get().addListener(flowRulesFile, appName, allRuleListeners.get(flowRulesFile));
        }

        // 监听降级规则
        String degradeRulesFile = buildDegradeRulesFileName(routeId);
        if (!allRuleListeners.containsKey(degradeRulesFile)){
            allRuleListeners.put(degradeRulesFile, new AbstractListener() {
                @Override
                public void receiveConfigInfo(String rules) {
                    doRefreshAppDegradeRules(routeId, rules);
                }
            });
            sentinelConfigService.get().addListener(degradeRulesFile, appName, allRuleListeners.get(degradeRulesFile));
        }
    }

    /**
     * 更新应用的流控规则
     * @param routeId 应用route id
     * @param rules 最新的规则（json列表）
     */
    private void doRefreshAppFlowRules(String routeId, String rules) {
        flowRulesRefreshLock.lock();
        try {

            if (Strings.isNullOrEmpty(rules)){
                rules = "[]";
            }

            // 应用最新的限流规则
            List<FlowRule> latestRules =
                    JsonUtil.toList(rules, new TypeReference<List<FlowRule>>() {});
            // 网关内存用routeId作前缀，避免不同应用冲突
            latestRules.forEach((r) -> r.setResource(routeId + SENTINEL_RULE_APP_SPLITTER + r.getResource()));
            Map<String, FlowRule> latestRulesMap = latestRules.stream().collect(Collectors.toMap(FlowRule::getResource, f -> f));

            // 应用当前的限流规则
            RouteAppRules routeRules = allAppRules.get(routeId);
            if (routeRules == null){
                routeRules = new RouteAppRules();
                routeRules.setRouteId(routeId);
                allAppRules.put(routeId, routeRules);
            }
            Map<String, FlowRule> currentRulesMap = routeRules.getFlows();

            // 新增的规则
            Set<String> addedRuleIds = new HashSet<>(latestRulesMap.keySet());
            addedRuleIds.removeAll(currentRulesMap.keySet());

            // 删除的规则
            Set<String> removedRuleIds = new HashSet<>(currentRulesMap.keySet());
            removedRuleIds.removeAll(latestRulesMap.keySet());

            // 网关当前的限流规则（包含所有应用）
            List<FlowRule> globalFlowRules = FlowRuleManager.getRules();
            ListIterator<FlowRule> iterator = globalFlowRules.listIterator();
            while (iterator.hasNext()) {
                FlowRule currentRule = iterator.next();
                if(removedRuleIds.contains(currentRule.getResource())){
                    // 删除无效的规则
                    iterator.remove();
                    continue;
                }
                // 更新存在的规则
                FlowRule latestRule = latestRulesMap.get(currentRule.getResource());
                if (latestRule != null){
                    iterator.set(latestRule);
                }
            }

            // 添加新规则
            for(String addedRuleId : addedRuleIds){
                globalFlowRules.add(latestRulesMap.get(addedRuleId));
            }

            // 更新应用流控规则
            routeRules.setFlows(latestRulesMap);

            // 更新全局流控规则
            FlowRuleManager.loadRules(globalFlowRules);
        } finally {
            flowRulesRefreshLock.unlock();
        }
    }

    /**
     * 更新应用的降级规则
     * @param routeId 应用route id
     * @param rules 最新的规则（json列表）
     */
    private void doRefreshAppDegradeRules(String routeId, String rules) {
        degradeRulesRefreshLock.lock();
        try {

            if (Strings.isNullOrEmpty(rules)){
                rules = "[]";
            }

            // 应用最新的降级规则
            List<DegradeRule> latestRules =
                    JsonUtil.toList(rules, new TypeReference<List<DegradeRule>>() {});
            // 网关内存用routeId作前缀，避免不同应用冲突
            latestRules.forEach((r) -> r.setResource(routeId + SENTINEL_RULE_APP_SPLITTER + r.getResource()));
            Map<String, DegradeRule> latestRulesMap = latestRules.stream().collect(Collectors.toMap(DegradeRule::getResource, d -> d));

            // 应用当前的限流规则
            RouteAppRules routeRules = allAppRules.get(routeId);
            if (routeRules == null){
                routeRules = new RouteAppRules();
                allAppRules.put(routeId, routeRules);
            }
            Map<String, DegradeRule> currentRulesMap = routeRules.getDegrades();

            // 新增的规则
            Set<String> addedRuleIds = new HashSet<>(latestRulesMap.keySet());
            addedRuleIds.removeAll(currentRulesMap.keySet());

            // 删除的规则
            Set<String> removedRuleIds = new HashSet<>(currentRulesMap.keySet());
            removedRuleIds.removeAll(latestRulesMap.keySet());

            // 网关当前的降级规则（包含所有应用）
            List<DegradeRule> globalDegradeRules = DegradeRuleManager.getRules();
            ListIterator<DegradeRule> iterator = globalDegradeRules.listIterator();
            while (iterator.hasNext()) {
                DegradeRule currentRule = iterator.next();
                if(removedRuleIds.contains(currentRule.getResource())){
                    // 删除无效的规则
                    iterator.remove();
                    continue;
                }
                // 更新存在的规则
                DegradeRule latestRule = latestRulesMap.get(currentRule.getResource());
                if (latestRule != null){
                    iterator.set(latestRule);
                }
            }

            // 添加新规则
            for(String addedRuleId : addedRuleIds){
                globalDegradeRules.add(latestRulesMap.get(addedRuleId));
            }

            // 更新应用流控规则
            routeRules.setDegrades(latestRulesMap);

            // 更新全局流控规则
            DegradeRuleManager.loadRules(globalDegradeRules);
        } finally {
            degradeRulesRefreshLock.unlock();
        }
    }

    /**
     * 卸载应用规则（当该路由不存在后）
     * @param routeId 路由ID
     */
    private void endListenRules(String routeId) {

        RouteAppRules routeAppRules = allAppRules.get(routeId);

        // 取消流控规则监听
        String flowRulesFile = buildFlowRulesFileName(routeId);
        sentinelConfigService.get().removeListener(flowRulesFile, appName, allRuleListeners.get(flowRulesFile));
        allRuleListeners.remove(flowRulesFile);

        // 卸载流控规则
        List<FlowRule> globalFlowRules = FlowRuleManager.getRules();
        globalFlowRules.removeIf(flowRule -> routeAppRules.getFlows().containsKey(flowRule.getResource()));
        FlowRuleManager.loadRules(globalFlowRules);

        // 取消降级规则监听
        String degradeRulesFile = buildDegradeRulesFileName(routeId);
        sentinelConfigService.get().removeListener(degradeRulesFile, appName, allRuleListeners.get(degradeRulesFile));
        allRuleListeners.remove(degradeRulesFile);
        // 卸载降级规则
        List<DegradeRule> globalDegradeRules = DegradeRuleManager.getRules();
        globalDegradeRules.removeIf(degradeRule -> routeAppRules.getDegrades().containsKey(degradeRule.getResource()));
        DegradeRuleManager.loadRules(globalDegradeRules);

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
