package spring.cloud.ali.common.config;


import brave.Tracer;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import spring.cloud.ali.common.component.db.TracingInterceptor;

import java.util.List;


public class MybatisConfig {

    @Autowired
    public MybatisConfig(List<SqlSessionFactory> factories, Tracer tracer){
        for (SqlSessionFactory factory : factories){
            factory.getConfiguration().addInterceptor(new TracingInterceptor(tracer));
        }
    }
}
