package ch.idsia.adaptive.backend.config;

import org.keycloak.adapters.springsecurity.KeycloakConfiguration;
import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

import java.util.Objects;

@KeycloakConfiguration
@AutoConfigureAfter(SecurityConfig.class)
@ConditionalOnProperty(name = "keycloak.enabled", havingValue = "true")
class SecurityKeycloakConfig extends KeycloakWebSecurityConfigurerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(SecurityKeycloakConfig.class);

	@Value("${adapquest.keycloak.role}")
	private String role = "";

	@Value("${adapquest.keycloak.admin}")
	private String admin = "admin";

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) {
		final SimpleAuthorityMapper simpleAuthorityMapper = new SimpleAuthorityMapper();
		simpleAuthorityMapper.setPrefix("ROLE_");
		simpleAuthorityMapper.setConvertToUpperCase(true);

		final KeycloakAuthenticationProvider keycloakAuthenticationProvider = keycloakAuthenticationProvider();
		keycloakAuthenticationProvider.setGrantedAuthoritiesMapper(simpleAuthorityMapper);
		auth.authenticationProvider(keycloakAuthenticationProvider);
	}

	@Bean
	@Override
	protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
		return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		super.configure(http);
		if (Objects.isNull(role) || role.isEmpty()) {
			http.authorizeRequests()
					.antMatchers("/", "/css/**", "/webjars/**", "/img/**").permitAll()
					.and()
					.authorizeRequests()
					.antMatchers("/console*").hasRole(admin)
					.anyRequest()
					.authenticated();
		} else {
			http.authorizeRequests()
					.antMatchers("/", "/css/**", "/webjars/**", "/img/**").permitAll()
					.and()
					.authorizeRequests()
					.antMatchers("/demo**", "/survey**").hasRole(role)
					.antMatchers("/console*").hasRole(admin)
					.anyRequest()
					.authenticated();
		}
	}
}
