package spring.cloud.ali.common.component.db;

import brave.Span;
import brave.Tracer;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.sql.Connection;

@Intercepts({
    @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class TracingInterceptor implements Interceptor {

    private final DefaultReflectorFactory reflectorFactory = new DefaultReflectorFactory();

    private final Tracer tracer;

    public TracingInterceptor(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        Span mybatisSpan = tracer.nextSpan().start().name("mybatis").remoteServiceName("mysql");
        if (mybatisSpan == null){
            return invocation.proceed();
        }

        try {
            mybatisSpan.annotate("Prepare statement start");
            StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
            MetaObject metaObject = MetaObject.forObject(statementHandler,
                    SystemMetaObject.DEFAULT_OBJECT_FACTORY, SystemMetaObject.DEFAULT_OBJECT_WRAPPER_FACTORY, reflectorFactory);
            MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
            mybatisSpan.tag("id", mappedStatement.getId());
            mybatisSpan.tag("sql", statementHandler.getBoundSql().getSql());
            mybatisSpan.annotate("Prepare statement end");

            mybatisSpan.annotate("Execute statement start");
            Object res = invocation.proceed();
            mybatisSpan.annotate("Execute statement end");
            return res;
        } finally {
            mybatisSpan.finish();
        }
    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof StatementHandler) {
            return Plugin.wrap(target, this);
        } else {
            return target;
        }
    }
}
