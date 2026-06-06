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
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

/**
 * MySQL 主数据源配置
 */
@Configuration
// 扫描 MySQL Mapper 接口，指定 sqlSessionFactory 为 mysqlSqlSessionFactory
@MapperScan(basePackages = "com.luckzll.demo.mapper.mysql", 
           sqlSessionFactoryRef = "mysqlSqlSessionFactory")
public class DataSourceMysqlConfig {

    // 读取 spring.datasource.mysql 配置，创建数据源
    @Bean(name = "mysqlDataSource")
    @Primary  // 标记为主数据源（必须）
    @ConfigurationProperties(prefix = "spring.datasource.mysql")
    public DataSource mysqlDataSource() {
        return DataSourceBuilder.create().build();
    }

    // MySQL 事务管理器
    @Bean(name = "mysqlTransactionManager")
    @Primary
    public DataSourceTransactionManager mysqlTransactionManager() {
        return new DataSourceTransactionManager(mysqlDataSource());
    }

    // MySQL SqlSessionFactory
    @Bean(name = "mysqlSqlSessionFactory")
    @Primary
    public SqlSessionFactory mysqlSqlSessionFactory(@Qualifier("mysqlDataSource") DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        // 配置 MyBatis 规则（驼峰命名等）
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        factoryBean.setConfiguration(configuration);
        // 加入 MyBatis-Plus 插件（分页等，没有可以省略）
        factoryBean.setPlugins(new MybatisPlusInterceptor());
        // 指定 MySQL Mapper XML 路径
        factoryBean.setMapperLocations(new org.springframework.core.io.support.PathMatchingResourcePatternResolver()
                .getResources("classpath:mapper/mysql/**/*.xml"));
        return factoryBean.getObject();
    }

    // MySQL SqlSessionTemplate
    @Bean(name = "mysqlSqlSessionTemplate")
    @Primary
    public SqlSessionTemplate mysqlSqlSessionTemplate(@Qualifier("mysqlSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}