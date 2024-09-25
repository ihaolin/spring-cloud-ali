package spring.cloud.ali.common.config;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import spring.cloud.ali.common.interceptor.RedisInterceptor;

public class RedisConfig {

    @SuppressWarnings("unchecked")
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {

        // 创建 RedisTemplate 实例
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 配置序列化方式
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        // 使用代理工厂添加拦截器
        ProxyFactory factory = new ProxyFactory();
        factory.setTarget(template);
        factory.addAdvice(new RedisInterceptor());

        // 返回代理后的 RedisTemplate
        return (RedisTemplate<String, Object>) factory.getProxy();
    }
}
