package ch.idsia.adaptive.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    24.11.2020 13:39
 */
@Configuration
@AutoConfigureAfter(WebConfig.class)
@PropertySource({"classpath:persistence.properties", "classpath:hibernate.properties"})
@EnableJpaRepositories(
		basePackages = "ch.idsia.adaptive.backend.persistence.dao",
		entityManagerFactoryRef = "networkEntityManager",
		transactionManagerRef = "networkTransactionManager"
)
public class PersistenceConfig {

	@Value("${jdbc.driverClassName}")
	private String driverClassName;
	@Value("${jdbc.url}")
	private String userUrl;
	@Value("${jdbc.user}")
	private String user;
	@Value("${jdbc.pass}")
	private String pass;

	@Value("${hibernate.hbm2ddl.auto}")
	private String hbm2dll;
	@Value("${hibernate.dialect}")
	private String dialect;

	@Primary
	@Bean
	public DataSource dataSource() {
		DriverManagerDataSource ds = new DriverManagerDataSource();
		ds.setDriverClassName(driverClassName);
		ds.setUrl(userUrl);
		ds.setUsername(user);
		ds.setPassword(pass);

		return ds;
	}

	@Primary
	@Bean
	public LocalContainerEntityManagerFactoryBean networkEntityManager() {
		LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
		em.setDataSource(dataSource());
		em.setPackagesToScan("ch.idsia.adaptive.backend.persistence.model");

		HibernateJpaVendorAdapter va = new HibernateJpaVendorAdapter();
		em.setJpaVendorAdapter(va);

		Map<String, Object> properties = new HashMap<>();
		properties.put("hibernate.hbm2dll.auto", hbm2dll);
		properties.put("hibernate.dialect", dialect);
		em.setJpaPropertyMap(properties);

		return em;
	}

	@Primary
	@Bean
	public PlatformTransactionManager networkTransactionManager() {
		JpaTransactionManager tm = new JpaTransactionManager();
		tm.setEntityManagerFactory(networkEntityManager().getObject());
		return tm;
	}
}

