package ch.idsia.adaptive.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
 * Project: AdapQuest
 * Date:    24.11.2020 13:39
 */
@Configuration
@AutoConfigureAfter(WebConfig.class)
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "ch.idsia.adaptive.backend.persistence.dao")
public class PersistenceConfig {
	private static final Logger logger = LoggerFactory.getLogger(PersistenceConfig.class);

	@Value("${db.dbms:memory}")
	private String dbms;

	@Value("${db.hostname:localhost}")
	private String hostname;

	@Value("${db.port:5432}")
	private String port;

	@Value("${db.schema:adaptive}")
	private String schema;

	@Value("${db.username:ada}")
	private String username;

	@Value("${db.password:ada}")
	private String password;

	private final Environment env;

	@Autowired
	public PersistenceConfig(Environment env) {
		this.env = env;
	}

	private void configurePostgreSQL(DriverManagerDataSource ds) {
		logger.info("Database platform: Postgres");
		ds.setDriverClassName("org.postgresql.Driver");
		ds.setUrl("jdbc:postgresql://" + hostname + ":" + port + "/" + schema);
	}

	private void configureMySQL(DriverManagerDataSource ds) {
		logger.info("Database platform: MySQL");
		ds.setDriverClassName("com.mysql.jdbc.Driver");
		ds.setUrl("jdbc:mysql://" + hostname + ":" + port + "/" + schema);
	}

	private void configureMemory(DriverManagerDataSource ds) {
		logger.info("Database platform: Memory");
		ds.setDriverClassName("org.h2.Driver");
		ds.setUrl("jdbc:h2:mem:" + schema + ";DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;");
	}

	@Bean
	public DataSource dataSource() {
		DriverManagerDataSource ds = new DriverManagerDataSource();
		ds.setUsername(username);
		ds.setPassword(password);

		switch (dbms) {
			case "postgresql":
				configurePostgreSQL(ds);
				break;

			case "mysql":
				configureMySQL(ds);
				break;

			default:
				logger.warn("Invalid DBMS={}, using in memory", dbms);
			case "memory":
				configureMemory(ds);
				break;
		}
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
		properties.put("hibernate.show_sql", env.getProperty("spring.jpa.show-sql", "false"));
		properties.put("hibernate.globally_quoted_identifiers", env.getProperty("spring.jpa.hibernate.globally_quoted_identifiers", "false"));

		switch (dbms) {
			case "postgresql":
				properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQL82Dialect");
				break;
			case "mysql":
				properties.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
				break;
			default:
			case "memory":
				properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
		}

		return properties;
	}

	@Bean
	public PlatformTransactionManager transactionManager() {
		JpaTransactionManager tm = new JpaTransactionManager();
		tm.setEntityManagerFactory(entityManagerFactory().getObject());
		return tm;
	}
}

