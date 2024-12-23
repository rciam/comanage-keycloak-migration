package org.rciam.keycloak.comanage_migration.comanage;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.rciam.keycloak.comanage_migration.common.Utils;
import java.time.LocalDateTime;

public class ComanageUserGroupMembership {

    public ComanageUserGroupMembership(){}

    private String username;
    private String groupName;
    private String status;
    private String title;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Utils.DATE_TIME_FORMAT)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime validFrom;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Utils.DATE_TIME_FORMAT)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime membershipExpiresAt;
    private String groupRole;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDateTime getMembershipExpiresAt() {
        return membershipExpiresAt;
    }

    public void setMembershipExpiresAt(LocalDateTime membershipExpiresAt) {
        this.membershipExpiresAt = membershipExpiresAt;
    }

    public String getGroupRole() {
        return groupRole;
    }

    public void setGroupRole (String groupRoles) {
        this.groupRole = groupRoles;
    }
}
