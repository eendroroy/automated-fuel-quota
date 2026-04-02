package io.github.eendroroy.fuelquota;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableTransactionManagement
@EnableConfigurationProperties
public class AutomatedFuelQuotaApplication {
    public static void main(String[] args) {
        SpringApplication.run(AutomatedFuelQuotaApplication.class, args);
    }
}
