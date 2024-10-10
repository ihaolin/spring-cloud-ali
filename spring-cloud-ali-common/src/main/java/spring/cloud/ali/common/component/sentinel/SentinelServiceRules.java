package spring.cloud.ali.common.component.sentinel;

import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static spring.cloud.ali.common.component.sentinel.SentinelConfigService.SENTINEL_DEGRADE_RULES;
import static spring.cloud.ali.common.component.sentinel.SentinelConfigService.SENTINEL_FLOW_RULES;

/**
 * 微服务Sentinel规则管理
 */
@Slf4j
public class SentinelServiceRules {

    @Value("${spring.application.name}")
    private String appName;

    @Resource
    private SentinelConfigService sentinelConfigService;

    private volatile Map<String, FlowRule> flowRuleMap = Collections.emptyMap();

    private volatile Map<String, DegradeRule> degradeRuleMap = Collections.emptyMap();

    @EventListener
    public void onAppReady(ApplicationReadyEvent event) throws NacosException {



        sentinelConfigService.initFlowRules(SENTINEL_FLOW_RULES, appName, new SentinelConfigService.RuleListener<FlowRule>() {
            @Override
            public void postRefresh(List<FlowRule> refreshed) {
                flowRuleMap = refreshed.stream().collect(Collectors.toMap(FlowRule::getResource, f -> f));
                log.info("{} refreshed flow rules: {}", appName, flowRuleMap);
            }
        });
        sentinelConfigService.initDegradeRules(SENTINEL_DEGRADE_RULES, appName, new SentinelConfigService.RuleListener<DegradeRule>() {
            @Override
            public void postRefresh(List<DegradeRule> refreshed) {
                degradeRuleMap = refreshed.stream().collect(Collectors.toMap(DegradeRule::getResource, f -> f));
                log.info("{} refreshed degrade rules: {}", appName, degradeRuleMap);
            }
        });
    }

    public Map<String, FlowRule> getFlowRules(){
        return flowRuleMap;
    }

    public Map<String, DegradeRule> getDegradeRules(){
        return degradeRuleMap;
    }
}
