package spring.cloud.ali.common.interceptor;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.nacos.api.config.ConfigFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.AbstractListener;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.shaded.com.google.common.base.Strings;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import spring.cloud.ali.common.util.JsonUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static spring.cloud.ali.common.enums.HttpRespStatus.HTTP_REQUEST_TOO_MANY;

/**
 * 基于Sentinel实现的微服务API限流拦截器
 * TODO 增加默认限流策略
 */
@Slf4j
@Component
public class ServiceSentinelInterceptor implements HandlerInterceptor {

    /**
     * 请求路径解析器
     */
    private static final PathPatternParser PATH_PARSER = new PathPatternParser();

    /**
     * 流控规则文件
     */
    private static final String FLOW_RULES = "flow-rules.json";

    /**
     * 资源
     */
    private static final String RESOURCE_SPLITTER = "#";

    /**
     * 流控规则更新锁
     */
    private final Lock flowRulesRefreshLock = new ReentrantLock();

    /**
     * 应用名称
     */
    @Value("${spring.application.name}")
    private String appName;

    /**
     * nacos配置中心地址
     */
    @Value("${spring.cloud.nacos.config.server-addr}")
    private String nacosServer;

    /**
     * nacos中，存放sentinel规则的命名空间
     */
    @Value("${spring.cloud.sentinel.interceptor.namespace}")
    private String sentinelNamespace;

    /**
     * 用于监听应用规则文件
     */
    private ConfigService configService;

    /**
     * 流控规则映射<resource, 流控规则>
     */
    private volatile Map<String, FlowRule> flowRuleMap;

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        try {
            initConfigService();
            initAppRules();
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    private void initConfigService() {
        Properties properties = new Properties();
        properties.setProperty("serverAddr", nacosServer);
        properties.setProperty("namespace", sentinelNamespace);

        try {
            configService = ConfigFactory.createConfigService(properties);
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    private void initAppRules() throws NacosException {

        // 初始化流控规则
        String flowRules = configService.getConfig(FLOW_RULES, appName, 5000);
        doRefreshAppFlowRules(flowRules);

        // 监听流控规则变化
        configService.addListener(FLOW_RULES, appName, new AbstractListener() {
            @Override
            public void receiveConfigInfo(String rules) {
                doRefreshAppFlowRules(rules);
            }
        });
    }

    /**
     * 更新应用的流控规则
     * @param rules 最新的规则（json列表）
     */
    private void doRefreshAppFlowRules(String rules) {
        flowRulesRefreshLock.lock();
        try {

            if (Strings.isNullOrEmpty(rules)){
                rules = "[]";
            }

            // 应用最新的限流规则
            List<FlowRule> latestRules =
                    JsonUtil.toList(rules, new TypeReference<List<FlowRule>>() {});
            Map<String, FlowRule> latestRulesMap = latestRules.stream().collect(Collectors.toMap(FlowRule::getResource, f -> f));

            // 应用当前的限流规则
            List<FlowRule> currentRules = FlowRuleManager.getRules();
            Map<String, FlowRule> currentRulesMap = currentRules.stream().collect(Collectors.toMap(FlowRule::getResource, f -> f));

            // 新增的规则
            Set<String> addedRuleIds = new HashSet<>(latestRulesMap.keySet());
            addedRuleIds.removeAll(currentRulesMap.keySet());

            // 删除的规则
            Set<String> removedRuleIds = new HashSet<>(currentRulesMap.keySet());
            removedRuleIds.removeAll(latestRulesMap.keySet());

            // 删除不存在的，替换存在的
            ListIterator<FlowRule> iterator = currentRules.listIterator();
            while (iterator.hasNext()) {
                FlowRule currentRule = iterator.next();
                if(removedRuleIds.contains(currentRule.getResource())){
                    // 删除无效的规则
                    iterator.remove();
                    continue;
                }
                // 替换存在的规则
                FlowRule latestRule = latestRulesMap.get(currentRule.getResource());
                if (latestRule != null){
                    iterator.set(latestRule);
                }
            }

            // 添加新规则
            for(String addedRuleId : addedRuleIds){
                currentRules.add(latestRulesMap.get(addedRuleId));
            }

            // 更新流控规则
            FlowRuleManager.loadRules(currentRules);
            flowRuleMap = currentRules.stream().collect(Collectors.toMap(FlowRule::getResource, f -> f));

        } finally {
            flowRulesRefreshLock.unlock();
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
                        PATH_PARSER.parse(flowResource.replace(resourcePrefix, ""))
                                .matchAndExtract(PathContainer.parsePath(request.getRequestURI()));
                if (matched != null){
                    // 匹配到了
                    return flowResource;
                }
            }
        }

        return null;
    }
}
