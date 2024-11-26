package org.rciam.keycloak.comanage_migration.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import org.rciam.keycloak.comanage_migration.common.Utils;
import java.time.LocalDate;
import java.util.List;

public class GroupEnrollmentConfigurationRepresentation {

    private String id;
    private String name;
    private Boolean active;
    private Boolean requireApproval;
    @JsonFormat(shape=JsonFormat.Shape.STRING, pattern= Utils.DATE_FORMAT)
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate validFrom;
    private Long membershipExpirationDays;
    private GroupAupRepresentation aup;
    private Boolean visibleToNotMembers;
    private Boolean multiselectRole;
    private List<String> groupRoles;

    private Boolean commentsNeeded;

    private String commentsLabel;

    private String commentsDescription;

    public GroupEnrollmentConfigurationRepresentation(String id){
        this.id = id;
    }

    public GroupEnrollmentConfigurationRepresentation(){  }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean isActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getRequireApproval() {
        return requireApproval;
    }

    public void setRequireApproval(Boolean requireApproval) {
        this.requireApproval = requireApproval;
    }

    public Long getMembershipExpirationDays() {
        return membershipExpirationDays;
    }

    public void setMembershipExpirationDays(Long membershipExpirationDays) {
        this.membershipExpirationDays = membershipExpirationDays;
    }

    public GroupAupRepresentation getAup() {
        return aup;
    }

    public void setAup(GroupAupRepresentation aup) {
        this.aup = aup;
    }

    public Boolean isVisibleToNotMembers() {
        return visibleToNotMembers;
    }

    public void setVisibleToNotMembers(Boolean visibleToNotMembers) {
        this.visibleToNotMembers = visibleToNotMembers;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public Boolean getMultiselectRole() {
        return multiselectRole;
    }

    public void setMultiselectRole(Boolean multiselectRole) {
        this.multiselectRole = multiselectRole;
    }

    public List<String> getGroupRoles() {
        return groupRoles;
    }

    public void setGroupRoles(List<String> groupRoles) {
        this.groupRoles = groupRoles;
    }

    public Boolean getCommentsNeeded() {
        return commentsNeeded;
    }

    public void setCommentsNeeded(Boolean commentsNeeded) {
        this.commentsNeeded = commentsNeeded;
    }

    public String getCommentsLabel() {
        return commentsLabel;
    }

    public void setCommentsLabel(String commentsLabel) {
        this.commentsLabel = commentsLabel;
    }

    public String getCommentsDescription() {
        return commentsDescription;
    }

    public void setCommentsDescription(String commentsDescription) {
        this.commentsDescription = commentsDescription;
    }

}
