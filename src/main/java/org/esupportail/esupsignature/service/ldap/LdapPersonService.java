package org.esupportail.esupsignature.service.ldap;

import org.esupportail.esupsignature.ldap.OrganizationalUnitLdap;
import org.esupportail.esupsignature.ldap.OrganizationalUnitLdapRepository;
import org.esupportail.esupsignature.ldap.PersonLdap;
import org.esupportail.esupsignature.ldap.PersonLdapRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(prefix = "spring.ldap", name = "base")
public class LdapPersonService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Resource
    private LdapTemplate ldapTemplate;

    @Resource
    private PersonLdapRepository personLdapRepository;

    @Resource
    private OrganizationalUnitLdapRepository organizationalUnitLdapRepository;

    private Map<String, LdapTemplate> ldapTemplates;

    @Autowired
    public LdapPersonService(Map<String, LdapTemplate> ldapTemplates) {
        this.ldapTemplates = ldapTemplates;
    }

    public List<PersonLdap> search(String searchString, String ldapTemplateName) {
        LdapTemplate ldapTemplateSelected = ldapTemplate;
        if (ldapTemplateName != null && !ldapTemplateName.isEmpty() && ldapTemplates.containsKey(ldapTemplateName)) {
            ldapTemplateSelected = ldapTemplates.get(ldapTemplateName);
        }
        if (ldapTemplateSelected != null) {
            List<PersonLdap> results = personLdapRepository.findByDisplayNameStartingWithIgnoreCaseOrCnStartingWithIgnoreCaseOrDisplayNameStartingWithIgnoreCaseOrUidOrMailStartingWith(searchString, searchString, searchString, searchString);
            List<PersonLdap> filteredPersons = new ArrayList<>();
            for(PersonLdap personLdap : results) {
                if(personLdap.getEduPersonAffiliation().contains("member") || personLdap.getEduPersonAffiliation().contains("staff")) {
                    filteredPersons.add(personLdap);
                }
            }
            return filteredPersons;
        } else {
            log.debug("No ldapTemplate found -> LdapPersonService.searchByCommonName result is empty");
        }
        return new ArrayList<>();
    }


    public OrganizationalUnitLdap getOrganizationalUnitLdap(String supannCodeEntite) {
        List<OrganizationalUnitLdap> organizationalUnitLdap = organizationalUnitLdapRepository.findBySupannCodeEntite(supannCodeEntite);
        if(organizationalUnitLdap.size() > 0) {
            return organizationalUnitLdap.get(0);
        }
        return null;
    }

}