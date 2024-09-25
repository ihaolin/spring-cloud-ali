package spring.cloud.ali.common.component;

import com.alibaba.csp.sentinel.slots.block.AbstractRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.nacos.api.config.ConfigFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.AbstractListener;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import spring.cloud.ali.common.util.JsonUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 基于Nacos存储的Sentinel服务
 * 涉及到Sentinel规则动态变更的场景
 *  1.不同场景feign熔断，api限流，redis熔断等，都需要监听相关规则
 *    一个应用实例保证有一个ConfigService实例
 *  2.Sentinel规则是按类型单例的，刷新需要进行并发控制
 */
@Slf4j
public class SentinelConfigService implements InitializingBean {

    private static final String SPLITTER = "#";

    /**
     * 流控规则刷新锁
     */
    private final Lock flowRulesRefreshLock = new ReentrantLock();

    /**
     * 降级规则刷新锁
     */
    private final Lock degradeRulesRefreshLock = new ReentrantLock();

    /**
     * nacos配置中心地址
     */
    @Value("${spring.cloud.nacos.config.server-addr}")
    private String nacosServer;

    /**
     * nacos中，用于存放sentinel规则的命名空间（id）
     */
    @Value("${spring.cloud.sentinel.nacos.namespace}")
    private String nacosNamespace;

    /**
     * Nacos配置中心客户端
     */
    private ConfigService configService;

    /**
     * Nacos配置监听器（卸载规则时，需要移除监听器）
     *  key：group#dataId
     *  value：监听器对象
     */
    private final Map<String, Listener> groupDataListeners = new HashMap<>();

    /**
     * Nacos配置规则（卸载规则时，需要卸载规则）
     *  key：group#dataId
     *  value：规则列表
     */
    private final Map<String, List<? extends AbstractRule>> groupDataRules = new HashMap<>();

    @Override
    public void afterPropertiesSet() {
        initConfigService();
    }

    private void initConfigService() {

        if (Strings.isNullOrEmpty(nacosNamespace)){
            log.warn("nacosNamespace is empty, won't be initialized");
            return;
        }

        Properties properties = new Properties();
        properties.setProperty("serverAddr", nacosServer);
        properties.setProperty("namespace", nacosNamespace);

        try {
            configService = ConfigFactory.createConfigService(properties);
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 初始化流控规则
     */
    public void initFlowRules(String dataId, String group, RuleListener<FlowRule> listener) throws NacosException {

        // 初始化流控规则
        String latestRules = configService.getConfig(dataId, group, 5000);
        refreshFlowRules(dataId, group, latestRules, listener);

        // 监听流控规则变化
        Listener configListener = new AbstractListener() {
            @Override
            public void receiveConfigInfo(String rules) {
                refreshFlowRules(dataId, group, rules, listener);
            }
        };
        configService.addListener(dataId, group, configListener);
        groupDataListeners.put(group + SPLITTER + dataId, configListener);
    }

    private void refreshFlowRules(String dataId, String group, String latestFlowRules, RuleListener<FlowRule> listener) {
        List<FlowRule> latestRules = Collections.emptyList();
        if (!Strings.isNullOrEmpty(latestFlowRules)){
            latestRules = JsonUtil.toList(latestFlowRules, new TypeReference<List<FlowRule>>() {
            });
        }
        if (listener != null){
            listener.prevRefresh(latestRules);
        }
        List<FlowRule> refreshedRules = doRefreshFlowRules(dataId, group, latestRules);
        if (listener != null){
            listener.postRefresh(refreshedRules);
        }
        groupDataRules.put(group + SPLITTER + dataId, refreshedRules);
    }

    /**
     * 刷新应用的流控规则
     * @param groupDataLatestRules 最新的流控规则
     */
    @SuppressWarnings("unchecked")
    private List<FlowRule> doRefreshFlowRules(String dataId, String group, List<FlowRule> groupDataLatestRules) {
        flowRulesRefreshLock.lock();
        try {
            String groupDataKey = group + SPLITTER + dataId;
            List<FlowRule> groupDataFlowRules = (List<FlowRule>) groupDataRules.get(groupDataKey);
            List<FlowRule> refreshedRules = doRefreshRules(FlowRuleManager.getRules(), groupDataFlowRules, groupDataLatestRules);
            FlowRuleManager.loadRules(refreshedRules);
            return refreshedRules;
        } finally {
            flowRulesRefreshLock.unlock();
        }
    }

    /**
     * 销毁流控规则
     * @param dataId 配置文件ID
     * @param group 配置分组
     */
    public void destroyFlowRules(String dataId, String group){
        flowRulesRefreshLock.lock();
        try {
            FlowRuleManager.loadRules(doDestroyRules(dataId, group, FlowRuleManager.getRules()));
        } finally {
            flowRulesRefreshLock.unlock();
        }
    }

    /**
     * 初始化熔断规则
     */
    public void initDegradeRules(String dataId, String group, RuleListener<DegradeRule> listener) throws NacosException {

        // 初始化流控规则
        String latestRules = configService.getConfig(dataId, group, 5000);
        refreshDegradeRules(dataId, group, latestRules, listener);

        // 监听流控规则变化
        Listener configListener = new AbstractListener() {
            @Override
            public void receiveConfigInfo(String rules) {
                refreshDegradeRules(dataId, group, rules, listener);
            }
        };
        configService.addListener(dataId, group, configListener);
        groupDataListeners.put(group + SPLITTER + dataId, configListener);
    }

    private void refreshDegradeRules(String dataId, String group, String latestDegradeRules, RuleListener<DegradeRule> listener) {
        List<DegradeRule> latestRules = Collections.emptyList();
        if (!Strings.isNullOrEmpty(latestDegradeRules)){
            latestRules = JsonUtil.toList(latestDegradeRules, new TypeReference<List<DegradeRule>>() {
            });
        }
        if (listener != null){
            listener.prevRefresh(latestRules);
        }
        List<DegradeRule> refreshedRules = doRefreshDegradeRules(dataId, group, latestRules);
        if (listener != null){
            listener.postRefresh(refreshedRules);
        }
        groupDataRules.put(group + SPLITTER + dataId, refreshedRules);
    }

    /**
     * 刷新应用的熔断规则
     * @param groupDataLatestRules 最新的熔断规则
     * @return 刷新后的规则
     */
    @SuppressWarnings("unchecked")
    private List<DegradeRule> doRefreshDegradeRules(String dataId, String group, List<DegradeRule> groupDataLatestRules) {
        degradeRulesRefreshLock.lock();
        try {
            String groupDataKey = group + SPLITTER + dataId;
            List<DegradeRule> groupDataDegradeRules = (List<DegradeRule>) groupDataRules.get(groupDataKey);
            List<DegradeRule> refreshedRules = doRefreshRules(DegradeRuleManager.getRules(), groupDataDegradeRules, groupDataLatestRules);
            DegradeRuleManager.loadRules(refreshedRules);
            return refreshedRules;
        } finally {
            degradeRulesRefreshLock.unlock();
        }
    }

    /**
     * 销毁降级规则
     * @param dataId 配置文件ID
     * @param group 配置分组
     */
    public void destroyDegradeRules(String dataId, String group){
        degradeRulesRefreshLock.lock();
        try {
            DegradeRuleManager.loadRules(doDestroyRules(dataId, group, DegradeRuleManager.getRules()));
        } finally {
            degradeRulesRefreshLock.unlock();
        }
    }

    /**
     * 刷新内存中的规则（特定类型，如流控，降级等）
     * @param allRules     内存中所有规则
     * @param groupDataCurrentRules 当前的规则（ group-dataId维度），见groupDataRules
     * @param groupDataLatestRules  最新的规则（ group-dataId维度）
     * @return 刷新后的规则
     */
    private static <T extends AbstractRule> List<T> doRefreshRules(List<T> allRules, List<T> groupDataCurrentRules, List<T> groupDataLatestRules) {

        allRules = allRules == null ? Collections.emptyList() : allRules;
        groupDataCurrentRules = groupDataCurrentRules == null ? Collections.emptyList() : groupDataCurrentRules;
        groupDataLatestRules = groupDataLatestRules == null ? Collections.emptyList() : groupDataLatestRules;

        Map<String, T> currentRulesMap = groupDataCurrentRules.stream().collect(Collectors.toMap(AbstractRule::getResource, r -> r));

        Map<String, T> latestRulesMap = groupDataLatestRules.stream().collect(Collectors.toMap(AbstractRule::getResource, r -> r));

        // 新增的规则
        Set<String> addedRuleIds = new HashSet<>(latestRulesMap.keySet());
        addedRuleIds.removeAll(currentRulesMap.keySet());

        // 删除的规则
        Set<String> removedRuleIds = new HashSet<>(currentRulesMap.keySet());
        removedRuleIds.removeAll(latestRulesMap.keySet());

        // 删除不存在的，替换存在的
        ListIterator<T> iterator = allRules.listIterator();
        while (iterator.hasNext()) {
            AbstractRule currentRule = iterator.next();
            if(removedRuleIds.contains(currentRule.getResource())){
                // 删除无效的规则
                iterator.remove();
                continue;
            }
            // 替换存在的规则
            T latestRule = latestRulesMap.get(currentRule.getResource());
            if (latestRule != null){
                iterator.set(latestRule);
            }
        }

        // 添加新规则
        for(String addedRuleId : addedRuleIds){
            allRules.add(latestRulesMap.get(addedRuleId));
        }

        return allRules;
    }

    /**
     * 销毁内存中的规则
     * @param dataId 配置文件ID
     * @param group 配置分组
     * @param currentRules 当前内存中的规则
     * @return 销毁后的最新内存规则
     * @param <T> AbstractRule
     */
    private <T extends AbstractRule> List<T> doDestroyRules(String dataId, String group, List<T> currentRules){
        String groupDataId = group + SPLITTER + dataId;

        // 取消监听
        Listener listener = groupDataListeners.get(groupDataId);
        if (listener != null){
            configService.removeListener(dataId, group, listener);
        }

        // 卸载规则
        List<? extends AbstractRule> removingRules = groupDataRules.remove(groupDataId);
        if (CollectionUtils.isEmpty(removingRules)){
            return currentRules;
        }

        Map<String, AbstractRule> removingRulesMap =
                removingRules.stream().collect(Collectors.toMap(AbstractRule::getResource, r -> r));

        if (!CollectionUtils.isEmpty(removingRules)){
            if (!CollectionUtils.isEmpty(currentRules)){
                currentRules = currentRules.stream()
                        .filter((r) -> !removingRulesMap.containsKey(r.getResource())).collect(Collectors.toList());
            }
        }

        return currentRules;
    }

    /**
     * 规则刷新监听器
     * @param <T> AbstractRule
     */
    public interface RuleListener<T extends AbstractRule>{

        /**
         * 刷新规则前回调（比如预处理规则resource前缀等）
         * @param refreshing 刷新前的规则
         */
        default void prevRefresh(List<T> refreshing){

        }

        /**
         * 刷新规则后回调（比如保存规则列表，用于路径资源匹配等）
         * @param refreshed 刷新后的规则
         */
        default void postRefresh(List<T> refreshed){

        }
    }
}
