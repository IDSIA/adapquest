package ch.idsia.adaptive.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    24.11.2020 13:39
 */
@Configuration
@AutoConfigureAfter(WebConfig.class)
@PropertySource("classpath:persistence.properties")
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "ch.idsia.adaptive.backend.persistence.dao")
public class PersistenceConfig {

	private final Environment env;

	@Autowired
	public PersistenceConfig(Environment env) {
		this.env = env;
	}

	@Bean
	public DataSource dataSource() {
		DriverManagerDataSource ds = new DriverManagerDataSource();
		ds.setDriverClassName(env.getProperty("spring.datasource.driverClassName", ""));
		ds.setUrl(env.getProperty("spring.datasource.url", ""));
		ds.setUsername(env.getProperty("spring.datasource.username", ""));
		ds.setPassword(env.getProperty("spring.datasource.password", ""));
		return ds;
	}

	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
		LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
		em.setDataSource(dataSource());
		em.setPackagesToScan("ch.idsia.adaptive.backend.persistence.model");

		JpaVendorAdapter va = new HibernateJpaVendorAdapter();
		em.setJpaVendorAdapter(va);
		em.setJpaProperties(additionalProperties());

		return em;
	}

	private Properties additionalProperties() {
		Properties properties = new Properties();
		properties.put("hibernate.hbm2ddl.auto", env.getProperty("spring.jpa.hibernate.ddl-auto", ""));
		properties.put("hibernate.dialect", env.getProperty("spring.jpa.database-platform", ""));
		properties.put("hibernate.show_sql", env.getProperty("spring.jpa.show-sql", "false"));
		properties.put("hibernate.globally_quoted_identifiers", env.getProperty("spring.jpa.hibernate.globally_quoted_identifiers", "false"));
		return properties;
	}

	@Bean
	public PlatformTransactionManager transactionManager() {
		JpaTransactionManager tm = new JpaTransactionManager();
		tm.setEntityManagerFactory(entityManagerFactory().getObject());
		return tm;
	}
}

