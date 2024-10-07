package spring.cloud.ali.common.config;


import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Tracer;
import spring.cloud.ali.common.interceptor.MyBatisTraceInterceptor;

import java.util.List;


public class MybatisConfig {

    @Autowired
    public MybatisConfig(List<SqlSessionFactory> factories, Tracer tracer){
        for (SqlSessionFactory factory : factories){
            factory.getConfiguration().addInterceptor(new MyBatisTraceInterceptor(tracer));
        }
    }
}
