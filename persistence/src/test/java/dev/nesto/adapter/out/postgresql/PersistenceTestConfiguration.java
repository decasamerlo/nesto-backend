package dev.nesto.adapter.out.postgresql;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackageClasses = NodeEntity.class)
@EnableJpaRepositories(basePackageClasses = SpringDataNodeRepository.class)
class PersistenceTestConfiguration {}
