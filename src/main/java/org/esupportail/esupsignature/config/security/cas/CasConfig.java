package org.esupportail.esupsignature.config.security.cas;

import org.esupportail.esupsignature.service.security.cas.CasSecurityServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.support.LdapContextSource;

@Configuration
@ConditionalOnProperty(prefix = "security.cas", name = "service")
@EnableConfigurationProperties(CasProperties.class)
public class CasConfig {

	private static final Logger logger = LoggerFactory.getLogger(CasConfig.class);

	private LdapContextSource ldapContextSource;

	public CasConfig(@Autowired(required = false) LdapContextSource ldapContextSource) {
		this.ldapContextSource = ldapContextSource;
	}

	@Bean
	public CasSecurityServiceImpl CasSecurityServiceImpl() {
		if(ldapContextSource.getUserDn() != null) {
			return new CasSecurityServiceImpl();
		} else {
			logger.error("cas config found without needed ldap config, cas security will be disabled");
			return null;
		}
	}

}
