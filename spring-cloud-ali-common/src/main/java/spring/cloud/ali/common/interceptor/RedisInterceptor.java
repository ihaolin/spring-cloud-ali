package spring.cloud.ali.common.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unchecked")
@Slf4j
public class RedisInterceptor implements MethodInterceptor {

    private final Map<Class<?>, Object> operationProxies = new ConcurrentHashMap<>();

    @Nullable
    @Override
    public Object invoke(@Nonnull MethodInvocation invocation) throws Throwable {

        String operationFor = invocation.getMethod().getName();

        // 拦截指定操作集
        switch (operationFor) {
            case "opsForValue":
            case "opsForSet":
            case "opsForHash":
            case "opsForList":
                return getOperationProxy(Objects.requireNonNull(invocation.proceed()));
        }

        return invocation.proceed();
    }

    public <T> T getOperationProxy(Object opt) {
        return (T) operationProxies.computeIfAbsent(opt.getClass(), clazz -> createOperationProxy(opt));
    }

    private <T> T createOperationProxy(T target) {
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setTarget(target);
        proxyFactory.addAdvice(new RedisOperationInterceptor());
        return (T) proxyFactory.getProxy();
    }

    static class RedisOperationInterceptor implements MethodInterceptor{

        @Nullable
        @Override
        public Object invoke(@Nonnull MethodInvocation invocation) throws Throwable {
            String operationName = invocation.getMethod().getName();
            Object[] args = invocation.getArguments();

            if (args.length > 0 && args[0] instanceof String) {
                String operationKey = (String) args[0];
                log.info("redis operation: {} {}", operationName, operationKey);
            }

            return invocation.proceed();
        }
    }
}
