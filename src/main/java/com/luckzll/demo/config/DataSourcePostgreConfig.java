package com.luckzll.demo.config;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

/**
 * PostgreSQL 次数据源配置
 */
@Configuration
// 扫描 PostgreSQL Mapper 接口，指定 sqlSessionFactory 为 postgreSqlSessionFactory
@MapperScan(basePackages = "com.luckzll.demo.mapper.postgresql", 
           sqlSessionFactoryRef = "postgreSqlSessionFactory")
public class DataSourcePostgreConfig {

    // 读取 spring.datasource.postgresql 配置，创建数据源
    @Bean(name = "postgreDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.postgresql")
    public DataSource postgreDataSource() {
        return DataSourceBuilder.create().build();
    }

    // PostgreSQL 事务管理器
    @Bean(name = "postgreTransactionManager")
    public DataSourceTransactionManager postgreTransactionManager() {
        return new DataSourceTransactionManager(postgreDataSource());
    }

    // PostgreSQL SqlSessionFactory
    @Bean(name = "postgreSqlSessionFactory")
    public SqlSessionFactory postgreSqlSessionFactory(@Qualifier("postgreDataSource") DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        // 配置 MyBatis 规则
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        factoryBean.setConfiguration(configuration);
        factoryBean.setPlugins(new MybatisPlusInterceptor());
        // 指定 PostgreSQL Mapper XML 路径
        factoryBean.setMapperLocations(new org.springframework.core.io.support.PathMatchingResourcePatternResolver()
                .getResources("classpath:mapper/postgresql/**/*.xml"));
        return factoryBean.getObject();
    }

    // PostgreSQL SqlSessionTemplate
    @Bean(name = "postgreSqlSessionTemplate")
    public SqlSessionTemplate postgreSqlSessionTemplate(@Qualifier("postgreSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}