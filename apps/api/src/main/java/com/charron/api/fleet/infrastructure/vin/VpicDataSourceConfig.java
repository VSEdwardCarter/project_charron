
package com.charron.api.fleet.infrastructure.vin;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;


@ConditionalOnProperty(
        name = "charron.vpic.enabled",
        havingValue = "true",
        matchIfMissing = true
)

@Configuration
public class VpicDataSourceConfig {

    @Bean(name = "vpicDataSource", destroyMethod = "close")
    public HikariDataSource vpicDataSource(
            @Value("${VPIC_JDBC_URL}") String jdbcUrl,
            @Value("${VPIC_DB_USERNAME}") String username,
            @Value("${VPIC_DB_PASSWORD}") String password
    ) {
        HikariDataSource dataSource = new HikariDataSource();

        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setPoolName("CHARRON-vPIC");
        dataSource.setMaximumPoolSize(3);
        dataSource.setMinimumIdle(0);

        return dataSource;
    }

    @Bean("vpicJdbcTemplate")
    public JdbcTemplate vpicJdbcTemplate(
            @Qualifier("vpicDataSource") HikariDataSource dataSource
    ) {
        return new JdbcTemplate(dataSource);
    }
}
