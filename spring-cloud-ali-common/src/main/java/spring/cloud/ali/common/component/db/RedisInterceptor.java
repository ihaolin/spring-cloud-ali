package spring.cloud.ali.common.component.db;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.nacos.shaded.com.google.common.base.Strings;
import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.util.pattern.PathPattern;
import spring.cloud.ali.common.component.sentinel.SentinelServiceRules;
import spring.cloud.ali.common.exception.RedisException;
import spring.cloud.ali.common.util.WebUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RedisInterceptor implements MethodInterceptor {

    private static final String RESOURCE_PREFIX = "redis";

    private static final String RESOURCE_SPLITTER = "#";

    private final Map<Class<?>, Object> operationProxies = new ConcurrentHashMap<>();

    @Value("${spring.application.name}")
    private String appName;

    @Autowired
    private SentinelServiceRules sentinelServiceRules;

    @Nullable
    @Override
    public Object invoke(@Nonnull MethodInvocation invocation) throws Throwable {

        String opsForName = invocation.getMethod().getName();

        // 拦截指定操作集
        switch (opsForName) {
            case "opsForValue":
            case "opsForSet":
            case "opsForZSet":
            case "opsForHash":
            case "opsForList":
                return getOperationProxy(Objects.requireNonNull(invocation.proceed()), opsForName);
        }

        return invocation.proceed();
    }

    @SuppressWarnings("unchecked")
    public <T> T getOperationProxy(Object opt, String opsForName) {
        return (T) operationProxies.computeIfAbsent(opt.getClass(), clazz -> createOperationProxy(opt, opsForName));
    }

    @SuppressWarnings("unchecked")
    private <T> T createOperationProxy(T target, String opsForName) {
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setTarget(target);
        String fmtOpsName = opsForName.replace("opsFor", "").toLowerCase();
        proxyFactory.addAdvice(new RedisOperationInterceptor(fmtOpsName));
        return (T) proxyFactory.getProxy();
    }

    class RedisOperationInterceptor implements MethodInterceptor{

        private final String opsForName;

        public RedisOperationInterceptor(String opsForName) {
            this.opsForName = opsForName;
        }

        @Nullable
        @Override
        public Object invoke(@Nonnull MethodInvocation invocation) throws Throwable {

            Object[] args = invocation.getArguments();
            if (args.length == 0 || !(args[0] instanceof String)){
                return invocation.proceed();
            }

            Entry sentinelEntry = null;
            String resource = resolveResource(opsForName, invocation);
            try {

                if(Strings.isNullOrEmpty(resource)){
                    // 未匹配到降级规则
                    return invocation.proceed();
                }

                sentinelEntry = SphU.entry(resource);

                return invocation.proceed();
            } catch (BlockException e){
                log.error("redis operation blocked: resource={}, rule={}", resource, e.getRule());
                throw e;
            } catch (Throwable e){
                log.error("redis operation exception: resource={}, error={}", resource, Throwables.getStackTraceAsString(e));
                RedisException re = new RedisException(e);
                Tracer.traceEntry(re, sentinelEntry);
                throw re;
            } finally {
                if (sentinelEntry != null){
                    sentinelEntry.exit();
                }
            }
        }
    }

    private String resolveResource(String opsForName, MethodInvocation invocation) {

        Map<String, DegradeRule> degradeRuleMap = sentinelServiceRules.getDegradeRules();

        String optName = invocation.getMethod().getName();
        String optKey = (String) invocation.getArguments()[0];

        String resourcePrefix = RESOURCE_PREFIX + RESOURCE_SPLITTER + opsForName + RESOURCE_SPLITTER;
        String optNameKey = optName + RESOURCE_SPLITTER + optKey;
        String resource = resourcePrefix + optNameKey;
        if (degradeRuleMap.containsKey(resource)){
            // 精确匹配到了
            return resource;
        }

        // 尝试模式匹配，如users:123 -> users:{id}
        for (String degradeResource : degradeRuleMap.keySet()){
            if (degradeResource.contains("{")){
                // redis#value#get#users:{userId} -> get#users:{userId}
                String fmtDegradeResource = degradeResource.replace(resourcePrefix, "");
                PathPattern.PathMatchInfo matched = WebUtil.matchPatten(fmtDegradeResource, optNameKey);
                if (matched != null){
                    // 匹配到了
                    return degradeResource;
                }
            }
        }

        return null;
    }
}
