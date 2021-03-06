package org.esupportail.esupsignature.service.extvalue.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.ldap.OrganizationalUnitLdap;
import org.esupportail.esupsignature.ldap.PersonLdap;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.extvalue.ExtValue;
import org.esupportail.esupsignature.service.ldap.LdapPersonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class LdapExtValue implements ExtValue {

	private static final Logger logger = LoggerFactory.getLogger(LdapExtValue.class);

	@Resource
	private UserService userService;

	private LdapPersonService ldapPersonService;

	public LdapExtValue(@Autowired(required = false) LdapPersonService ldapPersonService) {
		this.ldapPersonService = ldapPersonService;
	}

	@Override
	public String getValueByName(String name, User user) {
		Object value = initValues(user).get(name);
		if(value != null){
			if (name.equals("schacDateOfBirth")) {
				String schacDateOfBirth = value.toString();
				if(!schacDateOfBirth.isEmpty()) {
					DateFormat originalFormat = new SimpleDateFormat("yyyyMMdd");
					DateFormat targetFormat = new SimpleDateFormat("dd/MM/yyyy");
					try {
						Date date = originalFormat.parse(schacDateOfBirth);
						return targetFormat.format(date);
					} catch (ParseException e) {
						logger.warn("unable to parse date " + name);
					}
				}
			}
			return value.toString();
		} else {
			return "";
		}
	}

	@Override
	public String getName() {
		return "ldap";
	}

	@Override
	public Map<String, Object> initValues(User user) {
		Map<String, Object> values = new HashMap<>();
		if(user != null) {
			PersonLdap personLdap = userService.findPersonLdapByUser(user);
			if (personLdap != null && values.size() == 0) {
				ObjectMapper oMapper = new ObjectMapper();
				values.putAll(oMapper.convertValue(personLdap, Map.class));
				if(values.containsKey("postalAddress") && values.get("postalAddress") != null) {
					String postalAddress = values.get("postalAddress").toString();
					if (postalAddress != null) {
						values.put("postalAddress", postalAddress.replaceAll("\\$", " \n"));
					}
				}
				if(personLdap.getSupannEntiteAffectationPrincipale() != null && ldapPersonService != null) {
					OrganizationalUnitLdap organizationalUnitLdap = ldapPersonService.getOrganizationalUnitLdap(personLdap.getSupannEntiteAffectationPrincipale());
					if (organizationalUnitLdap != null) {
						values.put("organizationalUnit-postalAddress", organizationalUnitLdap.getPostalAddress());
						values.put("organizationalUnit-description", organizationalUnitLdap.getDescription());
					}
				}
			}
		}
		return values;
	}
	
}
