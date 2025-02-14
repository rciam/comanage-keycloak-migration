package org.rciam.keycloak.comanage_migration.common;

import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.rciam.keycloak.comanage_migration.comanage.ComanageUserGroupMembership;
import org.rciam.keycloak.comanage_migration.comanage.ComanageUserRepresentation;
import org.rciam.keycloak.comanage_migration.config.KeycloakConfig;
import org.rciam.keycloak.comanage_migration.dtos.GroupEnrollmentConfigurationRepresentation;
import org.rciam.keycloak.comanage_migration.dtos.UserGroupMembershipExtensionRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class ConvertFromComanageToKeycloak {

    private static final Logger logger = LoggerFactory.getLogger(ConvertFromComanageToKeycloak.class);

    private static final String EGI_SSO = "egi-sso";
    private static final String IGTF_DEMO = "igtf-proxy-demo";
    private static final Map<String, String> aliasMap = Map.of("bitbucket", "bitbucket", "facebook", "facebook",
            "github", "github", "google", "google",
            "linkedin", "linkedin-openid-connect", "orcid", "orcid");

    private final KeycloakConfig keycloakConfig;

    @Autowired
    public ConvertFromComanageToKeycloak(KeycloakConfig keycloakConfig) {
        this.keycloakConfig = keycloakConfig;
    }

    public void convertUser(ComanageUserRepresentation comanageUser, UserRepresentation user) {
        user.setUsername(comanageUser.getUsername());
        user.setEmail(comanageUser.getEmails().get(0));
        user.setFirstName(comanageUser.getFirstname());
        user.setLastName(comanageUser.getLastname());
        user.setEnabled(comanageUser.isEnabled());
        user.setEmailVerified(true);
        if (user.getAttributes() == null) {
            user.setAttributes(new HashMap<>());
        }
        user.getAttributes().put("uid", Stream.of(comanageUser.getUid()).toList());
        user.getAttributes().put("terms_and_conditions", Stream.of(String.valueOf(comanageUser.getTerms_and_conditions().toInstant(ZoneOffset.UTC).toEpochMilli()/ 1000)).toList());

        if (comanageUser.getSshPublicKeys() != null && comanageUser.getSshPublicKeys().isEmpty())
            user.getAttributes().put("sshKeys", comanageUser.getSshPublicKeys());

        if (user.getFederatedIdentities() == null) {
            user.setFederatedIdentities(new ArrayList<>());
        }

        Map<String, FederatedIdentityRepresentation> uniqueIdentities = new HashMap<>();
        List<String> existingIdentities = user.getFederatedIdentities().stream()
                .map(FederatedIdentityRepresentation::getIdentityProvider).toList();

        comanageUser.getFederatedIdentities().forEach(x -> {
            String keycloakAlias = convertIdPAlias(x.getIdentityProvider());
            if (keycloakAlias != null && !existingIdentities.contains(keycloakAlias)) {
                x.setIdentityProvider(keycloakAlias);
                if (uniqueIdentities.containsKey(x.getIdentityProvider())) {
                    logger.warn("{} has duplicate connection with Identity Provider: {}. Only one will be kept.", comanageUser.getUsername(), x.getIdentityProvider());
                } else {
                    uniqueIdentities.put(x.getIdentityProvider(), x);
                }
            } else if (keycloakAlias == null) {
                logger.warn("User with username {} : federated identity with comanage {} identifier does not exist in Keycloak.",
                        comanageUser.getUsername(), x.getIdentityProvider());
            }
        });

        user.getFederatedIdentities().addAll(uniqueIdentities.values());
    }

    public UserGroupMembershipExtensionRepresentation convertMember(ComanageUserGroupMembership comanageMember, boolean toplevel, List<String> newRoles, List<String> existingRoles) {
        UserGroupMembershipExtensionRepresentation member = new UserGroupMembershipExtensionRepresentation();
        UserRepresentation user = new UserRepresentation();
        user.setUsername(comanageMember.getUsername());
        member.setUser(user);
        member.setValidFrom(comanageMember.getValidFrom().toLocalDate());
        member.setMembershipExpiresAt(comanageMember.getMembershipExpiresAt() == null ? null : comanageMember.getMembershipExpiresAt().toLocalDate());
        List<String> groupRoles = new ArrayList<>();
        groupRoles.add(comanageMember.getGroupRole());
        if (!"member".equals(comanageMember.getGroupRole())) {
            groupRoles.add("member");
            if (!existingRoles.contains(comanageMember.getGroupRole()))
                newRoles.add(comanageMember.getGroupRole());
        }
        if (!"".equals(comanageMember.getTitle())) {
            groupRoles.add(comanageMember.getTitle().toLowerCase());
            if (!existingRoles.contains(comanageMember.getTitle().toLowerCase()))
                newRoles.add(comanageMember.getTitle().toLowerCase());
        }
        if (toplevel) {
            groupRoles.add(Utils.DEFAULT_TOPLEVEL_ROLE);
        }
        member.setGroupRoles(groupRoles);

        return member;

    }

    private String convertIdPAlias(String comanageAlias) {
        String egiCheckInUrl = keycloakConfig.getEgiCheckInUrl();

        if (comanageAlias.startsWith(egiCheckInUrl)) {
            String x = comanageAlias.replace(egiCheckInUrl, "").replace("/saml2/idp/metadata.php", "");
            return aliasMap.get(x);
        } else if ("https://www.egi.eu/idp/shibboleth".equals(comanageAlias)) {
            return EGI_SSO;
        } else if ("https://edugain-proxy-pilot.igtf.net/simplesaml/saml2/idp/metadata.php".equals(comanageAlias)) {
            return IGTF_DEMO;
        } else {
           return URLEncoder.encode(getBase64(comanageAlias), StandardCharsets.UTF_8);
        }
    }

    private String getBase64(String str) {
        return URLEncoder.encode(Base64.getEncoder().withoutPadding().encodeToString(str.getBytes()), StandardCharsets.UTF_8);
    }
}
