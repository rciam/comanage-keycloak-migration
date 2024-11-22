package org.rciam.keycloak.comanage_migration.comanage;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.rciam.keycloak.comanage_migration.common.Utils;

import java.time.LocalDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ComanageUserRepresentation {

    public ComanageUserRepresentation(){}

    private String username;
    private String firstname;
    private String lastname;
    private List<String> emails;
    private boolean enabled;
    private String uid;
    private List<String> sshPublicKeys;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Utils.DATE_FORMAT)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime terms_and_conditions;
    private List<FederatedIdentityRepresentation> federatedIdentities;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public List<String> getEmails() {
        return emails;
    }

    public void setEmails(List<String> emails) {
        this.emails = emails;
    }

    public List<String> getSshPublicKeys() {
        return sshPublicKeys;
    }

    public void setSshPublicKeys(List<String> sshPublicKeys) {
        this.sshPublicKeys = sshPublicKeys;
    }

    public List<FederatedIdentityRepresentation> getFederatedIdentities() {
        return federatedIdentities;
    }

    public void setFederatedIdentities(List<FederatedIdentityRepresentation> federatedIdentities) {
        this.federatedIdentities = federatedIdentities;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public LocalDateTime getTerms_and_conditions() {
        return terms_and_conditions;
    }

    public void setTerms_and_conditions(LocalDateTime terms_and_conditions) {
        this.terms_and_conditions = terms_and_conditions;
    }
}
