package org.rciam.keycloak.comanage_migration.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.rciam.keycloak.comanage_migration.common.Utils;

import java.time.LocalDate;
import java.util.List;

public class UserGroupMembershipExtensionRepresentation {

    private String id;
    private GroupRepresentation group;
    private UserRepresentation user;
    private String status;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Utils.DATE_FORMAT)
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate validFrom;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Utils.DATE_FORMAT)
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate membershipExpiresAt;
    private GroupEnrollmentConfigurationRepresentation groupEnrollmentConfiguration;
    private List<String> groupRoles;

    public UserGroupMembershipExtensionRepresentation() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public GroupRepresentation getGroup() {
        return group;
    }

    public void setGroup(GroupRepresentation group) {
        this.group = group;
    }

    public UserRepresentation getUser() {
        return user;
    }

    public void setUser(UserRepresentation user) {
        this.user = user;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getMembershipExpiresAt() {
        return membershipExpiresAt;
    }

    public void setMembershipExpiresAt(LocalDate membershipExpiresAt) {
        this.membershipExpiresAt = membershipExpiresAt;
    }

    public GroupEnrollmentConfigurationRepresentation getGroupEnrollmentConfiguration() {
        return groupEnrollmentConfiguration;
    }

    public void setGroupEnrollmentConfiguration(GroupEnrollmentConfigurationRepresentation groupEnrollmentConfiguration) {
        this.groupEnrollmentConfiguration = groupEnrollmentConfiguration;
    }

    public List<String> getGroupRoles() {
        return groupRoles;
    }

    public void setGroupRoles(List<String> groupRoles) {
        this.groupRoles = groupRoles;
    }
}
