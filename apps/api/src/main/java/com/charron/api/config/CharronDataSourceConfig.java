
package com.charron.api.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class CharronDataSourceConfig {

    @Bean(name = "dataSource", destroyMethod = "close")
    @Primary
    public HikariDataSource dataSource(
            @Value("${spring.datasource.url}") String jdbcUrl,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password
    ) {
        HikariDataSource dataSource = new HikariDataSource();

        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setPoolName("CHARRON-Operational");

        return dataSource;
    }

    @Bean(name = "jdbcTemplate")
    @Primary
    public JdbcTemplate jdbcTemplate(
            @Qualifier("dataSource") HikariDataSource dataSource
    ) {
        return new JdbcTemplate(dataSource);
    }
}
