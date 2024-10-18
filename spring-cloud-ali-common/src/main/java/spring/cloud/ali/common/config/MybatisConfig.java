package spring.cloud.ali.common.config;


import brave.Tracer;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.context.annotation.Bean;
import spring.cloud.ali.common.component.db.TracingInterceptor;

import javax.sql.DataSource;


public class MybatisConfig {

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource, Tracer tracer) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        sessionFactory.addPlugins(new TracingInterceptor(tracer));
        return sessionFactory.getObject();
    }
}
